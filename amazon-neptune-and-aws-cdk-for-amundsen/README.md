# Welcome to the Amazon Neptune and AWS CDK for Amundsen project!

## Overview

This project has an associated blog which provides extensive detail on Amundsen, as well as greater detail regarding this solution. Please consider reading the blog: https://aws.amazon.com/blogs/database/category/database/amazon-neptune/.

### What is Amundsen?

Amundsen is a data discovery and metadata engine for improving the productivity of data analysts, data scientists and engineers when interacting with data. It does that today by indexing data resources (tables, dashboards, streams, etc.) and powering a page-rank style search based on usage patters (e.g. highly queried tables show up earlier than less queried tables). Think of it as Google search for data. The project is named after Norwegian explorer Roald Amundsen, the first person to discover the South Pole. For more details, visit the following page: https://github.com/amundsen-io/amundsen.

### Why this Project?

The goal of this project is to simplify the provisioning and configuration of an environment for you to take advantage of Amundsen. This project leverages Amazon Neptune and Amazon Elasticsearch Service for the Amundsen Metadata and Search Services, and uses AWS Cloud Development Kit (CDK) to synthesize CloudFormation templates necessary to provision the infrastructure as code. In addition, this project provisions RDS for PostgreSQL, Amazon RedShift, and other resources to streamline the loading and indexing of sample data from an existing Amazon Neptune sample project - `Knowledge Graph Chatbot Full Stack Application` Example https://github.com/aws-samples/amazon-neptune-samples/blob/master/gremlin/chatbot-full-stack-application/README.md. 

## Solution Overview

### Architecture

<img src="./images/amazon-neptune-and-aws-cdk-for-amundsen-solution overview.png">

### Customization

The `cdk.json` file tells the CDK Toolkit how to execute your app.

Customize the CDK toolkit with the following custom variables in `cdk.json`:
<ul>
    <li><code>vpc-cidr</code></li>
    <li><code>rds-engine</code></li>
    <li><code>rds-port</code></li>
    <li><code>rds-database</code></li>
    <li><code>sample-data-s3-bucket</code></li>
    <li><code>sample-data-rds-dump-filename</code></li>
    <li><code>sample-data-redshift-query-s3-bucket</code></li>
    <li><code>sample-data-redshift-query-filename</code></li>
    <li><code>application</code></li>
    <li><code>environment</code></li>
</ul>

## Getting Started

### Pre-Requisites

In order to get started, you will need an AWS account, preferably free from any production workloads. Also, either an IAM role to deploy with from Cloud9 or an IAM user with admin permissions as the stacks we will be deploying require significant access.
Once we have that in place, it’s time to get ready to deploy.

### Cloud9
AWS Cloud9 is a cloud-based integrated development environment (IDE) that lets you write, run, and debug your code with just a browser. Cloud9 comes pre-configured with many of the dependencies we require for this blog post, such as git, npm, and AWS CDK.

Create a Cloud9 environment from the AWS console. Provide the required Name, and leave the remaining default values. Once your Cloud9 environment has been created, you should have access to a terminal window.

### Service-Linked Role

Amazon Elasticsearch Service uses AWS Identity and Access Management (IAM) service-linked roles. A service-linked role is a unique type of IAM role that is linked directly to Amazon ES. Service-linked roles are predefined by Amazon ES and include all the permissions that the service requires to call other AWS services on your behalf. Must create a Service Linked Role for Amazon Elasticsearch if it does not already exist. To create a service-linked role for Amazon ES, issue the following command:

<pre><code>
aws iam create-service-linked-role --aws-service-name es.amazonaws.com
</code></pre>

### Build and Deploy

From the terminal window, let’s clone the GitHub repo, install packages, build, and synthesize the CloudFormation templates. Issue the following commands in a terminal window in Cloud9. The following sections describe the resources created by the respective stacks. By default, AWS CDK will prompt the user to deploy changes. If you want to skip confirmations, add the following command line option to the AWS CDK commands below.  <code>--require-approval never</code>. In addition, you can optionally deploy all stacks by issuing <code>cdk deploy --all</code> rather than issuing separate <code>cdk deploy `<stack name>`</code> commands.

<pre><code>
git clone https://github.com/aws-samples/amazon-neptune-samples
cd amazon-neptune-and-aws-cdk-for-amundsen/amundsen-cdk
# Update to latest npm
npm install -g npm@latest
# Install packages
npm install
# Build
npm run build
# Synthesize CloudFormation
cdk synth
# Deploy each stack
cdk deploy Amundsen-Blog-VPC-Stack
cdk deploy Amundsen-Blog-RDS-Stack
cdk deploy Amundsen-Blog-Redshift-Stack
cdk deploy Amundsen-Blog-Bastion-Stack
cdk deploy Amundsen-Blog-Amundsen-Stack
cdk deploy Amundsen-Blog-Databuilder-Stack
</code></pre>

The Amundsen Frontend Hostname will appear in multiple places. First, the CDK output will include the following output:

`Amundsen-Blog-Amundsen-Stack.amundsenfrontendhostname = <amundsen-frontend-hostname>`

As well, the associated CloudFormation stack `Amundsen-Blog-Amundsen-Stack` will have a key-value pair under outputs with the key `amundsenfrontendhostname`.

## License Summary

This library is licensed under the MIT-0 License. See the LICENSE file.