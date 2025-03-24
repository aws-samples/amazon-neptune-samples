# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import boto3
import json
from neo4j import Auth
from botocore.awsrequest import AWSRequest
from botocore.auth import (
    SigV4Auth,
    _host_from_url,
)


class NeptuneBoltAuthToken(Auth):
    """Creates the SigV4 auth with botl auth

    Args:
        Auth (Auth): The auth token
    """

    def __init__(self, url: str, **parameters):
        """The init function"""

        creds = boto3.Session().get_credentials().get_frozen_credentials()
        request = AWSRequest(method="GET", url=url + "/opencypher")
        request.headers.add_header("Host", _host_from_url(request.url))
        sigv4 = SigV4Auth(creds, "neptune-db", boto3.Session().region_name)
        sigv4.add_auth(request)

        auth_obj = {
            hdr: request.headers[hdr] for hdr in ["Authorization", "X-Amz-Date", "X-Amz-Security-Token", "Host"]
        }
        auth_obj["HttpMethod"] = request.method
        creds: str = json.dumps(auth_obj)
        super().__init__("basic", "username", creds, "realm", **parameters)
