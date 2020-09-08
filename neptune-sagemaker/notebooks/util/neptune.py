from __future__  import print_function
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import *
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import *
from tornado.httpclient import HTTPError

'''
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import os
import json
import urllib.request
import time
import warnings
import sys
from neptune_python_utils.gremlin_utils import GremlinUtils
from neptune_python_utils.endpoints import Endpoints, Endpoint
from neptune_python_utils.bulkload import BulkLoad

class Neptune:
    
    def __init__(self):
        self.connections = []
        
    def close(self):
        for connection in self.connections:
            connection.close()
  
    def remoteConnection(self, neptune_endpoint=None, neptune_port=None, show_endpoint=True):
        connection = GremlinUtils(Endpoints(neptune_endpoint, neptune_port)).remote_connection(show_endpoint)
        self.connections.append(connection)
        return connection      
       
    def graphTraversal(self, neptune_endpoint=None, neptune_port=None, show_endpoint=True, connection=None):
        if connection is None:
            connection = self.remoteConnection(neptune_endpoint, neptune_port, show_endpoint)
        self.connections.append(connection)
        return GremlinUtils(Endpoints(neptune_endpoint, neptune_port)).traversal_source(show_endpoint, connection)
    
    def bulkLoadAsync(self, source, format='csv', role=None, region=None, neptune_endpoint=None, neptune_port=None):
        bulkload = BulkLoad(source, format, role, region=region, endpoints=Endpoints(neptune_endpoint, neptune_port))
        return bulkload.load_async()
    
    def bulkLoad(self, source, format='csv', role=None, region=None, neptune_endpoint=None, neptune_port=None, interval=2):        
        bulkload = BulkLoad(source, format, role, region=region, endpoints=Endpoints(neptune_endpoint, neptune_port))
        bulkload.load(interval)
        
    def bulkLoadStatus(self, status_url):
        return status_url.status()
            
    def sparql_endpoint(self, neptune_endpoint=None, neptune_port=None):
        return Endpoints(neptune_endpoint, neptune_port).sparql_endpoint()
    
    def clear(self, neptune_endpoint=None, neptune_port=None, batch_size=200, edge_batch_size=None, vertex_batch_size=None):
        print('clearing data...')
        self.clearGremlin(neptune_endpoint, neptune_port, batch_size, edge_batch_size, vertex_batch_size)
        self.clearSparql(neptune_endpoint, neptune_port)
        print('done')
            
    def clearGremlin(self, neptune_endpoint=None, neptune_port=None, batch_size=200, edge_batch_size=None, vertex_batch_size=None):
        if edge_batch_size is None:
            edge_batch_size = batch_size
        if vertex_batch_size is None:
            vertex_batch_size = batch_size
        g = self.graphTraversal(neptune_endpoint, neptune_port, False) 
        has_edges = True
        edge_count = None
        while has_edges:
            if edge_count is None:
                print('clearing property graph data [edge_batch_size={}, edge_count=Unknown]...'.format(edge_batch_size))
            else:
                print('clearing property graph data [edge_batch_size={}, edge_count={}]...'.format(edge_batch_size, edge_count))
            g.E().limit(edge_batch_size).drop().toList()
            edge_count = g.E().count().next()
            has_edges = (edge_count > 0)
        has_vertices = True
        vertex_count = None
        while has_vertices:
            if vertex_count is None:
                print('clearing property graph data [vertex_batch_size={}, vertex_count=Unknown]...'.format(vertex_batch_size))
            else:
                print('clearing property graph data [vertex_batch_size={}, vertex_count={}]...'.format(vertex_batch_size, vertex_count))
            g.V().limit(vertex_batch_size).drop().toList()
            vertex_count = g.V().count().next()
            has_vertices = (vertex_count > 0)
            
    def clearSparql(self, neptune_endpoint=None, neptune_port=None):
        print('clearing rdf data...')
        sparql_endpoint = self.sparql_endpoint(neptune_endpoint, neptune_port)
        data = 'update=DROP%20ALL'
        request_parameters = sparql_endpoint.prepare_request('POST', data, headers={'Content-Type':'application/x-www-form-urlencoded'})
        req = urllib.request.Request(request_parameters.uri, data=data.encode('utf8'), headers=request_parameters.headers)
        response = urllib.request.urlopen(req, timeout=3600)
        
       

statics.load_statics(globals())

del globals()['range']
del globals()['map']

neptune = Neptune()