import * as cdk from '@aws-cdk/core';
import { Duration } from '@aws-cdk/core';
import {
  Vpc,
  SecurityGroup,
  Port,
  GatewayVpcEndpointAwsService,
} from '@aws-cdk/aws-ec2';
import * as neptune from '@aws-cdk/aws-neptune';
import { Bucket, HttpMethods, EventType } from '@aws-cdk/aws-s3';
import { Code, Runtime, Function } from '@aws-cdk/aws-lambda';
import { S3EventSource } from '@aws-cdk/aws-lambda-event-sources';
import {
  Role,
  ServicePrincipal,
  PolicyStatement,
  Effect,
} from '@aws-cdk/aws-iam';

export class NeptuneRestIamStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: any) {
    super(scope, id, props);

    const databaseAccessPolicy = new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'xray:PutTraceSegments',
        'xray:PutTelemetryRecords',
        'ec2:CreateNetworkInterface',
        'ec2:DescribeNetworkInterfaces',
        'ec2:DetachNetworkInterface',
        'ec2:DeleteNetworkInterface',
        'rds:*', // POC ONLY
        'sts:AssumeRole',
        'secretsmanager:*', // POC ONLY
        'kms:*',
        'neptune-db:*', // POC ONLY
        'logs:*', // POC ONLY
      ],
    });
    databaseAccessPolicy.addAllResources();
    const dbAccessRole = new Role(this, 'personalizeAccess', {
      assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
    });
    dbAccessRole.addToPolicy(databaseAccessPolicy);

    const vpcOutput = new Vpc(this, 'vpc', {
      enableDnsHostnames: true,
      enableDnsSupport: true,
      cidr: '172.30.0.0/16',
      gatewayEndpoints: {
        S3: {
          service: GatewayVpcEndpointAwsService.S3,
        },
      },
    });
    const dbSecurityGroup = new SecurityGroup(this, 'neptuneSg', {
      vpc: vpcOutput,
      allowAllOutbound: true,
    });
    new cdk.CfnOutput(this, 'SecurityGroupId', {
      value: dbSecurityGroup.securityGroupId,
    });
    const subnets = vpcOutput.privateSubnets;
    const subnetIds = subnets.map((subnet) => subnet.subnetId);
    new cdk.CfnOutput(this, 'Subnets', {
      value: subnetIds.toLocaleString(),
    });
    dbSecurityGroup.addIngressRule(
      dbSecurityGroup,
      Port.tcp(8182),
      'Neptune Ingress'
    );
    const neptuneDbClusterParameterGroup = new neptune.CfnDBClusterParameterGroup(
      this,
      'neptuneDbParameterGroup',
      {
        description: 'neptuneClusterParameterGroup',
        family: 'neptune1',
        // tslint:disable-next-line: quotemark object-literal-key-quotes
        parameters: { neptune_enable_audit_log: 'true' },
      }
    );
    const subnetGroup = new neptune.CfnDBSubnetGroup(
      this,
      'neptuneDbSubnetGroup',
      {
        subnetIds: subnetIds,
        dbSubnetGroupDescription: 'Neptune Subnet Group',
        // tslint:disable-next-line: object-literal-shorthand
      }
    );

    const bulkLoaderRole = new Role(this, 'NeptuneLoadFromS3', {
      assumedBy: new ServicePrincipal('rds.amazonaws.com'),
      description:
        'Allows Neptune to access Amazon S3 resources on your behalf.',
    });

    bulkLoaderRole.addToPolicy(
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: ['s3:Get*', 's3:List*'],
        resources: ['*'],
      })
    );
    // trust policy to assume a role (sts:AssumeRole)
    bulkLoaderRole.grantPassRole(new ServicePrincipal('rds.amazonaws.com'));
    new cdk.CfnOutput(this, 'BulkLoaderRoleArn', {
      value: bulkLoaderRole.roleArn,
    });

    const neptuneDbCluster = new neptune.CfnDBCluster(
      this,
      'neptuneDbCluster',
      {
        dbClusterIdentifier: 'neptune-test-cluster',
        iamAuthEnabled: true,
        dbSubnetGroupName: subnetGroup.ref,
        dbClusterParameterGroupName: neptuneDbClusterParameterGroup.ref,
        vpcSecurityGroupIds: [dbSecurityGroup.securityGroupId],
        associatedRoles: [{ roleArn: bulkLoaderRole.roleArn }],
      }
    );
    neptuneDbCluster.addDependsOn(subnetGroup);
    neptuneDbCluster.addDependsOn(neptuneDbClusterParameterGroup);
    const neptuneDbInstanceParameterGroup = new neptune.CfnDBParameterGroup(
      this,
      'neptuneInstanceDbParameterGroup',
      {
        description: 'neptuneInstanceParameterGroup',
        family: 'neptune1',
        // tslint:disable-next-line: quotemark object-literal-key-quotes
        parameters: { neptune_query_timeout: 20000 },
      }
    );
    const neptuneDbInstance = new neptune.CfnDBInstance(
      this,
      'neptuneDbInstance',
      {
        dbInstanceClass: 'db.t3.medium',
        dbClusterIdentifier: neptuneDbCluster.dbClusterIdentifier,
        dbParameterGroupName: neptuneDbInstanceParameterGroup.ref,
      }
    );
    neptuneDbInstance.addDependsOn(neptuneDbCluster);
    neptuneDbInstance.addDependsOn(neptuneDbInstanceParameterGroup);
    new cdk.CfnOutput(this, 'NeptuneInstanceEndpoint', {
      value: neptuneDbInstance.attrEndpoint,
    });

    new cdk.CfnOutput(this, 'NeptuneClusterEndpoint', {
      value: neptuneDbCluster.attrEndpoint,
    });

    const clusterStateFunction = new Function(this, 'clusterState', {
      runtime: Runtime.NODEJS_12_X,
      handler: 'index.getClusterStatus',
      code: Code.fromAsset('./build'),
      memorySize: 128,
      timeout: Duration.seconds(5),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        REGION: this.region,
        NEPTUNE_PORT: '8182',
      },
    });

    const bulkUploadFunction = new Function(this, 'bulkUpload', {
      runtime: Runtime.NODEJS_12_X,
      handler: 'index.bulkUploadHandler',
      code: Code.fromAsset('./build'),
      timeout: Duration.seconds(15),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        FORMAT: 'csv',
        IAM_ROLE_ARN: bulkLoaderRole.roleArn,
        REGION: this.region,
        NEPTUNE_PORT: '8182',
      },
    });

    const allBulkJobsFunction = new Function(this, 'getAllBulkJobs', {
      runtime: Runtime.NODEJS_12_X,
      handler: 'index.getAllBulkJobsHandler',
      code: Code.fromAsset('./build'),
      timeout: Duration.seconds(5),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        REGION: this.region,
        NEPTUNE_PORT: '8182',
      },
    });

    const countVerticesFunction = new Function(this, 'countVertices', {
      runtime: Runtime.NODEJS_12_X,
      handler: 'index.countVerticesHandler',
      code: Code.fromAsset('./build'),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        FORMAT: 'csv',
        IAM_ROLE_ARN: bulkLoaderRole.roleArn,
        REGION: this.region,
        NEPTUNE_PORT: '8182',
      },
    });

    const uploadBucket = new Bucket(this, 'upload', {
      cors: [
        {
          allowedMethods: [HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT],
          allowedOrigins: ['*'],
          allowedHeaders: ['*'],
          exposedHeaders: [
            'x-amz-server-side-encryption',
            'x-amz-request-id',
            'x-amz-id-2',
          ],
          maxAge: 3000,
        },
      ],
    });

    bulkUploadFunction.addEventSource(
      new S3EventSource(uploadBucket, {
        events: [EventType.OBJECT_CREATED],
        filters: [{ suffix: '.csv' }],
      })
    );
  }
}
