import sys
import logging
import json
from gremlin_python import statics
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.anonymous_traversal import traversal
from gremlin_python.process.traversal import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from gremlin_python.process.traversal import T


class NeptuneWriter:
    # Global Variables
    # Array containing the HTML of each blogs paginated blog post titles and previews

    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    g = None
    conn = None

    def __init__(self, server: str, port: int, ssl: bool):
        super().__init__()
        self.g, self.conn = self.__connect_to_neptune(server, port, ssl)

    def __del__(self):
        self.conn.close()

    def __connect_to_neptune(self, server: str, port: int, ssl: bool):
        """Creates a connection to Neptune and returns the traversal source"""
        hostname = f'{server}:{port}'
        if ssl:
            endpoint = f'wss://{hostname}/gremlin'
        else:
            endpoint = f'ws://{hostname}/gremlin'
        self.logger.info(endpoint)
        connection = DriverRemoteConnection(endpoint, 'g')
        gts = traversal().withRemote(connection)
        return (gts, connection)

    def __add_post(self, t, post):
        t = (
            t.V(post['url'])
            .fold()
            .coalesce(
                __.unfold(),
                __.addV('post')
                .property(T.id, post['url'])
                .property('title', post['title'])
                .property('thumbnail', post['thumbnail'])
                .property('post_date', post['date'])
                .property('img_src', post['img_src'])
                .property('img_height', post['img_height'])
                .property('img_width', post['img_width'])
            ).as_('post')
        )

        return t

    def __add_author(self, t, author, post_url):
        t = (
            t.V(author['name'])
            .fold()
            .coalesce(
                __.unfold(),
                __.addV('author')
                .property(T.id, author['name'])
                .property('name', author['name'])
            ).as_('p').addE('written_by').from_(__.V(post_url))
        )
        # Conditionally add the img_src, img_height, and img_width property if they do not exist
        if "img_src" in author.keys():
            t = (
                t.sideEffect(
                    __.select('p').hasNot('img_src')
                    .property('img_src', author['img_src'])
                    .property('thumbnail', author['thumbnail'])
                    .property('img_height', author['img_height'])
                    .property('img_width', author['img_width'])
                )
            )
        return t

    def __add_entities(self, t, entity, post_url):
        t = (
            t.V(f'{entity["Text"]}_{entity["Type"]}')
            .fold()
            .coalesce(
                __.unfold(),
                __.addV(entity["Type"].lower())
                .property(T.id, f'{entity["Text"]}_{entity["Type"]}')
                .property("text", entity["Text"])
                .property("type", entity["Type"])
            ).addE('found_in').from_(__.V(post_url))
            .property('score', entity['Score'])
        )

        return t

    def __add_tag(self, t, tag, post_url):
        t = (
            t.V(tag)
            .fold()
            .coalesce(
                __.unfold(),
                __.addV('tag')
                .property(T.id, tag)
                .property('tag', tag)
            ).addE('tagged').from_(__.V(post_url))
        )

        return t

    def process_post(self, post):
        t = self.g
        # Add post
        t = self.__add_post(t, post)
        # Add Authors and edge to Post
        for a in post['authors']:
            t = self.__add_author(t, a, post['url'])
        # Add entities and edge to Post
        for e in post['entities']:
            if e['Score'] >= 0.85:
                t = self.__add_entities(t, e, post['url'])
        # Add tags and edge to Post
        for tag in post['tags']:
            t = self.__add_tag(t, tag, post['url'])

        t.next()