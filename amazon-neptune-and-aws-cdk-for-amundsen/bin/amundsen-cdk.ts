#!/usr/bin/env node
import 'source-map-support/register';
import {App} from 'aws-cdk-lib';
import {VpcStack} from "../lib/vpc-stack";
import {RdsStack} from "../lib/rds-stack";
import {RedshiftStack} from "../lib/redshift-stack";
import {BastionStack} from "../lib/bastion-stack";
import {AmundsenStack} from "../lib/amundsen-stack";
import {DatabuilderStack} from "../lib/databuilder-stack";

const app = new App();
    	
const vpcStackEntity = new VpcStack(app, 'Amundsen-Blog-VPC-Stack');

const rdsStackEntity = new RdsStack(app, 'Amundsen-Blog-RDS-Stack', {
	vpc: vpcStackEntity.vpc,
	ingressSecurityGroup: vpcStackEntity.ingressSecurityGroup,
	rdsSecret: vpcStackEntity.rdsSecret
});

const redshiftStackEntity = new RedshiftStack(app, 'Amundsen-Blog-Redshift-Stack', {
	vpc: vpcStackEntity.vpc,
	ingressSecurityGroup: vpcStackEntity.ingressSecurityGroup,
	egressSecurityGroup: vpcStackEntity.egressSecurityGroup,
	redshiftSecret: vpcStackEntity.redshiftSecret,
	rdsSecret: vpcStackEntity.rdsSecret
});

const amundsenStackEntity = new AmundsenStack(app, 'Amundsen-Blog-Amundsen-Stack', {
	vpc: vpcStackEntity.vpc,
	ingressSecurityGroup: vpcStackEntity.ingressSecurityGroup,
	airflowS3Bucket: vpcStackEntity.airflowS3Bucket
});

const bastionStackEntity = new BastionStack(app, 'Amundsen-Blog-Bastion-Stack', {
	vpc: vpcStackEntity.vpc,
	rdsInstance: rdsStackEntity.rdsInstance,
	redshiftCluster: redshiftStackEntity.cluster,
	redshiftRole: redshiftStackEntity.role,
	rdsSecret: vpcStackEntity.rdsSecret,
	redshiftSecret: vpcStackEntity.redshiftSecret
});

const databuilderStack = new DatabuilderStack(app, 'Amundsen-Blog-Databuilder-Stack', {
	vpc: vpcStackEntity.vpc,
	ingressSecurityGroup: vpcStackEntity.ingressSecurityGroup,
	cluster: amundsenStackEntity.fargateCluster,
	s3Bucket: vpcStackEntity.airflowS3Bucket,
	esDomain: amundsenStackEntity.esDomain,
	neptuneCluster: amundsenStackEntity.neptuneCluster,
	rdsInstance: rdsStackEntity.rdsInstance,
	rdsSecret: vpcStackEntity.rdsSecret,
});

app.synth();
