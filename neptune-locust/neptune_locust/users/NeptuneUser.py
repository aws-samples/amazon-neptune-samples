# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import time
from locust import User
import boto3
import json
from botocore.config import Config
from urllib.parse import urlparse
import logging


class NeptuneLanguage:
    OPEN_CYPHER = "OPEN_CYPHER"
    GREMLIN = "GREMLIN"
    SPARQL = "SPARQL"


class NeptuneUser(User):
    """
    A User that runs a using the Amazon Boto3 SDK for Neptune
    """

    def __init__(self, *args, **kwargs):
        """The init function"""
        super().__init__(*args, **kwargs)
        self.logger = logging.getLogger(__name__)

        self.graphId = None

        # Parse out the graphId from the hostname
        if self.host:
            self.logger.debug("NeptuneUser host: %s", self.host)
            parts = urlparse(self.host)
            config = Config(retries={"max_attempts": 1})
            if parts.hostname is not None and not parts.hostname.startswith("g-"):
                # This is a Neptune Database Cluster
                self.client = boto3.client("neptunedata", config=config, endpoint_url=self.host)
                self.logger.debug("Creating Neptune Database session for %s", self.host)
            else:
                # This is a Neptune Analytics Graph
                # Set the graph id for later use by pulling it either from the full URL or just the hostname
                graphId = parts.hostname.split(".")[0] if parts.hostname else self.host
                if graphId is not None and graphId.startswith("g-"):
                    self.graphId = graphId
                    self.client = boto3.client("neptune-graph", config=config)
                    self.logger.debug("Creating Neptune Graph session for %s", self.graphId)
                else:
                    self.logger.error("Host name does not point to a valid graphId")
                    self.client = boto3.client("neptunedata", config=config, endpoint_url=self.host)
                    self.logger.debug("Creating Neptune Database session for %s", self.host)
        else:
            self.logger.info("Currently no client exists so one will needed to be added via the connect() method.")

    def connect(self, client: boto3.client) -> None:
        """This takes a client object to be used by the User

        Args:
            client (boto3.client): _description_
        """
        self.logger.debug("Setting client to custom client")
        self.client = client

    def on_stop(self) -> None:
        """Closes the connection on stop"""
        self.logger.debug("Closing session")
        self.client = None
        return super().on_stop()

    def query(
        self,
        query: str,
        parameters: map = None,
        name: str = "",
        language: NeptuneLanguage = NeptuneLanguage.OPEN_CYPHER,
    ) -> str:
        """Runs the provided query with the provided parameters

        Args:
            query (str): The query to run
            parameters (map, optional): A dictionary of parameters. Defaults to None.
            name (str, optional): The query name for the results. Defaults to "".
            language (NeptuneLanguage, optional): The query language being run. Defaults to NeptuneLanguage.OPEN_CYPHER.
        """
        try:
            tic = time.perf_counter()
            self.logger.debug("Host: %s", self.host)
            if self.graphId is not None:
                # Run Neptune Analytics queries
                self.logger.debug("Querying graph %s", self.graphId)
                tic = time.perf_counter()
                if parameters:
                    resp = self.client.execute_query(
                        graphIdentifier=self.graphId,
                        queryString=query,
                        parameters=parameters,
                        language=language,
                    )
                else:
                    resp = self.client.execute_query(
                        graphIdentifier=self.graphId,
                        queryString=query,
                        language=language,
                    )
                toc = time.perf_counter()
                if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
                    self.environment.events.request.fire(
                        request_type=self.__class__.__base__.__name__,
                        language=language,
                        name=name,
                        response_time=(toc - tic) * 1000,
                        response_length=len(resp),
                        exception=None,
                        context=self.context(),
                    )
                    return resp["payload"].read().decode("UTF-8")
                else:
                    raise Exception
            else:
                # Run Neptune Database queries
                self.logger.debug("Querying graph %s", self.host)
                tic = time.perf_counter()
                if language == NeptuneLanguage.OPEN_CYPHER:
                    if parameters:
                        resp = self.client.execute_open_cypher_query(
                            openCypherQuery=query,
                            parameters=json.dumps(parameters),
                        )
                    else:
                        resp = self.client.execute_open_cypher_query(openCypherQuery=query)
                elif language == NeptuneLanguage.GREMLIN:
                    resp = self.client.execute_gremlin_query(gremlinQuery=query)
                elif language == NeptuneLanguage.SPARQL:
                    raise NotImplementedError
                else:
                    raise ValueError("Unsupported language")
                toc = time.perf_counter()
                if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
                    self.environment.events.request.fire(
                        request_type=self.__class__.__base__.__name__,
                        language=language,
                        name=name,
                        response_time=(toc - tic) * 1000,
                        response_length=len(resp),
                        exception=None,
                        context=self.context(),
                    )

                    return resp["result"] if "result" in resp else resp["results"]
                else:
                    raise Exception
        except Exception as e:
            self.logger.debug(e)
            self.logger.debug(query)
            toc = time.perf_counter()
            self.environment.events.request.fire(
                request_type=self.__class__.__base__.__name__,
                language=language,
                name=name,
                response_time=(toc - tic) * 1000,
                response_length=0,
                exception=e,
                context=self.context(),
            )
            raise e
