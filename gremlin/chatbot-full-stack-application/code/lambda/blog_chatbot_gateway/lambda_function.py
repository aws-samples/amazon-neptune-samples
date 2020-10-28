# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import json
import logging
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import traversal
from gremlin_python.process.strategies import *
from gremlin_python.process.traversal import *
from gremlin_python.structure.graph import Path, Vertex, Edge
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from helper import connectToNeptune, edge_to_dict, vertex_to_dict

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)


def posts_by_author(g, authorname):
    paths = (
        g.V().
        has('author', 'name', authorname).
        inE('written_by').outV().
        path().by(__.valueMap(True)).toList()
    )

    vertices = []
    edges = []
    for path in paths:
        start = vertex_to_dict(path[0])
        end = vertex_to_dict(path[2])
        vertices.append(start)
        vertices.append(end)
        edges.append(edge_to_dict(path[1], end['id'], start['id']))

    vertices = list(
        {frozenset(item.items()): item for item in vertices}.values())
    edges = list({frozenset(item.items()): item for item in edges}.values())
    return {'vertices': vertices, 'edges': edges}


def posts_by_topic(g, topic):
    paths = (
        g.V().
        has('tag', 'tag', topic).
        inE('tagged').outV().
        path().by(__.valueMap(True)).toList()
    )

    vertices = []
    edges = []
    for path in paths:
        start = vertex_to_dict(path[0])
        end = vertex_to_dict(path[2])
        vertices.append(start)
        vertices.append(end)
        edges.append(edge_to_dict(path[1], end['id'], start['id']))

    vertices = list(
        {frozenset(item.items()): item for item in vertices}.values())
    edges = list({frozenset(item.items()): item for item in edges}.values())
    return {'vertices': vertices, 'edges': edges}


def posts_by_topic_and_author(g, topic, author):
    paths = (
        g.V().has('tag', 'tag', topic).
        inE('tagged').outV().dedup().outE('written_by').
        inV().has('author', 'name', author).path().by(__.valueMap(True))
    )

    vertices = []
    edges = []
    for path in paths:
        tag = vertex_to_dict(path[0])
        post = vertex_to_dict(path[2])
        author = vertex_to_dict(path[4])
        vertices.append(tag)
        vertices.append(post)
        vertices.append(author)
        edges.append(edge_to_dict(path[1], post['id'], tag['id']))
        edges.append(edge_to_dict(path[3], post['id'], author['id']))

    vertices = list(
        {frozenset(item.items()): item for item in vertices}.values())
    edges = list({frozenset(item.items()): item for item in edges}.values())
    return {'vertices': vertices, 'edges': edges}


def coauthored_posts(g, author1, author2):
    paths = (g.V().has('author', 'name', author1).as_('a').
             inE('written_by').outV().hasLabel('post').
             outE('written_by').inV().where(neq('a')).
             has('author', 'name', author2).path().by(__.valueMap(True))
             )

    vertices = []
    edges = []
    for path in paths:
        author1 = vertex_to_dict(path[0])
        post = vertex_to_dict(path[2])
        author2 = vertex_to_dict(path[4])
        vertices.append(author1)
        vertices.append(post)
        vertices.append(author2)
        edges.append(edge_to_dict(path[1], post['id'], author1['id']))
        edges.append(edge_to_dict(path[3], post['id'], author2['id']))

    vertices = list(
        {frozenset(item.items()): item for item in vertices}.values())
    edges = list({frozenset(item.items()): item for item in edges}.values())
    return {'vertices': vertices, 'edges': edges}


def process_request(params):
    g, conn = connectToNeptune()
    res = None
    if 'authorname' in params.keys() and 'topic' in params.keys():
        res = posts_by_topic_and_author(
            g, params['topic'], params['authorname'])
    elif 'authorname' in params.keys():
        res = posts_by_author(g, params['authorname'])
    elif 'topic' in params.keys():
        res = posts_by_topic(g, params['topic'])
    elif 'author1' in params.keys() and 'author2' in params.keys():
        res = coauthored_posts(g, params['author1'], params['author2'])
    conn.close()
    return res


def lambda_handler(event, context):
    logger.debug('******')
    logger.debug(json.dumps(event))
    logger.debug('******')

    params = {k: v for k, v in event.items() if v is not None}

    if params is None or len(params.keys()) == 0:
        return {
            'statusCode': 400,
            'headers': {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Credentials": True
            },
            'body': 'You must include at least one parameter to filter post on.'
        }

    res = process_request(params)
    return {
        'statusCode': 200,
        'headers': {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Credentials": True,
            "content-type": "application/json"
        },
        'body': json.dumps(res)
    }
