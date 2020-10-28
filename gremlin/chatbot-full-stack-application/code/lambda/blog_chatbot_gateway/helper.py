# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import os
import logging
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import traversal
from gremlin_python.process.strategies import *
from gremlin_python.process.traversal import *
from gremlin_python.structure.graph import Path, Vertex, Edge
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

#
# Establish a Web Socket Connection to Neptune using the configured server and port.
#


def connectToNeptune():
    """Creates a connection to Neptune and returns the traversal source"""
    envok = True
    if 'NEPTUNE_ENDPOINT' in os.environ and 'NEPTUNE_PORT' in os.environ:
        server = os.environ["NEPTUNE_ENDPOINT"]
        port = os.environ["NEPTUNE_PORT"]
        endpoint = f'wss://{server}:{port}/gremlin'
        logger.info(endpoint)
        connection = DriverRemoteConnection(endpoint, 'g')
        gts = traversal().withRemote(connection)
        return (gts, connection)
    else:
        logging.error("Internal Configuraiton Error Occurred.  ")
        return None


def vertex_to_dict(vertex):
    d = {}
    for k in vertex.keys():
        if isinstance(vertex[k], list):
            d[str(k)] = vertex[k][0]
        else:
            d[str(k)] = vertex[k]
    d['id'] = d.pop('T.id')
    d['label'] = d.pop('T.label')
    return d


def edge_to_dict(edge, start_id, end_id):
    d = {}
    for k in edge.keys():
        if isinstance(edge[k], list):
            d[str(k)] = edge[k][0]
        else:
            d[str(k)] = edge[k]
    d['id'] = d.pop('T.id')
    d['label'] = d.pop('T.label')
    d['source'] = start_id
    d['target'] = end_id
    return d
