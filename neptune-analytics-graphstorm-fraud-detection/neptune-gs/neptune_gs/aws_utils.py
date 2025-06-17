"""
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0

Collection of utilities to interact with AWS services
"""

import boto3
from botocore.exceptions import ClientError


def get_graph_id_by_name(graph_name: str) -> str:
    """Retrieve the unique id of a graph from its name.

    NOTE: Will return the of the first graph of given name, which
    might not be unique.

    """
    client = boto3.client("neptune-graph")
    paginator = client.get_paginator("list_graphs")

    try:
        for page in paginator.paginate():
            for graph in page["graphs"]:
                if graph["name"] == graph_name:
                    return graph["id"]
        raise ValueError(f"No graph found with name: {graph_name}")

    except ClientError as e:
        raise RuntimeError("Error retrieving graph") from e
