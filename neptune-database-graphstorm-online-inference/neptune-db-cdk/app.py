#!/usr/bin/env python3
import os
import aws_cdk as cdk
from neptune_infra.neptune_stack import NeptuneStack

app = cdk.App()

# Get configuration from context or use defaults
prefix = app.node.try_get_context("prefix") or ""
neptune_instance_type = (
    app.node.try_get_context("neptune_instance_type") or "db.r8g.xlarge"
)
assert neptune_instance_type.startswith("db."), (
    f"NeptuneDB instances types should start with 'db.', got {neptune_instance_type}"
)
notebook_instance_type = (
    app.node.try_get_context("notebook_instance_type") or "ml.m5.4xlarge"
)
assert notebook_instance_type.startswith("ml."), (
    f"SageMaker notebook instance types should start with 'ml.', got {notebook_instance_type}"
)

# Create stack name with prefix
stack_name = f"{prefix}NeptuneInfraStack" if prefix else "NeptuneInfraStack"

NeptuneStack(
    app,
    stack_name,
    prefix=prefix,
    neptune_instance_type=neptune_instance_type,
    notebook_instance_type=notebook_instance_type,
    env=cdk.Environment(
        account=os.getenv("CDK_DEFAULT_ACCOUNT"),
        region=os.getenv("CDK_DEFAULT_REGION", "us-east-1"),
    ),
)

app.synth()
