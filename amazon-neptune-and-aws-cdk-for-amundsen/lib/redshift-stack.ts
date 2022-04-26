// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {App, Stack, StackProps} from 'aws-cdk-lib';
import {Cluster, ClusterType} from '@aws-cdk/aws-redshift-alpha';
import {Secret} from 'aws-cdk-lib/aws-secretsmanager';
import {SubnetType, Vpc, SecurityGroup} from "aws-cdk-lib/aws-ec2";
import {Effect, PolicyStatement, Role, ServicePrincipal} from 'aws-cdk-lib/aws-iam';

export interface RedshiftStackProps extends StackProps {
	vpc: Vpc;
	ingressSecurityGroup: SecurityGroup;
	egressSecurityGroup: SecurityGroup;
	redshiftSecret: Secret;
	rdsSecret: Secret;
}

export class RedshiftStack extends Stack {
	readonly cluster: Cluster;
	readonly role: Role;

	constructor(scope: App, id: string, props: RedshiftStackProps) {
		super(scope, id, props);
		
		/*
		Redshift Federated Query to Postgres
		*/
		
		this.role = new Role(this, 'Redshift-Federated-Query-Role', {
			assumedBy: new ServicePrincipal('redshift.amazonaws.com'),
		});
		
		this.role.addToPolicy(new PolicyStatement({
			effect: Effect.ALLOW,
			actions: [
                'secretsmanager:GetResourcePolicy',
                'secretsmanager:GetSecretValue',
                'secretsmanager:DescribeSecret',
                'secretsmanager:ListSecretVersionIds'
            ],
			resources: [
				props.redshiftSecret.secretArn, 
				props.rdsSecret.secretArn
			],
		}));
		
		this.role.addToPolicy(new PolicyStatement({
			effect: Effect.ALLOW,
			actions: [
                'secretsmanager:GetRandomPassword',
                'secretsmanager:ListSecrets'
            ],
			resources: ['*'],
		}));
		
		this.cluster = new Cluster(this, 'Redshift-Cluster', {
	    	masterUser: {
	    		masterUsername: props.redshiftSecret.secretValueFromJson('username').toString(),
	    		masterPassword: props.redshiftSecret.secretValueFromJson('password')
	    	},
	    	vpc: props.vpc,
	    	clusterType: ClusterType.SINGLE_NODE,
	    	securityGroups: [props.ingressSecurityGroup, props.egressSecurityGroup],
	    	vpcSubnets: props.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			}),
			roles: [this.role]
		});

	}
}
