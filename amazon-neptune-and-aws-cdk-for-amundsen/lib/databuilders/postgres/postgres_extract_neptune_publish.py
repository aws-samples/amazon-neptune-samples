# Copyright Contributors to the Amundsen project.
# SPDX-License-Identifier: Apache-2.0

"""
This script extracts data from a postgres database and loads into Neptune and Elasticsearch without using an Airflow DAG.
"""

import logging
import os
import uuid
import boto3
import textwrap

from elasticsearch import Elasticsearch
from pyhocon import ConfigFactory
from sqlalchemy.ext.declarative import declarative_base

from databuilder.clients.neptune_client import NeptuneSessionClient
from databuilder.extractor.postgres_metadata_extractor import PostgresMetadataExtractor
from databuilder.extractor.es_last_updated_extractor import EsLastUpdatedExtractor
from databuilder.extractor.neptune_search_data_extractor import NeptuneSearchDataExtractor
from databuilder.extractor.sql_alchemy_extractor import SQLAlchemyExtractor

from databuilder.job.job import DefaultJob
from databuilder.loader.file_system_elasticsearch_json_loader import FSElasticsearchJSONLoader
from databuilder.loader.file_system_neptune_csv_loader import FSNeptuneCSVLoader
from databuilder.publisher.elasticsearch_constants import (
    DASHBOARD_ELASTICSEARCH_INDEX_MAPPING, USER_ELASTICSEARCH_INDEX_MAPPING,
)
from databuilder.publisher.elasticsearch_publisher import ElasticsearchPublisher
from databuilder.publisher.neptune_csv_publisher import NeptuneCSVPublisher
from databuilder.task.task import DefaultTask
from databuilder.transformer.base_transformer import ChainedTransformer, NoopTransformer
from databuilder.transformer.dict_to_model import MODEL_CLASS, DictToModel
from databuilder.transformer.generic_transformer import (
    CALLBACK_FUNCTION, FIELD_NAME, GenericTransformer,
)

es_host = os.getenv('ES_HOST')
assert es_host, 'ES_HOST environment variable must be set'
neptune_host = os.getenv('NEPTUNE_HOST')
assert neptune_host, 'NEPTUNE_HOST environment variable must be set'
neptune_port = os.getenv('NEPTUNE_PORT', 8182)
neptune_iam_role_name = os.getenv('NEPTUNE_IAM_ROLE','amundsen-dev-neptune-role')
S3_BUCKET_NAME = os.getenv('S3_BUCKET_NAME')
assert S3_BUCKET_NAME, 'A S3 bucket is needed to load the data from'
S3_DATA_PATH = os.getenv('S3_DATA_PATH', 'amundsen_data')
AWS_REGION = os.environ.get('AWS_REGION')
assert AWS_REGION, 'AWS_REGION environment variable must be set'
postgres_host = os.getenv('POSTGRES_HOST')
assert postgres_host, 'POSTGRES_HOST environment variable must be set'
postgres_port = os.getenv('POSTGRES_PORT','5432')
postgres_user = os.getenv('POSTGRES_USER','postgres')
postgres_password = os.getenv('POSTGRES_PASSWORD')
assert postgres_password, 'POSTGRES_PASSWORD environment variable must be set'
postgres_database = os.getenv('POSTGRES_DATABASE','sample')
postgres_schema = os.getenv('POSTGRES_SCHEMA','chatbot')

es = Elasticsearch(
    '{}'.format(es_host),
    scheme="https",
    port=443,
)

Base = declarative_base()

NEPTUNE_ENDPOINT = '{}:{}'.format(neptune_host, neptune_port)

LOGGER = logging.getLogger(__name__)

def connection_string():
    return "postgresql://%s:%s@%s:%s/%s" % (postgres_user, postgres_password, postgres_host, postgres_port, postgres_database)

def run_postgres_job(job_name):
    tmp_folder = '/var/tmp/amundsen/{job_name}'.format(job_name=job_name)
    node_files_folder = '{tmp_folder}/nodes'.format(tmp_folder=tmp_folder)
    relationship_files_folder = '{tmp_folder}/relationships'.format(tmp_folder=tmp_folder)

    loader = FSNeptuneCSVLoader()
    publisher = NeptuneCSVPublisher()

    where_clause_suffix = textwrap.dedent(
    """
        where table_schema = '{}'
    """.format(postgres_schema)
    )

    job_config = ConfigFactory.from_dict({
        f'extractor.postgres_metadata.{PostgresMetadataExtractor.WHERE_CLAUSE_SUFFIX_KEY}': where_clause_suffix,
        f'extractor.postgres_metadata.{PostgresMetadataExtractor.USE_CATALOG_AS_CLUSTER_NAME}': True,
        f'extractor.postgres_metadata.extractor.sqlalchemy.{SQLAlchemyExtractor.CONN_STRING}': connection_string(),
        loader.get_scope(): {
            FSNeptuneCSVLoader.NODE_DIR_PATH: node_files_folder,
            FSNeptuneCSVLoader.RELATION_DIR_PATH: relationship_files_folder,
            FSNeptuneCSVLoader.SHOULD_DELETE_CREATED_DIR: True,
            FSNeptuneCSVLoader.JOB_PUBLISHER_TAG: 'unique_tag'
        },
        publisher.get_scope(): {
            NeptuneCSVPublisher.NODE_FILES_DIR: node_files_folder,
            NeptuneCSVPublisher.RELATION_FILES_DIR: relationship_files_folder,
            NeptuneCSVPublisher.AWS_S3_BUCKET_NAME: S3_BUCKET_NAME,
            NeptuneCSVPublisher.AWS_BASE_S3_DATA_PATH: S3_DATA_PATH,
            NeptuneCSVPublisher.NEPTUNE_HOST: NEPTUNE_ENDPOINT,
            NeptuneCSVPublisher.AWS_IAM_ROLE_NAME: neptune_iam_role_name,
            NeptuneCSVPublisher.AWS_REGION: AWS_REGION
        },
    })

    DefaultJob(
        conf=job_config,
        task=DefaultTask(extractor=PostgresMetadataExtractor(), loader=loader),
        publisher=publisher).launch()

