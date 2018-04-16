from aws_services import load_aws_services_to_neptune
from aws_regions import load_aws_regions_to_neptune
from aws_ec2_instance_prices import load_ec2_pricing_data_to_neptune


from socket import gaierror
import gremlin_interface
from gremlin_python.structure.graph import Graph
from gremlin_python.process.graph_traversal import GraphTraversalSource
from gremlin_python.driver.driver_remote_connection import DriverRemoteConnection

import sys

from timeit import default_timer as timer
import datetime


def load_data(graph_traversal: GraphTraversalSource):
    print("Data Loading Started\n")
    start_time = timer()

    # load AWS Regions
    load_aws_regions_to_neptune(graph_traversal)
    time_after_loading_region = timer()
    time_taken = str(datetime.timedelta(seconds=time_after_loading_region - start_time))
    print("Time Taken to load AWS-Regions:" + time_taken + "\n")

    # load AWS Services
    load_aws_services_to_neptune(graph_traversal)
    time_after_loading_services = timer()
    time_taken = str(datetime.timedelta(seconds=time_after_loading_services - time_after_loading_region))
    print("Time Taken to load AWS-Services:" + time_taken + "\n")

    # load EC2 Instance Pricing
    load_ec2_pricing_data_to_neptune(graph_traversal)
    time_after_loading_ec2_pricing = timer()
    time_taken = str(datetime.timedelta(seconds=time_after_loading_ec2_pricing - time_after_loading_services))
    print("Time Taken to load EC2 Instance Pricing:" + time_taken + "\n")

    end_time = timer()
    time_taken = str(datetime.timedelta(seconds=end_time - start_time))
    print("Data Loading Completed in:" + time_taken + "\n")


if __name__ == '__main__':
    # Verify args
    if len(sys.argv) != 3:
        print ('Usage: python main.py <neptune-endpoint> <neptune-port>')
        sys.exit(1)

    neptune_endpoint = sys.argv[1]

    try:
        neptune_port = int(sys.argv[2])
    except ValueError:
        print("Please enter a valid port number e.g. 8182")
        sys.exit(1)

    # Create a connection to Neptune
    graph = Graph()
    try:
        g = gremlin_interface.get_gremlin_connection(neptune_endpoint, neptune_port)
        load_data(g)
    except gaierror:
        print("Could not establish connection with the Neptune Server. Invalid Connection Parameters")
        sys.exit(1)
