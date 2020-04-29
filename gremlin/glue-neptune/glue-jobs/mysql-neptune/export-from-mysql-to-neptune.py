import sys, boto3, os

from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from awsglue.transforms import ApplyMapping
from awsglue.transforms import RenameField
from awsglue.transforms import SelectFields
from awsglue.dynamicframe import DynamicFrame
from pyspark.sql.functions import lit
from pyspark.sql.functions import format_string
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import *
from neptune_python_utils.glue_neptune_connection_info import GlueNeptuneConnectionInfo
from neptune_python_utils.glue_gremlin_client import GlueGremlinClient
from neptune_python_utils.glue_gremlin_csv_transforms import GlueGremlinCsvTransforms
from neptune_python_utils.endpoints import Endpoints
from neptune_python_utils.gremlin_utils import GremlinUtils

args = getResolvedOptions(sys.argv, ['JOB_NAME', 'DATABASE_NAME', 'TABLE_PREFIX', 'NEPTUNE_CONNECTION_NAME', 'AWS_REGION', 'CONNECT_TO_NEPTUNE_ROLE_ARN'])

sc = SparkContext()
glueContext = GlueContext(sc)
 
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

database = args['DATABASE_NAME']
product_table = '{}salesdb_product'.format(args['TABLE_PREFIX'])
product_category_table = '{}salesdb_product_category'.format(args['TABLE_PREFIX'])
supplier_table = '{}salesdb_supplier'.format(args['TABLE_PREFIX'])

gremlin_endpoints = GlueNeptuneConnectionInfo(args['AWS_REGION'], args['CONNECT_TO_NEPTUNE_ROLE_ARN']).neptune_endpoints(args['NEPTUNE_CONNECTION_NAME'])
gremlin_client = GlueGremlinClient(gremlin_endpoints)

# Product vertices

print("Creating Product vertices...")

datasource0 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = product_table, transformation_ctx = "datasource0")
datasource1 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = product_category_table, transformation_ctx = "datasource1")
datasource2 = datasource0.join( ["CATEGORY_ID"],["CATEGORY_ID"], datasource1, transformation_ctx = "join")

applymapping1 = ApplyMapping.apply(frame = datasource2, mappings = [("NAME", "string", "name:String", "string"), ("UNIT_PRICE", "decimal(10,2)", "unitPrice", "string"), ("PRODUCT_ID", "int", "productId", "int"), ("QUANTITY_PER_UNIT", "int", "quantityPerUnit:Int", "int"), ("CATEGORY_ID", "int", "category_id", "int"), ("SUPPLIER_ID", "int", "supplierId", "int"), ("CATEGORY_NAME", "string", "category:String", "string"), ("DESCRIPTION", "string", "description:String", "string"), ("IMAGE_URL", "string", "imageUrl:String", "string")], transformation_ctx = "applymapping1")
applymapping1 = GlueGremlinCsvTransforms.create_prefixed_columns(applymapping1, [('~id', 'productId', 'p'),('~to', 'supplierId', 's')])
selectfields1 = SelectFields.apply(frame = applymapping1, paths = ["~id", "name:String", "category:String", "description:String", "unitPrice", "quantityPerUnit:Int", "imageUrl:String"], transformation_ctx = "selectfields1")

selectfields1.toDF().foreachPartition(gremlin_client.upsert_vertices('Product', batch_size=100))

# Supplier vertices

print("Creating Supplier vertices...")

datasource3 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = supplier_table, transformation_ctx = "datasource3")

applymapping2 = ApplyMapping.apply(frame = datasource3, mappings = [("COUNTRY", "string", "country:String", "string"), ("ADDRESS", "string", "address:String", "string"), ("NAME", "string", "name:String", "string"), ("STATE", "string", "state:String", "string"), ("SUPPLIER_ID", "int", "supplierId", "int"), ("CITY", "string", "city:String", "string"), ("PHONE", "string", "phone:String", "string")], transformation_ctx = "applymapping1")
applymapping2 = GlueGremlinCsvTransforms.create_prefixed_columns(applymapping2, [('~id', 'supplierId', 's')])
selectfields3 = SelectFields.apply(frame = applymapping2, paths = ["~id", "country:String", "address:String", "city:String", "phone:String", "name:String", "state:String"], transformation_ctx = "selectfields3")

selectfields3.toDF().foreachPartition(gremlin_client.upsert_vertices('Supplier', batch_size=100))

# SUPPLIER edges

print("Creating SUPPLIER edges...")

applymapping1 = RenameField.apply(applymapping1, "~id", "~from")
applymapping1 = GlueGremlinCsvTransforms.create_edge_id_column(applymapping1, '~from', '~to')
selectfields2 = SelectFields.apply(frame = applymapping1, paths = ["~id", "~from", "~to"], transformation_ctx = "selectfields2")
        
selectfields2.toDF().foreachPartition(gremlin_client.upsert_edges('SUPPLIER', batch_size=100))

# End

job.commit()

print("Done")