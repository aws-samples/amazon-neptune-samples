from enum_aws_regions import AwsRegions
from enum_continents import Continents
import pycountry_convert
import boto3
import botocore
from gremlin_python.process.graph_traversal import GraphTraversalSource
from enum_vertex_edge_labels import VertexEdgeLabels
import gremlin_interface


def add_country_continent_edge(country_iso_3166_code, graph_traversal):
    """
    Add Edge from Country Vertex to Continent Vertex.
    :param country_iso_3166_code: Country Code in ISO 3166 Format. Upper Case.
    :type country_iso_3166_code: str
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :return:
    """
    # Fetch Origin and Destination Vertex Id's
    country_vertex_id = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                           VertexEdgeLabels.vertex_label_country.value,
                                                           {'code': country_iso_3166_code.upper()})[0]

    continent_code = pycountry_convert.country_alpha2_to_continent_code(country_iso_3166_code.upper())
    continent_vertex_id = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                             VertexEdgeLabels.vertex_label_continent.value,
                                                             {'code': continent_code})[0]

    # Fetch Edge List
    edge_list = gremlin_interface.fetch_edge_list(graph_traversal, country_vertex_id, continent_vertex_id,
                                                 VertexEdgeLabels.edge_label_country_to_continent.value)

    # Create  Edge if it does not exist
    if len(edge_list) == 0:
        gremlin_interface.add_edge(graph_traversal, country_vertex_id, continent_vertex_id,
                                   VertexEdgeLabels.edge_label_country_to_continent.value)


def add_country_vertex(country_iso_3166_code, graph_traversal: GraphTraversalSource):
    """
    Create a Country Vertex.
    :param country_iso_3166_code: Country Code in ISO 3166 Format. Upper Case.
    :type country_iso_3166_code: str
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :return:
    """
    country_property_dict = {'code': country_iso_3166_code,
                             'name': pycountry_convert.country_alpha2_to_country_name(country_iso_3166_code.upper())}

    # Fetch Country Vertices
    vertex_list = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                     VertexEdgeLabels.vertex_label_country.value,
                                                     country_property_dict)

    # Create Country Vertex if does not exist
    if len(vertex_list) == 0:
        country_vertex_id = gremlin_interface.add_vertex(graph_traversal, VertexEdgeLabels.vertex_label_country.value)
        gremlin_interface.add_update_vertex_properties(graph_traversal, country_vertex_id, country_property_dict)

    # Add Edge between Country & Continent
    add_country_continent_edge(country_iso_3166_code, graph_traversal)


def get_az_count_per_region(region_code):
    """
    Fetch number of Availability Zones in specified AWS-Region.
    :param region_code: AWS-Region Code e.g. us-east-1.
    :type  region_code: str
    :return: Returns number of availability zones.
    ":rtype: int
    """
    ec2 = boto3.client('ec2', region_name=region_code)
    try:
        response = ec2.describe_availability_zones()
        az = response['AvailabilityZones']
        return len(az)
    except botocore.exceptions.ClientError:
        return 0


def create_edge_aws_region_to_country(graph_traversal, aws_region_code, country_iso_3166_code):
    """
    Add Edge from AWS-Region Vertex to Country Vertex
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param aws_region_code: AWS Region Code e.g. us-east-1.
    :type aws_region_code: str
    :param country_iso_3166_code: Country code in ISO 3166 format. Upper Case
    :type country_iso_3166_code: str
    :return:
    """
    aws_region_vertex_id = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                              VertexEdgeLabels.vertex_label_aws_region.value,
                                                              {'code': aws_region_code})[0]

    country_vertex_id = gremlin_interface.fetch_vertex_list(graph_traversal,
                                                           VertexEdgeLabels.vertex_label_country.value,
                                                           {'code': country_iso_3166_code})[0]

    # Fetch Edge List
    edge_list = gremlin_interface.fetch_edge_list(graph_traversal,
                                                 aws_region_vertex_id, country_vertex_id,
                                                 VertexEdgeLabels.edge_label_awsRegion_to_country.value)

    # Add edge from AWS-Region to Country, if edge does not exist
    if len(edge_list) == 0:
        gremlin_interface.add_edge(graph_traversal, aws_region_vertex_id, country_vertex_id,
                                   VertexEdgeLabels.edge_label_awsRegion_to_country.value)


def create_aws_region_vertices(graph_traversal: GraphTraversalSource):
    """
    Create AWS-Region Vertices. Add Edges between AWS-Region to Country Vertices.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :return:
    """
    print("Start --- Loading AWS Regions to Neptune")

    for aws_region in AwsRegions:
        aws_region_prop_dict = aws_region.value

        # Append AZ Count to aws_region_prop_dict
        number_of_azs = get_az_count_per_region(aws_region_prop_dict['code'])
        aws_region_prop_dict['az_count'] = number_of_azs

        # Fetch aws_region vertices
        aws_region_vertex_label = VertexEdgeLabels.vertex_label_aws_region.value
        aws_region_vertex_list = gremlin_interface.fetch_vertex_list(graph_traversal, aws_region_vertex_label,
                                                                    aws_region_prop_dict, ['code'])

        # Create AWS-Region Vertex if does not exist
        aws_region_vertex_id = ''
        if len(aws_region_vertex_list) == 0:
            aws_region_vertex_id = gremlin_interface.add_vertex(graph_traversal, aws_region_vertex_label)
            gremlin_interface.add_update_vertex_properties(graph_traversal, aws_region_vertex_id, aws_region_prop_dict)

        # Create Country Vertex & Edge between Country --> Continent
        add_country_vertex(aws_region_prop_dict['country_code'], graph_traversal)

        # Add Edge from AWS-Region to Country
        create_edge_aws_region_to_country(graph_traversal, aws_region_prop_dict['code'],
                                          aws_region_prop_dict['country_code'])

    print("Completed --- Loading AWS Regions to Neptune\n")


def create_continent_vertices(graph_traversal):
    """
    Create Continent Vertices in Neptune.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :return:
    """
    print("Start --- Creating Continent Vertices")
    for continent in Continents:
        continent_prop_dict = continent.value
        vertex_label = VertexEdgeLabels.vertex_label_continent.value

        # Fetch Continent Vertex
        continent_vertex_list = gremlin_interface.fetch_vertex_list(graph_traversal, vertex_label,
                                                                   continent_prop_dict, ['code'])

        # If no Vertex exist, create the Continent Vertex
        if len(continent_vertex_list) == 0:
            continent_vertex_id = gremlin_interface.add_vertex(graph_traversal, vertex_label)
            gremlin_interface.add_update_vertex_properties(graph_traversal, continent_vertex_id, continent_prop_dict)
    print("Completed --- Creating Continent Vertices\n")


def load_aws_regions_to_neptune(graph_traversal: GraphTraversalSource):
    create_continent_vertices(graph_traversal)
    create_aws_region_vertices(graph_traversal)
