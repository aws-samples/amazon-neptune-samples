# Neptune Infrastructure CDK

This CDK project deploys an Amazon Neptune database with a SageMaker notebook instance for secure access and model training.

## Architecture

The stack creates:
1. VPC with public and private subnets
2. Neptune cluster in private subnet
3. SageMaker notebook instance in private subnet
4. Required VPC endpoints and security groups
5. IAM roles and policies for Neptune and SageMaker access

## Prerequisites

1. npm installed (required to install AWS CDK CLI)
2. AWS CDK CLI installed (`npm install -g aws-cdk`)
3. Python 3.9+
4. AWS credentials configured

## Deployment

1. Create and activate a virtual environment:
```bash
python -m venv .venv
source .venv/bin/activate  # Linux/macOS on bash
# .venv\Scripts\activate     # Windows
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Deploy the stack:

Single stack deployment:
```bash
cdk deploy
```

Multiple stack deployment:
You can customize the deployment using context variables:

1. `prefix`: Prefix for resource names (e.g., "dev-", "staging-", "prod-")
2. `neptune_instance_type`: Neptune DB instance type (default: "db.r8g.4xlarge")
3. `notebook_instance_type`: SageMaker notebook instance type (default: "ml.m5.4xlarge")

Examples:

```bash
# Deploy dev environment with default instance types
cdk deploy --context prefix=dev-

# Deploy staging with custom instance types
cdk deploy --context prefix=staging- \
          --context neptune_instance_type=db.r8g.8xlarge \
          --context notebook_instance_type=ml.m5.8xlarge

```

The prefix will be applied to all resources in the stack to ensure uniqueness. For example:
- `dev-NeptuneVPC`
- `dev-NeptuneCluster`
- `dev-NeptuneNotebook`

Notes:
- Include a hyphen (-) at the end of your prefix if you want resources to be named like `dev-ResourceName` instead of `devResourceName`
- Choose Neptune instance types from the [Neptune pricing page](https://aws.amazon.com/neptune/pricing/)
- Choose SageMaker instance types from the [SageMaker pricing page](https://aws.amazon.com/sagemaker/pricing/)

## Accessing Neptune

### 1. SageMaker Notebook Setup

The SageMaker notebook instance is automatically configured with:
- Graph notebook environment
- Neptune connection settings
- Required IAM permissions
- Security group rules for Neptune access

### 2. Opening the Notebook

1. Go to the AWS SageMaker console
2. Find your notebook instance (name from stack outputs)
3. Click "Open JupyterLab"
4. The environment variables for Neptune connection are pre-configured:
   - GRAPH_NOTEBOOK_AUTH_MODE=IAM
   - GRAPH_NOTEBOOK_SSL=True
   - GRAPH_NOTEBOOK_HOST=<neptune-endpoint>
   - GRAPH_NOTEBOOK_PORT=8182

### 3. Testing Connectivity

The notebook environment includes the graph notebook package with built-in connectivity testing. You can run:

```python
%graph_notebook_config
```

A successful connection will show Neptune's configuration status.

### 4. Common Issues

If you encounter connection issues:

1. Verify security group rules:
   - SageMaker -> Neptune (port 8182)
   - Neptune <- SageMaker (port 8182)

2. Check IAM permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "neptune-db:GetEngineStatus",
        "neptune-db:ReadDataViaQuery",
        "neptune-db:WriteDataViaQuery",
        "neptune-db:DeleteDataViaQuery",
        "neptune-db:GetQueryStatus",
        "neptune-db:CancelQuery"
      ],
      "Resource": ["arn:aws:neptune-db:region:account:*/database"]
    }
  ]
}
```

For detailed troubleshooting steps, see `neptune-connection-troubleshooting.md`.

## Stack Outputs

The stack provides several useful outputs:
- `NeptuneEndpoint`: Neptune cluster endpoint
- `NeptunePort`: Neptune port (8182)
- `NotebookInstanceName`: SageMaker notebook instance name
- `NeptuneRoleArn`: IAM role ARN for Neptune
- `DataBucketName`: S3 bucket for data loading
- `VpcId`: VPC ID for SageMaker endpoints
- `PrivateSubnetIds`: Private subnet IDs for SageMaker endpoints (comma-separated)
- `SageMakerSecurityGroupId`: Security group ID for SageMaker endpoints
- `SageMakerEndpointRoleArn`: IAM role ARN for SageMaker endpoints with scoped permissions:
  - CloudWatch:
    - Logs: Limited to /aws/sagemaker/Endpoints/* log groups
    - Metrics: Limited to AWS/SageMaker namespace
  - S3: Read-only access to model artifacts path
  - ECR:
    - Pull access to all repositories in account
    - GetAuthorizationToken for ECR authentication
    - VPC:
      - Full EC2 permissions for VPC networking:
        - CreateNetworkInterface, DeleteNetworkInterface
        - CreateNetworkInterfacePermission, DeleteNetworkInterfacePermission
        - DescribeVpcEndpoints, DescribeDhcpOptions
        - DescribeVpcs, DescribeSubnets
        - DescribeSecurityGroups, DescribeNetworkInterfaces
      - S3 VPC endpoint access:
        - Access to model artifacts bucket
        - Access to Amazon Linux package repositories

### Using with SageMaker Endpoints

When deploying SageMaker endpoints that need to access Neptune DB, you can
use the CDK outputs to deploy the endpoint:

```bash
python graphstorm/sagemaker/launch/launch_realtime_endpoint.py \
  --vpc-subnet-ids $(cdk output --context prefix=<your-prefix> PrivateSubnetIds | tr ',' ' ') \
  --vpc-security-group-ids $(cdk output --context prefix=<your-prefix> SageMakerSecurityGroupId) \
  [other required arguments...]
```

The endpoints will be deployed in the same VPC as Neptune DB, using:
- Private subnets with NAT Gateway access
- The same security group as SageMaker notebooks, which already has:
  - Outbound access to Neptune DB on port 8182
  - Outbound access to S3 VPC endpoint for model loading
  - Inbound rules configured in Neptune's security group

For internet-accessible Neptune endpoints, you can omit the VPC configuration arguments.

## Security

The infrastructure is designed with security best practices:
- No public internet access to Neptune
- IAM authentication enabled
- Least privilege permissions
- VPC endpoints for AWS services
- SageMaker notebook in private subnet
- Scoped IAM roles for specific resources
- CloudWatch logging enabled

## Cleanup

To avoid charges, **after deleting all deployed endpoints** you can delete the stack

```bash
# Make sure to provide any context variables if used
cdk destroy # [--context prefix=dev-]
```

It's important to delete any deployed SageMaker endpoints **before** running `cdk destroy`, otherwise
`cdk destroy` might fail. The reason is that the endpoint is using a network interface
within the VPC, so we can't destroy the VPC before all network interfaces attached
to it are deleted.


`cdk destroy` will remove all resources except:
- S3 bucket (retained to prevent data loss)
- CloudWatch logs
- SageMaker endpoints created outside the CDK - these are not managed by CDK.

> NOTE: If you created additional notebooks, endpoints or resources attached to the same VPC you may run into errors deleting the stack.
  This can happen if e.g. you create a new graph notebook through the Neptune console, attaching it to the same Neptune DB instance.
  Ensure you stop and delete all notebook instances/endpoints that are part of the VPC before trying to delete the stack again.
