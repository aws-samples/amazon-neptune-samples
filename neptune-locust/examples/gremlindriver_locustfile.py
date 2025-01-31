# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from locust import task, run_single_user
from neptune_locust.users import GremlinDriverUser
import os


class Example_GremlinDriver(GremlinDriverUser):
    # You can set the host explicitly here if you want to do this for debugging
    # host = "wss://<INSERT CLUSTER:PORT URL HERE>"

    @task
    def task(self):
        resp = self.query(self.g.inject(1), name=self.__class__.__name__)
        print(resp)


if __name__ == "__main__":
    os.environ["USE_IAM"] = "true"
    run_single_user(Example_GremlinDriver)
