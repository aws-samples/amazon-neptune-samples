from enum_aws_regions import AwsRegions
from urllib.request import urlopen
import json
from timeit import default_timer as timer
import datetime
from gremlin_python.process.graph_traversal import GraphTraversalSource
from sys import stdout
import time

from enum_vertex_edge_labels import VertexEdgeLabels
import gremlin_interface

ondemand_offerTermCode = 'JRTCKXETXF'
upfront_fee_rate_code = '2TG2D8R56U'
recurring_fee_rate_code = '6YS6EN2CT7'


# Load Prices for Product
def add_product_priceTerm_and_edge(graph_traversal, offer_type, aws_region_code, product_vertex, product_offers_dict):
    for product_ondemand_offer in product_offers_dict.keys():
        # Setting the Attributes for ondemand offer vertex
        offer_vertex_attributes = {}
        if offer_type == 'OnDemand':
            offer_vertex_attributes = {'term_type': 'OnDemand'}
        elif offer_type == 'Reserved':
            offer_vertex_attributes = {'term_type': 'Reserved'}

        offerTermCode = product_offers_dict[product_ondemand_offer]['offerTermCode']
        offer_vertex_attributes['offer_term_code'] = offerTermCode

        # Add any termAttributes to offer_vertex_attributes
        offer_vertex_attributes.update(product_offers_dict[product_ondemand_offer]['termAttributes'])

        # Create Offer Term Vertex
        offer_vertex_list = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                                        VertexEdgeLabels.vertex_label_offer_term.value,
                                                                        {'offer_term_code':offerTermCode})
        if len(offer_vertex_list) == 0:
            price_term_vertex = gremlin_interface.add_vertex(graph_traversal,
                                                                 VertexEdgeLabels.vertex_label_offer_term.value)
            gremlin_interface.add_update_vertex_properties(graph_traversal,
                                                           price_term_vertex,
                                                           offer_vertex_attributes)
        else:
            price_term_vertex = offer_vertex_list[0]

        # Create Edge From AWS-Region Vertex to Offer Term Vertex
        product_sku = product_offers_dict[product_ondemand_offer]['sku']
        price_edge_attr = {}
        price_edge_attr['sku'] = product_sku

        # Adding Recurring Fees to Price_Edge Attribute
        price_code = product_sku + '.' + offerTermCode + '.' + recurring_fee_rate_code
        recurring_fee_price_dict = product_offers_dict[product_ondemand_offer]['priceDimensions'][price_code]
        price_edge_attr['recurring_fee_usd'] = recurring_fee_price_dict['pricePerUnit']['USD']
        price_edge_attr['recurring_fee_unit'] = recurring_fee_price_dict['unit']
        price_edge_attr['recurring_fee_description'] = recurring_fee_price_dict['description']

        # Add UpFront Fee to Price_Edge Attribute
        price_code = product_sku + '.' + offerTermCode + '.' + upfront_fee_rate_code
        upfront_fee_price_dict = product_offers_dict[product_ondemand_offer]['priceDimensions'].pop(price_code, None)
        if upfront_fee_price_dict is not None:
            price_edge_attr['upfront_fee_usd'] = recurring_fee_price_dict['pricePerUnit']['USD']
            price_edge_attr['upfront_fee_unit'] = recurring_fee_price_dict['unit']
            price_edge_attr['upfront_fee_description'] = recurring_fee_price_dict['description']

        aws_region_vertex = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                                VertexEdgeLabels.vertex_label_aws_region.value,
                                                                {'code': aws_region_code})[0]
        price_edge_list = gremlin_interface.fetch_edge_list(graph_traversal,
                                                            aws_region_vertex,
                                                            price_term_vertex,
                                                            VertexEdgeLabels.edge_label_awsRegion_to_priceTerm.value,
                                                            price_edge_attr)
        if len(price_edge_list) == 0:
            region_priceTerm_edge = gremlin_interface.add_edge(graph_traversal,aws_region_vertex, price_term_vertex,
                                                    VertexEdgeLabels.edge_label_awsRegion_to_priceTerm.value)
            gremlin_interface.add_update_edge_properties(graph_traversal, region_priceTerm_edge, price_edge_attr)

        # Create Edge From Term to Product
        price_product_edge_list = gremlin_interface.fetch_edge_list(graph_traversal,
                                                                    price_term_vertex,
                                                                    product_vertex,
                                                                    VertexEdgeLabels.edge_label_priceTerm_to_product.value)
        if len(price_product_edge_list) == 0:
            price_product_edge = gremlin_interface.add_edge(graph_traversal,
                                                            price_term_vertex,
                                                            product_vertex,
                                                            VertexEdgeLabels.edge_label_priceTerm_to_product.value)


