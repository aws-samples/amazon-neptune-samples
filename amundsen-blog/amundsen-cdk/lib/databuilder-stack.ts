// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import * as cdk from '@aws-cdk/core';
import * as ec2 from '@aws-cdk/aws-ec2';
import * as iam from '@aws-cdk/aws-iam';
import * as neptune from '@aws-cdk/aws-neptune';
import * as ecs from '@aws-cdk/aws-ecs';
import * as es from '@aws-cdk/aws-elasticsearch';
import * as elbv2 from '@aws-cdk/aws-elasticloadbalancingv2';
import {Bucket} from '@aws-cdk/aws-s3';
import * as rds from '@aws-cdk/aws-rds';
import {Secret} from '@aws-cdk/aws-secretsmanager';
import * as events from '@aws-cdk/aws-events';
import * as targets from '@aws-cdk/aws-events-targets';

export interface DatabuilderStackProps extends cdk.StackProps {
	vpc: ec2.Vpc;
	ingressSecurityGroup: ec2.SecurityGroup;
	cluster: ecs.Cluster;
	s3Bucket: Bucket;
	esDomain: es.CfnDomain;
  neptuneCluster: neptune.CfnDBCluster;
  rdsInstance: rds.DatabaseInstance
  rdsSecret: Secret;
}

export class DatabuilderStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props: DatabuilderStackProps) {
    super(scope, id, props);

    const application = this.node.tryGetContext('application');
	  const environment = this.node.tryGetContext('environment');

    var subnets = props.vpc.privateSubnets.map((a) => {
      return a.subnetId;
    });

    const executionRole = new iam.Role(this, 'ExecutionRole', {
      roleName: `${application}-${environment}-databuilder-execution-role`,
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy')
      ]
    });

    const taskRole = new iam.Role(this, 'TaskRole', {
      roleName: `${application}-${environment}-databuilder-task-role`,
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy')
      ]
    });
    
    const taskPolicy = new iam.Policy(this, 'DatabuilderTaskPolicy', {
      policyName: `${application}-${environment}-amundsen-databuilder-policy`,
      roles: [
        taskRole
      ],
    });

    taskPolicy.addStatements(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
      ],
      resources: [
        `arn:aws:logs:${this.region}:${this.account}:log-group:*:log-stream:*`,
        `arn:aws:logs:${this.region}:${this.account}:log-group:*`,
      ],
    }));

    // Add an ES policy to a Role
    taskPolicy.addStatements(
      new iam.PolicyStatement({
        resources: [
          `arn:aws:es:${this.region}:${this.account}:domain/*`,
          `arn:aws:rds:${this.region}:${this.account}:cluster/*`,
          `arn:aws:s3:::*`,
          `arn:aws:neptune-db:${this.region}:${this.account}:cluster-*`,
        ],
        actions: [
          "es:ESHttpGet",
          "es:ESHttpPut",
          "es:ESHttpPost",
          "es:ESHttpHead",
          "s3:*",
          "neptune-db:*"
        ],
        effect: iam.Effect.ALLOW,
    }));
    
    const taskDefinition = new ecs.FargateTaskDefinition(this, 'Amundsen-Databuilder-Postgres', {
      cpu: 1024,
      memoryLimitMiB: 4096,
      executionRole: executionRole,
      taskRole: taskRole
    });

    const container = taskDefinition.addContainer('AmundsenDatabuilderPostgresContainer', {
      image: ecs.ContainerImage.fromRegistry('public.ecr.aws/a8u6m7l5/amundsen-blog-databuilder-postgres:latest'),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'amundsen-databuilder-postgres' }),
      environment: {
        AWS_REGION: `${this.region}`,
        ES_HOST: props.esDomain.attrDomainEndpoint,
        NEPTUNE_HOST: props.neptuneCluster.attrEndpoint,
        NEPTUNE_PORT: props.neptuneCluster.attrPort,
        S3_BUCKET_NAME: props.s3Bucket.bucketName,
        POSTGRES_HOST: props.rdsInstance.dbInstanceEndpointAddress,
        POSTGRES_PORT: props.rdsInstance.dbInstanceEndpointPort,
      },
      secrets: {
        POSTGRES_USER: ecs.Secret.fromSecretsManager(props.rdsSecret, 'username'),
        POSTGRES_PASSWORD: ecs.Secret.fromSecretsManager(props.rdsSecret, 'password'),
      },
      cpu: 256,
      memoryLimitMiB: 512
    });
    
    const rule = new events.Rule(this, 'Amundsen-Databuilder-Rule', {
      schedule: events.Schedule.expression('rate(5 minutes)')
    });
    
    rule.addTarget(new targets.EcsTask({
      cluster: props.cluster,
      taskDefinition,
      taskCount: 1
    }));

  }
}