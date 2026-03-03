# IAM Permissions

AWS Identity and Access Management (IAM) controls who can access your AWS resources and what actions they can perform. This section outlines the specific permissions needed for these agents. For more information about IAM, see the [AWS IAM documentation](https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html).

This document outlines the AWS IAM permissions required to run the Neptune agent samples.

## Required AWS Services

The agents in this sample require access to:
- **Amazon Bedrock** - For AI model invocations
- **Amazon Neptune** - For graph database operations

## Minimum Required Permissions

### Amazon Bedrock Permissions

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "bedrock:InvokeModel",
                "bedrock:InvokeModelWithResponseStream"
            ],
            "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude-*"
        }
    ]
}
```

### Amazon Neptune Permissions

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "neptune-db:connect",
                "neptune-db:ReadDataViaQuery",
                "neptune-db:WriteDataViaQuery",
                "neptune-db:DeleteDataViaQuery"
            ],
            "Resource": "arn:aws:neptune-db:REGION:ACCOUNT_ID:cluster-CLUSTER_ID/*"
        }
    ]
}
```

Replace:
- `REGION` with your AWS region (e.g., `us-east-1`)
- `ACCOUNT_ID` with your AWS account ID
- `CLUSTER_ID` with your Neptune cluster resource ID

## Complete IAM Policy Example

Here's a complete policy combining all required permissions:

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
            "Resource": "arn:aws:bedrock:*::foundation-model/anthropic.claude-*"
        },
        {
            "Sid": "NeptuneAccess",
            "Effect": "Allow",
            "Action": [
                "neptune-db:connect",
                "neptune-db:ReadDataViaQuery",
                "neptune-db:WriteDataViaQuery",
                "neptune-db:DeleteDataViaQuery"
            ],
            "Resource": "arn:aws:neptune-db:REGION:ACCOUNT_ID:cluster-CLUSTER_ID/*"
        }
    ]
}
```

## Setup Options

### Option 1: IAM User with Access Keys

1. Create an IAM user in the AWS Console
2. Attach the policy above to the user
3. Generate access keys for the user
4. Configure AWS CLI with the credentials:
   ```bash
   aws configure
   ```

### Option 2: IAM Role (Recommended for EC2/ECS)

1. Create an IAM role with the policy above
2. Attach the role to your EC2 instance or ECS task
3. No additional configuration needed - credentials are automatically provided

### Option 3: IAM Role with Assume Role

1. Create an IAM role with the policy above
2. Configure your AWS CLI profile to assume the role:
   ```ini
   [profile neptune-agents]
   role_arn = arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME
   source_profile = default
   ```
3. Set the profile in your environment:
   ```bash
   export AWS_PROFILE=neptune-agents
   ```

## Finding Your Neptune Cluster Resource ID

To find your Neptune cluster resource ID for the IAM policy:

```bash
aws neptune describe-db-clusters \
    --db-cluster-identifier YOUR_CLUSTER_NAME \
    --query 'DBClusters[0].DbClusterResourceId' \
    --output text
```

## Verifying Permissions

After configuring IAM permissions, verify access:

1. Test Bedrock access:
   ```bash
   aws bedrock list-foundation-models --region us-east-1
   ```

2. Test Neptune access by running the test suite:
   ```bash
   python -m pytest tests/test_mcp.py
   ```

## Troubleshooting

### Access Denied Errors

If you encounter `AccessDeniedException` errors:

1. Verify your AWS credentials are configured:
   ```bash
   aws sts get-caller-identity
   ```

2. Check that the IAM policy is attached to your user/role

3. Ensure the resource ARNs in the policy match your actual resources

4. For Neptune, verify network access (security groups, VPC configuration)

### Bedrock Model Access

If you get errors about model access:

1. Verify the model is enabled in your AWS account:
   - Go to Amazon Bedrock console
   - Navigate to "Model access"
   - Request access to Claude models if not already enabled

2. Ensure your region supports the model you're trying to use

## Security Best Practices

1. **Principle of Least Privilege**: Only grant the minimum permissions required
2. **Use IAM Roles**: Prefer IAM roles over long-term access keys when possible
3. **Rotate Credentials**: Regularly rotate access keys if using IAM users
4. **Resource-Specific Policies**: Limit Neptune access to specific clusters
5. **Monitor Usage**: Use AWS CloudTrail to monitor API calls
6. **Separate Environments**: Use different IAM roles/users for dev, test, and production

## Additional Resources

- [Amazon Bedrock Security](https://docs.aws.amazon.com/bedrock/latest/userguide/security.html)
- [Amazon Neptune Security](https://docs.aws.amazon.com/neptune/latest/userguide/security.html)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

---

## Legal

**Copyright © 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.**

**License:** MIT-0 (MIT No Attribution)

This sample code is made available under the MIT-0 license. See the [LICENSE](../../LICENSE) file for details.

**Terms of Use:**
- This is sample code for demonstration and educational purposes
- Not intended for production use without proper review and testing
- No warranties or guarantees provided
- Users are responsible for testing, validating, and adapting the code for their use cases
- AWS service usage will incur costs according to AWS pricing
