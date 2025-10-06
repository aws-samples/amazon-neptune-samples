import boto3
import json
import time, datetime
from botocore.config import Config


def main():
    # Configuration - Replace with your actual Neptune cluster details
    cluster_endpoint = "my-cluster-name.cluster-abcdefgh1234.my-region.neptune.amazonaws.com"
    port = 8182
    region = "my-region"
    
    # Configure Neptune client
    # This disables retries and sets the client timeout to infinite (relying on Neptune's query timeout)
    endpoint_url = f"https://{cluster_endpoint}:{port}"
    config = Config(
        region_name=region,
        retries={'max_attempts': 1},
        read_timeout=None
    )
    
    client = boto3.client("neptunedata", config=config, endpoint_url=endpoint_url)
    
    # ========================================
    # Example 1: Execute Gremlin Query
    # ========================================
    currentTime = datetime.datetime.now().isoformat()
    gremlin_query = f"""
        g.mergeV([(T.label):'person']).
            option(Merge.onCreate,[name:'alice', age:10]).
            option(Merge.onMatch,[lastUpdated:single(datetime('{currentTime}'))])
    """
    try:
        response = client.execute_gremlin_query(
            gremlinQuery=gremlin_query,
            serializer="application/vnd.gremlin-v2.0+json;types=false"
        )
        print(f"Gremlin Result: {json.dumps(response.get('result', {}), indent=2)}")
    except Exception as e:
        print(f"Gremlin query failed: {e}")

    # ========================================
    # Example 2: Execute openCypher Query
    # ========================================
    cypher_query = "MATCH (n) RETURN n LIMIT 5"
    try:
        response = client.execute_open_cypher_query(openCypherQuery=cypher_query)
        print("openCypher Results:")
        for item in response.get('results', []):
            print(f"  {json.dumps(item, indent=2)}")
    except Exception as e:
        print(f"openCypher query failed: {e}")

    # ========================================
    # Example 3: Get Property Graph Summary
    # ========================================
    try:
        response = client.get_propertygraph_summary(mode="basic")
        print(f"Graph Summary: {json.dumps(response.get('payload', {}), default=serialize_datetime, indent=2)}")
    except Exception as e:
        print(f"Property graph summary failed: {e}")

    # ========================================
    # Example 4: Bulk Loader Job
    # ========================================
    # Update with the S3 bucket where your bulk load files are located
    # Ensure the IAM role listed here is attached to your Neptune cluster:
    #     https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM-add-role-cluster.html
    s3_source = 's3://your-bucket/your-data/'
    iam_role = 'arn:aws:iam::your-account:role/NeptuneLoadFromS3'
    
    # Start loader job
    loader_response = client.start_loader_job(
        source=s3_source,
        format='csv',
        iamRoleArn=iam_role,
        s3BucketRegion=region,
        failOnError=False,
        # change to parallelism to 'OVERSUBSCRIBE' to get max load throughput
        parallelism='HIGH'
    )
    load_id = loader_response['payload']['loadId']
    print(f'Started loader job: {load_id}')
    
    # Monitor job completion
    while True:
        status_response = client.get_loader_job_status(loadId=load_id)
        job_status = status_response['payload']['overallStatus']['status']
        print(f'Job status: {job_status}')
        
        if job_status in ['LOAD_COMPLETED', 'LOAD_FAILED', 'LOAD_CANCELLED']:
            break
        
        time.sleep(10)

# Helper method for serializing datetime from property graph summary
def serialize_datetime(obj):
    if isinstance(obj, datetime.datetime):
        return obj.isoformat()
    raise TypeError("Type not serializable")

if __name__ == "__main__":
    main()