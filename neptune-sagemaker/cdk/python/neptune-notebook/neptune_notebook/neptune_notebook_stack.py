from aws_cdk import (
    Stack,
    Fn,
    Tags,
    Aws,
    aws_ec2 as ec2, 
    aws_iam as iam, 
    aws_sagemaker as sagemaker,
    RemovalPolicy
)
import aws_cdk.aws_neptune_alpha as neptune
from constructs import Construct

class NeptuneNotebookStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create a VPC
        neptune_vpc = ec2.Vpc(self, "Neptune-CDK-VPC")

        # Cluster Parameters
        cluster_params = neptune.ClusterParameterGroup(self, "Neptune-CDK-ClusterParams",
            description="Neptune CDK Cluster parameter group",
            parameters={
                "neptune_enable_audit_log": "1"
            }
        )

        db_params = neptune.ParameterGroup(self, "Neptune-CDK-DbParams",
            description="Neptune CDK Db parameter group",
            parameters={
                "neptune_query_timeout": "120000"
            }
        )

        # Create a Neptune Cluster
        neptune_cluster = neptune.DatabaseCluster(self, "Neptune-CDK-Cluster",
            vpc=neptune_vpc,
            instance_type=neptune.InstanceType.T3_MEDIUM,
            cluster_parameter_group=cluster_params,
            parameter_group=db_params,
            port=8182,
            removal_policy=RemovalPolicy.DESTROY
        )        

        # Allow access within the security group
        neptune_cluster.connections.allow_default_port_internally()

        neptune_cluster.node.add_dependency(neptune_vpc)

        # Create a Notebook connecting to the Neptune Cluster
        self.create_notebook(neptune_vpc, neptune_cluster)

    def create_notebook(self, neptune_vpc: ec2.Vpc, neptune_cluster: neptune.DatabaseCluster):
        """
        Create a Notebook Instance attached to the Neptune Cluster
        """
        # Create an IAM policy for the Notebook
        notebook_role_policy_doc = iam.PolicyDocument()
        notebook_role_policy_doc.add_statements(iam.PolicyStatement(**{
            "effect": iam.Effect.ALLOW,
            "resources": ["arn:aws:s3:::aws-neptune-notebook",
                "arn:aws:s3:::aws-neptune-notebook/*"],
            "actions": ["s3:GetObject",
                "s3:ListBucket"]
            })
        )
        
        # Allow Notebook to access Neptune Cluster
        notebook_role_policy_doc.add_statements(iam.PolicyStatement(**{
        "effect": iam.Effect.ALLOW,
        "resources": ["arn:aws:neptune-db:{region}:{account}:{cluster_id}/*".format(
            region=Aws.REGION, account=Aws.ACCOUNT_ID, cluster_id=neptune_cluster.cluster_resource_identifier)],
        "actions": ["neptune-db:connect"]
            })
        )

        # Create a role and add the policy to it
        notebook_role = iam.Role(self, 'Neptune-CDK-Notebook-Role',
            role_name='AWSNeptuneNotebookRole-CDK',
            assumed_by=iam.ServicePrincipal('sagemaker.amazonaws.com'),
            inline_policies={
                'AWSNeptuneNotebook-CDK': notebook_role_policy_doc
            }
        )

        notebook_lifecycle_script = f'''#!/bin/bash
sudo -u ec2-user -i <<'EOF'
echo "export GRAPH_NOTEBOOK_AUTH_MODE=DEFAULT" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_HOST={neptune_cluster.cluster_endpoint.hostname}" >> ~/.bashrc
echo "export GRAPH_NOTEBOOK_PORT=8182" >> ~/.bashrc
echo "export NEPTUNE_LOAD_FROM_S3_ROLE_ARN=" >> ~/.bashrc
echo "export AWS_REGION={Aws.REGION}" >> ~/.bashrc
aws s3 cp s3://aws-neptune-notebook/graph_notebook.tar.gz /tmp/graph_notebook.tar.gz
rm -rf /tmp/graph_notebook
tar -zxvf /tmp/graph_notebook.tar.gz -C /tmp
/tmp/graph_notebook/install.sh
EOF
'''
        notebook_lifecycle_config = sagemaker.CfnNotebookInstanceLifecycleConfig(self, 'NpetuneWorkbenchLifeCycleConfig',
            notebook_instance_lifecycle_config_name='aws-neptune-cdk-example-LC',
            on_start=[sagemaker.CfnNotebookInstanceLifecycleConfig.NotebookInstanceLifecycleHookProperty(
            content=Fn.base64(notebook_lifecycle_script)
            )]
        )

        neptune_security_group = neptune_cluster.connections.security_groups[0]
        neptune_security_group_id = neptune_security_group.security_group_id
        neptune_subnet = neptune_vpc.select_subnets(subnet_type=ec2.SubnetType.PRIVATE_WITH_NAT).subnets[0]
        neptune_subnet_id = neptune_subnet.subnet_id

        notebook = sagemaker.CfnNotebookInstance(self, 'CDKNeptuneWorkbench',
            instance_type='ml.t3.medium',
            role_arn=notebook_role.role_arn,
            lifecycle_config_name=notebook_lifecycle_config.notebook_instance_lifecycle_config_name,
            notebook_instance_name='aws-neptune-workbench',
            root_access='Disabled',
            security_group_ids=[neptune_security_group_id],
            subnet_id=neptune_subnet_id,
            direct_internet_access='Enabled',
        )
        Tags.of(notebook).add('aws-neptune-cluster-id', neptune_cluster.cluster_identifier)
        Tags.of(notebook).add('aws-neptune-resource-id', neptune_cluster.cluster_resource_identifier)

        notebook.node.add_dependency(neptune_cluster)
        notebook.node.add_dependency(neptune_security_group)
        notebook.node.add_dependency(neptune_subnet)        
