'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import json
import urllib.request
import os
import time
from neptune_python_utils.endpoints import Endpoints

class BulkLoad:
    
    def __init__(self, source, format='csv', role=None, region=None, endpoints=None):
        
        self.source = source
        self.format = format
        
        if role is None:
            assert ('NEPTUNE_LOAD_FROM_S3_ROLE_ARN' in os.environ), 'role is missing.'
            self.role = os.environ['NEPTUNE_LOAD_FROM_S3_ROLE_ARN']
        else:
            self.role = role
            
        if region is None:
            assert ('AWS_REGION' in os.environ), 'region is missing.'
            self.region = os.environ['AWS_REGION']
        else:
            self.region = region
        
        if endpoints is None:
            self.endpoints = Endpoints()
        else:
            self.endpoints = endpoints
            
    def __load_from(self, source, format, role, region):
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
    
    def load_async(self):
        localised_source = self.source.replace('${AWS_REGION}', self.region)
        loader_url = self.endpoints.loader_endpoint()
        json_payload = self.__load_from(localised_source, self.format, self.role, self.region)
        print('''curl -X POST \\
    -H 'Content-Type: application/json' \\
    {} -d \'{}\''''.format(loader_url, json.dumps(json_payload, indent=4)))
        load_id = self.__load(loader_url, json_payload)
        return BulkLoadStatus(self.endpoints.load_status_endpoint(load_id))
    
    def load(self, interval=2):
        status = self.load_async()
        print('status_uri: {}'.format(status.uri()))
        status.wait(interval)

class BulkLoadStatus:
    
    def __init__(self, status_uri):
        self.status_uri = status_uri
        
    def status(self):
        req = urllib.request.Request(self.status_uri)
        response = urllib.request.urlopen(req)
        jsonresponse = json.loads(response.read().decode('utf8'))
        status = jsonresponse['payload']['overallStatus']['status']
        return (status, jsonresponse)
    
    def uri(self):
        return self.status_uri
    
    def wait(self, interval=2):
        while True:
            status, jsonresponse = self.status()
            if status == 'LOAD_COMPLETED':
                print('load completed')
                break
            if status == 'LOAD_IN_PROGRESS':
                print('loading... {} records inserted'.format(jsonresponse['payload']['overallStatus']['totalRecords']))
                time.sleep(interval)
            else:
                raise Exception(jsonresponse)