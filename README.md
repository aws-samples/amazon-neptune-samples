## Amazon Neptune Samples

Samples and documentation for using the Amazon Neptune graph database service

## Tools and Utilities

You may also be interested in the [Amazon Neptune Tools](https://github.com/awslabs/amazon-neptune-tools) github repository, which includes tools for data export, conversion, gremlin client load balancing, and more.

### Amazon Neptune Graphs and Jupyter Notebooks

__[August 2021]__ The Neptune-SageMaker examples in this repository have been deprecated in favour of the [Amazon Neptune Workbench](https://docs.aws.amazon.com/neptune/latest/userguide/graph-notebooks.html). We recommend that you use the Workbench for all new Neptune notebook development. Alternatively, you can create your own notebooks using the [_neptune-python-utils_ library](https://github.com/awslabs/amazon-neptune-tools/tree/master/neptune-python-utils). Note that _neptune-python-utils_ supports Gremlin Python 3.5.x. As such, it is not compatible with the Neptune Workbench, which currently supports 3.4.x.

<del>Whether you’re creating a new graph data model and queries, or exploring an existing graph dataset, it can be useful to have an interactive query environment that allows you to visualize the results. This [directory](neptune-sagemaker/README.md) has samples from two blog posts to show you how to achieve this by connecting an Amazon SageMaker notebook to an Amazon Neptune database. Using the notebook, you load data into the database, query it and visualize the results.</del>

- <del>[Blog Post: Analyze Amazon Neptune Graphs Using Amazon Sagemaker Jupyter Notebooks](https://aws.amazon.com/blogs/database/analyze-amazon-neptune-graphs-using-amazon-sagemaker-jupyter-notebooks/)</del>
- <del>[Blog Post: Let Me Graph that for You - Part 1 - Air Routes](https://aws.amazon.com/blogs/database/let-me-graph-that-for-you-part-1-air-routes/)</del>

### Writing to Amazon Neptune from Amazon Kinesis Data Streams

This [example](gremlin/stream-2-neptune/) demonstrates using an Amazon Kinesis Data Stream and AWS Lambda to issue batch writes to Amazon Neptune. The code samples use the Gremlin API, but it can be readily adapted for RDF graphs as well.

### ETL Process for Transforming and Loading Data Into Amazon Neptune

The following [lab](gremlin/etl-from-relational-model) uses the open [IMDB dataset](https://www.imdb.com/interfaces/). This is a small subset of the full IMDB.com application. With this dataset, we want to develop an application that allows for us to find whether or not an actor or actress is no more than [six degrees separated from the actor Kevin Bacon](https://en.wikipedia.org/wiki/Six_Degrees_of_Kevin_Bacon). In this example, AWS Glue and Amazon Athena are used to discover and transform the relational model used by IMDB into a graph model that can be loaded into Amazon Neptune. This pattern can be used to transform other relational models into graph models for similar purposes.

### Gremlin

#### Chatbot Full Stack Application

This is an [example](gremlin/chatbot-full-stack-application/README.md) of using Amazon Neptune in combination with Amazon Comprehend and Amazon Lex to build a full stack knowledge graph application with NLP and Natural Language Search capabilities.

#### Collaborative Filtering

This is an [example](gremlin/collaborative-filtering/README.md) of using Gremlin and making recommendations using collaborative filtering. It includes examples of loading data and Gremlin traversals.

#### Visualize data in Amazon Neptune using VIS.js library

This [GitHub lab](gremlin/visjs-neptune) will take you through hands-on excercise of visualizing graph data in Amazon Neptune using VIS.js library. Amazon Neptune is a fast, reliable, fully-managed graph database service available from AWS. With Amazon Neptune you can use open source and popular graph query languages such as Apache TinkerPop Gremlin for property graph databases or SPARQL for W3C RDF model graph databases.

#### Property Graph Data Models

These [examples](gremlin/property-graph-data-modelling) demonstrate a "working backwards" approach to designing and implementing an application graph data model and queries based on a backlog of use cases.

The design process here is shown in "slow motion", with each use case triggering a revision to the data model, the introduction of new queries, and updates to existing queries. In many real-world design sessions you would compress these activities and address several use cases at once, converging on a model more immediately. The intention here is to unpack the iterative and evolutionary nature of the many modelling processes that often complete in the "blink of an eye".

### RDF & SPARQL

#### PG-Schema for RDF

An [experiment](./pg-schema-for-rdf) to extend the PG-Schema proposal to work with RDF also.

**More coming soon!**

### Related samples

#### Serverless Application with AWS AppSync and Amazon Neptune

You may also be interested in this [example serverless application](https://github.com/aws-samples/aws-appsync-calorie-tracker-workshop) that uses AWS AppSync GraphQL and Amazon Neptune.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.
