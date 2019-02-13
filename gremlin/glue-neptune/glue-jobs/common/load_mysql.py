import sys
import os
import boto3
import botocore
import pymysql.cursors
from urlparse import urlsplit
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job

'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

LOCAL_SCRIPT_NAME = 'script.sql'

args = getResolvedOptions(sys.argv, ['BUCKET_NAME', 'KEY', 'CONNECTION_NAME'])
glueContext = GlueContext(SparkContext.getOrCreate())

def download_script(bucket, key, script_name):
    s3 = boto3.resource('s3')
    s3.Bucket(bucket).download_file(key, script_name)

def mysql_connection(connection_name):

    proxy_url = glueContext._jvm.AWSConnectionUtils.getGlueProxyUrl()
    glue_endpoint = glueContext._jvm.AWSConnectionUtils.getGlueEndpoint()
    region = glueContext._jvm.AWSConnectionUtils.getRegion()
    
    if not proxy_url[8:].startswith('null'):
        os.environ['https_proxy'] = proxy_url
        
    glue = boto3.client('glue', endpoint_url=glue_endpoint, region_name=region)
    mysql_connection = glue.get_connection(Name=connection_name)

    jdbc_url = mysql_connection['Connection']['ConnectionProperties']['JDBC_CONNECTION_URL']
    username = mysql_connection['Connection']['ConnectionProperties']['USERNAME']
    password = mysql_connection['Connection']['ConnectionProperties']['PASSWORD']
    
    netloc = urlsplit(urlsplit(jdbc_url).path).netloc
    host_and_port = netloc.split(':')
    host = host_and_port[0]
    port = int(host_and_port[1])
    
    return pymysql.connect(host=host, port=port, user=username, password=password)


def parse_sql(filename):
    
    DELIMITER = ';'
    statements = []
    statement = ''
    
    data = open(filename, 'r').readlines()
    
    for lineno, line in enumerate(data):
        if not line.strip():
            continue

        if line.startswith('--'):
            continue

        if 'DELIMITER' in line:
            DELIMITER = line.split()[1]
            continue

        if (DELIMITER not in line):
            statement += line.replace(DELIMITER, ';')
            continue

        if statement:
            statement += line
            statements.append(statement.strip())
            statement = ''
        else:
            statements.append(line.strip())
    return statements

def load_db(filename, connection_name):
    
    statements = parse_sql(filename)
    connection = mysql_connection(connection_name)

    try:
        with connection.cursor() as cursor:
            for statement in statements:
                cursor.execute(statement)
            connection.commit()
    finally:
        connection.close()
    
download_script(args['BUCKET_NAME'], args['KEY'], LOCAL_SCRIPT_NAME)
load_db(LOCAL_SCRIPT_NAME,  args['CONNECTION_NAME']) 