def _str_to_list(str_val):
    return str_val.split(',')


def create_es_publisher_sample_job(elasticsearch_index_alias='table_search_index',
                                   elasticsearch_doc_type_key='table',
                                   model_name='databuilder.models.table_elasticsearch_document.TableESDocument',
                                   entity_type='table',
                                   elasticsearch_mapping=None):
    """
    :param elasticsearch_index_alias:  alias for Elasticsearch used in
                                       amundsensearchlibrary/search_service/config.py as an index
    :param elasticsearch_doc_type_key: name the ElasticSearch index is prepended with. Defaults to `table` resulting in
                                       `table_{uuid}`
    :param model_name:                 the Databuilder model class used in transporting between Extractor and Loader
    :param entity_type:                Entity type handed to the `Neo4jSearchDataExtractor` class, used to determine
                                       Cypher query to extract data from Neo4j. Defaults to `table`.
    :param elasticsearch_mapping:      Elasticsearch field mapping "DDL" handed to the `ElasticsearchPublisher` class,
                                       if None is given (default) it uses the `Table` query baked into the Publisher
    """
    # loader saves data to this location and publisher reads it from here
    extracted_search_data_path = '/var/tmp/amundsen/search_data.json'
    loader = FSElasticsearchJSONLoader()
    extractor = NeptuneSearchDataExtractor()

    task = DefaultTask(
        loader=loader,
        extractor=extractor,
        transformer=NoopTransformer()
    )

    # elastic search client instance
    elasticsearch_client = es
    # unique name of new index in Elasticsearch
    elasticsearch_new_index_key = '{}_'.format(elasticsearch_doc_type_key) + str(uuid.uuid4())
    publisher = ElasticsearchPublisher()
    session = boto3.Session()
    aws_creds = session.get_credentials()
    aws_access_key = aws_creds.access_key
    aws_access_secret = aws_creds.secret_key
    aws_token = aws_creds.token

    job_config = ConfigFactory.from_dict({
        extractor.get_scope(): {
            NeptuneSearchDataExtractor.ENTITY_TYPE_CONFIG_KEY: entity_type,
            NeptuneSearchDataExtractor.MODEL_CLASS_CONFIG_KEY: model_name,
            'neptune.client': {
                NeptuneSessionClient.NEPTUNE_HOST_NAME: NEPTUNE_ENDPOINT,
                NeptuneSessionClient.AWS_REGION: AWS_REGION,
                NeptuneSessionClient.AWS_ACCESS_KEY: aws_access_key,
                NeptuneSessionClient.AWS_SECRET_ACCESS_KEY: aws_access_secret,
                NeptuneSessionClient.AWS_SESSION_TOKEN: aws_token
            }
        },
        'loader.filesystem.elasticsearch.file_path': extracted_search_data_path,
        'loader.filesystem.elasticsearch.mode': 'w',
        publisher.get_scope(): {
            'file_path': extracted_search_data_path,
            'mode': 'r',
            'client': elasticsearch_client,
            'new_index': elasticsearch_new_index_key,
            'doc_type': elasticsearch_doc_type_key,
            'alias': elasticsearch_index_alias
        }
    })

    # only optionally add these keys, so need to dynamically `put` them
    if elasticsearch_mapping:
        job_config.put('publisher.elasticsearch.{}'.format(ElasticsearchPublisher.ELASTICSEARCH_MAPPING_CONFIG_KEY),
                       elasticsearch_mapping)

    job = DefaultJob(
        conf=job_config,
        task=task,
        publisher=ElasticsearchPublisher()
    )
    return job


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    LOGGER.info('ES Host: ' +  es_host);    
    LOGGER.info('Neptune Host: ' + neptune_host);
    LOGGER.info('Neptune Port: ' + str(neptune_port));
    LOGGER.info('Neptune IAM Role Name: ' + neptune_iam_role_name);
    LOGGER.info('S3 Bucket Name: ' + S3_BUCKET_NAME);
    LOGGER.info('S3 Data Path: ' + S3_DATA_PATH);
    LOGGER.info('AWS Region: ' + AWS_REGION);
    LOGGER.info('Postgres Host: ' + postgres_host);
    LOGGER.info('Postgres Port: ' + postgres_port);
    LOGGER.info('Postgres User: ' + postgres_user);
    #LOGGER.info('Postgres Password: ' + postgres_password);
    LOGGER.info('Postgres Database: ' + postgres_database);
    LOGGER.info('Postgres Schema: ' + postgres_schema);

    run_postgres_job('amundsen_blog_postgres_sample');

    job_es_table = create_es_publisher_sample_job(
        elasticsearch_index_alias='table_search_index',
        elasticsearch_doc_type_key='table',
        entity_type='table',
        model_name='databuilder.models.table_elasticsearch_document.TableESDocument')
    job_es_table.launch()

    job_es_user = create_es_publisher_sample_job(
        elasticsearch_index_alias='user_search_index',
        elasticsearch_doc_type_key='user',
        model_name='databuilder.models.user_elasticsearch_document.UserESDocument',
        entity_type='user',
        elasticsearch_mapping=USER_ELASTICSEARCH_INDEX_MAPPING
    )
    job_es_user.launch()
