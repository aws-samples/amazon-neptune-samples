from enum_aws_regions import AwsRegions
from urllib.request import urlopen
import json
from pprint import pprint
from timeit import default_timer as timer
import datetime

from gremlin_python.structure.graph import Graph
from gremlin_python.structure.graph import Vertex
from gremlin_python.process.graph_traversal import GraphTraversalSource
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import Cardinality



ondemand_offerTermCode = 'JRTCKXETXF'
upfront_fee_rate_code = '2TG2D8R56U'
recurring_fee_rate_code = '6YS6EN2CT7'


def create_edge_price_product(price_vertex_id, product_vertex_id, graph_traversal: GraphTraversalSource):
    number_of_edges = len(graph_traversal.V(price_vertex_id).outE().as_('e') \
                          .inV().hasId(product_vertex_id).select('e').toList())
    if number_of_edges == 0:
        print("Creating Edge between Price-Vertex and Product-Vertex:", price_vertex_id, "--FOR_PRODUCT-->", product_vertex_id)
        graph_traversal.V(price_vertex_id).addE('FOR_PRODUCT').to(Vertex(product_vertex_id)).next()


def create_edge_region_price(aws_region_code, price_vertex_id, graph_traversal: GraphTraversalSource):
    number_of_edges = len(graph_traversal.V().hasLabel('aws_region').has('code', aws_region_code).outE().as_('e') \
                        .inV().hasId(price_vertex_id).select('e').toList())

    if number_of_edges == 0:
        print("Creating Edge between AWS-Region and Price:", aws_region_code, "--CHARGES-->", price_vertex_id)
        graph_traversal.V().hasLabel('aws_region').has('code', aws_region_code) \
            .addE('CHARGES').to(Vertex(price_vertex_id)).next()


def create_price_vertex(price_attr_dict: dict, price_label, graph_traversal: GraphTraversalSource):
    traversal = graph_traversal.V().hasLabel(price_label)
    for attr_key in price_attr_dict.keys():
        traversal = traversal.has(attr_key, price_attr_dict[attr_key])
    try:
        vertex = traversal.next()
        print('Price Vertex Exists')
        return vertex
    except StopIteration:
        print('Adding price vertex')
        vertex = graph_traversal.addV(price_label).next()
        for key in price_attr_dict.keys():
            graph_traversal.V(vertex).property(Cardinality.single, key, price_attr_dict[key]).next()
        return vertex


def get_product_reserved_pricing(product_sku, reserved_pricing_dict: dict):
    reserved_price_list = []
    print("Fetching Reserved Prices for product:", product_sku)
    try:
        product_term_offers_dict = reserved_pricing_dict[product_sku]
        for term_offer in product_term_offers_dict.keys():
            reserved_price = {'price_type': 'reserved', 'price_currency': 'USD', 'upfront_fee': '0',
                              'upfront_fee_unit': 'Quantity', 'recurring_fee': '0', 'recurring_fee_unit': 'Hrs'}

            reserved_price['LeaseContractLength'] = product_term_offers_dict[term_offer]['termAttributes']['LeaseContractLength']
            reserved_price['OfferingClass'] = product_term_offers_dict[term_offer]['termAttributes'][
                'OfferingClass']
            reserved_price['PurchaseOption'] = product_term_offers_dict[term_offer]['termAttributes'][
                'PurchaseOption']

            # Pricing Dimensions
            for pricing_dimension in product_term_offers_dict[term_offer]['priceDimensions'].keys():
                current_rate_code =  pricing_dimension.split('.')
                if current_rate_code == upfront_fee_rate_code:
                    reserved_price['upfront_fee'] = product_term_offers_dict[term_offer][current_rate_code]['pricePerUnit'][
                      'USD']
                if current_rate_code == recurring_fee_rate_code:
                    reserved_price['recurring_fee'] = product_term_offers_dict[term_offer][current_rate_code]['pricePerUnit'][
                      'USD']
            reserved_price_list.append(reserved_price)
    except KeyError:
        print(product_sku, "- Does not have reserved pricing")
    return reserved_price_list


