'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import json
import redis
import logging
import os
import lambda_function
from commons import *
from handler import AbstractHandler, HandlerResponse
from neptune_python_utils.gremlin_utils import GremlinUtils
from neptune_python_utils.endpoints import Endpoints

logger = logging.getLogger('NeptuneStreamHandler')
logger.setLevel(logging.INFO)

class VertexMetrics:
    
    def __init__(self, elasticache_endpoint):
        self.ec = redis.StrictRedis(host=elasticache_endpoint, port=6379)
        
    def most_popular_vertices(self):
        return self.ec.zrevrange('degree_centrality', 0, 2, withscores=True)
        
    def vertex_count(self):
        return self.ec.get('vertex_count')
        
    def set_vertex_count(self, count):
        self.ec.set('vertex_count', count)
        
    def increment_vertex_count(self):
        self.ec.incr('vertex_count')
        
    def decrement_vertex_count(self):
        self.ec.decr('vertex_count')
        
    def update_degree_centrality(self, v_id, centrality):
        self.ec.zadd('degree_centrality', {v_id:centrality})

class VertexMetricsService:
    
    def __init__(self, neptune_endpoint, elasticache_endpoint):
        GremlinUtils.init_statics(globals())
        gremlin_utils = GremlinUtils(Endpoints(neptune_endpoint=neptune_endpoint))
        self.vertext_metrics = VertexMetrics(elasticache_endpoint)
        self.neptune_connection = gremlin_utils.remote_connection()
        self.g = gremlin_utils.traversal_source(connection=self.neptune_connection)
        
    def __init_vertex_count(self):
        count = self.g.V().count().next()
        self.vertext_metrics.set_vertex_count(count)

    def __increment_vertex_count(self):
        if self.vertext_metrics.vertex_count() is None:
            self.__init_vertex_count()
        self.vertext_metrics.increment_vertex_count()
        
    def __decrement_vertex_count(self):
        if self.vertext_metrics.vertex_count() is None:
            self.__init_vertex_count()
        self.vertext_metrics.decrement_vertex_count()
        
    def __update_degree_centrality(self, v_id):
        centrality = self.g.V(v_id).inE().count().next()
        self.vertext_metrics.update_degree_centrality(v_id, centrality)
        
    def handle_event(self, op, data):
        
        type = data['type']
        if op == ADD_OPERATION:
            if type == 'vl':
                self.__increment_vertex_count()
            if type == 'e':
                self.__update_degree_centrality(data['to'])
                
        if op == REMOVE_OPERATION:
            if type == 'vl':
                self.__decrement_vertex_count()
            if type == 'e':
                self.__update_degree_centrality(data['to'])
            
        
    def close(self):
        self.neptune_connection.close()


class NeptuneStreamHandler(AbstractHandler):

    def handle_records(self, stream_log):
        
        params = json.loads(os.environ['AdditionalParams'])
        svc = VertexMetricsService(
            params['neptune_endpoint'], 
            params['elasticache_endpoint'])
        
        records = stream_log[RECORDS_STR]
        
        try:
            for record in records:
            
                svc.handle_event(record[OPERATION_STR], record[DATA_STR])
                yield HandlerResponse(
                    record[EVENT_ID_STR][OP_NUM_STR], 
                    record[EVENT_ID_STR][COMMIT_NUM_STR], 
                    1)
            
        except Exception as e:
            logger.error('Error occurred - {}'.format(str(e)))
            raise e
        finally:
            svc.close()
       
        
            
        

