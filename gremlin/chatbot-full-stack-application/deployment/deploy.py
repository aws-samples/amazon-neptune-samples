# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import boto3
import json
import logging
import time
import io
import logging
import sys
from amazon_lex_bot_deploy import amazon_lex_bot_deploy
from aws.blog_parser.blog_parser import AwsBlogParser
from aws.comprehend.process_posts import ComprehendProcessor

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

cfn_client = boto3.client('cloudformation')
lambda_client = boto3.client('lambda')
neptune_client = boto3.client('neptune')
lex_client = boto3.client('lex-models')

url = 'https://aws.amazon.com/blogs/database/category/database/amazon-neptune/'


def main(neptune_cluster_name: str) -> int:
    # Get and choose neptune instances
    describe_db_clusters_response = neptune_client.describe_db_clusters(
        DBClusterIdentifier=neptune_cluster_name
    )
    db_cluster = describe_db_clusters_response.get('DBClusters')[0]

    cluster_endpoint = db_cluster.get('Endpoint')
    cluster_port = db_cluster.get('Port')
    cluster_resource_id = db_cluster.get('DbClusterResourceId')

    security_group_ids = list(map(lambda s: s.get(
        'VpcSecurityGroupId'), db_cluster.get('VpcSecurityGroups')))

    instance_id = db_cluster.get('DBClusterMembers')[
        0].get('DBInstanceIdentifier')

    describe_db_instances_response = neptune_client.describe_db_instances(
        DBInstanceIdentifier=instance_id
    )

    subnet_group = describe_db_instances_response.get(
        'DBInstances')[0].get('DBSubnetGroup')

    vpc_id = subnet_group.get('VpcId')

    subnet_ids = list(map(lambda s: s.get('SubnetIdentifier'),
                          subnet_group.get('Subnets')))

    # Run cloud formation tempalte with parameters
    params = [
        {
            'ParameterKey': 'SecurityGroupId',
            'ParameterValue': ",".join(security_group_ids)
        },
        {
            'ParameterKey': 'SubnetIds',
            'ParameterValue': ",".join(subnet_ids)
        },
        {
            'ParameterKey': 'ClusterEndpoint',
            'ParameterValue': cluster_endpoint
        },
        {
            'ParameterKey': 'ClusterPort',
            'ParameterValue': str(cluster_port)
        }
    ]

    # check for the stack and if it does not exist then create it
    outputs = None
    validation_lambda_arn = None
    identity_pool_id = None
    api_invoke_url = None
    logger.info('Checking Stack Status')
    try:
        stack = cfn_client.describe_stacks(
            StackName='Chatbot'
        )
    except Exception as e:
        stack = None

    if stack is None:  # stack does not exist
        logger.info('Stack does not exist, creating...')
        f = open("chatbot.yaml", "r")
        template = f.read()
        res = cfn_client.create_stack(
            StackName='Chatbot',
            TemplateBody=template,
            Capabilities=[
                'CAPABILITY_IAM'
            ],
            Parameters=params,
        )

        if 'ResponseMetadata' in res and \
                res['ResponseMetadata']['HTTPStatusCode'] < 300:
            active = True
            while active:
                time.sleep(30)
                stack_status = cfn_client.describe_stacks(
                    StackName=res['StackId'])
                logger.info(
                    f'Current stack deployment status {stack_status["Stacks"][0]["StackStatus"]}')
                if not stack_status['Stacks'][0]['StackStatus'] == 'CREATE_IN_PROGRESS':
                    active = False
                    outputs = stack_status['Stacks'][0]['Outputs']
        else:
            logger.error(
                "There was an Unexpected error. response: {0}".format(json.dumps(res)))
    else:
        outputs = stack['Stacks'][0]['Outputs']

    logger.info('Stack Exists, fetching the output parameters')
    for o in outputs:
        if o['OutputKey'] == 'ValidationLambdaARN':
            validation_lambda_arn = o['OutputValue']
        if o['OutputKey'] == 'ApiGatewayInvokeURL':
            api_invoke_url = o['OutputValue']
        if o['OutputKey'] == 'IdentityPoolId':
            identity_pool_id = o['OutputValue']

    logger.info('Beginning Lex Deployment')
    # Create the Lex bot
    amazon_lex_bot_deploy.lex_deploy(
        lex_schema_file='blog_chatbot_Export.json', lambda_endpoint=validation_lambda_arn)

    lex_bot = lex_client.get_bot(name='blog_chatbot', versionOrAlias='$LATEST')

    logger.info(f'Completed Lex Deployment')
    logger.info(f'Beginning Data Loading')
    logger.info(f'Retrieving Blog Posts for {url}')
    results = AwsBlogParser(url).parse()
    logger.info('Creating Processor for Amazon Comprehend')
    comprehend = ComprehendProcessor()
    for r in results:
        logger.info(f'Processing post {r["title"]}')
        comprehend.process(r)
        response = lambda_client.invoke(
            FunctionName="chatbot-database-loader",
            InvocationType='Event',
            Payload=json.dumps(r)
        )

    logger.info(f'Deployment Complete')
    logger.info(f'*******************')
    logger.info(
        f'Copy the text below into the config.json in the web-ui project')
    logger.info(f'*******************')
    logger.info(f'"SERVER_URL": "{api_invoke_url}"')
    logger.info(f'*******************')
    logger.info(
        f'Copy the text below into the aws-exports.js in the web-ui project')
    logger.info(f'aws_project_region: "{boto3.session.Session().region_name}"')
    logger.info(f'aws_cognito_identity_pool_id: "{identity_pool_id}"')
    logger.info(f'aws_cognito_region: "{boto3.session.Session().region_name}"')
    logger.info(f'region: "{boto3.session.Session().region_name}"')
    logger.info(f'*******************')

    exit(0)


if len(sys.argv) != 2:
    print("You must specify a command line arguement for the database name")
    exit(0)
main(sys.argv[1])
