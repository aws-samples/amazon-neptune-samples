// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {App, Stack, StackProps} from '@aws-cdk/core';
import {SubnetType, Vpc, BastionHostLinux} from '@aws-cdk/aws-ec2'
import {Effect, ManagedPolicy, Policy, PolicyStatement, Role} from '@aws-cdk/aws-iam';
import {DatabaseInstance} from '@aws-cdk/aws-rds';
import {Cluster} from '@aws-cdk/aws-redshift';
import {Secret} from '@aws-cdk/aws-secretsmanager';

export interface BastionStackProps extends StackProps {
	vpc: Vpc;
	rdsInstance: DatabaseInstance;
	redshiftCluster: Cluster;
	redshiftRole: Role;
	rdsSecret: Secret;
	redshiftSecret: Secret;
}

export class BastionStack extends Stack {
	readonly host: BastionHostLinux;

    constructor(scope: App, id: string, props: BastionStackProps) {
    	super(scope, id, props);

		this.host = new BastionHostLinux(this, 'Bastion-Host', 
			{
				vpc: props.vpc,
				subnetSelection: props.vpc.selectSubnets({
					subnetType: SubnetType.PRIVATE
				})
			});
			
		//	Add S3 and Secrets Manager managed policies
		this.host.role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess'));
		this.host.role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('SecretsManagerReadWrite'));
		
		// The following policy allows Bastion Host to connect to Neptune
	    const neptuneConnectPolicy = new Policy(this, 'NeptuneConnectPolicy', {
	      policyName: `amundsen-bastion-neptune-connect-policy`,
	      roles: [
	        this.host.role
	      ],
	    });
	    
	    neptuneConnectPolicy.addStatements(
	      new PolicyStatement({
	        resources: [
	          `arn:aws:neptune-db:${this.region}:${this.account}:cluster-*`,
	        ],
	        actions: [
	          "neptune-db:*"
	        ],
	        effect: Effect.ALLOW,
	    }));

		//	Get RDS Host from RDS Instance Endpoint
		const rdsHost = props.rdsInstance.dbInstanceEndpointAddress;
		const rdsPort = props.rdsInstance.dbInstanceEndpointPort;
		const rdsDatabase = this.node.tryGetContext('rds-database');

		//	Get RDS PostgreSQL Dump S3 Bucket & Filename
		const sampleS3Bucket = this.node.tryGetContext('sample-data-s3-bucket');
		const rdsDumpFilename = this.node.tryGetContext('sample-data-rds-dump-filename');

		//	Get Redshift Query S3 Bucket & Filename
		const redshiftQueryS3Bucket = this.node.tryGetContext('sample-data-redshift-query-s3-bucket');
		const redshiftQueryFilename = this.node.tryGetContext('sample-data-redshift-query-filename');
		
		//	Get Redshift Host from Redshift Cluster Endpoint
		const redshiftHost = props.redshiftCluster.clusterEndpoint.hostname;
		const redshiftRole = props.redshiftRole.roleArn;
		
		const redshiftFQ = `CREATE EXTERNAL SCHEMA IF NOT EXISTS chatbot_external FROM POSTGRES DATABASE '${rdsDatabase}' SCHEMA 'chatbot' URI '${rdsHost}' PORT ${rdsPort} IAM_ROLE '${redshiftRole}' SECRET_ARN '${props.rdsSecret.secretArn}'`;
		
		this.host.instance.userData.addCommands(
			'yum update -y',
			'sudo amazon-linux-extras install epel -y',
			'sudo touch /etc/yum.repos.d/pgdg.repo', 
			'echo "[pgdg13]" | sudo tee -a /etc/yum.repos.d/pgdg.repo', 
			'echo "name=PostgreSQL 13 for RHEL/CentOS 7 - x86_64" | sudo tee -a /etc/yum.repos.d/pgdg.repo', 
			'echo "baseurl=https://download.postgresql.org/pub/repos/yum/13/redhat/rhel-7-x86_64" | sudo tee -a /etc/yum.repos.d/pgdg.repo',
			'echo "enabled=1" | sudo tee -a /etc/yum.repos.d/pgdg.repo',
			'echo "gpgcheck=0" | sudo tee -a /etc/yum.repos.d/pgdg.repo',
			'sudo yum install postgresql13 -y',
			'sudo yum install jq -y',
			`aws s3 cp s3://${sampleS3Bucket}/${rdsDumpFilename} .`,
			'export PGPASSWORD=$(aws secretsmanager get-secret-value --secret-id ' + props.rdsSecret.secretArn + ' --region ' + this.region + ' | jq -r ".SecretString" | jq -r ".password")',
			'export RDSUID=$(aws secretsmanager get-secret-value --secret-id ' + props.rdsSecret.secretArn + ' --region ' +  this.region + ' | jq -r ".SecretString" | jq -r ".username")',
			'pg_restore -v -h ' + rdsHost + ' -U $RDSUID -d ' + rdsDatabase + ' -p ' + rdsPort + ' ' + rdsDumpFilename,
			'export PGPASSWORD=$(aws secretsmanager get-secret-value --secret-id ' + props.redshiftSecret.secretArn + ' --region ' + this.region + ' | jq -r ".SecretString" | jq -r ".password")',
			'export REDSHIFTUID=$(aws secretsmanager get-secret-value --secret-id ' + props.redshiftSecret.secretArn + ' --region ' +  this.region + ' | jq -r ".SecretString" | jq -r ".username")',
			'psql -h ' + redshiftHost + ' -U $REDSHIFTUID -d dev -p 5439 --no-password -c "' + redshiftFQ + '"',
			`aws s3 cp s3://${redshiftQueryS3Bucket}/${redshiftQueryFilename} .`,
			'psql -h ' + redshiftHost + ' -U $REDSHIFTUID -d dev -p 5439 --no-password -f "' + redshiftQueryFilename + '"',
			'echo "Amundsen Neptune Sample Load"',
			`aws s3 cp s3://${sampleS3Bucket}/sample_data_loader_neptune.py .`,
			`aws s3 cp s3://${sampleS3Bucket}/sample_table.csv .`
		);

    }
}
