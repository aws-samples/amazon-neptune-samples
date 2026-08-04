import {
  Stack,
  StackProps,
  CfnOutput,
  Duration,
  aws_ec2 as ec2,
  aws_neptune as neptune,
  aws_s3 as s3,
  aws_lambda as lambda,
  aws_lambda_event_sources as lambdaEventSources,
  aws_iam as iam,
} from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class NeptuneRestIamStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const databaseAccessPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
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
    const dbAccessRole = new iam.Role(this, 'personalizeAccess', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
    });
    dbAccessRole.addToPolicy(databaseAccessPolicy);

    const vpcOutput = new ec2.Vpc(this, 'vpc', {
      enableDnsHostnames: true,
      enableDnsSupport: true,
      ipAddresses: ec2.IpAddresses.cidr('172.30.0.0/16'),
      gatewayEndpoints: {
        S3: {
          service: ec2.GatewayVpcEndpointAwsService.S3,
        },
      },
    });
    const dbSecurityGroup = new ec2.SecurityGroup(this, 'neptuneSg', {
      vpc: vpcOutput,
      allowAllOutbound: true,
    });
    new CfnOutput(this, 'SecurityGroupId', {
      value: dbSecurityGroup.securityGroupId,
    });
    const subnets = vpcOutput.privateSubnets;
    const subnetIds = subnets.map((subnet) => subnet.subnetId);
    new CfnOutput(this, 'Subnets', {
      value: subnetIds.toLocaleString(),
    });
    dbSecurityGroup.addIngressRule(
      dbSecurityGroup,
      ec2.Port.tcp(8182),
      'Neptune Ingress'
    );
    const neptuneDbClusterParameterGroup = new neptune.CfnDBClusterParameterGroup(
      this,
      'neptuneDbParameterGroup',
      {
        description: 'neptuneClusterParameterGroup',
        family: 'neptune1.4',
        parameters: { neptune_enable_audit_log: 'true' },
      }
    );
    const subnetGroup = new neptune.CfnDBSubnetGroup(
      this,
      'neptuneDbSubnetGroup',
      {
        subnetIds: subnetIds,
        dbSubnetGroupDescription: 'Neptune Subnet Group',
      }
    );

    const bulkLoaderRole = new iam.Role(this, 'NeptuneLoadFromS3', {
      assumedBy: new iam.ServicePrincipal('rds.amazonaws.com'),
      description:
        'Allows Neptune to access Amazon S3 resources on your behalf.',
    });

    bulkLoaderRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: ['s3:Get*', 's3:List*'],
        resources: ['*'],
      })
    );
    // trust policy to assume a role (sts:AssumeRole)
    bulkLoaderRole.grantPassRole(new iam.ServicePrincipal('rds.amazonaws.com'));
    new CfnOutput(this, 'BulkLoaderRoleArn', {
      value: bulkLoaderRole.roleArn,
    });

    const neptuneDbCluster = new neptune.CfnDBCluster(
      this,
      'neptuneDbCluster',
      {
        dbClusterIdentifier: 'neptune-test-cluster',
        engineVersion: '1.4.8.0',
        iamAuthEnabled: true,
        dbSubnetGroupName: subnetGroup.ref,
        dbClusterParameterGroupName: neptuneDbClusterParameterGroup.ref,
        vpcSecurityGroupIds: [dbSecurityGroup.securityGroupId],
        associatedRoles: [{ roleArn: bulkLoaderRole.roleArn }],
      }
    );
    neptuneDbCluster.addResourceDependency(subnetGroup);
    neptuneDbCluster.addResourceDependency(neptuneDbClusterParameterGroup);
    const neptuneDbInstanceParameterGroup = new neptune.CfnDBParameterGroup(
      this,
      'neptuneInstanceDbParameterGroup',
      {
        description: 'neptuneInstanceParameterGroup',
        family: 'neptune1.4',
        parameters: { neptune_query_timeout: 20000 },
      }
    );
    const neptuneDbInstance = new neptune.CfnDBInstance(
      this,
      'neptuneDbInstance',
      {
        dbInstanceClass: 'db.t4g.medium',
        dbClusterIdentifier: neptuneDbCluster.dbClusterIdentifier,
        dbParameterGroupName: neptuneDbInstanceParameterGroup.ref,
      }
    );
    neptuneDbInstance.addResourceDependency(neptuneDbCluster);
    neptuneDbInstance.addResourceDependency(neptuneDbInstanceParameterGroup);
    new CfnOutput(this, 'NeptuneInstanceEndpoint', {
      value: neptuneDbInstance.attrEndpoint,
    });

    new CfnOutput(this, 'NeptuneClusterEndpoint', {
      value: neptuneDbCluster.attrEndpoint,
    });

    const clusterStateFunction = new lambda.Function(this, 'clusterState', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.getClusterStatus',
      code: lambda.Code.fromAsset('./build'),
      memorySize: 128,
      timeout: Duration.seconds(5),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        NEPTUNE_PORT: '8182',
      },
    });

    const bulkUploadFunction = new lambda.Function(this, 'bulkUpload', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.bulkUploadHandler',
      code: lambda.Code.fromAsset('./build'),
      timeout: Duration.seconds(15),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        FORMAT: 'csv',
        IAM_ROLE_ARN: bulkLoaderRole.roleArn,
        NEPTUNE_PORT: '8182',
      },
    });

    const allBulkJobsFunction = new lambda.Function(this, 'getAllBulkJobs', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.getAllBulkJobsHandler',
      code: lambda.Code.fromAsset('./build'),
      timeout: Duration.seconds(5),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        NEPTUNE_PORT: '8182',
      },
    });

    const countVerticesFunction = new lambda.Function(this, 'countVertices', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.countVerticesHandler',
      code: lambda.Code.fromAsset('./build'),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        NEPTUNE_PORT: '8182',
      },
    });

    const addDataFunction = new lambda.Function(this, 'addData', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.addDataToNeptuneHandler',
      code: lambda.Code.fromAsset('./build'),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        NEPTUNE_PORT: '8182',
      },
    });

    const dropAllFunction = new lambda.Function(this, 'dropAll', {
      runtime: lambda.Runtime.NODEJS_24_X,
      handler: 'index.dropAllHandler',
      code: lambda.Code.fromAsset('./build'),
      role: dbAccessRole,
      securityGroups: [dbSecurityGroup],
      vpc: vpcOutput,
      environment: {
        NEPTUNE_ENDPOINT: neptuneDbCluster.attrEndpoint,
        NEPTUNE_PORT: '8182',
      },
    });

    const uploadBucket = new s3.Bucket(this, 'upload', {
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.POST, s3.HttpMethods.PUT],
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
      new lambdaEventSources.S3EventSource(uploadBucket, {
        events: [s3.EventType.OBJECT_CREATED],
        filters: [{ suffix: '.csv' }],
      })
    );

    new CfnOutput(this, 'UploadBucketName', {
      value: uploadBucket.bucketName,
    });
    new CfnOutput(this, 'ClusterStateFunctionName', {
      value: clusterStateFunction.functionName,
    });
    new CfnOutput(this, 'BulkUploadFunctionName', {
      value: bulkUploadFunction.functionName,
    });
    new CfnOutput(this, 'GetAllBulkJobsFunctionName', {
      value: allBulkJobsFunction.functionName,
    });
    new CfnOutput(this, 'CountVerticesFunctionName', {
      value: countVerticesFunction.functionName,
    });
    new CfnOutput(this, 'AddDataFunctionName', {
      value: addDataFunction.functionName,
    });
    new CfnOutput(this, 'DropAllFunctionName', {
      value: dropAllFunction.functionName,
    });
  }
}
