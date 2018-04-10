from enum_aws_regions import AwsRegions
from enum_continents import  Continents
import pycountry_convert
import boto3
import botocore
from gremlin_python.structure.graph import Graph
from gremlin_python.process.traversal import Cardinality
from gremlin_python.process.graph_traversal import GraphTraversalSource
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection


def create_continent_vertices(graph_traversal: GraphTraversalSource):
    print("Start --- Creating Continent Vertices")
    for continent in Continents:
        continent_prop_dict = continent.value
        if len(graph_traversal.V().has('code', continent_prop_dict['code']).toList()) == 0:
            continent_vertex = graph_traversal.addV('continent').next()
        else:
            continent_vertex = graph_traversal.V().has('code', continent_prop_dict['code']).next()
        # Setting Properties
        for key in continent_prop_dict:
            graph_traversal.V(continent_vertex).property(Cardinality.single, key, continent_prop_dict[key]).next()
    print("Completed --- Creating Continent Vertices\n")


def add_country_continent_edge(country_iso_3166_code, graph_traversal: GraphTraversalSource):
    continent_code = pycountry_convert.country_alpha2_to_continent_code(country_iso_3166_code.upper())
    continent_vertex = graph_traversal.V().hasLabel('continent').has('code', continent_code)
    number_of_edges = len(
        graph_traversal.V().hasLabel('country').has('code', country_iso_3166_code.upper()).outE().as_('e') \
             .inV().hasLabel('continent').has('code', continent_code).select('e').toList())

    if number_of_edges == 0:
        print("Creating Edge between Country and Continent:", country_iso_3166_code, "--LOCATED_IN_CONTINENT-->", continent_code)

        graph_traversal.V().hasLabel('country').has('code', country_iso_3166_code.upper()).addE('LOCATED_IN_CONTINENT').to(
            continent_vertex).next()


def add_country_vertex(country_iso_3166_code, graph_traversal: GraphTraversalSource):
    # Create Vertex or Fetch Vertex if exists
    if len(graph_traversal.V().has('code', country_iso_3166_code).toList()) == 0:
        print("Creating Country Vertex:", country_iso_3166_code.upper())

        # Creating Country Vertex
        graph_traversal.addV('country')\
            .property(Cardinality.single, 'code', country_iso_3166_code) \
            .property(Cardinality.single, 'name',
                      pycountry_convert.country_alpha2_to_country_name(country_iso_3166_code.upper())) \
            .next()

    # Add Edge between Country & Continent
    add_country_continent_edge(country_iso_3166_code, graph_traversal)


def add_region_country_edge(aws_region_code, country_iso_3166_code, graph_traversal: GraphTraversalSource):
    number_of_edges = len(
        graph_traversal.V().hasLabel('aws_region').has('code', aws_region_code).outE().as_('e') \
             .inV().hasLabel('country').has('code', country_iso_3166_code.upper()).select('e').toList())

    # Create Country Vertex if it does not exist
    add_country_vertex(country_iso_3166_code, graph_traversal)

    # Add Edge from AWS-Region to Country
    if number_of_edges == 0:
        print("Creating Edge between AWS-Region and Country:", aws_region_code, "--LOCATED_IN_COUNTRY-->", country_iso_3166_code)
        continent_vertex = graph_traversal.V().hasLabel('country').has('code', country_iso_3166_code.upper())
        graph_traversal.V().hasLabel('aws_region').has('code', aws_region_code) \
             .addE('LOCATED_IN_COUNTRY').to(continent_vertex).next()


def get_az_count_per_region(region_code):
    ec2 = boto3.client('ec2', region_name=region_code)
    try:
        response = ec2.describe_availability_zones()
        az = response['AvailabilityZones']
        return len(az)
    except botocore.exceptions.ClientError:
        return 0


def create_aws_region_vertices(graph_traversal: GraphTraversalSource):
    print("Start --- Loading AWS Regions to Neptune")

    for aws_region in AwsRegions:
        aws_region_prop_dict = aws_region.value
        print("Processing AWS-Region Vertex:", aws_region_prop_dict['code'])
        # Create or Fetch Vertex if exists
        if len(graph_traversal.V().has('code', aws_region_prop_dict['code']).toList()) == 0:
            print("Creating AWS-Region Vertex:", aws_region_prop_dict['code'])
            aws_region_vertex = graph_traversal.addV('aws_region').next()
        else:
            aws_region_vertex = graph_traversal.V().has('code', aws_region_prop_dict['code']).next()

        # Update Properties
        for key in aws_region_prop_dict.keys():
            graph_traversal.V(aws_region_vertex).property(Cardinality.single, key, aws_region_prop_dict[key]).next()

        # Add/Update AZ Property
        number_of_azs = get_az_count_per_region(aws_region_prop_dict['code'])
        graph_traversal.V(aws_region_vertex).property(Cardinality.single, 'az_count', number_of_azs).next()

        # Add Edge from AWS-Region to Country
        add_region_country_edge(aws_region_prop_dict['code'], aws_region_prop_dict['country_code'], graph_traversal)
    print("Completed --- Loading AWS Regions to Neptune\n")


def load_aws_regions_to_neptune(graph_traversal: GraphTraversalSource):
    create_continent_vertices(graph_traversal)
    create_aws_region_vertices(graph_traversal)