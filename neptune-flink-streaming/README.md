# Flink Streaming to Neptune Sample Application

This sample application demonstrates how to stream tabular graph data from Apache Flink to Amazon Neptune Database using Java and the AWS SDK. The application processes vertex and edge data in tabular format and writes them to Neptune as graph structures using batched operations.

**Important**: This is a sample application for reference purposes. Your internal teams should evaluate it for security, data protection, and regulatory compliance before using in production environments.

## Use Case

This application solves the challenge of streaming graph data from real-time data sources into Amazon Neptune for graph analytics. It's designed for scenarios where:

- Graph data arrives in tabular format from streaming sources (Kinesis, Kafka, etc.)
- You need to maintain vertex-before-edge ordering within data partitions
- You want to leverage managed Flink services for scalable stream processing
- You need efficient batched writes to Neptune to optimize performance

## Value Proposition

- **Managed Infrastructure**: Uses Amazon Managed Service for Apache Flink - no infrastructure to manage
- **Scalable Processing**: Supports parallel processing across multiple partitions while maintaining ordering guarantees
- **Efficient Writes**: Implements intelligent batching (up to 100 objects) to optimize Neptune write performance
- **Flexible Input Format**: Handles both tilde (~) and colon (:) column naming conventions
- **AWS Native**: Uses AWS SDK for seamless AWS integration

## Architecture

```
Kinesis Streams → Amazon Managed Flink → Amazon Neptune
     ↓                    ↓                    ↓
[Vertex Stream]    [Partition-aware      [Graph Database]
[Edge Stream]       Processing]
```

## Data Format

The sample data generator creates data in the format expected by Neptune ingestion pipelines:

### Vertex Data
Required columns:
- `~id` or `:ID` - Vertex identifier
- `~label` or `:LABEL` - Vertex type/label
- `partition_id` - Partition identifier (STRING)

Additional columns become vertex properties.

Example:
```json
{
  "data": "{\"~id\":\"person_1\",\"~label\":\"Person\",\"name\":\"Alice\",\"age\":25}",
  "partition_id": "partition_1"
}
```

### Edge Data
Required columns:
- `~id` or `:ID` - Edge identifier
- `~from` or `:START_ID` - Source vertex ID
- `~end` or `:END_ID` - Target vertex ID
- `~label` or `:TYPE` - Edge type/label
- `partition_id` - Partition identifier (STRING)

Additional columns become edge properties.

Example:
```json
{
  "data": "{\"~id\":\"works_at_1\",\"~from\":\"person_1\",\"~end\":\"company_1\",\"~label\":\"WORKS_AT\",\"position\":\"Engineer\"}",
  "partition_id": "partition_1"
}
```

## Prerequisites

