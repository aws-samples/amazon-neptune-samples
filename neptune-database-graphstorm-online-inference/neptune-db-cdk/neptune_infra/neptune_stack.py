import json

from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_neptune as neptune,
    aws_iam as iam,
    aws_s3 as s3,
    aws_sagemaker as sagemaker,
    RemovalPolicy,
    CfnOutput,
    Fn,
)
from constructs import Construct

NEPTUNE_VERSION = "1.4.5.1"  # Latest stable version
NEPTUNE_VERSION_FAMILY = "neptune1.4"


class NeptuneStack(Stack):
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        prefix: str = "",
        neptune_instance_type: str = "db.r8g.xlarge",
        notebook_instance_type: str = "ml.m5.4xlarge",
        **kwargs,
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Store prefix for resource naming
        self.prefix = prefix if prefix else ""
        if self.prefix and not self.prefix.endswith("-"):
            self.prefix += "-"

        # Create VPC with DNS support
        self.vpc = ec2.Vpc(
            self,
            f"{self.prefix}NeptuneVPC",
            max_azs=2,
            nat_gateways=1,
            enable_dns_hostnames=True,
            enable_dns_support=True,
            subnet_configuration=[
                ec2.SubnetConfiguration(
                    name="Public", subnet_type=ec2.SubnetType.PUBLIC, cidr_mask=24
                ),
                ec2.SubnetConfiguration(
                    name="Private",
                    subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS,
                    cidr_mask=24,
                ),
            ],
        )

        # Add S3 VPC Endpoint
        self.vpc.add_gateway_endpoint(
            "S3Endpoint", service=ec2.GatewayVpcEndpointAwsService.S3
        )

        # Add SSM VPC Endpoints needed for Session Manager
        self.vpc.add_interface_endpoint(
            "SSMEndpoint", service=ec2.InterfaceVpcEndpointAwsService.SSM
        )
        self.vpc.add_interface_endpoint(
            "SSMMessagesEndpoint",
            service=ec2.InterfaceVpcEndpointAwsService.SSM_MESSAGES,
        )
        self.vpc.add_interface_endpoint(
            "EC2MessagesEndpoint",
            service=ec2.InterfaceVpcEndpointAwsService.EC2_MESSAGES,
        )

        # Create Security Group
        self.neptune_sg = ec2.SecurityGroup(
            self,
            f"{self.prefix}NeptuneSecurityGroup",
            vpc=self.vpc,
            description="Security group for Neptune DB cluster",
            allow_all_outbound=True,
        )

        # Add SageMaker Runtime VPC Endpoint
        self.vpc.add_interface_endpoint(
            "SageMakerRuntimeEndpoint",
            service=ec2.InterfaceVpcEndpointAwsService.SAGEMAKER_RUNTIME,
            security_groups=[self.neptune_sg],
        )

        # Add SageMaker API endpoint
        self.vpc.add_interface_endpoint(
            "SageMakerApiEndpoint",
            service=ec2.InterfaceVpcEndpointAwsService.SAGEMAKER_API,
            security_groups=[self.neptune_sg],
        )

        # Create IAM Role for Neptune
        self.neptune_role = iam.Role(
            self,
            f"{self.prefix}NeptuneS3Role",
            assumed_by=iam.ServicePrincipal("rds.amazonaws.com").grant_principal,
            description="Role for Neptune DB to access S3",
        )

        # Add S3 read permissions to the role
        self.neptune_role.add_to_policy(
            iam.PolicyStatement(
                actions=["s3:Get*", "s3:List*"],
                resources=["*"],  # You might want to restrict this in production
            )
        )

        # Create S3 Bucket
        self.data_bucket = s3.Bucket(
            self,
            f"{self.prefix}NeptuneDataBucket",
            removal_policy=RemovalPolicy.RETAIN,
            encryption=s3.BucketEncryption.S3_MANAGED,
            versioned=True,
            auto_delete_objects=False,  # Safety measure
        )

        # Create Neptune Subnet Group
        neptune_subnet_group = neptune.CfnDBSubnetGroup(
            self,
            f"{self.prefix}NeptuneSubnetGroup",
            db_subnet_group_description="Subnet group for Neptune DB",
            subnet_ids=self.vpc.select_subnets(
                subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
            ).subnet_ids,
        )

        # Create Neptune Cluster Parameter Group
        neptune_cluster_params = neptune.CfnDBClusterParameterGroup(
            self,
            f"{self.prefix}NeptuneClusterParams",
            family=NEPTUNE_VERSION_FAMILY,
            description="Custom parameter group for Neptune cluster",
            parameters={
                "neptune_enable_audit_log": "1",
                "neptune_query_timeout": "120000",
            },
        )

        # Create Neptune DB Parameter Group
        neptune_db_params = neptune.CfnDBParameterGroup(
            self,
            f"{self.prefix}NeptuneDBParams",
            family=NEPTUNE_VERSION_FAMILY,
            description="Custom parameter group for Neptune DB instance",
            parameters={},
        )

        # Create Neptune Cluster
        self.neptune_cluster = neptune.CfnDBCluster(
            self,
            f"{self.prefix}NeptuneCluster",
            db_subnet_group_name=neptune_subnet_group.ref,
            vpc_security_group_ids=[self.neptune_sg.security_group_id],
            db_cluster_parameter_group_name=neptune_cluster_params.ref,
            engine_version=NEPTUNE_VERSION,
            iam_auth_enabled=True,
            associated_roles=[
                neptune.CfnDBCluster.DBClusterRoleProperty(
                    role_arn=self.neptune_role.role_arn,
                )
            ],
        )

        # Create Neptune Instance
        neptune_instance = neptune.CfnDBInstance(
            self,
            f"{self.prefix}NeptuneInstance",
            db_instance_class=neptune_instance_type,
            db_cluster_identifier=self.neptune_cluster.ref,
            db_parameter_group_name=neptune_db_params.ref,
            availability_zone=self.vpc.availability_zones[0],
        )

        # Add dependencies
        neptune_instance.add_dependency(self.neptune_cluster)
        self.neptune_cluster.add_dependency(neptune_subnet_group)

        # Output important information
        CfnOutput(
            self,
            "NeptuneEndpoint",
            value=self.neptune_cluster.attr_endpoint,
            description="Neptune Cluster Endpoint",
        )

        CfnOutput(
            self,
            "NeptunePort",
            value=self.neptune_cluster.attr_port,
            description="Neptune Port",
        )

        CfnOutput(
            self,
            "NeptuneRoleArn",
            value=self.neptune_role.role_arn,
            description="Neptune IAM Role ARN",
        )

        CfnOutput(
            self,
            "DataBucketName",
            value=self.data_bucket.bucket_name,
            description="S3 Bucket for Neptune Data",
        )

        # Create IAM role for SageMaker endpoints
        sagemaker_endpoint_role = iam.Role(
            self,
            f"{self.prefix}SageMakerEndpointRole",
            assumed_by=iam.ServicePrincipal("sagemaker.amazonaws.com"),
            description="Role for SageMaker inference endpoints",
        )

        # Add CloudWatch permissions - scoped to SageMaker endpoint logs and metrics
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "logs:CreateLogGroup",
                    "logs:CreateLogStream",
                    "logs:PutLogEvents",
                ],
                resources=[
                    f"arn:aws:logs:{Stack.of(self).region}:{Stack.of(self).account}:log-group:/aws/sagemaker/Endpoints/*",
                    f"arn:aws:logs:{Stack.of(self).region}:{Stack.of(self).account}:log-group:/aws/sagemaker/Endpoints/*:log-stream:*",
                ],
            )
        )
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=["cloudwatch:PutMetricData"],
                resources=[
                    "*"
                ],  # CloudWatch metrics don't support resource-level permissions
                conditions={"StringEquals": {"cloudwatch:namespace": "AWS/SageMaker"}},
            )
        )

        # Add S3 read access - scoped to model artifacts path
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=["s3:GetObject"],
                resources=[f"{self.data_bucket.bucket_arn}/model-artifacts/*"],
            )
        )

        # Add ECR permissions for pulling images
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                    "ecr:BatchCheckLayerAvailability",
                ],
                resources=[
                    # TODO: Scope down to graphstorm-inference repos?
                    f"arn:aws:ecr:{Stack.of(self).region}:{Stack.of(self).account}:repository/*"  # Allow access to all repositories in account
                ],
            )
        )

        # Add ECR authorization token permission
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=["ecr:GetAuthorizationToken"],
                resources=["*"],  # GetAuthorizationToken requires * resource
            )
        )

        # Add EC2 permissions for VPC access
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "ec2:DescribeVpcEndpoints",
                    "ec2:DescribeDhcpOptions",
                    "ec2:DescribeVpcs",
                    "ec2:DescribeSubnets",
                    "ec2:DescribeSecurityGroups",
                    "ec2:DescribeNetworkInterfaces",
                    "ec2:DeleteNetworkInterfacePermission",
                    "ec2:DeleteNetworkInterface",
                    "ec2:CreateNetworkInterfacePermission",
                    "ec2:CreateNetworkInterface",
                ],
                resources=[
                    "*"
                ],  # These EC2 actions don't support resource-level permissions
            )
        )

        # Add S3 VPC endpoint access
        sagemaker_endpoint_role.add_to_policy(
            iam.PolicyStatement(
                actions=["s3:GetObject"],
                resources=[
                    f"{self.data_bucket.bucket_arn}/*",  # Model artifacts
                    "arn:aws:s3:::amazonlinux.*.amazonaws.com/*",  # Allow package installation
                    "arn:aws:s3:::packages.*.amazonaws.com/*",
                    "arn:aws:s3:::repo.*.amazonaws.com/*",
                ],
            )
        )

        # Create security group for SageMaker notebook
        sagemaker_sg = ec2.SecurityGroup(
            self,
            f"{self.prefix}SageMakerSecurityGroup",
            vpc=self.vpc,
            description="Security group for SageMaker notebook instance",
            allow_all_outbound=True,
        )

        # Allow Neptune <- SageMaker
        self.neptune_sg.add_ingress_rule(
            peer=ec2.Peer.security_group_id(sagemaker_sg.security_group_id),
            connection=ec2.Port.tcp(8182),
            description="Allow SageMaker notebook to access Neptune",
        )

        # Allow SageMaker notebook to access VPC endpoints
        self.neptune_sg.add_ingress_rule(
            peer=ec2.Peer.security_group_id(sagemaker_sg.security_group_id),
            connection=ec2.Port.tcp(443),
            description="Allow SageMaker notebook to access VPC endpoints",
        )

        # Create IAM role for SageMaker notebook
        sagemaker_role = iam.Role(
            self,
            f"{self.prefix}SageMakerNotebookRole",
            assumed_by=iam.ServicePrincipal("sagemaker.amazonaws.com").grant_principal,
            managed_policies=[
                iam.ManagedPolicy.from_aws_managed_policy_name(
                    "AmazonSageMakerFullAccess"
                )
            ],
        )

        # Allow the notebook instance to create and push to ECR repos
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "ecr:CreateRepository",
                    "ecr:InitiateLayerUpload",
                    "ecr:UploadLayerPart",
                    "ecr:CompleteLayerUpload",
                    "ecr:PutImage",
                    "ecr:BatchCheckLayerAvailability",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                ],
                resources=[
                    f"arn:aws:ecr:{Stack.of(self).region}:{Stack.of(self).account}:repository/*"
                ],
            )
        )

        # Add ECR authorization token permission
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=["ecr:GetAuthorizationToken"],
                resources=["*"],  # GetAuthorizationToken requires * resource
            )
        )

        # Add Neptune DB access permissions
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "neptune-db:CancelQuery",
                    "neptune-db:DeleteDataViaQuery",
                    "neptune-db:GetEngineStatus",
                    "neptune-db:GetLoaderJobStatus",
                    "neptune-db:GetQueryStatus",
                    "neptune-db:ReadDataViaQuery",
                    "neptune-db:ResetDatabase",
                    "neptune-db:StartLoaderJob",
                    "neptune-db:WriteDataViaQuery",
                ],
                resources=[
                    f"arn:aws:neptune-db:{Stack.of(self).region}:{Stack.of(self).account}:"
                    f"{self.neptune_cluster.attr_cluster_resource_id}/*"
                ],
            )
        )

        # Add CloudWatch Logs permissions - scoped to SageMaker notebook logs
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=[
                    "logs:CreateLogGroup",
                    "logs:CreateLogStream",
                    "logs:PutLogEvents",
                ],
                resources=[
                    f"arn:aws:logs:{Stack.of(self).region}:{Stack.of(self).account}:log-group:/aws/sagemaker/NotebookInstances/*",
                    f"arn:aws:logs:{Stack.of(self).region}:{Stack.of(self).account}:log-group:/aws/sagemaker/NotebookInstances/*:*",
                    f"arn:aws:logs:{Stack.of(self).region}:{Stack.of(self).account}:log-group:/aws/sagemaker/NotebookInstances/*:log-stream:*",
                ],
            )
        )

        # Add SageMaker notebook instance describe permissions
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=["sagemaker:DescribeNotebookInstance"],
                resources=[
                    f"arn:aws:sagemaker:{Stack.of(self).region}:{Stack.of(self).account}:notebook-instance/*"
                ],
            )
        )

        # Add S3 access permissions for project bucket
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=["s3:Get*", "s3:List*", "s3:Put*", "s3:Delete*"],
                resources=[
                    self.data_bucket.bucket_arn,
                    f"{self.data_bucket.bucket_arn}/*",
                ],
            )
        )

        # Add S3 access permissions for Neptune notebook and samples
        sagemaker_role.add_to_policy(
            iam.PolicyStatement(
                actions=["s3:GetObject", "s3:ListBucket"],
                resources=[
                    "arn:aws:s3:::aws-neptune-notebook",
                    "arn:aws:s3:::aws-neptune-notebook/*",
                    f"arn:aws:s3:::aws-neptune-notebook-{Stack.of(self).region}",
                    f"arn:aws:s3:::aws-neptune-notebook-{Stack.of(self).region}/*",
                    f"arn:aws:s3:::aws-neptune-customer-samples-{Stack.of(self).region}",
                    f"arn:aws:s3:::aws-neptune-customer-samples-{Stack.of(self).region}/*",
                ],
            )
        )

        # Create graph-notebook lifecycle configuration script for the SageMaker notebook
        subnets_json_string = json.dumps(
            self.vpc.select_subnets(
                subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
            ).subnet_ids
        )
        lifecycle_config_script = f"""#!/bin/bash

sudo -u ec2-user -i <<'EOF'
#!/bin/bash
set -ex

# Set up graph notebook
echo "export GRAPH_NOTEBOOK_AUTH_MODE=IAM" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_SSL=True" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_SERVICE=neptune-db" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_HOST={self.neptune_cluster.attr_endpoint}" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_PORT=8182" >> ~/.bashrc
echo "export NEPTUNE_LOAD_FROM_S3_ROLE_ARN={self.neptune_role.role_arn}" >> ~/.bashrc
echo "export AWS_REGION={Stack.of(self).region}" >> ~/.bashrc

# Create environment config file
mkdir -p /home/ec2-user/SageMaker
cat > /home/ec2-user/SageMaker/cdk_outputs.json << 'JSONEOF'
{{
    "ACCOUNT_ID": "{Stack.of(self).account}",
    "AWS_REGION": "{Stack.of(self).region}",
    "NDB_STACK_S3_BUCKET": "{self.data_bucket.bucket_name}",
    "NDB_STACK_ENDPOINT_ROLE": "{sagemaker_endpoint_role.role_arn}",
    "VPC_SUBNET_IDS": {subnets_json_string},
    "VPC_SECURITY_GROUP_IDS": ["{sagemaker_sg.security_group_id}"]
}}
JSONEOF

aws s3 cp s3://aws-neptune-notebook-{Stack.of(self).region}/graph_notebook.tar.gz /tmp/graph_notebook.tar.gz
rm -rf /tmp/graph_notebook
tar -zxvf /tmp/graph_notebook.tar.gz -C /tmp
chmod +x /tmp/graph_notebook/install_jl4x.sh
/tmp/graph_notebook/install_jl4x.sh

# Install additional dependencies to system python3 kernel
/home/ec2-user/anaconda3/envs/JupyterSystemEnv/bin/python \
    -m pip install matplotlib==3.10.5 seaborn==0.13.2


# Set up GraphStorm kernel
conda create -n gsf -y python=3.10
conda activate gsf
conda install -y ipykernel
python -m ipykernel install --user --name=gsf
# Install graphstorm deps for notebooks
pip install numpy==1.26.4
pip install \
    dgl==1.1.3 -f https://data.dgl.ai/wheels-internal/repo.html
pip install \
    torch==2.1.0 --index-url https://download.pytorch.org/whl/cpu
pip install \
    boto3 \
    joblib \
    pandas \
    matplotlib \
    pyarrow==19.0.1 \
    sentence-transformers

# Clone and and install graphstorm from source
git clone --branch v0.5 https://github.com/awslabs/graphstorm.git /tmp/graphstorm
pip install /tmp/graphstorm
rm -rf /tmp/graphstorm

echo "graphstorm environment created, use 'gsf' kernel!"

EOF
"""

        # Create lifecycle configuration
        lifecycle_config = sagemaker.CfnNotebookInstanceLifecycleConfig(
            self,
            "NeptuneNotebookLifecycleConfig",
            on_start=[
                sagemaker.CfnNotebookInstanceLifecycleConfig.NotebookInstanceLifecycleHookProperty(
                    content=Fn.base64(lifecycle_config_script)
                )
            ],
        )

        # Create SageMaker notebook instance
        notebook_instance = sagemaker.CfnNotebookInstance(
            self,
            f"{self.prefix}NeptuneNotebook",
            instance_type=notebook_instance_type,
            lifecycle_config_name=lifecycle_config.attr_notebook_instance_lifecycle_config_name,
            role_arn=sagemaker_role.role_arn,
            subnet_id=self.vpc.select_subnets(
                subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
            ).subnet_ids[0],
            volume_size_in_gb=20,
            security_group_ids=[sagemaker_sg.security_group_id],
        )

        # Output the notebook instance name
        CfnOutput(
            self,
            "NotebookInstanceName",
            value=notebook_instance.ref,
            description="SageMaker Notebook Instance Name",
        )

        # Add outputs for VPC and security group configuration
        CfnOutput(
            self,
            "VpcId",
            value=self.vpc.vpc_id,
            description="VPC ID for SageMaker endpoints",
        )

        CfnOutput(
            self,
            "PrivateSubnetIds",
            value=",".join(
                self.vpc.select_subnets(
                    subnet_type=ec2.SubnetType.PRIVATE_WITH_EGRESS
                ).subnet_ids
            ),
            description="Private subnet IDs for SageMaker endpoints (comma-separated)",
        )

        CfnOutput(
            self,
            "SageMakerSecurityGroupId",
            value=sagemaker_sg.security_group_id,
            description="Security group ID for SageMaker endpoints",
        )

        # Add SageMaker endpoint role output
        CfnOutput(
            self,
            "SageMakerEndpointRoleArn",
            value=sagemaker_endpoint_role.role_arn,
            description="IAM role ARN for SageMaker endpoints",
        )
