# Neptune Connection Troubleshooting Guide

This guide helps troubleshoot connectivity issues when accessing Amazon Neptune through a SageMaker notebook instance on the same VPC.

## Prerequisites

1. SageMaker notebook instance deployed and running **on the same VPC as the the NeptuneDB instance**.
2. AWS credentials configured with appropriate permissions:
   - SageMaker notebook access
   - Neptune IAM authentication permissions

## Testing Connectivity

### 1. Environment Variables

First, verify the environment variables are properly set in the notebook:

```bash
# From a notebook cell
!env | grep GRAPH_NOTEBOOK
```

Should show:
```
GRAPH_NOTEBOOK_AUTH_MODE=IAM
GRAPH_NOTEBOOK_SSL=True
GRAPH_NOTEBOOK_SERVICE=neptune-db
GRAPH_NOTEBOOK_HOST=<your-neptune-endpoint>
GRAPH_NOTEBOOK_PORT=8182
```

### 2. Graph Notebook Configuration

Test the graph notebook configuration:

```python
%graph_notebook_config
```

This should display your current Neptune connection settings.

### 3. Basic Connectivity Test

Try a simple status query:

```python
%%gremlin
g.V().limit(1).count()
```

## Common Issues and Solutions

### 1. Connection Timeout
```
Connection error: HTTPSConnectionPool(host='...', port=8182): Max retries exceeded
```

**Solutions:**
- Verify notebook instance is in the correct VPC/subnet
- Check security group allows traffic from notebook to Neptune (port 8182)
- Ensure VPC endpoints are properly configured

### 2. Authentication Errors
```
Missing Authentication Token or Access Denied
```

**Solutions:**
- Verify SageMaker role has proper Neptune permissions:
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
- Check AWS credentials are properly configured
- Ensure IAM authentication is enabled on the Neptune cluster

### 3. Graph Notebook Package Issues
```
ModuleNotFoundError or ImportError
```

**Solutions:**
- Verify lifecycle configuration completed successfully:
  ```bash
  cat /var/log/jupyter/jupyter.log
  ```
- Try reinstalling graph notebook package:
  ```bash
  pip install --upgrade graph-notebook
  ```
- Restart the notebook kernel

### 4. SSL/TLS Issues
```
SSLError or EOF occurred in violation of protocol
```

**Solutions:**
- Verify GRAPH_NOTEBOOK_SSL is set to True
- Check GRAPH_NOTEBOOK_HOST doesn't include protocol prefix
- Ensure proper Neptune endpoint is being used

## Verifying Success

A successful connection should allow you to:
1. Run Gremlin queries
2. View Neptune status information
3. Access graph visualization tools

Example status check:
```python
from graph_notebook_client.client_provider import ClientProvider
client = ClientProvider()
client.status()
```

Should return:
```json
{
  "status": "healthy",
  "startTime": "...",
  "dbEngineVersion": "...",
  "role": "writer",
  ...
}
```

## Additional Resources

- [Neptune Security Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/security.html)
- [Graph Notebook GitHub Repository](https://github.com/aws/graph-notebook)
- [Neptune IAM Authentication](https://docs.aws.amazon.com/neptune/latest/userguide/iam-auth.html)
- [SageMaker VPC Configuration](https://docs.aws.amazon.com/sagemaker/latest/dg/appendix-notebook-and-internet-access.html)
