import json
import httplib
import logging
import boto3
from urllib2 import build_opener, HTTPHandler, Request

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def handler(event, context):
    logger.info('REQUEST RECEIVED:\n {}'.format(event))
    logger.info('REQUEST RECEIVED:\n {}'.format(context))
    if event['RequestType'] == 'Create':
        logger.info('CREATE')
        
        dbClusterId = event['ResourceProperties']['DBClusterId']
        iamRoleArn = event['ResourceProperties']['NeptuneLoadFromS3IAMRoleArn']
        
        addIamRole(dbClusterId, iamRoleArn)
        sendResponse(event, context, "SUCCESS", { "Message": "Resource creation successful!" })
    elif event['RequestType'] == 'Update':
        logger.info('UPDATE')
        sendResponse(event, context, "SUCCESS", { "Message": "Resource update successful!" })
    elif event['RequestType'] == 'Delete':
        logger.info('DELETE')
        sendResponse(event, context, "SUCCESS", { "Message": "Resource deletion successful!" })
    else:
        logger.info('FAILED!')
        sendResponse(event, context, "FAILED", { "Message": "Unexpected event received from CloudFormation" })
        
def addIamRole(dbClusterId, iamRoleArn):
    logger.info('DBClusterId: {}'.format(dbClusterId))
    logger.info('NeptuneLoadFromS3IAMRoleArn: {}'.format(iamRoleArn))
   
    client = boto3.client('neptune')
    client.add_role_to_db_cluster(
        DBClusterIdentifier=dbClusterId,
        RoleArn=iamRoleArn
    )

def sendResponse(event, context, responseStatus, responseData):
    responseBody = json.dumps({
        "Status": responseStatus,
        "Reason": "See the details in CloudWatch Log Stream: " + context.log_stream_name,
        "PhysicalResourceId": context.log_stream_name,
        "StackId": event['StackId'],
        "RequestId": event['RequestId'],
        "LogicalResourceId": event['LogicalResourceId'],
        "Data": responseData
    })


    logger.info('ResponseURL: {}'.format(event['ResponseURL']))
    logger.info('ResponseBody: {}'.format(responseBody))

    opener = build_opener(HTTPHandler)
    request = Request(event['ResponseURL'], data=responseBody)
    request.add_header('Content-Type', '')
    request.add_header('Content-Length', len(responseBody))
    request.get_method = lambda: 'PUT'
    response = opener.open(request)
    print("Status code: {}".format(response.getcode()))
    print("Status message: {}".format(response.msg))
