# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import logging
import time
from locust import User
from neo4j import Driver


class BoltUser(User):
    """This is a Locust User that works on the Bolt protocol

    Raises:
        Exception: Generic Exception

    Returns:
        BoltUser: The Locust User
    """

    def __init__(self, *args, **kwargs):
        """The init function"""
        super().__init__(*args, **kwargs)
        self.logger = logging.getLogger(__name__)
        self.client = None
        self.session = None

    def on_stop(self):
        """Closes the connection on stop"""
        self._disconnect()
        return super().on_stop()

    def connect(self, driver: Driver) -> None:
        """This takes a neo4j.Driver object to be used by the User

        Args:
            driver (Driver): The Driver object that this system will use.
        """
        self.client = driver
        self.client.verify_connectivity()
        self.session = self.client.session()

    def _disconnect(self):
        """Disconnects the Driver"""
        self.client.close() if self.client is not None else None

    def query(self, query: str, parameters: map = None, name: str = ""):
        """Runs the provided query with the provided parameters

        Args:
            query (str): The query to run
            parameters (map, optional): A dictionary of parameters. Defaults to None.
            name (str, optional): The query name for the results. Defaults to "".
        """
        try:
            if not self.session:
                raise Exception("You must manually instatiate a bolt connection and pass it to the `connect()` method.")
            values = []
            tic = time.perf_counter()
            result = self.session.run(query.replace("\n", " "), parameters=parameters)
            values = result.data()
            result.consume()
            toc = time.perf_counter()
            self.environment.events.request.fire(
                request_type=self.__class__.__base__.__name__,
                language="BOLT",
                name=name,
                response_time=(toc - tic) * 1000,
                response_length=len(values),
                exception=None,
                context=self.context(),
            )
            return values
        except Exception as e:
            toc = time.perf_counter()
            self.environment.events.request.fire(
                request_type=self.__class__.__base__.__name__,
                language="BOLT",
                name=name,
                response_time=(toc - tic) * 1000,
                response_length=0,
                exception=e,
                context=self.context(),
            )
            self.logger.error("Failed to create the driver: %s", e)
