# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import json
import os
import logging
from writer import NeptuneWriter

logger = logging.getLogger()
logger.setLevel(logging.INFO)


def main(results: str, server: str, port: int, ssl: bool) -> int:
    logger.info(f'Creating Neptune Writer {server}')
    writer = NeptuneWriter(server, port, ssl)
    logger.info(f'Connection created')

    logger.info(f'Saving post')
    writer.process_post(results)


def lambda_handler(event, context):
    """Main handler"""
    logger.info('******')
    logger.info(json.dumps(event))
    logger.info('******')
    # Connect to Neptune
    server = os.environ["NEPTUNE_ENDPOINT"]
    port = os.environ["NEPTUNE_PORT"]
    main(results=event, server=server, port=port, ssl=True)
    return True
