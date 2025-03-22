# Locust Users for Amazon Neptune database

[Locust](https://locust.io/) is a powerful open source load testing tool that allows users to create rich and complex load testing scenarios that can be distributed.

The [User](https://docs.locust.io/en/stable/writing-a-locustfile.html#user-class) class in Locust represents a user/scenario for a test run.  While Locust provides base classes for HTTP based traffic it relies on third party libraries to support other protocols.

This library helps you to create performance tests on Amazon Neptune when using Locust by providing rich User classes for testing using the [Amazon Boto3 SDK](https://boto3.amazonaws.com/), [Gremlin Python](https://pypi.org/project/gremlinpython/), and [Amazon Neptune Bolt](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-opencypher-bolt.html).

## Installation

### Prerequisites

* Python 3.12 or greater

To install the package you can use the following command:

`pip install <URL to Desired Github Release>`


## Usage

These instructions are not meant as a complete tutorial on how to construct and build Locust tests.  
To understand the specifics of how to run Locust tests please refer to the Locust [Documentation](https://docs.locust.io/en/stable/what-is-locust.html).


Each of the `User` classes implements the standard interface with a `query()` method to run using the different protocols.

* `NeptuneUser` - This is a Locust `User` class that uses the boto3 SDK to run either openCypher or Gremlin queries against both Neptune Analytics and Neptune Database instances
* `GremlinDriverUser` - This is a Locust `User` class that uses the TinkerPop Gremlin Python driver and a web socket connection to Neptune Database
* `Bolt` - This is a Locust `User` class that uses the bolt driver and a web socket connection to Neptune Database

Both the `NeptuneUser` and `GremlinDriverUser` classes will automatically create the underlying drivers required for both IAM and non-IAM authenticated requests. The `NeptuneUser` class will handle this without any changes required to your code.  The `GremlinDriverUser` requires that you set an environment variable `USE_IAM` to `"true"` to enable signing of these requests.  

The `BoltUser` requires that you create the `Driver` external to the class, such as using the `on_start` [Event Hook](https://docs.locust.io/en/stable/extending-locust.html) which is then passed to the `connect(driver)` method.  In addition to the `User` classes above we have also provided a `NeptuneBoltAuthToken` class which can be used in conjunction with the bolt driver to add SigV4 authorization headers to bolt requests as are required by IAM enabled clusters.  An example of how to achieve this is shown below.

```
class Example_Bolt(BoltUser):
    # You can set the host explicitly here if you want to do this for debugging
    # host = "bolt://<INSERT CLUSTER:PORT URL HERE>"

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
```

## Examples

Within this repository, there are [example locustfiles](https://docs.locust.io/en/stable/what-is-locust.htm) located in the `/examples` folder showing how to use each of the 3 User Classes.
