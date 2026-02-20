# Agentic AI for Amazon Neptune Graph Development

This sample demonstrates how to use AI agents to automate the design, development, and optimization of graph database workloads on Amazon Neptune. Built using the Strands Framework and Amazon Bedrock, this example shows how agents can work independently or in coordinated workflows to accelerate graph application development.

**Important**: This is a sample application for reference purposes and is not intended to be used in production environments.

## What You'll Learn

- How to use AI agents to generate graph data models from use case descriptions
- Automated generation of realistic test data for Neptune databases
- Natural language to query translation
- Query performance analysis and optimization techniques
- Building multi-agent workflows for complex graph development tasks

## Architecture

This sample includes multiple specialized agents that can be used independently or chained together:

1. **Use Case Definition** → Converts topic descriptions into detailed use cases
2. **Model Generation** → Designs graph schemas from use cases
3. **Data Generation** → Creates realistic nodes and edges
4. **Query Generation** → Translates questions into graph queries
5. **Query Optimization** → Analyzes and improves query performance

Each agent uses Amazon Bedrock for AI capabilities and can interact with Neptune through the Neptune MCP Server.

## Sample Agents

The following specialized agents are included in this sample:

* **Use Case Definer Agent**
  - Takes a topic sentence or short statement about your workload
  - Generates detailed use cases with specific questions your solution can answer

* **Model Generator Agent**
  - Accepts detailed use cases and solution requirements
  - Designs comprehensive graph models for the specified use case
  - The current version only generates property graph models. Future enhancements may include RDF/SPARQL.

* **Data Generation Agents**
  - Creates thousands of nodes and edges with realistic data
  - Integrates with Neptune MCP Server for direct cluster writing
  - Brings use cases to life with interactive data

* **Query Generation Agent**
  - Converts natural language questions into valid queries
  - Uses Neptune MCP Server to validate queries against your graph
  - The current version only uses the openCypher language

* **Data Pattern Generation Agent**
  - Simulates specific scenarios (e.g., complex money laundering patterns)
  - Inserts targeted test data directly into Neptune via MCP Server
  - Useful for testing edge cases not covered by standard data generation

* **Query Explainer Agent**
  - Retrieves and analyzes query explain plans
  - Provides step-by-step query execution breakdown
  - Offers query improvement recommendations

* **Query Optimization Agent**
  - Generates multiple query variations
  - Validates query results for consistency
  - Analyzes performance using explain plans
  - Ranks queries by performance
  - Provides tradeoff analysis for different implementations

## Prerequisites

