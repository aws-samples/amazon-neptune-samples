# Property Graph Data Modelling

These examples demonstrate a "working backwards" approach to designing and implementing an application graph data model and queries based on a backlog of use cases.

The design process here is shown in "slow motion", with each use case triggering a revision to the data model, the introduction of new queries, and updates to existing queries. In many real-world design sessions you would compress these activities and address several use cases at once, converging on a model more immediately. The intention here is to unpack the iterative and evolutionary nature of the many modelling processes that often complete in the "blink of an eye".

## Scenario

Let's assume we're about to build an employment history application backed by a graph database. The application will store and manage details regarding people and the roles they have had with different companies.

We have an initial backlog of use cases:

 - Find the companies where X has worked, and their roles at those companies
 - Find the people who have worked for a company at a specific location during a particular time period
 - Find the people in more senior roles at the companies where X worked

## Design Process

![alt text](https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/images/data-modelling-00.png "Design Process")

 1. Work backwards from our application or end-user goals. For each use case we write down the questions we would have to put to the domain in order to facilitate the outcomes that motivate the use case. What would we need to know, find, or compute?
 2. Review these questions and identify candidate entities, attributes and relationships. These become the basis of our graph model, implemented using propery graph primitives.
 3. Review these questions and our prototype application graph model to determine how we would answer each question by traversing paths through the graph or matching structural patterns in the graph. Adjust the model until we are satisfied it faciliatates querying in an efficient and expressive manner.
 4. Continue to iterate over our use cases, refining your candidate model and queries as you introduce new features.

Once we have a candidate application graph model, there are a couple of additional steps, which we'll not cover in this example

 1. We can treat our candidate model as a target for any necessary data migration and integration scenarios. Identify existing sources of data and implement extract, transform and load (ETL) processes that ingest data into the target model.
 2. Design and implement write operations that our application or service will use insert, modify and if necessary delete data in the target application graph model.

## Best Practices

We'll design our model and queries in an executable fashion, using small sample datasets to cover each use case, and unit tests to validate our queries. These unit tests provide a "bedrock of asserted behaviour" that gives us fast feedback as we evolve our model and queries.

## Running the Example

You can download the examples as Jupyter notebooks or as HTML. Alternatively, you can run the examples inside Amazon Neptune from an Amazon SageMaker Jupyter notebook by launching an AWS CloudFormation stack using one of the links below.

The Neptune and SageMaker resources used in this example incur costs. With SageMaker hosted notebooks you pay simply for the Amazon EC2 instance that hosts the notebook. This example uses an ml.t2.medium instance, which is eligible for the AWS Free Tier.

Once stack creation has completed been created, open the Amazon SageMaker console and from the left-hand menu select _Notebook instances_. Choose _Open Jupyter_ in the Actions column. In the Jupyter window, open the _Neptune_ directory, and then the _property-graph-data-modelling_ directory.


| Region | Stack |
| ---- | ---- |
|US East (N. Virginia) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://us-east-1.console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|US East (Ohio) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://us-east-2.console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|US West (Oregon) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://us-west-2.console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|EU (Ireland) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|EU (London) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://eu-west-2.console.aws.amazon.com/cloudformation/home?region=eu-west-2#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|EU (Frankfurt) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://eu-central-1.console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/quickcreate?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
|Asia Pacific (Singapore) |  [<img src="https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png">](https://ap-southeast-1.console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#/stacks/create/review?templateURL=https://s3.amazonaws.com/aws-neptune-customer-samples/neptune-sagemaker/cloudformation-templates/neptune-sagemaker/neptune-sagemaker-base-stack.json&stackName=graph-data-modelling&param_NotebookContentS3Locations=s3://aws-neptune-customer-samples/neptune-sagemaker/notebooks%7Cproperty-graph-data-modelling/*) |
