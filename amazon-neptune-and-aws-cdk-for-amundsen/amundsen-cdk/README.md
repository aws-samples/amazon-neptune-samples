# Welcome to the Amundsen Blog CDK project!

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

 * `npm install`     install current package context
 * `npm run build`   compile typescript to js
 * `npm run watch`   watch for changes and compile
 * `npm run test`    perform the jest unit tests
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk synth`       emits the synthesized CloudFormation template

 ## Pre-requisites

Must create a Service Linked Role for Amazon Elasticsearch if it does not already exist.

<pre><code>
aws iam create-service-linked-role --aws-service-name es.amazonaws.com
</code></pre>

Customize the CDK toolkit with the following custom variables in `cdk.json`:
<ul>
    <li>vpc-cidr</li>
    <li>rds-engine</li>
    <li>rds-port</li>
    <li>rds-database</li>
    <li>sample-data-s3-bucket</li>
    <li>sample-data-rds-dump-filename</li>
    <li>sample-data-redshift-query-s3-bucket</li>
    <li>sample-data-redshift-query-filename</li>
    <li>application</li>
    <li>environment</li>
</ul>

<pre><code>
    npm run build
    cdk synth
    cdk deploy Amundsen-Blog-VPC-Stack
    cdk deploy Amundsen-Blog-RDS-Stack
    cdk deploy Amundsen-Blog-Redshift-Stack
    cdk deploy Amundsen-Blog-Bastion-Stack
    cdk deploy Amundsen-Blog-Amundsen-Stack
    cdk deploy Amundsen-Blog-Databuilder-Stack
</code></pre>