- AWS account with appropriate permissions
- Amazon Neptune Database cluster 
- Python 3.9 or later
- AWS CLI configured with credentials
- [uv package manager](https://docs.astral.sh/uv/) installed (for running Neptune MCP Server via `uvx`)
- Access to Amazon Bedrock with Claude 4.5 Sonnet model enabled

**Note:** The Neptune MCP Server is automatically started by the Python scripts using `uvx` - no separate installation or background process management is required.

### Creating an Amazon Neptune Database cluster
See [Creating a Neptune Cluster](https://docs.aws.amazon.com/neptune/latest/userguide/get-started-create-cluster.html) for instructions on creating an Amazon Neptune cluster. 
To simplify accessing the cluster from your desktop, review [Neptune Public Endpoints](https://docs.aws.amazon.com/neptune/latest/userguide/neptune-public-endpoints.html) to make the cluster publicly accessible.

For detailed IAM permission requirements, see [IAM Permissions](docs/setup/iam_permissions.md).

## Installation

1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd <repository-directory>
   ```

2. Create and activate a virtual environment:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install --upgrade pip
   pip install -r requirements.txt
   ```

5. Configure environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your Neptune endpoint, AWS region, and model ID
   ```

The Neptune MCP Server will be automatically started when you run the scripts - no additional setup needed.

## Usage

Each agent can be used independently or as part of a complete workflow. This example will generally take 20-40 minutes to fully complete, but it can take longer for complex use cases.

### Complete Workflow Example
1. Define your use case using the Use Case Definer Agent
2. Generate a graph model using the Model Generator Agent
3. Populate your database using Data Generation Agents
4. Generate queries using the Query Generation Agent
5. Create targeted test data using the Data Pattern Generation Agent (if needed)
6. Analyze and optimize queries using the Query Explainer and Optimization Agents

### Using Python Scripts Directly

Run the complete end-to-end workflow:
```bash
python full_end_to_end_workflow.py
```

Or run individual workflow steps:
```bash
python step_workflows/generate_usecase_from_topic.py
python step_workflows/generate_model_from_usecase.py
python step_workflows/generate_data_from_model.py
python step_workflows/generate_queries.py
python step_workflows/generate_scenario_data.py
python step_workflows/query_validation_and_optimization.py
```

### Using Kiro CLI (Optional)

If you have Kiro CLI installed, you can use the provided prompts for an interactive workflow:
- `@configure` - Set up Neptune connection
- `@generate-usecase` - Create use case from topic
- `@generate-model` - Design graph model
- `@generate-data` - Populate with test data
- `@generate-queries` - Create queries for your use case
- `@generate-scenario-data` - Ensure queries have appropriate test data

Detailed usage instructions for each agent are provided in the [documentation](docs/agents/index.md).

## Cost Considerations

This sample will incur costs for the following AWS services:

- **Amazon Neptune**: Instance costs plus storage
- **Amazon Bedrock**: Input and output tokens for Claude 4.5 Sonnet model invocations
- **Data Transfer**: Minimal charges for data transfer between services (typically negligible)

**NOTE:** The cost of this example will vary widely depending on the use case and model generated. The data generation step is the most expensive. If you want to limit costs, reduce the number of nodes, edges, and properties created in the model before running the data generation step. For general guidance, this example running in us-east-1 will cost around US$30 in Bedrock charges plus approximately US$0.75 to run a db.r8g.xlarge instance for one hour.

Use the [AWS Pricing Calculator](https://calculator.aws) to estimate costs based on your specific usage patterns.

## Cleanup

To avoid ongoing charges, delete the resources created by this sample:

1. If you created a Neptune cluster specifically for this sample, delete it through the AWS Console or CLI:
   ```bash
   aws neptune delete-db-cluster --db-cluster-identifier your-cluster-name --skip-final-snapshot
   ```

2. Remove local files:
   ```bash
   rm -rf output/
   rm .env
   ```

## Security

This sample implements multiple layers of security to protect your data and AWS resources.

### Credential Management

**Environment Variables (Recommended)**

Store sensitive configuration in a `.env` file (never commit to version control):

```bash
# .env
NEPTUNE_WRITER_ENDPOINT=your-cluster.cluster-xxxxx.us-east-1.neptune.amazonaws.com
LLM_MODEL=anthropic.claude-sonnet-4-20250514
AWS_REGION=us-east-1
```

**Securing .env Files:**

1. **Add to .gitignore** (critical):
   ```bash
   echo ".env" >> .gitignore
   ```

2. **Restrict file permissions**:
   ```bash
   chmod 600 .env
   ```

3. **Never commit to version control**: Use `.env.example` with placeholder values for documentation

4. **Avoid sharing**: Don't send .env files via email, Slack, or other communication channels

**AWS Credentials Management**

**Option 1: IAM Roles (Recommended)**

Use IAM roles when running on AWS compute services - no long-term credentials needed:

```bash
# No configuration required - credentials automatically provided
# Works on: EC2, ECS, Lambda, SageMaker, etc.
```

**Benefits:**
- No long-term credentials to manage or rotate
- Automatic credential rotation by AWS
- Reduced risk of credential exposure
- Easier to audit and manage permissions

**Option 2: IAM User Access Keys (Local Development)**

For local development, use AWS CLI credential configuration:

```bash
aws configure
```

**Option 3: Assume Minimal Temporary Credentials**

Create an IAM role and attach a policy containing the permissions listed in the [Minimum Required Permissions](#minimum-required-permissions) section. Using the ARN of the role as the parameter for `--role-arn`, run the following command using the AWS CLI:
```bash
aws sts assume-role --role-arn arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME --role-session-name my-agent-session
```

The output will be a JSON document with this structure:
```json
{
    "Credentials": {
        "AccessKeyId": "MY-ACCESS-KEY",
        "SecretAccessKey": "MY-SECRET-ACCESS-KEY",
        "SessionToken": "MY-SESSION-TOKEN",
        "Expiration": "2026-12-31T23:22:21+00:00"
    },
    "AssumedRoleUser": {
        "AssumedRoleId": "MY-ASSUMED-ROLE-ID",
        "Arn": "arn:aws:sts::ACCOUNT_ID:assumed-role/ROLE-NAME/SESSION-NAME"
    }
}
```
Set the environment variables for AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_SESSION_TOKEN in the .env file using the values for AccessKeyId, SecretAccessKey, and SessionToken, respectively.

**Credential Priority:**

The AWS SDK checks credentials in this order:
1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`), including those in the .env file
2. Shared credentials file (`~/.aws/credentials`)
3. IAM role (if running on AWS compute)

**NOTE**: This process may take 30-60 minutes depending on the complexity of your workload. If your credentials are short lived, you may need to refresh them several times during the process.

### IAM Permissions

Configure least-privilege IAM permissions for:
- Amazon Bedrock model invocation
- Amazon Neptune database operations

#### Minimum Required Permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "BedrockAccess",
            "Effect": "Allow",
            "Action": [
                "bedrock:InvokeModel",
                "bedrock:InvokeModelWithResponseStream"
            ],
            "Resource": [
               "arn:aws:bedrock:*::foundation-model/anthropic.claude-*",
               "arn:aws:bedrock:*::inference-profile/*"  
            ]
        },
        {
            "Sid": "NeptuneAccess",
               "Effect": "Allow",
               "Action": [
                "neptune-db:connect",
                "neptune-db:ReadDataViaQuery",
                "neptune-db:WriteDataViaQuery",
                "neptune-db:GetEngineStatus",
				    "neptune-db:GetGraphSummary"
            ],
            "Resource": "arn:aws:neptune-db:REGION:ACCOUNT_ID:cluster-CLUSTER_ID/*"
        }
    ]
}
```

Replace `REGION`, `ACCOUNT_ID`, and `CLUSTER_ID` with your actual values.

**Finding Your Neptune Cluster Resource ID:**
```bash
aws neptune describe-db-clusters \
    --db-cluster-identifier YOUR_CLUSTER_NAME \
    --query 'DBClusters[0].DbClusterResourceId' \
    --output text