# Fetch OnDemand Price for Product
def fetch_ondemand_pricing(product_sku, ondemand_dict):
    print("Fetching OnDemand Prices for product:", product_sku)
    try:
        on_demand_price = {
            "price_type": "on_demand",
            "price_currency": 'USD'
        }
        ondemand_product_price_attr = ondemand_dict[product_sku]\
                                                   [product_sku + '.' + ondemand_offerTermCode]\
                                                   ['priceDimensions'] \
                                                   [product_sku + '.' + ondemand_offerTermCode + '.' + recurring_fee_rate_code]
        on_demand_price['pricePerUnit'] = ondemand_product_price_attr['pricePerUnit']['USD']
        on_demand_price['description'] = ondemand_product_price_attr['description']
        on_demand_price['priceUnit'] = ondemand_product_price_attr['unit']
        return on_demand_price
    except KeyError:
        print(product_sku, "- Does not have OnDemand pricing")
        return None


# Create Edge between Product (EC2 Instance) and AWS Service (Amazon Elastic Compute Cloud (EC2)
def add_edge_product_to_service(product_vertex_id, aws_service_name, graph_traversal: GraphTraversalSource):
    product_vertex = graph_traversal.V(product_vertex_id).next()
    aws_service_vertex = graph_traversal.V().hasLabel('aws_service').has('name', aws_service_name).next()
    number_of_edges = len(
        graph_traversal.V(product_vertex).outE().hasLabel('IS_PRODUCT_OF').as_('e') \
            .inV().hasLabel('aws_service').has('name', aws_service_name).select('e').toList())
    if number_of_edges == 0:
        print('Adding Edge between Product and AWS Services:', product_vertex_id, '-->', aws_service_name)
        graph_traversal.V(product_vertex)\
            .addE('IS_PRODUCT_OF').to(aws_service_vertex).next()
    else:
        print('Edge already exists between Product and AWS Services:', product_vertex_id, '-->', aws_service_name)


def fetch_compute_instance_product_vertex_id(vertex_label, vertex_attr_dict, graph_traversal: GraphTraversalSource):
    compute_instance_unique_attr = ['instanceFamily', 'instanceType', 'tenancy', 'usagetype', 'operation',
                                    'operatingSystem', 'preInstalledSw', 'licenseModel']
    traversal = graph_traversal.V().hasLabel(vertex_label)
    for attr in compute_instance_unique_attr:
        traversal = traversal.has(attr, vertex_attr_dict[attr])
    try:
        vertex = traversal.next()
        return vertex.id
    except StopIteration:
        return ""


# Creates Product Vertex or return Product Vertex that exists
def add_product_vertex(vertex_label, vertex_attr_dict, graph_traversal: GraphTraversalSource):
    vertex_id = fetch_compute_instance_product_vertex_id(vertex_label, vertex_attr_dict, graph_traversal)
    if vertex_id == '':
        # Creating Product Vertex
        print(" --> Adding EC2 Instance Product")
        new_vertex = graph_traversal.addV(vertex_label).next()
        # Adding Properties
        for attr_key in vertex_attr_dict:
            graph_traversal.V(new_vertex).property(Cardinality.single, attr_key, vertex_attr_dict[attr_key]).next()
        return new_vertex.id
    else:
        print(" --> Instance Product already Exists")
        return vertex_id


