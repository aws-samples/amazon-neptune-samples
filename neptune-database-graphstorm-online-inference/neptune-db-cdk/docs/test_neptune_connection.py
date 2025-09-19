USAGE = """
Tests connectivity to Neptune DB by posting a simple status query, signed with local credentials

python test_neptune_connection.py neptunedbcluster-0fbrXXXXXXXX.cluster-cu2i55XXXXXX.us-east-1.neptune.amazonaws.com" [--port 8182]
"""

import argparse
import json

import boto3
import requests
import urllib3
from requests.adapters import HTTPAdapter
from botocore.auth import SigV4Auth
from botocore.awsrequest import AWSRequest
from urllib3.util import Retry

# Disable SSL warnings since we're using self-signed cert
urllib3.disable_warnings()


def test_neptune_connection(neptune_host, endpoint="localhost", port=8182):
    """Test connectivity to Neptune DB with IAM authentication.

    Args:
        endpoint: Neptune endpoint (default: localhost)
        port: Neptune port (default: 8182)
    """
    # Configure retry strategy
    retry_strategy = Retry(
        total=3, backoff_factor=1, status_forcelist=[500, 502, 503, 504]
    )

    # Set up AWS credentials for request signing
    session = boto3.Session()
    credentials = session.get_credentials()
    region = session.region_name

    # Set up session with retry
    session = requests.Session()
    adapter = HTTPAdapter(max_retries=retry_strategy)
    session.mount("https://", adapter)
    session.verify = False

    # Neptune endpoint for Host header
    status_endpoint = f"https://{endpoint}:{port}/status"

    try:
        print(f"Testing connection to {status_endpoint}...")

        # Create request for signing
        request = AWSRequest(
            method="GET", url=status_endpoint, headers={"Host": neptune_host}
        )
        SigV4Auth(credentials, "neptune-db", region).add_auth(request)

        # Get signed headers and make request
        headers = dict(request.headers)
        headers["Host"] = neptune_host  # Ensure Host header is set
        response = session.get(status_endpoint, headers=headers)

        if response.status_code == 200:
            print("Connection successful!")
            print(f"Neptune Status: {json.dumps(response.json(), indent=2)}")
            return True
        else:
            print(f"Connection failed with status code: {response.status_code}")
            print(f"Response: {response.text}")
            return False

    except requests.exceptions.RequestException as e:
        print(f"Connection error: {str(e)}")
        print("\nTroubleshooting steps:")
        print("1. If using port forwarding, verify port forwarding is active:")
        print("  aws ssm start-session \\")
        print("    --target i-064b7add2XXXXXXXX \\")
        print("    --document-name AWS-StartPortForwardingSessionToRemoteHost \\")
        print("    --parameters '{")
        print('      "portNumber":["8182"],')
        print('      "localPortNumber":["8182"],')
        print(f'      "host":["{neptune_host}"]')
        print("    }'")
        print("2. Check AWS credentials and permissions")
        print("3. Verify security group rules")
        return False


def parse_args():
    parser = argparse.ArgumentParser("Connection test for Neptune DB.", usage=USAGE)

    parser.add_argument(
        "endpoint",
        help="Neptune endpoint to use. Use localhost if using port forwarding",
    )
    parser.add_argument(
        "--port", help="Neptune port to use. Default: 8182", default=8182, type=int
    )

    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()

    neptune_host = args.endpoint  # "neptunedbcluster-0fbrg4b3wuzy.cluster-cu2i55qhflpm.us-east-1.neptune.amazonaws.com"

    # If running on same VPC, endpoint can be same as neptune_host
    # If using port forwarding, use "localhost"
    # endpoint = neptune_host
    endpoint = "localhost"

    test_neptune_connection(
        neptune_host=neptune_host, endpoint=endpoint, port=args.port
    )
