from pprint import pprint
from timeit import default_timer as timer
import datetime

import tornado
from socket import gaierror
from gremlin_python.structure.graph import Graph
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import Cardinality

from gremlin_python.structure.graph import Edge
from gremlin_python.structure.graph import Vertex

def get_gremlin_connection(neptune_server_endpoint, neptune_server_port=8182):
    """
    Create a connection to Neptune Server Endpoint and return Graph Traversal Source.
    :param neptune_server_endpoint: Neptune Server Endpoint-URL or IP-Address
    :type   neptune_server_endpoint: str
    :param neptune_server_port: Neptune Server Connection Port.
    :type neptune_server_port: int
    :return: Returns the GraphTraversalSource
    :rtype: gremlin_python.process.graph_traversal.GraphTraversalSource
    """
    graph = Graph()
    try:
        print("Connecting to Neptune Server")
        graph_traversal = graph.traversal().withRemote(
            DriverRemoteConnection('ws://' + neptune_server_endpoint + ':' + str(neptune_server_port) + '/gremlin', 'g'))
        print("Successfully connected to Neptune Server\n")
        return graph_traversal
    except gaierror:
        print("ERROR: Could not establish connection with the Neptune Server. Invalid Connection Parameters")
    except tornado.httpclient.HTTPError:
        print("ERROR: Timeout while connecting. Could not establish connection with the Neptune Server.")


def add_update_vertex_properties(graph_traversal, vertex, vertex_properties_dict={}):
    """
    Add new vertex properties or update Vertex properties if exists.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param vertex: Vertex
    :type vertex: gremlin_python.structure.graph.Vertex
    :param vertex_properties_dict: Vertex Properties Dictionary
    :type vertex_properties_dict: dict
    :return: Returns Vertex
    :rtype: gremlin_python.structure.graph.Vertex
    """
    graph_traversal = graph_traversal.V(vertex)
    if len(vertex_properties_dict.keys()) != 0:
        for vertex_attr in vertex_properties_dict.keys():
            graph_traversal.property(Cardinality.single, vertex_attr, vertex_properties_dict[vertex_attr])
    graph_traversal.next()
    return vertex


def add_vertex(graph_traversal, vertex_label):
    """
    Add a Gremlin Vertex if it does not exist.
    If Vertex exists, add/update the Vertex properties if changed.
    You can also specify 'exclude_attr_key_list' - a List of attributes to be excluded from the vertex_attr_dict.
    If 'exclude_attr_key_list' is not specified all attributes will be added to the Vertex.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param vertex_label: Vertex Label
    :type vertex_label: str
    :param vertex_prop_dict: Vertex Properties Dictionary
    :type vertex_prop_dict: dict
    :param exclude_attr_key_list: List of Properties to be excluded from vertex_prop_dict
    :type exclude_attr_key_list: list
    :return: Returns new Vertex.
    :rtype: gremlin_python.structure.graph.Vertex
    """
    vertex = graph_traversal.addV(vertex_label).next()
    return vertex


def fetch_vertex_list(graph_traversal, vertex_label, vertex_properties_dict, query_property_key_list=[]):
    """
    Fetch Gremlin vertices.
    query_property_list - specify a list of property names, if you want to limit the query only to the
    specified properties.
    Else all the properties in the vertex_properties_dict will be used in the query.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param vertex_label: Vertex Label
    :type vertex_label: str
    :param vertex_properties_dict: Vertex properties dictionary.
    :type vertex_properties_dict: dict
    :param query_property_key_list: List of query properties, if you want to query only based on a subset of properties
    in vertex_properties_dict.
    :type query_property_key_list: list
    :return: Returns list of Vertices.
    :rtype: list<gremlin_python.structure.graph.Vertex>
    """
    if len(query_property_key_list) == 0:
        query_property_key_list = vertex_properties_dict.keys()

    current_traversal = graph_traversal.V().hasLabel(vertex_label)
    for vertex_attr_key in query_property_key_list:
        current_traversal = current_traversal.has(vertex_attr_key, vertex_properties_dict[vertex_attr_key])
    # vertex_list = current_traversal.id().toList()
    vertex_list = current_traversal.toList()
    return vertex_list


def fetch_edge_list(graph_traversal, origin_vertex, destination_vertex, edge_label,
                   edge_properties_dict={}, query_property_key_list=[]):
    """
    Fetch Edge IDs.
    You can query for edges only between source and destination Vertex.
    Extend the query to include all properties defined in edge_properties_dict.
    Limit the query to a subset of edge_properties_dict by defining the query_property_key_list.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param origin_vertex: Origin Vertex
    :type origin_vertex: gremlin_python.structure.graph.Vertex
    :param destination_vertex: Destination Vertex
    :type destination_vertex: gremlin_python.structure.graph.Vertex
    :param edge_label: Edge Label
    :type edge_label: str
    :param edge_properties_dict: Edge Properties Dictionary
    :type edge_properties_dict: dict
    :param query_property_key_list: List of Query Properties, if you want to query only based on a subset of properties
    in the edge_properties_dict.
    :type query_property_key_list: list
    :return: Returns a list of Edges
    :rtype: list<gremlin_python.structure.graph.Edge>
    """
    if len(query_property_key_list) == 0:
        query_property_key_list = edge_properties_dict.keys()

    # Set Source Vertex
    graph_traversal = graph_traversal.V().hasId(origin_vertex.id).outE(edge_label)

    # Search Edge Properties
    if len(query_property_key_list) != 0:
        for property_key in query_property_key_list:
            graph_traversal = graph_traversal.has(property_key, edge_properties_dict[property_key])

    # Set Destination Vertex and get Edge IDs
    # graph_traversal = graph_traversal.as_('e').inV().hasId(destination_vertex_id).select('e').id()
    graph_traversal = graph_traversal.as_('e').inV().hasId(destination_vertex.id).select('e').id()

    edge_list = graph_traversal.toList()
    return edge_list


def add_edge(graph_traversal, origin_vertex, destination_vertex, edge_label):
    """
    Add Edge from Source Vertex to Destination Vertex.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param origin_vertex: Origin Vertex ID
    :type origin_vertex: gremlin_python.structure.graph.Vertex
    :param destination_vertex: Destination Vertex ID
    :type destination_vertex: gremlin_python.structure.graph.Vertex
    :param edge_label: Edge Label
    :type edge_label: str
    :return: Return Edge
    :rtype: gremlin_python.structure.graph.Edge
    """

    origin_vertex = graph_traversal.V(origin_vertex)
    destination_vertex = graph_traversal.V(destination_vertex)
    edge = graph_traversal.addE(edge_label).as_('e').from_(origin_vertex).to(destination_vertex).select('e').next()
    return edge


def add_update_edge_properties(graph_traversal, edge, edge_properties_dict={}):
    """
    Add or Update Edge Properties.
    :param graph_traversal: GraphTraversalSource
    :type graph_traversal: gremlin_python.process.graph_traversal.GraphTraversalSource
    :param edge_id: Edge-ID
    :type edge_id: str
    :param edge_properties_dict: Edge Properties Dictionary
    :type edge_properties_dict: dict
    :return: Returns Edge-ID
    :rtype: str
    """
    graph_traversal = graph_traversal.E(edge)
    if len(edge_properties_dict.keys()) != 0:
        for edge_property in edge_properties_dict.keys():
            graph_traversal = graph_traversal.property(edge_property, edge_properties_dict[edge_property])
        edge = graph_traversal.next()
    return edge