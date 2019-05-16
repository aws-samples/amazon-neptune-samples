from __future__  import print_function
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import *
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import *
from SPARQLWrapper import SPARQLWrapper, JSON, XML
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

class Neptune:

    def remoteConnection(self, neptune_endpoint=None, neptune_port=None, show_endpoint=True):
        neptune_gremlin_endpoint = self.gremlin_endpoint(neptune_endpoint, neptune_port)
        if show_endpoint:
            print('gremlin: ' + neptune_gremlin_endpoint)
        retry_count = 0
        while True:
            try:
                return DriverRemoteConnection(neptune_gremlin_endpoint,'g')
            except HTTPError as e:
                exc_info = sys.exc_info()
                if retry_count < 3:
                    retry_count+=1
                    print('Connection timeout. Retrying...')
                else:
                    raise exc_info[0].with_traceback(exc_info[1], exc_info[2])
    
    def graphTraversal(self, neptune_endpoint=None, neptune_port=None, show_endpoint=True, connection=None):
        if connection is None:
            connection = self.remoteConnection(neptune_endpoint, neptune_port, show_endpoint)
        return traversal().withRemote(connection)
    
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
        wrapper = SPARQLWrapper(self.sparql_endpoint(neptune_endpoint, neptune_port))
        wrapper.setReturnFormat(JSON)
        wrapper.method = 'POST'
        qry = """
            DROP ALL
        """
        wrapper.setQuery(qry)
        warnings.filterwarnings('ignore')
        wrapper.query().convert()
        warnings.resetwarnings()
    
    def __loadFrom(self, source, format, role, region):
        return { 
              'source' : source, 
              'format' : format,  
              'iamRoleArn' : role, 
              'region' : region, 
              'failOnError' : 'FALSE'
            }
    
    def __load(self, loader_url, data):    
        jsondataasbytes = json.dumps(data).encode('utf8')
        req = urllib.request.Request(loader_url, data=jsondataasbytes, headers={'Content-Type': 'application/json'})
        response = urllib.request.urlopen(req)
        jsonresponse = json.loads(response.read().decode('utf8'))
        return jsonresponse['payload']['loadId']
    
    def __wait(self, status_url, interval):
        while True:
            status, jsonresponse = self.bulkLoadStatus(status_url)
            if status == 'LOAD_COMPLETED':
                print('load completed')
                break
            if status == 'LOAD_IN_PROGRESS':
                print('loading... {} records inserted'.format(jsonresponse['payload']['overallStatus']['totalRecords']))
                time.sleep(interval)
            else:
                raise Exception(jsonresponse)
        
    def bulkLoadAsync(self, source, format='csv', role=None, region=None, neptune_endpoint=None, neptune_port=None):
        if role is None:
            role = os.environ['NEPTUNE_LOAD_FROM_S3_ROLE_ARN']
        if region is None:
            region = os.environ['AWS_REGION']
        source = source.replace('${AWS_REGION}', region)
        loader_url = self.loader_endpoint(neptune_endpoint, neptune_port)
        json_payload = self.__loadFrom(source, format, role, region)
        print('''curl -X POST \\
    -H 'Content-Type: application/json' \\
    {} -d \'{}\''''.format(loader_url, json.dumps(json_payload, indent=4)))
        load_id = self.__load(loader_url, json_payload)
        return loader_url + '/' + load_id
    
    def bulkLoad(self, source, format='csv', role=None, region=None, neptune_endpoint=None, neptune_port=None, interval=2):
        status_url = self.bulkLoadAsync(source, format, role, region, neptune_endpoint, neptune_port)
        print('status_url: {}'.format(status_url))
        self.__wait(status_url, interval)
        
    def bulkLoadStatus(self, status_url):
        req = urllib.request.Request(status_url)
        response = urllib.request.urlopen(req)
        jsonresponse = json.loads(response.read().decode('utf8'))
        status = jsonresponse['payload']['overallStatus']['status']
        return (status, jsonresponse)
        
    def gremlin_endpoint(self, neptune_endpoint=None, neptune_port=None):
        return self.__endpoint('ws', self.__neptune_endpoint(neptune_endpoint), self.__neptune_port(neptune_port), 'gremlin')
    
    def sparql_endpoint(self, neptune_endpoint=None, neptune_port=None):
        return self.__endpoint('http', self.__neptune_endpoint(neptune_endpoint), self.__neptune_port(neptune_port), 'sparql')
    
    def loader_endpoint(self, neptune_endpoint=None, neptune_port=None):
        return self.__endpoint('http', self.__neptune_endpoint(neptune_endpoint), self.__neptune_port(neptune_port), 'loader')
    
    def __endpoint(self, protocol, neptune_endpoint, neptune_port, suffix):
        return '{}://{}:{}/{}'.format(protocol, neptune_endpoint, neptune_port, suffix)
        
    def __neptune_endpoint(self, neptune_endpoint=None):
        if neptune_endpoint is None:
            neptune_endpoint = os.environ['NEPTUNE_CLUSTER_ENDPOINT']
        return neptune_endpoint
            
    def __neptune_port(self, neptune_port=None):
        if neptune_port is None:
            neptune_port = os.environ['NEPTUNE_CLUSTER_PORT']
        return neptune_port
        

statics.load_statics(globals())

del globals()['range']
del globals()['map']

neptune = Neptune()