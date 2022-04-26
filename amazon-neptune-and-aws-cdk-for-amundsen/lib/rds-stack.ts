// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {App, Stack, StackProps} from 'aws-cdk-lib';
import {DatabaseInstance, DatabaseInstanceEngine, StorageType, PostgresEngineVersion} from 'aws-cdk-lib/aws-rds';
import {Secret} from 'aws-cdk-lib/aws-secretsmanager';
import {InstanceClass, InstanceSize, InstanceType, SubnetType, Vpc, SecurityGroup} from 'aws-cdk-lib/aws-ec2';

export interface RDSStackProps extends StackProps {
	    vpc: Vpc;
	    ingressSecurityGroup: SecurityGroup;
	    rdsSecret: Secret;
}

export class RdsStack extends Stack {
	readonly rdsInstance: DatabaseInstance;

	constructor(scope: App, id: string, props: RDSStackProps) {
		super(scope, id, props);
		
		const rdsEngine = this.node.tryGetContext('rds-engine');
		const rdsPort = this.node.tryGetContext('rds-port');
		const rdsDatabase = this.node.tryGetContext('rds-database');
		var engine = DatabaseInstanceEngine.postgres({ version: PostgresEngineVersion.VER_12_10 });
		if (rdsEngine == 'MYSQL') engine = DatabaseInstanceEngine.MYSQL;

		this.rdsInstance = new DatabaseInstance(this, 'RDS-DB-Instance', {
			engine: engine,
		    instanceType: InstanceType.of(InstanceClass.T3, InstanceSize.MEDIUM),
			vpc: props.vpc,
			securityGroups: [props.ingressSecurityGroup],
			vpcSubnets: props.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			}),
			storageEncrypted: true,
			multiAz: false,
			autoMinorVersionUpgrade: false,
			allocatedStorage: 25,
			storageType: StorageType.GP2,
			deletionProtection: false,
			databaseName: rdsDatabase,
			credentials: {
				username: props.rdsSecret.secretValueFromJson('username').toString(),
				password: props.rdsSecret.secretValueFromJson('password')
			},
			port: rdsPort
		});

	}
}
