'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import json
import redis
import os
import boto3
import random
from neptune_python_utils.gremlin_utils import GremlinUtils
        
class MessageNetwork:
    
    @classmethod
    def user_id(cls, i):
        return 'user-{}'.format(i)
    
    def __init__(self):
        GremlinUtils.init_statics(globals())
        gremlin_utils = GremlinUtils()
        self.neptune_connection = gremlin_utils.remote_connection()
        self.g = gremlin_utils.traversal_source(connection=self.neptune_connection)
        
    def close(self):
        self.neptune_connection.close()
        
    def add_user(self, user_id):
        return (self.g.V(user_id).fold().coalesce(
          unfold(), 
          addV('User').
            property(id, user_id).
            property('firstname', '{}-first-name'.format(user_id)).
            property('lastname', '{}-last-name'.format(user_id)).
            property('age', random.randint(18, 50))).
          next())
        
    def get_highest_vertex_id(self):
        return self.g.V('highest_vertex_id').fold().coalesce(unfold().values('value'), constant(0)).next()
        
    def set_highest_vertex_id(self, highest_vertex_id):
        return self.g.V('highest_vertex_id').fold().coalesce(unfold(),addV().property(id, 'highest_vertex_id')).property(single, 'value', highest_vertex_id).next()
        
    def user_messaged_user(self, from_id, to_id):
        return self.g.V(from_id).addE('MESSAGED').to(V(to_id)).next()
        

def lambda_handler(event, context):
    
    message_network = MessageNetwork()
    
    highest_vertex_id = message_network.get_highest_vertex_id() if 'highestVertexId' not in event else event['highestVertexId']
    
    batch_size = int(os.environ['BATCH_SIZE'])
    index = 0
    
    while os.environ['IS_ACTIVE'] == 'True' and index < batch_size:
        
        if (highest_vertex_id < 1000):
            highest_vertex_id += 1
            message_network.add_user(MessageNetwork.user_id(highest_vertex_id))
        else:
            if random.randint(1,4) == 1:
                highest_vertex_id += 1
                message_network.add_user(MessageNetwork.user_id(highest_vertex_id))
            else:
                from_id = random.randint(1, highest_vertex_id)
                to_id = from_id
                while to_id == from_id:
                    to_id = random.randint(1, highest_vertex_id)
                message_network.user_messaged_user(MessageNetwork.user_id(from_id), MessageNetwork.user_id(to_id))
            
        index += 1
    
    message_network.set_highest_vertex_id(highest_vertex_id)
    message_network.close()
    
    print('highestVertexId: {}'.format(highest_vertex_id))
    
    if os.environ['IS_ACTIVE'] == 'True':
        
        payload = '{{"highestVertexId": {}}}'.format(highest_vertex_id)
        
        client = boto3.client('lambda')
        client.invoke(
            FunctionName=context.invoked_function_arn,
            InvocationType='Event',
            Payload=payload.encode()
        )