```

For detailed setup instructions, trust policies, and troubleshooting, see [IAM Permissions Documentation](docs/setup/iam_permissions.md).

### Neptune Security

**Encryption at Rest**

Enable encryption when creating your Neptune cluster:
```bash
aws neptune create-db-cluster \
    --db-cluster-identifier my-neptune-cluster \
    --engine neptune \
    --storage-encrypted \
    --kms-key-id arn:aws:kms:REGION:ACCOUNT_ID:key/KEY_ID
```

**AWS KMS Key Management:**

Neptune uses AWS Key Management Service (KMS) for encryption at rest. You can use either:

1. **AWS managed key** (default): Omit `--kms-key-id` parameter
   ```bash
   aws neptune create-db-cluster \
       --db-cluster-identifier my-neptune-cluster \
       --engine neptune \
       --storage-encrypted
   ```

2. **Customer managed key**: Create and manage your own KMS key
   ```bash
   # Create a KMS key
   aws kms create-key \
       --description "Neptune cluster encryption key"
   
   # Create an alias for easier reference
   aws kms create-alias \
       --alias-name alias/neptune-cluster-key \
       --target-key-id KEY_ID
   
   # Use the key when creating the cluster
   aws neptune create-db-cluster \
       --db-cluster-identifier my-neptune-cluster \
       --engine neptune \
       --storage-encrypted \
       --kms-key-id arn:aws:kms:REGION:ACCOUNT_ID:key/KEY_ID
   ```

**Important**: For existing clusters, encryption at rest cannot be modified. Create a new encrypted cluster and migrate data.

**Local Output File Security**

This sample generates output files in the `output/` directory containing:
- Use case descriptions
- Graph data models
- Generated queries
- Query execution statistics
- Potentially sensitive graph data patterns

**Protecting Local Files:**

1. **File System Encryption**: Use encrypted file systems for the working directory
   - Linux: LUKS, eCryptfs, or dm-crypt
   - macOS: FileVault
   - Windows: BitLocker

2. **Restrict File Permissions**:
   ```bash
   # Limit access to output directory
   chmod 700 output/
   
   # Ensure .env file is not readable by others
   chmod 600 .env
   ```

3. **Secure Deletion**: When removing sensitive files, use secure deletion tools
   ```bash
   # Linux
   shred -vfz -n 3 output/sensitive-file.txt
   
   # macOS
   rm -P output/sensitive-file.txt
   ```

4. **Exclude from Backups**: If output files contain sensitive data, exclude them from automatic backups or ensure backups are encrypted

5. **Clean Up Regularly**: Remove output files when no longer needed
   ```bash
   rm -rf output/
   ```

6. **Consider Encryption at Rest**: For highly sensitive data, encrypt individual files
   ```bash
   # Encrypt with GPG
   gpg --symmetric --cipher-algo AES256 output/sensitive-file.txt
   
   # Decrypt when needed
   gpg --decrypt output/sensitive-file.txt.gpg > output/sensitive-file.txt
   ```

**Encryption in Transit**

Neptune enforces HTTPS connections by default. The Neptune MCP Server automatically uses encrypted connections.

Verify your cluster requires SSL:
```bash
aws neptune describe-db-cluster-parameters \
    --db-cluster-parameter-group-name your-parameter-group \
    --query 'Parameters[?ParameterName==`neptune_enforce_ssl`]'