### AWS Resources
1. **Amazon Neptune Cluster** - Running in a VPC
2. **Amazon Kinesis Streams** - Two streams for vertices and edges. These streams are hardcoded to 'vertex-stream' and 'edge-stream' for vertices and edges, respectively. 
3. **S3 Bucket** - For deployment artifacts
4. **VPC Configuration** - Subnets and security groups
5. **CloudWatch Log Group** - You must have a CloudWatch log group created called `/aws/kinesis-analytics/flink-neptune-streaming-java`.  Within that log group, you need a log stream called `kinesis-analytics-log-stream`. See [Working with log groups and log streams](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/Working-with-log-groups-and-streams.html) for more information. The logging configuration in this example is not optimized for production. For best practices with logging in a production environment, please review [Logging in Managed Service for Apache Flink](https://docs.aws.amazon.com/managed-flink/latest/java/logging.html).

### IAM Permissions
The deployment script will create an IAM role with required permissions for the Flink application. 
The account running `deploy.sh` must have permissions to create a new IAM role and attach policies to it. 
If this is not permitted by your organization, deploying the job will fail, but the `deploy.sh` script will create a file called `iam_policy.json` containing the policy that would be added to the specified role. In addition, the policy `CloudWatchFullAccess` must also be attached. The trust policy also exists in the file `iam_trust_policy.json`.
- `kinesisanalytics:DescribeApplication` - For Flink application status
- `kinesisanalytics:StartApplication` - For starting Flink application
- `kinesisanalytics:StopApplication` - For stopping Flink application
- `kinesis:DescribeStream` - For reading stream metadata
- `kinesis:GetRecords` - For reading Kinesis stream data
- `kinesis:GetShardIterator` - For positioning in Kinesis streams
- `kinesis:ListShards` - For discovering stream shards
- `neptune-db:connect` - For connecting to Neptune cluster
- `neptune-db:ReadDataViaQuery` - For reading Neptune data (if needed)
- `neptune-db:WriteDataViaQuery` - For writing data to Neptune
- `s3:GetObject` - For reading deployment artifacts from S3
- `logs:CreateLogGroup` - For creating CloudWatch log groups
- `logs:CreateLogStream` - For creating CloudWatch log streams
- `logs:PutLogEvents` - For writing to CloudWatch logs
- `ec2:CreateNetworkInterface` - For VPC access
- `ec2:DescribeNetworkInterfaces` - For VPC network interface management
- `ec2:DeleteNetworkInterface` - For VPC cleanup
- `ec2:DescribeSubnets` - For VPC subnet validation
- `ec2:DescribeSecurityGroups` - For VPC security group validation
- `ec2:DescribeVpcs` - For VPC validation
- `ec2:DescribeDhcpOptions` - For VPC DHCP options validation
- `ec2:CreateNetworkInterfacePermission` - For VPC network interface permissions

### VPC Requirements
- **Private Subnets** - In same VPC as Neptune
- **VPC Endpoints** - For Kinesis access without internet routing
- **Security Group**:
  - Inbound Rules: Port 443 (HTTPS) from self
  - Outbound Rules: Port 443 (HTTPS) to self and Port 8182 (Neptune) to Neptune Security Group (or self if Neptune is using the same Security Group)
  NOTE: The `aws ec2 authorize-security-group-ingress` command in the VPC Endpoint Setup section adds the inbound rule referenced here.

### VPC Endpoint Setup (Required)
Create a VPC endpoint for Kinesis Data Streams to avoid connectivity timeouts:

```bash
# Create Kinesis Data Streams VPC endpoint
aws ec2 create-vpc-endpoint \
  --vpc-id your-vpc-id \
  --service-name com.amazonaws.us-east-1.kinesis-streams \
  --vpc-endpoint-type Interface \
  --subnet-ids subnet-1 subnet-2 \
  --security-group-ids your-sg \
  --region your-region

# Allow inbound HTTPS from the same security group (for VPC endpoint)
aws ec2 authorize-security-group-ingress \
  --group-id your-sg \
  --protocol tcp \
  --port 443 \
  --source-group your-sg \
  --region your-region
```

## Installation and Deployment

### 1. Configure Environment
Update the deployment script with your environment values:
```bash
cd java/deployment
# Copy the sample deploy script and edit it
cp deploy.sh.sample deploy.sh
```

Edit `deploy.sh` and set your specific values:
```bash
S3_BUCKET="your-deployment-bucket"
IAM_SERVICE_ROLE="FlinkServiceRoleForNeptune"
REGION="us-east-1"
NEPTUNE_CLUSTER="your-neptune-cluster"
VPC_SUBNET_1="subnet-1"
VPC_SUBNET_2="subnet-2"
VPC_SECURITY_GROUP="your-sg"
```

**Note**: The deploy script will automatically generate `application_config.json` and `iam_policy.json` from the respective `.sample` templates, replacing placeholders with your actual values.

### 2. Deploy Application
```bash
chmod +x deploy.sh
./deploy.sh
```

### 3. Start Application
```bash
aws kinesisanalyticsv2 start-application \
  --application-name flink-neptune-streaming-java \
  --run-configuration '{"ApplicationRestoreConfiguration":{"ApplicationRestoreType":"SKIP_RESTORE_FROM_SNAPSHOT"}}' \
  --region your-region
```

## Usage

### Generate Sample Data
```bash
cd python/src
export AWS_REGION=your-region
python sample_data_generator.py
```

### Monitor Application
```bash
# Check status
aws kinesisanalyticsv2 describe-application \
  --application-name flink-neptune-streaming-java \
  --region your-region

# View logs
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/kinesis-analytics/flink-neptune-streaming"
```

### Stop Application
```bash
aws kinesisanalyticsv2 stop-application \
  --application-name flink-neptune-streaming-java \
  --region your-region
```

## Key Features

### Partition-Aware Processing
- Vertices and edges with the same `partition_id` are processed by the same Flink task
- Ensures vertices are processed before edges within each partition
- Enables parallel processing across different partitions

### Intelligent Batching
- Batches writes up to 100 "objects" per Neptune API call
- Object count = populated properties + 1
- Automatically flushes batches when limit reached
- Improves Neptune write performance

### Error Handling
- Validates required fields before processing
- Logs detailed error information
- Continues processing other records on individual failures
- Maintains batch integrity

## Project Structure

```
├── java/                          # Java Flink application
│   ├── src/main/java/            # Java source code
│   │   └── com/amazonaws/samples/ # Neptune Flink processors
│   ├── pom.xml                   # Maven dependencies
│   └── deployment/               # Deployment configuration
├── python/                       # Python utilities
│   └── src/
│       ├── sample_data_generator.py  # Test data generator
│       └── requirements.txt          # Python dependencies
├── tests/
│   └── test_sample_data_generator.py # Unit tests for data generator
└── README.md                     # This file
```

## Customization

### Modify Record Counts
Edit `python/src/sample_data_generator.py`:
```python
generator.generate_sample_graph_data(
    vertex_stream="vertex-stream",
    edge_stream="edge-stream",
    num_vertices=50,    # Increase vertex count
    num_edges=100       # Increase edge count
)
```

### Adjust Parallelism
Set `PARALLELISM` environment variable in application configuration:
```json
{
  "PropertyGroupId": "kinesis.config",
  "PropertyMap": {
    "PARALLELISM": "8"
  }
}
```

### Change Batch Size
Modify `maxObjects` in `java/src/main/java/com/amazonaws/samples/NeptuneWriter.java`:
```java
this.maxObjects = 50;  // Reduce batch size (line 36)
```

## Troubleshooting

### Common Issues
1. **VPC Connectivity**: Ensure Flink can reach Neptune and Kinesis
   - **Required**: Create Kinesis Data Streams VPC endpoint (see VPC Endpoint Setup above)
   - **Required**: Allow inbound HTTPS (443) from Flink security group to itself
   - Restart Flink application after creating VPC endpoints
2. **IAM Permissions**: Verify service role has all required permissions
3. **Neptune Endpoint**: Use cluster endpoint, not instance endpoint
4. **Security Groups**: Check inbound/outbound rules
5. **Subnet Configuration**: Use private subnets with VPC endpoints

### Monitoring
- CloudWatch metrics for Flink application performance
- CloudWatch logs for application errors and processing status
- Neptune CloudWatch metrics for write performance

## License

This sample code is provided under the Apache 2.0 License. See LICENSE file for details.

## Support

This is a sample application provided as-is for reference purposes. For production use, please:
- Review and test thoroughly in your environment
- Implement appropriate security measures
- Add monitoring and alerting
- Consider data backup and recovery procedures
