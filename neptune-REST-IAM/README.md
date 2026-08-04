# Interaction with Neptune Cluster with IAM db authentication enabled

## About

This sample project shows how you can interact with a Neptune cluster when the
IAM db authentication is enabled.

According to the Neptune documentation,
the AWS Identity and Access Management (IAM) is an AWS service
that helps an administrator securely control access to AWS resources.
IAM administrators control who can be authenticated (signed in) and authorized
(have permissions) to use Neptune resources.
IAM is an AWS service that you can use with no additional charge.

You can use AWS Identity and Access Management (IAM) to authenticate to your Neptune DB instance or DB cluster. When IAM database authentication is enabled, each request must be signed using AWS Signature Version 4.

## Prerequisites

- [Node.js](https://nodejs.org/en/) version 24 or later (matches the Lambda runtime target)
- [AWS CDK CLI](https://docs.aws.amazon.com/cdk/v2/guide/cli.html) v2
- AWS credentials configured for your target account/region
- CDK bootstrapped in the target region (`cdk bootstrap`)

## How to install it?

This is a TypeScript [CDK v2](https://docs.aws.amazon.com/cdk/v2/guide/home.html) based project.

After you clone this repository, install the Node.js dependencies:

```bash
npm install
```

### Building

The project has two build steps:

1. **TypeScript compilation** (CDK infrastructure code):
   ```bash
   npm run build
   ```

2. **Webpack bundle** (Lambda handler code bundled into `./build`):
   ```bash
   npm run bundle
   ```

### Deploying

After dependencies are installed, deploy the CDK stack:

```bash
./deploy.bash <region>
```

For example:

```bash
./deploy.bash us-west-2
```

This operation may take around 10 minutes. If the deployment is successful
you will see output like:

```
Synth Complete, deploying Stack
NeptuneRestIamStack: deploying...

 ✅  NeptuneRestIamStack

Outputs:
NeptuneRestIamStack.BulkLoaderRoleArn = arn:aws:iam::123456789012:role/NeptuneRestIamStack-NeptuneLoadFromS3-XXXXX
NeptuneRestIamStack.NeptuneClusterEndpoint = neptune-test-cluster.cluster-xxxxx.us-west-2.neptune.amazonaws.com
NeptuneRestIamStack.NeptuneInstanceEndpoint = neptunedbinstance-xxxxx.us-west-2.neptune.amazonaws.com
NeptuneRestIamStack.SecurityGroupId = sg-0xxxxxxxxxx
NeptuneRestIamStack.Subnets = subnet-xxxxx,subnet-xxxxx
NeptuneRestIamStack.UploadBucketName = neptunerestiamstack-uploadxxxxx-xxxxx
NeptuneRestIamStack.ClusterStateFunctionName = NeptuneRestIamStack-clusterStateXXXXX-XXXXX
NeptuneRestIamStack.BulkUploadFunctionName = NeptuneRestIamStack-bulkUploadXXXXX-XXXXX
NeptuneRestIamStack.GetAllBulkJobsFunctionName = NeptuneRestIamStack-getAllBulkJobsXXXXX-XXXXX
NeptuneRestIamStack.CountVerticesFunctionName = NeptuneRestIamStack-countVerticesXXXXX-XXXXX
NeptuneRestIamStack.AddDataFunctionName = NeptuneRestIamStack-addDataXXXXX-XXXXX
NeptuneRestIamStack.DropAllFunctionName = NeptuneRestIamStack-dropAllXXXXX-XXXXX
```

This CDK stack installs a Neptune Cluster with IAM db authentication enabled and adds Lambda functions able to interact with the Neptune Cluster.

## What gets deployed

The stack creates the following resources:

- A VPC with private subnets and an S3 gateway endpoint
- A Neptune cluster (`db.t3.medium`) with IAM authentication enabled and audit logging
- An S3 bucket for CSV data uploads
- Six Lambda functions (deployed into the VPC so they can reach Neptune):
  - **getClusterStatus** — returns the Neptune cluster status via the REST API
  - **bulkUploadHandler** — triggered automatically when a `.csv` file is uploaded to the S3 bucket; kicks off a Neptune bulk load job
  - **getAllBulkJobsHandler** — lists all bulk load jobs via the REST API
  - **countVerticesHandler** — counts all vertices in the graph using Gremlin
  - **addDataToNeptuneHandler** — adds sample person vertices using Gremlin
  - **dropAllHandler** — drops all vertices in the graph using Gremlin
- IAM roles for Lambda execution and for Neptune to read from S3

## Using the stack

Once deployed, you can interact with the Neptune cluster by invoking the Lambda functions.

**Load data from S3:** Upload a CSV file to the S3 bucket created by the stack. The `bulkUploadHandler` Lambda triggers automatically and starts a Neptune bulk load job. Sample CSV files are in the `payload/gremlin/` directory:

```bash
aws s3 cp payload/gremlin/vertex.csv s3://<upload-bucket-name>/vertex.csv
aws s3 cp payload/gremlin/edge.csv s3://<upload-bucket-name>/edge.csv
```

**Invoke a Lambda directly:** Use the AWS CLI to call any of the functions. For example, to count vertices:

```bash
aws lambda invoke --function-name <countVertices-function-name> --region <region> output.json
cat output.json
```

**Check cluster status:**

```bash
aws lambda invoke --function-name <clusterState-function-name> --region <region> output.json
cat output.json
```

The function names and S3 bucket name can be found in the AWS CloudFormation console under the `NeptuneRestIamStack` stack resources, or via `aws cloudformation describe-stack-resources`.

## Interaction with Neptune Cluster

The interaction with the Neptune cluster can be done in two ways:

1. Over the Neptune REST API
2. Over the [Gremlin](https://tinkerpop.apache.org/gremlin.html) client

In both cases, all requests must be signed using
[AWS Signature Version 4](https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html).

All signing is performed using official AWS SDK v3 libraries (`@smithy/signature-v4`
and `@aws-sdk/credential-providers`). No third-party signing utilities are used.

### REST Based interaction

Here is an example for the REST based interaction
originated from the `bulkUploadHandler.ts` file:

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
        `File ${key} was processed with response ${JSON.stringify(
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

The `neptunePost` method can be found in `src/handlers/utils.ts`.
It uses `@smithy/signature-v4` to sign requests directly with AWS Signature
Version 4 and sends them via the native Node.js `fetch` API.

When running in AWS Lambda, credentials are automatically resolved from the
Lambda execution environment via `@aws-sdk/credential-providers`.

Without a signed request, all attempts to communicate with the Neptune cluster will result in an HTTP 403 error.

Here is example output from the `bulkUploadHandler` after the file `edge.csv` was uploaded:

```
2021-03-09T16:41:08.692Z  INFO  File edge.csv was processed with response response {
    "data": {
        "status": "200 OK",
        "payload": {
            "loadId": "f53d2153-95d4-4b23-b0af-b54753e353a5"
        }
    },
    "status": 200,
    "statusText": "OK"
}.
```

The same pattern applies to the `clusterStateHandler` and `getAllBulkJobsHandler` Lambdas.

### Gremlin based interaction

The [Apache TinkerPop Gremlin JavaScript client](https://www.npmjs.com/package/gremlin)
(v3.8+) is used with a WebSocket connection to Neptune. Request signing is handled
by computing SigV4 headers via `@smithy/signature-v4` and passing them to the
WebSocket upgrade request:

```typescript
const countVerticesHandler: TaskHandler = async (event: any, context: any) => {
  const result = await gremlinQuery<number>(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    countVertices,
    getVerticesCount
  );
  console.log('countVertices=', result);
  return result;
};

async function countVertices(g: GraphTraversalSource): Promise<any> {
  return g.V().count().next();
}
```

The `gremlinQuery` method can be found in `src/handlers/utils.ts`.
It creates a `DriverRemoteConnection` with SigV4-signed headers computed from
the AWS credential chain, which Neptune validates on the WebSocket handshake.
The same pattern applies to `addDataToNeptuneHandler`, `dropAllHandler`, and `countVerticesHandler`.
