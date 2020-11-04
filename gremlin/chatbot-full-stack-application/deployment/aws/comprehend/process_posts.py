# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import boto3
import sys
import json
import argparse
import logging
import unicodedata


class ComprehendProcessor:
    # Global Variables
    # Array containing the HTML of each blogs paginated blog post titles and previews

    logger = logging.getLogger()
    comprehend = None

    def __init__(self):
        super().__init__()
        self.comprehend = boto3.client(
            service_name='comprehend')

    def process(self, post: dict):
        # Split the text into paragraphs to allow for better processing
        paragraphs = post['post'].split('\n\n')
        post_list = []

        # For each paragraph truncate it to a 5000 byte UTF-8 encoded string that is required by Comprehend
        for p in paragraphs:
            if '\n' in p:
                post_list.append(truncateUTF8length(p, 5000))

        results = []
        i = 0
        # Loop through paragraphs and detect entities in batches of 25
        while i < len(post_list):
            res = self.comprehend.batch_detect_entities(
                TextList=post_list[i:i+25], LanguageCode='en')
            i += 25
            for r in res['ResultList']:
                if len(r['Entities']) > 0:
                    results.append(r['Entities'])

        # Create the entities key if it does not exist
        if not 'entities' in post.keys():
            post['entities'] = []

        # Individually append the entities
        for r in results:
            for i in r:
                post['entities'].append(i)


def truncateUTF8length(unicodeStr, maxsize):
    return str(unicodeStr.encode("utf-8")[:maxsize], "utf-8", errors="ignore")
