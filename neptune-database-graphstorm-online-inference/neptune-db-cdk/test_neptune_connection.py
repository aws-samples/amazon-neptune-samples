import requests
from requests.adapters import HTTPAdapter
from urllib3.util import Retry
import json
import urllib3
import boto3
from botocore.auth import SigV4Auth
from botocore.awsrequest import AWSRequest

# Disable SSL warnings since we're using self-signed cert
urllib3.disable_warnings()


def test_neptune_connection(neptune_endpoint, port=8182):
    """Test connectivity to Neptune DB with IAM authentication.

    Args:
        neptune_endpoint: Neptune cluster endpoint (e.g., your-cluster.region.neptune.amazonaws.com)
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

    # Neptune endpoint URL
    status_endpoint = f"https://{neptune_endpoint}:{port}/status"

    try:
        print(f"Testing connection to {status_endpoint}...")

        # Create request for signing
        request = AWSRequest(
            method="GET", url=status_endpoint, headers={"Host": neptune_endpoint}
        )
        SigV4Auth(credentials, "neptune-db", region).add_auth(request)

        # Get signed headers and make request
        headers = dict(request.headers)
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
        print("1. Verify VPC connectivity:")
        print("   - Check security group rules")
        print("   - Verify VPC endpoints are configured")
        print("2. Check AWS credentials and permissions")
        print("3. Ensure IAM authentication is enabled")
        return False


if __name__ == "__main__":
    # Replace with your Neptune endpoint
    neptune_endpoint = "your-cluster.region.neptune.amazonaws.com"
    test_neptune_connection(neptune_endpoint=neptune_endpoint)
