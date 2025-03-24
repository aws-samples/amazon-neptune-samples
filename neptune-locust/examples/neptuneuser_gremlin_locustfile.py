# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from locust import task, run_single_user
from neptune_locust.users import NeptuneLanguage, NeptuneUser


class Example_Gremlin(NeptuneUser):
    # You can set the host explicitly here if you want to do this for debugging
    # host = "https://<INSERT CLUSTER:PORT OR GRAPH URL HERE>"

    @task
    def task(self):
        resp = self.query(
            """g.inject(1)""",
            name=self.__class__.__name__,
            language=NeptuneLanguage.GREMLIN,
        )
        # print(resp)


if __name__ == "__main__":
    run_single_user(Example_Gremlin)