# Creates Product Vertex or return Product Vertex that exists
def add_product_vertex(vertex_label, vertex_attr_dict, graph_traversal: GraphTraversalSource):
    compute_instance_unique_attr = ['instanceFamily', 'instanceType', 'tenancy', 'operation',
                                    'operatingSystem', 'preInstalledSw', 'licenseModel']

    vertex_list = gremlin_interface.fetch_vertex_list(graph_traversal, vertex_label,
                                                     vertex_attr_dict, compute_instance_unique_attr)

    if len(vertex_list) == 0:
        compute_instance_vertex= gremlin_interface.add_vertex(graph_traversal, vertex_label)
        gremlin_interface.add_update_vertex_properties(graph_traversal, compute_instance_vertex, vertex_attr_dict)
        return compute_instance_vertex
    elif len(vertex_list) == 1:
        compute_instance_vertex = vertex_list[0]
        return compute_instance_vertex


def load_region_offer_to_neptune(aws_region_code, region_offer_dict: dict, graph_traversal: GraphTraversalSource):
    print('Processing products in Region:', aws_region_code)
    products_dict = region_offer_dict['products']
    ondemand_dict = region_offer_dict['terms'].pop('OnDemand', None)
    reserved_dict = region_offer_dict['terms'].pop('Reserved', None)
    product_counter = 0
    for product_key in products_dict.keys():
        product_counter = product_counter + 1
        if product_counter%5 == 0 or product_counter == len(products_dict.keys()):
            print('Processing product', product_counter,'of', len(products_dict.keys()),'\r')
        try:
            if products_dict[product_key]['productFamily'] == 'Compute Instance':

                # Setting Product Vertex Attributes
                products_attr_dict = products_dict[product_key]['attributes']
                # Removing location and usagetype
                products_attr_dict.pop('location', None)
                products_attr_dict.pop('usagetype', None)
                # Adding Product Family to Product Attribute
                products_attr_dict['productFamily'] = products_dict[product_key]['productFamily']

                # Creating Product Vertex
                product_vertex = add_product_vertex(VertexEdgeLabels.vertex_label_aws_product.value,
                                                       products_dict[product_key]['attributes'],
                                                       graph_traversal)

                # Create Edge between Compute Instance and Amazon EC2 service
                aws_service_vertex = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                                        VertexEdgeLabels.vertex_label_aws_service.value,
                                                                        {'name': 'Amazon Elastic Compute Cloud (EC2)'})[0]

                edge_list = gremlin_interface.fetch_edge_list(graph_traversal, product_vertex,
                                                 aws_service_vertex,
                                                 VertexEdgeLabels.edge_label_product_to_awsService.value)
                if len(edge_list) == 0:
                    gremlin_interface.add_edge(graph_traversal,
                                               product_vertex,
                                               aws_service_vertex,
                                               VertexEdgeLabels.edge_label_product_to_awsService.value)

            # load OnDemand prices for Product
            if ondemand_dict is not None:
                product_ondemand_offer_dict = ondemand_dict.pop(product_key, None)
                if product_ondemand_offer_dict is not None:
                    add_product_priceTerm_and_edge(graph_traversal, 'OnDemand', aws_region_code,
                                                    product_vertex, product_ondemand_offer_dict)

            # load Reserved prices for Product
            if reserved_dict is not None:
                product_reserved_offer_dict = reserved_dict.pop(product_key, None)
                if product_reserved_offer_dict is not None:
                    add_product_priceTerm_and_edge(graph_traversal, 'Reserved', aws_region_code,
                                                    product_vertex, product_reserved_offer_dict)

        except KeyError:
            print("SKU:", product_key, "in region:", aws_region_code, "does not have 'productFamily attribute")
            continue

    print('\nCompleted Processing products in Region:', aws_region_code), '\n'


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
                print("Data Loading for EC2 Instance Pricing Completed for region:" + region + 'in:' + time_taken + "\n")
    print('Completed --- Loading EC2 Instance Prices to Neptune\n')