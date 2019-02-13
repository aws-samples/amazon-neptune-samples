import sys
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
from glue_neptune.GremlinCsvTransforms import GremlinCsvTransforms

'''
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

args = getResolvedOptions(sys.argv, ['JOB_NAME', 'S3_OUTPUT_PATH', 'DATABASE_NAME', 'TABLE_PREFIX'])

sc = SparkContext()
glueContext = GlueContext(sc)

job = Job(glueContext)
job.init(args['JOB_NAME'], args)

nodes_path = '{}nodes'.format(args['S3_OUTPUT_PATH'])
edges_path = '{}edges'.format(args['S3_OUTPUT_PATH'])

database = args['DATABASE_NAME']
product_table = '{}salesdb_product'.format(args['TABLE_PREFIX'])
product_category_table = '{}salesdb_product_category'.format(args['TABLE_PREFIX'])
supplier_table = '{}salesdb_supplier'.format(args['TABLE_PREFIX'])
    
def writeCsvFile(datasource, path):
    dataframe = DynamicFrame.toDF(datasource).repartition(1)
    datasource = DynamicFrame.fromDF(dataframe, glueContext, 'write-csv')
    glueContext.write_dynamic_frame.from_options(frame = datasource, connection_type = "s3", connection_options = {"path": path}, format = "csv", transformation_ctx = "write-csv")  

# Product vertices

print "Creating Product vertices..."

datasource0 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = product_table, transformation_ctx = "datasource0")
datasource1 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = product_category_table, transformation_ctx = "datasource1")
datasource2 = datasource0.join( ["CATEGORY_ID"],["CATEGORY_ID"], datasource1, transformation_ctx = "join")

applymapping1 = ApplyMapping.apply(frame = datasource2, mappings = [("NAME", "string", "name:String", "string"), ("UNIT_PRICE", "decimal(10,2)", "unitPrice", "decimal(10,2)"), ("PRODUCT_ID", "int", "productId", "int"), ("QUANTITY_PER_UNIT", "int", "quantityPerUnit:Int", "int"), ("CATEGORY_ID", "int", "category_id", "int"), ("SUPPLIER_ID", "int", "supplierId", "int"), ("CATEGORY_NAME", "string", "category:String", "string"), ("DESCRIPTION", "string", "description:String", "string"), ("IMAGE_URL", "string", "imageUrl:String", "string")], transformation_ctx = "applymapping1")
applymapping1 = GremlinCsvTransforms.create_prefixed_columns(applymapping1, [('~id', 'productId', 'p'),('~to', 'supplierId', 's')])
selectfields1 = SelectFields.apply(frame = applymapping1, paths = ["~id", "name:String", "category:String", "description:String", "unitPrice", "quantityPerUnit:Int", "imageUrl:String"], transformation_ctx = "selectfields1")

writeCsvFile(GremlinCsvTransforms.addLabel(selectfields1, 'Product'), nodes_path)

# SUPPLIER edges

print "Creating SUPPLIER edges..."

applymapping1 = RenameField.apply(applymapping1, "~id", "~from")
applymapping1 = GremlinCsvTransforms.create_edge_id_column(applymapping1, '~from', '~to')
selectfields2 = SelectFields.apply(frame = applymapping1, paths = ["~id", "~from", "~to"], transformation_ctx = "selectfields2")

writeCsvFile(GremlinCsvTransforms.addLabel(selectfields2, 'SUPPLIER'), edges_path)

# Supplier vertices

print "Creating Supplier vertices..."

datasource3 = glueContext.create_dynamic_frame.from_catalog(database = database, table_name = supplier_table, transformation_ctx = "datasource3")

applymapping2 = ApplyMapping.apply(frame = datasource3, mappings = [("COUNTRY", "string", "country:String", "string"), ("ADDRESS", "string", "address:String", "string"), ("NAME", "string", "name:String", "string"), ("STATE", "string", "state:String", "string"), ("SUPPLIER_ID", "int", "supplierId", "int"), ("CITY", "string", "city:String", "string"), ("PHONE", "string", "phone:String", "string")], transformation_ctx = "applymapping1")
applymapping2 = GremlinCsvTransforms.create_prefixed_columns(applymapping2, [('~id', 'supplierId', 's')])
selectfields3 = SelectFields.apply(frame = applymapping2, paths = ["~id", "country:String", "address:String", "city:String", "phone:String", "name:String", "state:String"], transformation_ctx = "selectfields3")

writeCsvFile(GremlinCsvTransforms.addLabel(selectfields3, 'Supplier'), nodes_path)

# End

job.commit()

print "Done"