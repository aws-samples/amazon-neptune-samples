# Neptune Analytics GraphStorm Fraud Detection

This repository contains code and notebooks accompanying a blog post that demonstrates how to use Amazon Neptune Analytics and GraphStorm for fraud detection in financial transactions.

We use the IEEE CIS dataset for payment fraud detection.

## Prerequisites

Before running the notebooks, ensure you have:

1. Python >= 3.9 installed
2. Required Python packages (installed via `pip install -r requirements.txt`):
   - Core dependencies for graph processing
   - Visualization libraries (matplotlib)
   - Machine learning libraries (scikit-learn)
   - Data processing libraries (pandas, numpy)
3. An AWS account with access to:
   - Amazon Neptune Analytics
   - Amazon SageMaker AI
   - Amazon Simple Storage Service (S3)
   - Amazon Elastic Container Registry (ECR)

### Required AWS IAM Permissions

For demo purposes you can provide the IAM entity (e.g. role/user) that executes these notebooks with the following permissions:

1. For Neptune Analytics:
   - `AWSNeptuneAnalyticsFullAccess`
   - `AmazonS3FullAccess` (or more restricted S3 access based on your bucket)
   - For bulk import:
     - `AmazonS3ReadOnlyAccess` (or more restricted S3 access)
     - See [Bulk Import IAM Role Setup](https://docs.aws.amazon.com/neptune-analytics/latest/userguide/bulk-import-create-from-s3.html#create-iam-role-for-s3-access)
   - [Optional] AWS KMS permissions if using encrypted S3 buckets

2. For SageMaker and ECR:
   - `AmazonSageMakerFullAccess`
   - Permissions to pull images from SageMaker public ECR registry
   - Permissions to create repositories and push images to your account's private ECR registry
   - See [ECR IAM Policy Examples](https://docs.aws.amazon.com/AmazonECR/latest/userguide/security_iam_id-based-policy-examples.html)

3. For SageMaker Pipeline Execution:
   - A SageMaker execution role with:
     - `AmazonSageMakerFullAccess`
     - `AmazonS3FullAccess` (or more restricted S3 access as needed)
     - AWS KMS permissions if using encrypted S3 buckets
   - See [SageMaker Roles Documentation](https://docs.aws.amazon.com/sagemaker/latest/dg/sagemaker-roles.html)

> NOTE: These are **permissive** policies that you should only use for demo purposes.
  For production use cases ensure you are using policies that assign
  [least privilege permissions](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html#grant-least-privilege).

For detailed permissions and trust policy requirements, see:
- [Neptune Analytics IAM Roles](https://docs.aws.amazon.com/neptune-analytics/latest/userguide/security-iam.html)
- [Import/Export Permissions](https://docs.aws.amazon.com/neptune-analytics/latest/userguide/import-export-permissions.html)
- [Neptune Analytics Notebook Setup](https://docs.aws.amazon.com/neptune-analytics/latest/userguide/create-notebook-console.html#create-notebook-iam-role)

## Environment Setup

### Local Setup

1. Clone this repository:
```bash
git clone https://github.com/aws-samples/amazon-neptune-samples.git
cd neptune-analytics-graphstorm-fraud-detection
```

2. Install the required dependencies:
```bash
pip install -r requirements.txt
```

3. Clone GraphStorm repository
```bash
git clone https://github.com/awslabs/graphstorm.git $HOME/graphstorm
```

This will install all necessary packages, including the local `neptune-gs` package which is used to ease data import/export between Neptune Analytics and GraphStorm, and clone the GraphStorm repository that includes helper scripts to create and execute a
GraphStorm SageMaker Pipeline.

## Notebooks Overview

The repository contains a series of notebooks that guide you through the process:

1. **0-Data-Preparation.ipynb**
   - Creates a Neptune Analytics graph
   - Converts tabular IEEE CIS data to Neptune Analytics export format
   - Creates GraphStorm configuration JSON for graph construction

2. **1-SageMaker-Setup.ipynb**
   - Prepares the SageMaker environment, creating a Graph Notebook instance.
   - Builds and pushes necessary Docker images

3. **2-Deploy-Execute-Pipeline.ipynb**
   - Deploys and executes the fraud detection pipeline
   - Creates enriched graph data, attaching GNN embeddings and predictions for
     every transaction node.

4. **3-Use-Embeddings-Locally.ipynb**
   - Demonstrates how to use the generated embeddings
   - Creates t-SNE visualizations of embedding quality
   - Performs offline evaluation with ROC and Precision-Recall curves
   - Shows model performance metrics on test data

5. **4-Import-Embeddings-to-Neptune-Analytics.ipynb**
   - Imports enriched graph data back to Neptune Analytics

6. **5-Explore-With-Neptune-Analytics.ipynb**
   - Investigates GNN embeddings and predictions
   - Performs risk score validation using known labels
   - Conducts community detection to identify suspicious patterns
   - Analyzes feature combinations in high-risk transactions
   - Uses graph embeddings for similarity-based fraud detection
   - Demonstrates hybrid approach combining ML predictions with graph analytics

### SageMaker Instance Requirements

The default configuration uses `ml.m5.4xlarge` instances. Note:
- Your AWS account may have a default quota of 0 for this instance type
- Options if you encounter quota issues:
  1. [Request a quota increase](https://docs.aws.amazon.com/sagemaker/latest/dg/regions-quotas.html#regions-quotas-quotas) for processing and training jobs.
  2. Use alternative instance types with similar specifications (can be configured with the deployment script)

## Pipeline Execution

The GraphStorm SageMaker pipeline consists of the following steps:
1. GConstruct: Processes node identifiers, features, labels, and partitions the graph for distributed training
2. Training: Trains the classification model and produces output model and learnable embeddings
3. Inference: Generates GNN embeddings and predictions for every target node

The execution takes approximately 25 minutes and can be monitored through the SageMaker Studio Pipelines UI.

## Data Processing Pipeline

The overall workflow follows these main steps:

1. Create a Neptune Analytics graph and prepare the data
2. Export the graph data in a format compatible with GraphStorm
3. Use GraphStorm for graph neural network training
4. Generate embeddings and fraud predictions
5. Import enriched data back to Neptune Analytics:
   - Move training/validation/test splits to separate location
   - Import graph data with embeddings and predictions
   - Create vertex index for efficient querying
6. Analyze model performance:
   - Visualize embedding quality using t-SNE
   - Evaluate model accuracy using ROC and Precision-Recall curves
   - Assess performance on held-out test data
7. Explore results in Neptune Analytics:
   - Validate risk scores against known fraud labels
   - Detect suspicious communities using Louvain algorithm
   - Analyze feature combinations in high-risk transactions
   - Identify bridge features connecting fraudulent activities
   - Use graph embeddings to find similar suspicious transactions
   - Combine ML predictions with graph structure analysis

## Additional Resources

- [Neptune Analytics Documentation](https://docs.aws.amazon.com/neptune-analytics/latest/userguide/)
- [GraphStorm Documentation](https://graphstorm.readthedocs.io/)
- [Blog Post Link](<blog-post-url>)

## License

This project is licensed under the MIT-0 - see the LICENSE file for details.
