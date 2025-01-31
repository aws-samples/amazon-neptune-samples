# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import logging
from locust import task, run_single_user
from neptune_locust.users import BoltUser, NeptuneBoltAuthToken
from neo4j import GraphDatabase


class Example_Bolt(BoltUser):
    # You can set the host explicitly here if you want to do this for debugging
    # host = "https://<INSERT CLUSTER:PORT URL HERE>"

    def on_start(self):
        try:
            driver = GraphDatabase.driver(self.host, auth=NeptuneBoltAuthToken(self.host), encrypted=True)
            self.connect(driver)
        except ConnectionError as exception:
            logging.info("Caught %s", exception)
            self.user.environment.runner.quit()

    @task
    def task(self):
        resp = self.query("RETURN 1", name=self.__class__.__name__)
        print(resp)


if __name__ == "__main__":
    run_single_user(Example_Bolt)
