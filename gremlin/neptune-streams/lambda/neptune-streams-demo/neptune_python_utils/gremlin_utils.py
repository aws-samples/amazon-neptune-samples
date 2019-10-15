'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import os
import uuid

from neptune_python_utils.endpoints import Endpoints
from gremlin_python.driver.client import Client
from gremlin_python.driver.serializer import Processor, GraphSONMessageSerializer
from gremlin_python.structure.io import graphsonV3d0
from gremlin_python.driver import request
from gremlin_python.process import traversal
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import *
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import *
from tornado.httpclient import HTTPError
        
class GremlinUtils:
    
    @classmethod
    def init_statics(cls, globals):
        
        statics.load_statics(globals)

        del globals['range']
        del globals['map']
        del globals['min']
        del globals['sum']
        del globals['property']
        del globals['max']
    
    def __init__(self, endpoints=None):
                
        if endpoints is None:
            self.endpoints = Endpoints()
        else:
            self.endpoints = endpoints
            
    def remote_connection(self, show_endpoint=True):
        neptune_gremlin_endpoint = self.endpoints.gremlin_endpoint()
        if show_endpoint:
            print('gremlin: {}'.format(neptune_gremlin_endpoint))
        retry_count = 0
        while True:
            try:
                return DriverRemoteConnection(neptune_gremlin_endpoint, 'g')
            except HTTPError as e:
                exc_info = sys.exc_info()
                if retry_count < 3:
                    retry_count+=1
                    print('Connection timeout. Retrying...')
                else:
                    raise exc_info[0].with_traceback(exc_info[1], exc_info[2])
                    
    def traversal_source(self, show_endpoint=True, connection=None):
        if connection is None:
            connection = self.remote_connection(show_endpoint)
        return traversal().withRemote(connection)
    
    def client(self, pool_size=None, max_workers=None):
        return Client(self.endpoints.gremlin_endpoint(), 'g', pool_size=pool_size, max_workers=max_workers)
        
    def sessioned_client(self, session_id=None, pool_size=None, max_workers=None):
        return SessionedClient(
            self.endpoints.gremlin_endpoint(), 
            'g', 
            uuid.uuid4().hex if session_id is None else session_id,
            pool_size=pool_size, 
            max_workers=max_workers)
            
        
class Session(Processor):

    def authentication(self, args):
        return args

    def eval(self, args):
        return args
    
    def close(self, args):
        return args
    
class ExtendedGraphSONSerializersV3d0(GraphSONMessageSerializer):
     
    def __init__(self):
        reader = graphsonV3d0.GraphSONReader()
        writer = graphsonV3d0.GraphSONWriter()
        version = b"application/vnd.gremlin-v3.0+json"
        super(ExtendedGraphSONSerializersV3d0, self).__init__(reader, writer, version)
        self.session = Session(writer)

class SessionedClient(Client):
    
    def __init__(self, url, traversal_source, session_id, protocol_factory=None,
                 transport_factory=None, pool_size=None, max_workers=None,
                 message_serializer=ExtendedGraphSONSerializersV3d0(), username="", password=""):
        super(SessionedClient, self).__init__(url, traversal_source, protocol_factory,
                 transport_factory, pool_size, max_workers,
                 message_serializer, username, password)
        self._session_id = session_id
        
        
    def submitAsync(self, message, bindings=None):
        if isinstance(message, str):
            message = request.RequestMessage(
                processor='session', 
                op='eval',
                args={'gremlin': message,
                      'aliases': {'g': self._traversal_source},
                      'session': self._session_id,
                      'manageTransaction': False})
            if bindings:
                message.args.update({'bindings': bindings})
        else:
            raise Exception('Unsupported message type: {}'.format(type(message)))
        conn = self._pool.get(True)
        return conn.write(message)
    
    def close(self):
        message = request.RequestMessage(
                processor='session', 
                op='close',
                args={'session': self._session_id,
                      'manageTransaction': False,
                      'force': False})
        conn = self._pool.get(True)
        conn.write(message).result()
        super(SessionedClient, self).close()
        
        