```

**Network Security**

Neptune clusters run in a VPC and require proper network configuration for access.

**Option 1: Public Endpoints (Simplified Setup)**

Neptune supports public endpoints for easier development and testing without complex VPC configuration. This option requires IAM authentication and is suitable for development environments.

To enable public endpoints on a Neptune instance:

```bash
# Enable IAM authentication on the cluster (required for public endpoints)
aws neptune modify-db-cluster \
    --db-cluster-identifier your-cluster-name \
    --enable-iam-database-authentication

# Create or modify instance with public access
aws neptune modify-db-instance \
    --db-instance-identifier your-instance-name \
    --publicly-accessible
```

Configure security group to allow access from your IP:
```bash
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxx \
    --protocol tcp \
    --port 8182 \
    --cidr YOUR_IP_ADDRESS/32
```

For more details, see [Neptune Public Endpoints documentation](https://docs.aws.amazon.com/neptune/latest/userguide/neptune-public-endpoints.html).

**Option 2: VPC-Only Access (Production Recommended)**

For production environments, keep Neptune private within your VPC.

**Security Group Requirements:**

Your Neptune cluster must allow inbound connections on port 8182 from the source running this sample:

1. **Create a security group for Neptune:**
   ```bash
   aws ec2 create-security-group \
       --group-name neptune-agents-sg \
       --description "Security group for Neptune agents" \
       --vpc-id vpc-xxxxx
   ```

2. **Allow inbound traffic from your application's security group:**
   ```bash
   aws ec2 authorize-security-group-ingress \
       --group-id sg-xxxxx \
       --protocol tcp \
       --port 8182 \
       --source-group sg-yyyyy  # Your application's security group
   ```

**VPC Access Options:**

For local development with VPC-only Neptune clusters, ensure your machine can reach the VPC:
- Use a VPN connection to your VPC
- Use AWS Client VPN
- Deploy to an EC2 instance within the same VPC
- Ensure route tables allow traffic between your source and Neptune subnets

**IAM Database Authentication**

Neptune supports IAM database authentication for fine-grained access control:

1. **Enable IAM auth on your cluster:**
   ```bash
   aws neptune modify-db-cluster \
       --db-cluster-identifier my-neptune-cluster \
       --enable-iam-database-authentication
   ```

2. **Configure the Neptune MCP Server to use IAM auth** by setting environment variables:
   ```bash
   export NEPTUNE_USE_IAM_AUTH=true
   ```

3. **Ensure your IAM policy includes Neptune permissions** (see [IAM Permissions](docs/setup/iam_permissions.md))

### Amazon Bedrock Security

**Model Access Control**

1. **Enable model access in Bedrock console:**
   - Navigate to Amazon Bedrock → Model access
   - Request access to Claude models
   - Access is account and region-specific

2. **Restrict model access via IAM:**
   ```json
   {
       "Effect": "Allow",
       "Action": [
           "bedrock:InvokeModel"
       ],
       "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude-*"
   }
   ```

3. **Use VPC endpoints for private connectivity:**
   ```bash
   aws ec2 create-vpc-endpoint \
       --vpc-id vpc-xxxxx \
       --service-name com.amazonaws.REGION.bedrock-runtime \
       --route-table-ids rtb-xxxxx
   ```

**Data Privacy**

- Bedrock does not store or log your prompts or model outputs for base models
- Data is not used to train or improve base models
- All data in transit is encrypted using TLS 1.2+

### Application Security Controls

**Input Validation**

This sample uses AI agents that accept natural language inputs. Implement validation to prevent malicious inputs:

- **Sanitize user inputs**: Remove or escape special characters before passing to agents
- **Use structured formats**: Prefer JSON or predefined schemas over free-form text when possible
- **Validate input length**: Set maximum input sizes to prevent resource exhaustion
- **Test with untrusted inputs**: Always test agents in isolated environments first

**Output Filtering**

AI-generated queries and data should be validated before execution:

- **Query validation**: Parse and validate generated queries before execution
- **Review destructive operations**: Manually review queries containing DELETE, DROP, or DETACH operations
- **Use read-only credentials**: For query generation workflows, use Neptune credentials with only read permissions
- **Implement query allowlists**: In production, consider restricting to approved query patterns
- **Sanitize outputs**: Remove sensitive information from logs and error messages

**Rate Limiting and Abuse Prevention**

Protect against excessive API usage and cost overruns:

- **Implement rate limits**: Restrict number of agent executions per user/time period
- **Set execution timeouts**: Configure maximum execution time for agent workflows
- **Use circuit breakers**: Stop agent execution after repeated failures
- **Monitor token usage**: Track Bedrock API token consumption with CloudWatch
- **Set budget alerts**: Configure AWS Budgets to alert on unexpected spending
- **Implement backoff strategies**: Use exponential backoff for retries

Example rate limiting implementation:
```python
from time import time, sleep

class RateLimiter:
    def __init__(self, max_calls, time_window):
        self.max_calls = max_calls
        self.time_window = time_window
        self.calls = []
    
    def allow_request(self):
        now = time()
        self.calls = [t for t in self.calls if now - t < self.time_window]
        if len(self.calls) < self.max_calls:
            self.calls.append(now)
            return True
        return False
```

**Prompt Injection Mitigation**

Protect against prompt injection attacks that manipulate agent behavior:

- **Validate inputs**: Check for suspicious patterns (e.g., "ignore previous instructions")
- **Use structured prompts**: Separate user input from system instructions
- **Implement output validation**: Verify generated queries match expected patterns
- **Monitor for anomalies**: Track unusual agent behavior or outputs
- **Test with adversarial inputs**: Regularly test with known prompt injection techniques
- **Use separate environments**: Never test untrusted inputs against production data
- **Review agent logs**: Monitor for unexpected tool invocations or query patterns

### Reporting Security Issues

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information on reporting security issues.

## License

See [LICENSE](LICENSE) for license and copyright information.
