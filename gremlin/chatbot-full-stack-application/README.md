# Knowledge Graph Chatbot Full Stack Application Example

This folder provides a sample full stack application showing how to build a knowledge graph backed application with NLP and Natural Language search integration using Amazon Neptune, Amazon Comprehend, and Amazon Lex. This application has an accompanying blog post on the [AWS Databases blog](https://aws.amazon.com/blogs/database/category/database/amazon-neptune/).

# Architectual Overview

The solution in this post demonstrates how to build a full-stack application to use NLP and natural language search techniques to power an intelligent data extraction and insight platform. We will show how to construct this type of application by leveraging an Ingest/Curate/Exploit development paradigm that we have found works well to develop knowledge graph backed applications. To ingest data into our we will be using Python and Beautiful Soup (https://www.crummy.com/software/BeautifulSoup/) to scrape the AWS Database Blog site to generate and store semi-structured data which will be stored in our knowledge graph which is powered by Amazon Neptune. After we ingest this data we will enhance and curate our semi-structured data by using Python to call Amazon Comprehend to extract named entities from the blog post text. We will then connect these extracted entities within our knowledge graph to provide more contextually relevant connections within our blog data. Finally, we will wrap up our architecture by creating a React-based smart application, powered by Amazon Lex and AWS Amplify, that provides a web-based chatbot interface which will exploit the connected nature of the data within our knowledge graph to provide contextually relevant answers to questions asked.

# Deploying the Application

Now that we know what we are going to build let’s take a look at how to deploy the sample solution. Before getting started we need a few things.

- Create or identify an Amazon Neptune cluster to store the data. If you need to create a new cluster the steps for doing this can be found here (https://docs.aws.amazon.com/neptune/latest/userguide/get-started-create-cluster.html).
- A computer with Python 3.6+ installed.
- A computer with NodeJS 14 or greater installed as well as yarn (https://classic.yarnpkg.com/en/docs/install#mac-stable).
- The latest version of the git repository found here (https://github.com/aws-samples/amazon-neptune-samples). This can be retrieved using the command below:

```
git clone https://github.com/aws-samples/amazon-neptune-samples.git
```

With these prerequisites completed the next step is to run the deployment script which will:

- Deploy all the Lambda Functions
- Deploy the API Gateway required for our web application
- Deploy the Amazon Lex chatbot and configure the appropriate intents
- Create the Amazon Cognito Identity pool required for our chatbot to connect
- Run code that scrapes the blog posts, enhances the data, and loads it into our knowledge graph with data from the AWS Database blog
- Return the configuration values required for our web application to connect

To execute this deployment we first need to install all the prerequisites. This can be accomplished using the command below:

```
pip install -r requirements.txt
```

As part of this installation the boto3 (https://boto3.amazonaws.com/v1/documentation/api/latest/index.html) client is installed, which we will use as part of the deployment. However before we can use this, we need to configure the client using the following command:

```
aws configure
```

Enter the appropriate values for each field when prompted for a user that has permissions to create resources in the region specified. Once this step is complete, we are ready to deploy our application using the following command from within the deployment folder:

```
python deploy.py <INSERT NEPTUNE CLUSTER NAME>
```

This script will take several minutes to execute. When it is complete, it will provide several configuration values that we need for our web application. Go into the /code/web-ui/neptune-chatbot/src folder of the repository. In this folder you will need to locate the config.json as well as aws-exports.js files and copy the specified configuration parameters into the files. Once this is finished you can launch the web application using the following commands from the /code/web-ui/neptune-chatbot/ folder.

```
yarn install
yarn start
```

Once this start has completed you can access the web application at http://localhost:3000/.
