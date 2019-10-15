'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

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

