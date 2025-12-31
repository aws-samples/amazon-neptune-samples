# Interaction with Neptune Cluster with IAM db authentication enabled

## About

This sample project shows how you can interact with a Neptune cluster when the
IAM db authentication is enabled.

According with the Neptune documentation,
the AWS Identity and Access Management (IAM) is an AWS service
that helps an administrator securely control access to AWS resources.
IAM administrators control who can be authenticated (signed in) and authorized
(have permissions) to use Neptune resources.
IAM is an AWS service that you can use with no additional charge.

You can use AWS Identity and Access Management (IAM) to authenticate to your Neptune DB instance or DB cluster. When IAM database authentication is enabled, each request must be signed using AWS Signature Version 4.

## How to install it?

This is a typescript [CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) based project.
CDK is a [NodeJS](https://nodejs.org/en/) module, and this is the only perquisite.

This project was tested with NodeJs version 12.16.1.

After you clone this repository, ensure that all the node dependencies are
installed with the following command:

```bash
npm install
```

After all the nodeJS dependencies are claimed you can install the CDK stack with
the the following command:

```bash
./deploy.bash
```

This operation will may take around 10 mins. If the deployment is successful
then you may be able to see an output as the next one:

```bash
./deploy.bash
......
......
Synth Complete, deploying Stack
NeptuneRestIamStack: deploying...
[0%] start: Publishing ff6804beac5d60d6ec143c1d10f08e10591a5d671f588baf1cabf18e27949177:current
[100%] success: Published ff6804beac5d60d6ec143c1d10f08e10591a5d671f588baf1cabf18e27949177:current
NeptuneRestIamStack: creating CloudFormation changeset...
[██████████████████████████████████████████████████████████] (42/42)



 ✅  NeptuneRestIamStack

Outputs:
NeptuneRestIamStack.BulkLoaderRoleArn = arn:aws:iam::495161600685:role/NeptuneRestIamStack-NeptuneLoadFromS30422660D-G5ZI9V5C50J7
NeptuneRestIamStack.NeptuneClusterEndpoint = neptune-test-cluster.cluster-cl9mufznljov.eu-west-1.neptune.amazonaws.com
NeptuneRestIamStack.NeptuneInstanceEndpoint = neptunedbinstance-wnxhffni670a.cl9mufznljov.eu-west-1.neptune.amazonaws.com
NeptuneRestIamStack.SecurityGroupId = sg-0ead6abc144196f49
NeptuneRestIamStack.Subnets = subnet-0f759cdb19d8b13b3,subnet-081f56f0c0c288565

```

This CDK stack install an Neptune Cluster with IAM db authentication enabled and adds four six lambda able to interact with the Neptune Cluster.

### Interaction with Neptune Cluster

The interaction with the Neptune cluster can be done in two ways:

1. over the Neptune REST API
2. over the [Gremlin](https://tinkerpop.apache.org/gremlin.html) client. 

In botch cases you need to interact with the cluster you need to sing all the requests using
[aws sig4](https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html).

#### REST Based interaction

Here is an example for the REST based interaction
originated from the bulkUploadHandler.ts file:

```typescript
    try {
      const response = await neptunePost(
        process.env.NEPTUNE_ENDPOINT,
        process.env.NEPTUNE_PORT,
        process.env.AWS_DEFAULT_REGION,
        '/loader',
        body
      );
      console.log(
        `File ${key} was processed with response response ${JSON.stringify(
          response,
          null,
          2
        )}.`
      );
    } catch (e) {
      console.error(`File ${key} can not be processed.`);
      console.error(e);
    }
```

The `neptunePost` method can be found in the utils.ts file. 
The `neptunePost`  uses the [aws4-axios](https://www.npmjs.com/package/aws4-axios) 
to sign all the requests send to the Neptune Cluster using aws4.
The aws4 library uses your secretAccessKey, accessKeyId and sessionToken in order to sing the request.

If you use this code in a AWS Lambda, then the secretAccessKey, accessKeyId and sessionToken are obtained from the environment variables.

Without a sign request all the attempts to communicate with Neptune cluster will end with HTTP 403 error.

Here is the result bulkUploadHandlerv output after the file `edge.csv` was uploaded.

``
2021-03-09T16:41:08.692Z	17c6d7a7-c870-42eb-9b36-9585fbfac76b	INFO	File edge.csv was processed with response response {
    "data": {
        "status": "200 OK",
        "payload": {
            "loadId": "f53d2153-95d4-4b23-b0af-b54753e353a5"
        }
    },
    "status": 200,
    "statusText": "OK"
}.

``

Same logic for the clusterStateHandler and getAllBulkJobsHandler lambdas.

#### Gremlin based interaction

For Gremlin based access you need to use use a library able to sign the request.
The next example uses the [gremlin-aws-sigv4](https://www.npmjs.com/package/gremlin-aws-sigv4) 

```typescript
const countVerticesHandler: TaskHandler = async (event: any, context: any) => {
  const result: CountVerticesResult = await gremlinQuery(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    defaultGremlinOpts,
    context,
    countVertices,
    getVerticesCount
  );
  console.log('countVertices=', result);
  return result;
};

async function countVertices(
  g: gremlin.driver.AwsSigV4DriverRemoteConnection
): Promise<any> {
  return g.V().count().next();
}
```

The `gremlinQuery` method can be found in the utils.ts file. 
The `gremlinQuery` uses setup the gremlin client and cares about the signing 
the request.
Same logic for: `addDataToNeptuneHandler`, `dropAllHandler` and `countVerticesHandler`.
