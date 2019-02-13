import sys, boto3, os, datetime

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
from pyspark.sql.functions import col
from glue_neptune.NeptuneConnectionInfo import NeptuneConnectionInfo
from glue_neptune.NeptuneGremlinClient import NeptuneGremlinClient
from glue_neptune.GremlinCsvTransforms import GremlinCsvTransforms
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import *

'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

args = getResolvedOptions(sys.argv, ['JOB_NAME', 'DATABASE_NAME', 'TABLE_PREFIX', 'NEPTUNE_CONNECTION_NAME'])

sc = SparkContext()
glueContext = GlueContext(sc)
 
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

database = args['DATABASE_NAME']
order_table = '{}salesdb_sales_order'.format(args['TABLE_PREFIX'])
order_detail_table = '{}salesdb_sales_order_detail'.format(args['TABLE_PREFIX'])

gremlin_endpoint = NeptuneConnectionInfo(glueContext).neptune_endpoint(args['NEPTUNE_CONNECTION_NAME'])
neptune = NeptuneGremlinClient(gremlin_endpoint)

def get_last_checkpoint (client, tablename):
    conn = client.remote_connection()
    g = client.traversal_source(conn)
    checkpoint= (g.V().hasLabel('Checkpoint').has('table', tablename).fold().coalesce(
        __.unfold(),
        __.addV('Checkpoint').
        property('table', tablename).
        property('value', datetime.datetime(2015, 1, 1, 0, 0))).
    values('value').
    next())
    conn.close()
    return checkpoint
 
def update_checkpoint (client, tablename, checkpoint):
    conn = client.remote_connection()
    g = client.traversal_source(conn)
    g.V().hasLabel('Checkpoint').has('table', tablename).property(Cardinality.single, 'value', checkpoint).next()
    conn.close()
    return True
    
checkpoint = get_last_checkpoint(neptune, order_table)
newcheckpoint = checkpoint + datetime.timedelta(days=1)

print("Last checkpoint: "+ str(checkpoint))
print("New checkpoint : "+ str(newcheckpoint))

print "Creating Order vertices..."

datasource0 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = order_table, transformation_ctx = "datasource0")
df0 = datasource0.toDF().filter(col("ORDER_DATE") == checkpoint)
datasource1 = DynamicFrame.fromDF(df0, glueContext,'datasource1')

print "Total orders         : "+str(datasource0.count())
print "Orders for checkpoint: "+str(datasource1.count())

applymapping1 = ApplyMapping.apply(frame = datasource1, mappings = [("ORDER_DATE", "timestamp", "orderDate", "string"), ("SHIP_MODE", "string", "shipMode", "string"), ("SITE_ID", "double", "siteId", "int"), ("ORDER_ID", "int", "orderId", "int")], transformation_ctx = "applymapping1")
applymapping1 = GremlinCsvTransforms.create_prefixed_columns(applymapping1, [('~id', 'orderId', 'o')])
selectfields1 = SelectFields.apply(frame = applymapping1, paths = ["~id", "orderDate", "shipMode"], transformation_ctx = "selectfields1")

selectfields1.toDF().foreachPartition(neptune.add_vertices('Order'))

print "Creating OrderDetail vertices..."

datasource2 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = order_detail_table, transformation_ctx = "datasource1")
datasource3 = datasource2.join( ["ORDER_ID"],["ORDER_ID"], datasource1, transformation_ctx = "join")

print "Total order details         : "+str(datasource2.count())
print "Order details for checkpoint: "+str(datasource3.count())

applymapping2 = ApplyMapping.apply(frame = datasource3, mappings = [("DISCOUNT", "decimal(10,2)", "discount", "string"), ("UNIT_PRICE", "decimal(10,2)", "unitPrice", "string"), ("TAX", "decimal(10,2)", "tax", "string"), ("SUPPLY_COST", "decimal(10,2)", "supplyCost", "string"), ("PRODUCT_ID", "int", "productId", "int"), ("QUANTITY", "int", "quantity", "int"), ("LINE_ID", "int", "lineId", "int"), ("LINE_NUMBER", "int", "lineNumber", "int"), ("ORDER_ID", "int", "orderId", "int")], transformation_ctx = "applymapping2")
applymapping2 = GremlinCsvTransforms.create_prefixed_columns(applymapping2, [('~id', 'lineId', 'od')])
selectfields2 = SelectFields.apply(frame = applymapping2, paths = ["~id", "lineNumber", "quantity", "unitPrice", "discount", "supplyCost", "tax"], transformation_ctx = "selectfields2")

selectfields2.toDF().foreachPartition(neptune.add_vertices('OrderDetail'))

print "Creating ORDER_DETAIL edges..."

applymapping3 = RenameField.apply(applymapping2, "~id", "~to")
applymapping3 = GremlinCsvTransforms.create_prefixed_columns(applymapping3, [('~from', 'orderId', 'o')])
applymapping3 = GremlinCsvTransforms.create_edge_id_column(applymapping3, '~from', '~to')
selectfields3 = SelectFields.apply(frame = applymapping3, paths = ["~id", "~from", "~to", "lineNumber"], transformation_ctx = "selectfields3")

selectfields3.toDF().foreachPartition(neptune.add_edges('ORDER_DETAIL'))

print "Creating PRODUCT edges..."

applymapping4 = RenameField.apply(applymapping2, "~id", "~from")
applymapping4 = GremlinCsvTransforms.create_prefixed_columns(applymapping4, [('~to', 'productId', 'p')])
applymapping4 = GremlinCsvTransforms.create_edge_id_column(applymapping4, '~from', '~to')
selectfields4 = SelectFields.apply(frame = applymapping4, paths = ["~id", "~from", "~to"], transformation_ctx = "selectfields4")

selectfields4.toDF().foreachPartition(neptune.add_edges('PRODUCT'))

update_checkpoint(neptune, order_table, newcheckpoint)

job.commit()

print('Done')