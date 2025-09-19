# Neptune Database GraphStorm Online Inference

This project demonstrates how to set up and use Amazon Neptune with GraphStorm for online inference. The project consists of a CDK infrastructure deployment and a series of Jupyter notebooks that guide you through the process of data preparation, model training, and endpoint deployment.

## Project Structure

- `neptune-db-cdk/`: Contains the AWS CDK infrastructure code for deploying the required AWS resources
- `notebooks/`: Contains the Jupyter notebooks for the end-to-end workflow
- `requirements.txt` and `requirements-ndb.txt`: Python dependencies for the project

## Prerequisites

1. [AWS CDK CLI](https://docs.aws.amazon.com/cdk/v2/guide/getting-started.html) installed and configured
   ```bash
   npm install -g aws-cdk
   ```

2. [AWS credentials](https://docs.aws.amazon.com/IAM/latest/UserGuide/security-creds.html) configured with appropriate permissions
   ```bash
   aws configure
   ```

3. Python 3.9+ installed

4. Configured [AWS Quotas](https://docs.aws.amazon.com/general/latest/gr/aws_service_limits.html) for your AWS account to deploy:
* One `ml.m5.4xlarge` SageMaker Notebook Instance (Quota code: `L-862C1E14`)
* One `ml.r6i.xlarge` SageMaker endpoint (Quota code: `L-28FF72CA`)
* One `db.r8g.xlarge` Neptune Database Instance (should be available by default)

You can also select custom instance types to use during deployment, see
the instructions below.

To request quotas for the SageMaker instances you can use the AWS CLI or
[the AWS Console UI](https://us-east-1.console.aws.amazon.com/servicequotas/home/dashboard).

```bash
# Enter your desired deployment region
REGION=us-east-1
# Request at least 1 ml.m5.4xlarge for notebook instance usage
aws service-quotas request-service-quota-increase \
  --service-code sagemaker \
  --quota-code L-862C1E14 \
  --desired-value 1 \
  --region $REGION

# Request at least 1 ml.r6i.xlarge for endpoint usage
aws service-quotas request-service-quota-increase \
  --service-code sagemaker \
  --quota-code L-28FF72CA \
  --desired-value 1 \
  --region $REGION
```

## Getting Started

### 1. Set up and Deploy CDK Infrastructure

1. Navigate to the CDK project directory:
   ```bash
   cd neptune-db-cdk
   ```

2. Create and activate a virtual environment:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # Linux/macOS
   ```

3. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Deploy the CDK stack:
   ```bash
   # Basic deployment to your default region
   cdk deploy

   # Or deploy with custom configuration/region
   export CDK_DEFAULT_REGION=us-east-1
   cdk deploy --context prefix=dev- \
             --context neptune_instance_type=db.r8g.xlarge \
             --context notebook_instance_type=ml.m5.4xlarge
   ```

   This will create the necessary AWS resources including:
   - Amazon Neptune database cluster
   - Amazon SageMaker notebook instance
   - Required IAM roles and security groups
   - VPC with public/private subnets and security configurations

The deployment will output important information like:
- Neptune endpoint and port
- SageMaker notebook instance name
- VPC and subnet IDs
- Security group IDs
- IAM role ARNs

### 2. Access the Notebooks

1. After the CDK deployment completes, navigate to the AWS SageMaker console
2. Find the newly created notebook instance, it will have a name similar to `<optional-prefix>NeptuneNotebook-9KgSB9g7SNBN`
3. Click "Open JupyterLab"
4. Upload all files from the `notebooks/` directory to your notebook instance

### 3. Run the Notebooks

The notebooks should be executed in order:

1. `0-Data-Preparation.ipynb`: Prepare the data for training
2. `1-Load-Data-Into-Neptune-DB.ipynb`: Load the prepared data into Neptune
3. `2-Model-Training.ipynb`: Train the GraphStorm model
4. `3-GraphStorm-Endpoint-Deployment.ipynb`: Deploy the trained model
5. `4-Sample-graph-and-invoke-endpoint.ipynb`: Test the deployed endpoint

Note: In future versions, these notebooks and python files will be automatically available in the notebook instance through repository cloning.

## Additional Resources

- For Neptune connection troubleshooting, refer to `neptune-db-cdk/neptune-connection-troubleshooting.md`
- For detailed CDK infrastructure information, check the README in the `neptune-db-cdk/` directory

## Cleanup

To avoid ongoing charges, you can delete the infrastructure when no longer needed:

### SageMaker endpoint cleanup

If you run through notebook `3-GraphStorm-Endpoint-Deployment.ipynb` you will deploy a SageMaker inference endpoint. To avoid charges to your account you will need to
delete the endpoint. Refer to the [SageMaker documentation for details](https://docs.aws.amazon.com/sagemaker/latest/dg/realtime-endpoints-delete-resources.html), e.g. to delete the endpoint using the AWS CLI use:

```bash
aws sagemaker delete-endpoint --endpoint-name <endpoint-name>
```

It's important to delete the endpoint **before** running `cdk destroy`, otherwise
`cdk destroy` might fail. The reason is that the endpoint is using a network interface
within the VPC, so we can't destroy the VPC before all network interfaces attached
to it are deleted.


### CDK cleanup

If you no longer need access to the Neptune Database and SageMaker notebook instance, **after deleting all deployed endpoints** you can destroy the CDK resources using

```bash
# Ensure you are at the CDK project directory
cd neptune-db-cdk
# Make sure to provide any context variables if used
cdk destroy # [--context prefix=dev-]
```

Note: The S3 bucket and CloudWatch logs will be retained to prevent data loss. You will need to empty and delete the bucket manually
