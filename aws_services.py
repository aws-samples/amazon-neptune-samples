import bs4 as bs
from urllib.request import urlopen
from gremlin_python.process.traversal import Cardinality
from gremlin_python.process.graph_traversal import GraphTraversalSource


def create_service_region_edges(table_header: list, service_row: list, graph_traversal: GraphTraversalSource):
    for item_num in range(1, len(service_row)):
        if service_row[item_num] == '\uf00c' or service_row[item_num] == '✓':
            # add edge between aws-service and aws-region
            number_of_edges = len(
                graph_traversal.V().hasLabel('aws_region').has('descriptive_name', table_header[item_num]).outE().as_('e')\
                    .inV().hasLabel('aws_service').has('name', service_row[0]).select('e').toList())

            # Add Edge if it does not exist and if Region Vertex exists
            if number_of_edges == 0 and len(graph_traversal.V().hasLabel('aws_region').has('descriptive_name', table_header[item_num]).toList()) !=0:
                print("Creating Edge between AWS-Region and AWS-Service:",
                      table_header[item_num], "--OFFERS_SERVICE-->", service_row[0])

                service_vertex = graph_traversal.V().hasLabel('aws_service').has('name', service_row[0])
                graph_traversal.V().hasLabel('aws_region').has('descriptive_name', table_header[item_num])\
                    .addE('OFFERS_SERVICE').to(service_vertex).next()


def create_aws_service_vertex(service_name: str, graph_traversal: GraphTraversalSource):
    if len(graph_traversal.V().hasLabel('aws_service').has('name', service_name).toList()) == 0:
        graph_traversal.addV('aws_service') \
            .property(Cardinality.single, 'name', service_name)\
            .next()


def create_aws_services_vertex_edges(header_row: list, current_row: list, graph_traversal: GraphTraversalSource):
    create_aws_service_vertex(current_row[0], graph_traversal)
    create_service_region_edges(header_row, current_row, graph_traversal)


def clean_service_row(service_row: list):
    for item in service_row:
        # Removing White Spaces
        service_row[service_row.index(item)] = item.strip()
        service_row = list(filter('\xa0'.__ne__, service_row))  # remove '\xa0'
    return service_row


def clean_header_row(header_item_row: list):
    for item in header_item_row:
        header_item_row[header_item_row.index(item)] = item.strip()
    return header_item_row


def map_aws_service_regions(html_table, graph_traversal: GraphTraversalSource):
    row_counter = 1
    table_header = []
    for tr in html_table.find_all('tr'):
        td = tr.find_all('td')
        row = [i.text for i in td]
        if len(row) != 0:
            if row_counter == 1:
                table_header = clean_header_row(row)
            else:
                # service_rows.append(clean_header_row(row))
                row = clean_header_row(row)
                create_aws_services_vertex_edges(table_header, row, graph_traversal)
        row_counter += row_counter


def fetch_raw_data_from_regional_product_services_page():
    aws_regions_endpoints_url = 'https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services/'
    print('Scraping', aws_regions_endpoints_url, 'to get raw html data for AWS Services and their Region Availability')
    service_table_block_list = []
    with urlopen(aws_regions_endpoints_url) as html_page:
        soup = bs.BeautifulSoup(html_page.read().decode('utf-8'), 'lxml')
        for service_table_block in soup.find_all('li', class_='content-item'):
            service_table_block_list.append(service_table_block)
            # print(li_element.find_all('table'))
    return service_table_block_list


def create_aws_service_region_mapping(graph_traversal: GraphTraversalSource):
    # Fetch Service-Region HTML Table from the regional-product-services page
    tables_blocks_in_page = fetch_raw_data_from_regional_product_services_page()

    for table_block in tables_blocks_in_page:
        # print(type(table_block.find_all('table')[0]))
        map_aws_service_regions(table_block.find_all('table')[0], graph_traversal)


def load_aws_services_to_neptune(graph_traversal: GraphTraversalSource):
    print("Start --- Loading AWS Services to Neptune\n")
    create_aws_service_region_mapping(graph_traversal)
    print("Completed --- Loading AWS Services to Neptune\n")