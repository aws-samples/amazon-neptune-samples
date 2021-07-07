// Original Copyright Peter Hanssens
// Original SPDX-License-Identifier: Apache-2.0
// Modified Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// Modified SPDX-License-Identifier: MIT-0
import * as cdk from '@aws-cdk/core';
import * as ec2 from '@aws-cdk/aws-ec2';
import * as iam from '@aws-cdk/aws-iam';
import * as neptune from '@aws-cdk/aws-neptune';
import * as ecs from '@aws-cdk/aws-ecs';
import * as es from '@aws-cdk/aws-elasticsearch';
import * as elbv2 from '@aws-cdk/aws-elasticloadbalancingv2';
import {Bucket} from '@aws-cdk/aws-s3';

export interface AmundsenStackProps extends cdk.StackProps {
	vpc: ec2.Vpc;
	ingressSecurityGroup: ec2.SecurityGroup;
	airflowS3Bucket: Bucket;
}

export class AmundsenStack extends cdk.Stack {
  readonly fargateCluster : ecs.Cluster;
  readonly esDomain: es.CfnDomain;
  readonly neptuneCluster: neptune.CfnDBCluster;
  
  constructor(scope: cdk.App, id: string, props: AmundsenStackProps) {
    super(scope, id, props);

	const application = this.node.tryGetContext('application');
	const environment = this.node.tryGetContext('environment');

    var subnets = props.vpc.privateSubnets.map((a) => {
      return a.subnetId;
    });

    // Elasticsearch Cluster

    //  Assumes Service Linked Role for Elasticsearch has been created.
    
    const iamPolicy = new iam.PolicyStatement({
      resources: [
        `arn:aws:es:${this.region}:${this.account}:domain/*`
      ],
      actions: [
        "es:*"
      ],
      effect: iam.Effect.ALLOW,
      principals: [
        new iam.AnyPrincipal()
      ]
    });

    const iamPolicyDoc = new iam.PolicyDocument({statements: [iamPolicy]});

    this.esDomain = new es.CfnDomain(this, 'AmundsenESDomain', {
      domainName: `${application}-${environment}-es-domain`,
      elasticsearchClusterConfig: {
        instanceCount: 1,
        instanceType: 't3.small.elasticsearch',
      },
      ebsOptions: {
        ebsEnabled: true,
        volumeSize: 10,
      },
      elasticsearchVersion: '6.7',
      vpcOptions: {
        securityGroupIds: [props.ingressSecurityGroup.securityGroupId],
        subnetIds: [props.vpc.privateSubnets[0].subnetId]
      },
      accessPolicies: iamPolicyDoc,
    });

    const NeptuneCloudWatchPolicyStatement = new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          'logs:CreateLogGroup',
          'logs:PutRetentionPolicy',
          'logs:CreateLogStream',
          'logs:PutLogEvents',
          'logs:DescriptLogStreams',
          'logs:GetLogEvents'
        ],
        resources: [
          `arn:${this.partition}:logs:${this.account}:${this.region}:log-group:/aws/neptune/*`,
          `arn:${this.partition}:logs:${this.account}:${this.region}:log-group:/aws/neptune/*:log-stream:*`
        ]
      }
    );
    
    const NeptuneCloudWatchPolicyDocument = new iam.PolicyDocument();
    NeptuneCloudWatchPolicyDocument.addStatements(NeptuneCloudWatchPolicyStatement);

    const NeptuneCloudWatchPolicy = new iam.ManagedPolicy(this, 'NeptuneS3Policy', {
      description: 'An IAM Policy that allows Neptune cluster log events to be sent to CloudWatch',
      managedPolicyName: `${application}-${environment}-neptune-cloudwatch-policy`,
      document: NeptuneCloudWatchPolicyDocument
    });

    const NeptuneS3PolicyStatement = new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          's3:Get*',
          's3:List*',
          "es:ESHttpGet",
          "es:ESHttpPut",
          "es:ESHttpPost",
          "es:ESHttpHead"
        ],
        resources: [
          `arn:aws:s3:::*`,
          `arn:aws:es:${this.region}:${this.account}:domain/*`,
        ]
      }
    );
    
    const NeptuneS3PolicyDocument = new iam.PolicyDocument();
    NeptuneS3PolicyDocument.addStatements(NeptuneS3PolicyStatement);
    
    const NeptuneS3Policy = new iam.ManagedPolicy(this, 'NeptuneS3PolicyDocument', {
      description: 'An IAM Policy that allows s3 and elasticsearch access',
      managedPolicyName: `${application}-${environment}-neptune-s3-policy`,
      document: NeptuneS3PolicyDocument
    });

    const neptuneRole = new iam.Role(this, 'NeptuneRole', {
      roleName: `${application}-${environment}-neptune-role`,
      assumedBy: new iam.ServicePrincipal('rds.amazonaws.com'),
      managedPolicies: [
        NeptuneS3Policy,
        NeptuneCloudWatchPolicy
      ]
    });

    const neptuneSubnetGroup = new neptune.CfnDBSubnetGroup(this, 'NeptuneSubnetGroup', {
      dbSubnetGroupDescription: 'private subnets',
      dbSubnetGroupName: `${application}-${environment}-neptune-subnet-group`,
      subnetIds: subnets
    });

    const neptuneClusterParameterGroup = new neptune.CfnDBClusterParameterGroup(this, 
      'NeptuneClusterParameterGroup', {
      name: `${application}-${environment}-neptune-cluster-pg`,
      description: 'Neptune cluster parameter group',
      family: 'neptune1',
      parameters: {
        neptune_enable_audit_log: 1
      },
    });

    new neptune.CfnDBClusterParameterGroup(this, 
      'NeptuneDBParameterGroup', {
      name: `${application}-${environment}-neptune-db-pg`,
      description: 'Neptune db parameter group',
      family: 'neptune1',
      parameters: {
        neptune_query_timeout: 120000
      },
    });

    this.neptuneCluster = new neptune.CfnDBCluster(this, 'NeptuneCluster', {
      dbClusterParameterGroupName: neptuneClusterParameterGroup.name,
      backupRetentionPeriod: 7,
      associatedRoles: [
        {
          roleArn: neptuneRole.roleArn,
          featureName: 'neptune-role'
        }
      ],
      dbClusterIdentifier: `${application}-${environment}-neptune-cluster`,
      dbSubnetGroupName: neptuneSubnetGroup.dbSubnetGroupName,
      iamAuthEnabled: true,
      port: 8182,
      preferredBackupWindow: '02:00-03:00',
      preferredMaintenanceWindow: 'mon:03:00-mon:04:00',
      storageEncrypted: true,
      vpcSecurityGroupIds: [
        props.ingressSecurityGroup.securityGroupId
      ],
      tags: [
        {
          key: 'hello',
          value: 'world'
        }
      ]
    });

    new neptune.CfnDBInstance(this, 'NeptuneInstance', {
      dbInstanceClass: 'db.t3.medium',
      allowMajorVersionUpgrade: true,
      autoMinorVersionUpgrade: true,
      availabilityZone: props.vpc.availabilityZones[0],
      dbClusterIdentifier: `${application}-${environment}-neptune-cluster`,
      dbInstanceIdentifier: `${application}-${environment}-neptune-instance`,
      dbSubnetGroupName: neptuneSubnetGroup.dbSubnetGroupName,
      preferredMaintenanceWindow: 'mon:03:00-mon:04:00',
      tags: [
        {
          key: 'blah',
          value: 'world'
        }
      ]
    }).addDependsOn(this.neptuneCluster);

    // Fargate Cluster
    this.fargateCluster = new ecs.Cluster(this, 'FargateCluster', {
      clusterName: `${application}-${environment}-amundsen-cluster`,
      vpc: props.vpc,
      containerInsights: true,
    });

    const executionRole = new iam.Role(this, 'ExecutionRole', {
      roleName: `${application}-${environment}-amundsen-execution-role`,
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy')
      ]
    });

    const taskRole = new iam.Role(this, 'TaskRole', {
      roleName: `${application}-${environment}-amundsen-task-role`,
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy')
      ]
    });

    const taskPolicy = new iam.Policy(this, 'TaskPolicy', {
      policyName: `${application}-${environment}-amundsen-container-policy`,
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
          "s3:Get*",
          "s3:List*",
          "neptune-db:*"
        ],
        effect: iam.Effect.ALLOW,
    }));

    const amundsenFrontend = new ecs.FargateTaskDefinition(this, 'AmundsenFrontend', {
      cpu: 1024,
      executionRole: executionRole,
      memoryLimitMiB: 4096,
      taskRole: taskRole,
    });

    const frontendContainer = amundsenFrontend.addContainer('AmundsenFrontendContainer', {
      image: ecs.ContainerImage.fromRegistry('amundsendev/amundsen-frontend:latest'),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'amundsen-frontend' }),
      environment: {
        // LOG_LEVEL: 'DEBUG',
        // PORT: '5000',
        SEARCHSERVICE_BASE: 'http://localhost:5001',
        METADATASERVICE_BASE: 'http://localhost:5002',
        FRONTEND_SVC_CONFIG_MODULE_CLASS: 'amundsen_application.config.TestConfig',
      },
      cpu: 256,
      memoryLimitMiB: 512,
    });

    frontendContainer.addPortMappings({
      containerPort: 5000
    });

    const metadataContainer = amundsenFrontend.addContainer('AmundsenMetadataContainer', {
      image: ecs.ContainerImage.fromRegistry('amundsendev/amundsen-metadata:latest'),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'amundsen-metadata' }),
      environment: {
        // LOG_LEVEL: 'DEBUG',
        // PORT: '5002',
        METADATA_SVC_CONFIG_MODULE_CLASS: 'metadata_service.config.NeptuneConfig',
        AWS_REGION: `${this.region}`,
        S3_BUCKET_NAME: props.airflowS3Bucket.bucketName,
        IGNORE_NEPTUNE_SHARD: 'True',
        PROXY_CLIENT: 'NEPTUNE',
        PROXY_PORT: '8182',
        PROXY_HOST: `wss://${this.neptuneCluster.attrEndpoint}:8182/gremlin`,
        PROXY_ENCRYPTED: 'True',
        PROXY_VALIDATE_SSL: 'False',
      },
      cpu: 256,
      memoryLimitMiB: 512
    });

    metadataContainer.addPortMappings({
      containerPort: 5002
    });

    const searchContainer = amundsenFrontend.addContainer('AmundsenSearchContainer', {
      image: ecs.ContainerImage.fromRegistry('amundsendev/amundsen-search:latest'),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'amundsen-search' }),
      environment: {
        PROXY_CLIENT: 'ELASTICSEARCH',
        CREDENTIALS_PROXY_USER: '',
        CREDENTIALS_PROXY_PASSWORD: '',
        LOG_LEVEL: 'DEBUG',
        PORT: '5001',
        PROXY_PORT: '443',
        PROXY_ENDPOINT: `https://${this.esDomain.attrDomainEndpoint}`,
      },
      cpu: 256,
      memoryLimitMiB: 512
    });

    searchContainer.addPortMappings({
      containerPort: 5001
    });
    
    const alb = new elbv2.ApplicationLoadBalancer(this, 'LoadBalancer', {
      vpc: props.vpc,
      internetFacing: true,
    });

    const amundsenListener = alb.addListener('Listener', {
      port: 80,
      defaultAction: elbv2.ListenerAction.fixedResponse(200)
    });

	var ecsSecurityGroup = new ec2.SecurityGroup(this, 'ECS-Ingress', {
		vpc: props.vpc,
		allowAllOutbound: true,
		securityGroupName: 'EcsIngressSecurityGroup',
	});
	
	ecsSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.allTraffic());

    const amundsenService = new ecs.FargateService(this, 'FargateFrontendService', {
      cluster: this.fargateCluster,
      serviceName: `${application}-${environment}-amundsen-frontend-service`,
      securityGroups: [
        ecsSecurityGroup
      ],
      vpcSubnets: 
        props.vpc.selectSubnets({
		    subnetType: ec2.SubnetType.PRIVATE
	    }),
      taskDefinition: amundsenFrontend,
      assignPublicIp: false,
      desiredCount: 1,
    });

    amundsenListener.addTargets('amundsenListener', {
      port: 80,
      priority: 1,
      healthCheck:{
        path: '/healthcheck',
        protocol: elbv2.Protocol.HTTP,
        port: '5000',
      },
      targets: [amundsenService],
      pathPattern: '/*',
    });

    alb.loadBalancerDnsName;

    // 👇 create an Output
    new cdk.CfnOutput(this, 'amundsen-frontend-hostname', {
      value: alb.loadBalancerDnsName,
      description: 'Amundsen Frontend Hostname',
      exportName: 'amundsen-frontend-hostname',
    });

    console.log('Access Amundsen here: https://' + alb.loadBalancerDnsName);

  }
}
