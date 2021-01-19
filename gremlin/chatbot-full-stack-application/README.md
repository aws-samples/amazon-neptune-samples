# Knowledge Graph Chatbot Full Stack Application Example

This folder provides a sample full stack application showing how to build a knowledge graph backed application with NLP and Natural Language search integration using Amazon Neptune, Amazon Comprehend, and Amazon Lex. This application has an accompanying blog post on the [AWS Databases blog](https://aws.amazon.com/blogs/database/category/database/amazon-neptune/). The functionality shown here is to highlight how an integration of these components can work together but is not meant to be an exhaustive example of best practices.

# Architectual Overview

The solution in this post demonstrates how to build a full-stack application to use NLP and natural language search techniques to power an intelligent data extraction and insight platform. We will show how to construct this type of application by leveraging an Ingest/Curate/Exploit development paradigm that we have found works well to develop knowledge graph backed applications. To ingest data into our we will be using Python and Beautiful Soup (https://www.crummy.com/software/BeautifulSoup/) to scrape the AWS Database Blog site to generate and store semi-structured data which will be stored in our knowledge graph which is powered by Amazon Neptune. After we ingest this data we will enhance and curate our semi-structured data by using Python to call Amazon Comprehend to extract named entities from the blog post text. We will then connect these extracted entities within our knowledge graph to provide more contextually relevant connections within our blog data. Finally, we will wrap up our architecture by creating a React-based smart application, powered by Amazon Lex and AWS Amplify, that provides a web-based chatbot interface which will exploit the connected nature of the data within our knowledge graph to provide contextually relevant answers to questions asked.

# Deploying the Application

Now that we know what we are going to build let’s take a look at how to deploy the sample solution. Before getting started we need a few things.

- A machine running Docker, either a laptop or a server
- An AWS account with the ability to create resources

With these prerequisites completed the next step is to run the CloudFormation deployment script which will:

- Deploy all the Lambda Functions
- Deploy the API Gateway required for our web application
- Deploy the Amazon Lex chatbot and configure the appropriate intents
- Create the Amazon Cognito Identity pool required for our chatbot to connect
- Run code that scrapes the blog posts, enhances the data, and loads it into our knowledge graph with data from the AWS Database blog
- Return the configuration values required for our web application to connect

To run the script click on the link below for the region you would like to create the resources in:

- [us-east-2](https://us-east-2.console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/create/review?templateURL=https://aws-neptune-customer-samples.s3.amazonaws.com/chatbot-blog/cfn-templates/overall.yaml)

- [us-east-1](https://us-east-1.console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/create/review?templateURL=https://aws-neptune-customer-samples.s3.amazonaws.com/chatbot-blog/cfn-templates/overall.yaml)

- [us-west-2](https://us-west-2.console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/create/review?templateURL=https://aws-neptune-customer-samples.s3.amazonaws.com/chatbot-blog/cfn-templates/overall.yaml)

- [eu-west-2](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/create/review?templateURL=https://aws-neptune-customer-samples.s3.amazonaws.com/chatbot-blog/cfn-templates/overall.yaml)

Upon completion of the CloudFormation template the output tab will list the following values which you will need to run the web front end.

- ApiGatewayInvokeURL
- IdentityPoolId

With these values copied you can run the following command to create the web interface for this, pasting in the appropriate parameters:

`docker run -td -p 3000:3000 -e IDENTITY_POOL_ID=<IdentityPoolId Value> -e API_GATEWAY_INVOKE_URL=<ApiGatewayInvokeURL Value> -e REGION=<Selected Region> public.ecr.aws/a8u6m7l5/neptune-chatbot:latest`

Once this container has completed you can access the web application at http://localhost:3000/.

# Using the Application

Once you have sucessfully started the application you can try out the chatbot integration with some of the following phrases:

- Show me all posts by Ian Robinson
- What has Dave Bechberger written on Amazon Neptune?
- Have Ian Robinson and Kelvin Lawrence worked together
- Show me posts by Taylor Rigg
  (This should prompt for Taylor Riggan then answer "Yes")
- Show me all posts on Amazon Neptune

Refreshing the browser will clear the canvas and chatbox.

# Clean up

To clean up the resources being used you can delete the CloudFormation stack created.
