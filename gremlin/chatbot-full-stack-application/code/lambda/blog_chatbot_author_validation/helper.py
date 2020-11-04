# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import os
import logging
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import traversal
from gremlin_python.process.strategies import *
from gremlin_python.process.traversal import *
from gremlin_python.structure.graph import Path, Vertex, Edge
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

""" --- Helpers for handling Gremlin connections --- """


def connectToNeptune():
    """Creates a connection to Neptune and returns the traversal source"""
    envok = True
    if 'NEPTUNE_ENDPOINT' in os.environ and 'NEPTUNE_PORT' in os.environ:
        server = os.environ["NEPTUNE_ENDPOINT"]
        port = os.environ["NEPTUNE_PORT"]
        endpoint = f'wss://{server}:{port}/gremlin'
        logger.info(endpoint)
        connection = DriverRemoteConnection(endpoint, 'g')
        gts = traversal().withRemote(connection)
        return (gts, connection)
    else:
        logging.error("Internal Configuraiton Error Occurred.  ")
        return None


""" --- Helpers to build responses which match the structure of the necessary dialog actions --- """


def elicit_slot(session_attributes, intent_name, slots, slot_to_elicit, message, response_card):
    """Builds the response to elicit slot clarification"""
    return {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'ElicitSlot',
            'intentName': intent_name,
            'slots': slots,
            'slotToElicit': slot_to_elicit,
            'message': message,
            'responseCard': response_card
        }
    }


def delegate(session_attributes, fulfillment_state, message):
    """Build a slot to delegate the intent"""
    response = {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'Delegate'
        }
    }

    return response


def confirm_intent(session_attributes, intent_name, slots, message, response_card):
    return {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'ConfirmIntent',
            'intentName': intent_name,
            'slots': slots,
            'message': message,
            'responseCard': response_card
        }
    }


def close(session_attributes, fulfillment_state, message):
    """Build a slot to close the intent"""
    response = {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'Close',
            'fulfillmentState': fulfillment_state,
            'message': message
        }
    }

    return response


def build_response_card(title, subtitle, options):
    """
    Build a responseCard with a title, subtitle, and an optional set of options which should be displayed as buttons.
    """
    buttons = None
    if options is not None:
        buttons = []
        for i in range(min(5, len(options))):
            buttons.append(options[i])

    return {
        'contentType': 'application/vnd.amazonaws.card.generic',
        'version': 1,
        'genericAttachments': [{
            'title': title,
            'subTitle': subtitle,
            'buttons': buttons
        }]
    }