def load_region_offer_to_neptune(aws_region_code, region_offer_dict: dict, graph_traversal: GraphTraversalSource):
    print('Processing products in Region:', aws_region_code)
    products_dict = region_offer_dict['products']
    ondemand_dict = region_offer_dict['terms']['OnDemand']
    reserved_dict = region_offer_dict['terms']['Reserved']
    for product_key in products_dict.keys():
        try:
            if products_dict[product_key]['productFamily'] == 'Compute Instance':
                # Processing Product Vertex
                print("\n\nProcessing SKU:", product_key, end='')
                product_vertex_id = add_product_vertex("product", products_dict[product_key]['attributes'], graph_traversal)
                add_edge_product_to_service(product_vertex_id, 'Amazon Elastic Compute Cloud (EC2)', graph_traversal)

                # Process On-Demand Pricing
                product_ondemand_price_dict = fetch_ondemand_pricing(product_key, ondemand_dict)
                if product_ondemand_price_dict is not None:
                    on_demand_price_vertex = create_price_vertex(product_ondemand_price_dict, 'on-demand-price', graph_traversal)
                    create_edge_region_price(aws_region_code, on_demand_price_vertex.id, graph_traversal)
                    create_edge_price_product(on_demand_price_vertex.id, product_vertex_id, graph_traversal)

                # Process Reserved Pricing
                product_reserved_price_list = get_product_reserved_pricing(product_key, reserved_dict)
                if len(product_reserved_price_list) > 0:
                    for reserved_price_offer in product_reserved_price_list:
                        reserved_price_vertex = create_price_vertex(reserved_price_offer, 'reserved_price', graph_traversal)
                        create_edge_region_price(aws_region_code, reserved_price_vertex.id, graph_traversal)
                        create_edge_price_product(reserved_price_vertex.id, product_vertex_id, graph_traversal)
        except KeyError:
            continue
    print('Completed Processing products in Region:', aws_region_code), '\n\n'


def get_product_currentVersionUrl_from_currentRegionIndex(region_index_url):
    print("Downloading AWS-PRICE-API Regional Offer-index")
    with urlopen(region_index_url) as json_file:
        region_index = json.loads(json_file.read().decode('utf-8'))
        return region_index['regions']


def get_product_currentRegionIndexUrl_from_offerIndex(price_api_endpoint, product_offerCode):
    print("Downloading AWS-PRICE-API main offer-index")
    offer_index_url = price_api_endpoint+'/offers/v1.0/aws/index.json'
    with urlopen(offer_index_url) as json_file:
        offer_index = json.loads(json_file.read().decode('utf-8'))
        return offer_index['offers'][product_offerCode]['currentRegionIndexUrl']


def load_ec2_pricing_data_to_neptune(graph_traversal: GraphTraversalSource):
    print('Start --- Loading EC2 Instance Prices to Neptune')

    price_api_endpoints = 'https://pricing.us-east-1.amazonaws.com'
    product_region_index_url = price_api_endpoints + get_product_currentRegionIndexUrl_from_offerIndex(price_api_endpoints, 'AmazonEC2')
    regional_offer = get_product_currentVersionUrl_from_currentRegionIndex(product_region_index_url)

    for region in regional_offer.keys():

        traversal = graph_traversal.V().hasLabel('aws_region').has('code', region)
        if len(traversal.toList()) != 0:
            region_offer_json_url = price_api_endpoints + regional_offer[region]['currentVersionUrl']
            print("Downloading AWS-PRICE-API Offer JSON for region:", region)
            with urlopen(region_offer_json_url) as json_file:
                region_offer_dict = json.loads(json_file.read().decode('utf-8'))
                start_time = timer()
                load_region_offer_to_neptune(region, region_offer_dict, graph_traversal)
                end_time = timer()
                time_taken = str(datetime.timedelta(seconds=end_time - start_time))
                print('################################################################################################')
                print("Data Loading for EC2 Instance Pricing Completed for region:" + region + 'in:' + time_taken + "\n")
                print('################################################################################################')
    print('Completed --- Loading EC2 Instance Prices to Neptune')

# if __name__ == '__main__':
#     # Neptune Connection
#     neptune_endpoint = '34.229.104.116'
#     neptune_port = 8182
#
#     graph = Graph()
#     g = graph.traversal().withRemote(
#         DriverRemoteConnection('ws://' + neptune_endpoint + ':' + str(neptune_port) + '/gremlin', 'g'))
#
#     print("Data Loading Started\n")
#     start_time = timer()
#     load_ec2_pricing_data_to_neptune(g)
#     end_time = timer()
#     time_taken = str(datetime.timedelta(seconds=end_time - start_time))
#     print("Data Loading Completed in:" + time_taken + "\n")



