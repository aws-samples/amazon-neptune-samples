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
import boto3

def lambda_handler(event, context):
    
    operation = event['operation']

    client = boto3.client('lambda')
    
    config = client.get_function_configuration(
        FunctionName=os.environ['WORKLOAD_ARN']
    )
    
    env_vars = config['Environment']
    
    if operation == 'start':
        print('Starting workload')
        env_vars['Variables']['IS_ACTIVE'] = 'True'
        client.update_function_configuration(
            FunctionName=os.environ['WORKLOAD_ARN'],
            Environment=env_vars
        )
        client.invoke(
            FunctionName=os.environ['WORKLOAD_ARN'],
            InvocationType='Event'
        )
    else:
        print('Stopping workload')
        env_vars['Variables']['IS_ACTIVE'] = 'False'
        client.update_function_configuration(
            FunctionName=os.environ['WORKLOAD_ARN'],
            Environment=env_vars
        )

