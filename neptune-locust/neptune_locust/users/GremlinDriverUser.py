# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import time
import os
import boto3
import logging
from locust import User
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import __
from gremlin_python.process.strategies import *
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection
from botocore.auth import SigV4Auth
from botocore.awsrequest import AWSRequest


class GremlinDriverUser(User):
    """
    A User that runs a Gremlin Python websockets based client
    """

    def __init__(self, *args, **kwargs):
        """Init function"""
        super().__init__(*args, **kwargs)
        self.logger: logging.Logger = logging.getLogger(__name__)
        self.logger.addHandler(logging.NullHandler())
        self.remoteConn = None
        self.g = None
        self._setup_connect()

    def on_stop(self):
        """Closes the connection on stop"""
        self.disconnect()
        return super().on_stop()

    def connect(self, driver: DriverRemoteConnection) -> None:
        """This takes a driver object to be used by the User

        Args:
            driver (DriverRemoteConnection): The DriverRemoteConnection object that this system will use.
        """
        self.remoteConn = driver
        self.g = Graph().traversal().withRemote(self.remoteConn)
        self.g.inject(1).next()

    def _setup_connect(self):
        """Sets up the connection."""
        try:
            self.logger.info(f"Connecting to host {self.host}")

            if "USE_IAM" in os.environ and bool(os.environ["USE_IAM"]):
                self.remoteConn = self._prepare_iam_connection()
            else:
                self.remoteConn = DriverRemoteConnection(f"{self.host}/gremlin", "g")

            self.g = Graph().traversal().withRemote(self.remoteConn)
            self.g.inject(1).next()
        except Exception as e:
            self.logger.error("Failed to create the driver: %s", e)
            raise

    def _prepare_iam_connection(self) -> DriverRemoteConnection:
        """Prepare the GremlinDriver for IAM auth'd cluster by adding SigV4 auth

        Returns:
            DriverRemoteConnection: The Gremlin DriverRemoteConnection
        """
        creds = boto3.Session().get_credentials().get_frozen_credentials()
        request = AWSRequest(method="GET", url=self.host + "/gremlin", data=None)
        SigV4Auth(creds, "neptune-db", boto3.Session().region_name).add_auth(request)

        return DriverRemoteConnection(self.host + "/gremlin", "g", headers=request.headers.items())

    def disconnect(self) -> None:
        self.remoteConn.close() if self.remoteConn is not None else None

    def query(self, query: str, name: str = "") -> object:
        """Runs the provided query with the provided parameters

        Args:
            query (str): The query to run
            name (str, optional): The query name for the results. Defaults to "".
        """
        cnt = 0
        try:
            tic = time.perf_counter()
            result = query.to_list()
            toc = time.perf_counter()
            self.environment.events.request.fire(
                request_type=self.__class__.__base__.__name__,
                language="GREMLIN_DRIVER",
                name=name,
                response_time=(toc - tic) * 1000,
                response_length=len(result),
                exception=None,
                context=self.context(),
            )
            return result
        except Exception as e:
            toc = time.perf_counter()
            self.environment.events.request.fire(
                request_type=self.__class__.__base__.__name__,
                language="GREMLIN_DRIVER",
                name=name,
                response_time=(toc - tic) * 1000,
                response_length=cnt,
                exception=e,
                context=self.context(),
            )
            self.logger.error("Failed to create the driver: %s", e)
            self.logger.error(e)
