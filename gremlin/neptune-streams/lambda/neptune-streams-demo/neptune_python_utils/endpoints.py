# Copyright 2019 Amazon.com, Inc. or its affiliates.
# All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#    http://aws.amazon.com/apache2.0/
#
# or in the "license" file accompanying this file.
# This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. See the License for the specific language governing permissions
# and limitations under the License.

import os

class Endpoints:
    
    def __init__(self, neptune_endpoint=None, neptune_port=None):
        if neptune_endpoint is None:
            assert ('NEPTUNE_CLUSTER_ENDPOINT' in os.environ), 'neptune_endpoint is missing.'
            self.neptune_endpoint = os.environ['NEPTUNE_CLUSTER_ENDPOINT']
        else:
            self.neptune_endpoint = neptune_endpoint
            
        if neptune_port is None:
            self.neptune_port = 8182 if 'NEPTUNE_CLUSTER_PORT' not in os.environ else os.environ['NEPTUNE_CLUSTER_PORT']
        else:
            self.neptune_port = neptune_port
            
    def gremlin_endpoint(self):
        return self.__endpoint('wss', self.neptune_endpoint, self.neptune_port, 'gremlin')
    
    def sparql_endpoint(self):
        return self.__endpoint('https', self.neptune_endpoint, self.neptune_port, 'sparql')
    
    def loader_endpoint(self):
        return self.__endpoint('https', self.neptune_endpoint, self.neptune_port, 'loader')
    
    def load_status_endpoint(self, load_id):
        return '{}/{}'.format(self.loader_endpoint(), load_id)
        
    def status_endpoint(self):
        return self.__endpoint('https', self.neptune_endpoint, self.neptune_port, 'status')
        
    def gremlin_stream_endpoint(self):
        return self.__endpoint('https', self.neptune_endpoint, self.neptune_port, 'gremlin/stream')
        
    def sparql_stream_endpoint(self):
        return self.__endpoint('https', self.neptune_endpoint, self.neptune_port, 'sparql/stream')
    
    def __endpoint(self, protocol, neptune_endpoint, neptune_port, suffix):
        return '{}://{}:{}/{}'.format(protocol, neptune_endpoint, neptune_port, suffix)
    