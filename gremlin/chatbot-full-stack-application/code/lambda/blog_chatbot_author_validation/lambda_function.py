# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import json
import datetime
import time
import os
import math
import random
import logging
import ast
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import traversal
from gremlin_python.process.strategies import *
from gremlin_python.process.traversal import *
from gremlin_python.structure.graph import Path, Vertex, Edge
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from helper import connectToNeptune, elicit_slot, close, confirm_intent

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)


def build_options(authors):
    """
    Build a list of potential options for a given slot, to be used in responseCard generation.
    """
    resp = []
    for x in authors:
        resp.append({'text': x, 'value': x})
    return resp


""" --- Functions that control the bot's behavior --- """


def elicit_author(intent_request, output_session_attributes):
    """
    Create the content for the elict slot behavior for retriving an author
    """
    slots = intent_request['currentIntent']['slots']
    return elicit_slot(
        output_session_attributes,
        intent_request['currentIntent']['name'],
        slots,
        'author',
        {
            'contentType': 'PlainText',
            'content': 'What author would you like to see posts by?'
        },
        None
    )


def get_authors(intent_request, g):
    """
    Validates and returns that the specified author exists in the system
    """
    author = intent_request['currentIntent']['slots']['author']
    source = intent_request['invocationSource']
    output_session_attributes = intent_request['sessionAttributes'] if intent_request['sessionAttributes'] is not None else {
    }

    logger.debug(f'Slot Values: {intent_request["currentIntent"]["slots"]}')
    if author is None:     # I Need the author
        logger.debug(f'Need to elicit author')
        return elicit_author(intent_request, output_session_attributes)
    elif author is not None:  # I have the author
        results = g.V().has('author', 'name', TextP.containing(
            author)).values('name').toList()

        print("*******")
        print(len(results))
        print("*******")
        if len(results) == 1:
            # They have confirmed the author so close successfully
            if intent_request['currentIntent']['confirmationStatus'] == 'Confirmed':
                return close(
                    output_session_attributes,
                    "Fulfilled",
                    {
                        'contentType': 'PlainText',
                        'content': f'Showing posts for {results[0]}'
                    }
                )
            # They denied the author so close failed
            elif intent_request['currentIntent']['confirmationStatus'] == 'Denied':
                return close(
                    output_session_attributes,
                    "Failed",
                    {
                        'contentType': 'PlainText',
                        'content': f'Sorry please try again'
                    }
                )
            else:
                # They sent in an author the first time that was an exact match so close successfully
                if intent_request['currentIntent']['slots']['author'] == results[0]:
                    return close(
                        output_session_attributes,
                        "Fulfilled",
                        {
                            'contentType': 'PlainText',
                            'content': f'Showing posts for {results[0]}'
                        }
                    )
                else:
                    # They sent in an author that was not an exact match so confirm the intent
                    intent_request['currentIntent']['slots']['author'] = results[0]

                    return confirm_intent(
                        output_session_attributes,
                        intent_request['currentIntent']['name'],
                        intent_request['currentIntent']['slots'],
                        {
                            'contentType': 'PlainText',
                            'content': f'Do you mean {results[0]}?'
                        },
                        None
                    )
        elif len(results) == 0:
            return elicit_author(intent_request, output_session_attributes)
        else:
            options = build_options(results)
            return elicit_slot(output_session_attributes,
                               intent_request['currentIntent']['name'],
                               intent_request['currentIntent']['slots'],
                               'author',
                               None,
                               build_response_card(
                                   "Which Author?", "Please choose an author", options)
                               )


""" --- Intents --- """


""" --- Main handler --- """


def lambda_handler(event, context):
    """Main handler"""
    # Connect to Neptune
    g, conn = connectToNeptune()
    logger.debug('******')
    logger.debug(json.dumps(event))
    logger.debug('******')
    resp = get_authors(event, g)
    conn.close()
    return resp
