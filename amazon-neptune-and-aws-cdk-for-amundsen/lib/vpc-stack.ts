// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
import {App, RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Peer, Port, SecurityGroup, SubnetType, Vpc, InterfaceVpcEndpoint, InterfaceVpcEndpointAwsService, GatewayVpcEndpoint, GatewayVpcEndpointAwsService} from 'aws-cdk-lib/aws-ec2'
import {Secret} from 'aws-cdk-lib/aws-secretsmanager';
import {Bucket, BlockPublicAccess} from 'aws-cdk-lib/aws-s3';

export class VpcStack extends Stack {
    readonly vpc: Vpc;
    readonly ingressSecurityGroup: SecurityGroup;
    readonly egressSecurityGroup: SecurityGroup;
    readonly ssmVPCE: InterfaceVpcEndpoint;
    readonly ssmMessagesVPCE: InterfaceVpcEndpoint;
    readonly ec2MessagesVPCE: InterfaceVpcEndpoint;
    readonly ecrMessagesVPCE: InterfaceVpcEndpoint;
    readonly ecrDockerMessagesVPCE: InterfaceVpcEndpoint;
    readonly s3VPCE: GatewayVpcEndpoint;
    readonly rdsSecret: Secret;
    readonly redshiftSecret: Secret;
    readonly airflowS3Bucket: Bucket;

    constructor(scope: App, id: string, props?: StackProps) {
    	super(scope, id, props);
  
		const cidr = this.node.tryGetContext('vpc-cidr');

		this.vpc = new Vpc(this, 'VPC', {
			cidr: cidr,
			maxAzs: 2,
			subnetConfiguration: [
			{
				cidrMask: 26,
				name: 'Public',
				subnetType: SubnetType.PUBLIC,
			},
			{
				cidrMask: 26,
				name: 'Private',
				subnetType: SubnetType.PRIVATE,
			}
			],
			natGateways: 1
		});
		
		this.ingressSecurityGroup = new SecurityGroup(this, 'Ingress', {
			vpc: this.vpc,
			allowAllOutbound: true,
			securityGroupName: 'IngressSecurityGroup',
		});
	
		//	Allow traffic to TCP/5432 (Postgres), TCP/5439 (Redshift), TCP/8182 (Neptune), TCP/80 & TCP/443 (ES)
		this.ingressSecurityGroup.addIngressRule(Peer.ipv4(cidr), Port.tcp(5432));
		this.ingressSecurityGroup.addIngressRule(Peer.ipv4(cidr), Port.tcp(5439));
		this.ingressSecurityGroup.addIngressRule(Peer.ipv4(cidr), Port.tcp(8182));
		this.ingressSecurityGroup.addIngressRule(Peer.ipv4(cidr), Port.tcp(80));
		this.ingressSecurityGroup.addIngressRule(Peer.ipv4(cidr), Port.tcp(443));
	
		this.egressSecurityGroup = new SecurityGroup(this, 'Egress', {
			vpc: this.vpc,
			allowAllOutbound: true,
			securityGroupName: 'EgressSecurityGroup',
		});
		
		this.ssmVPCE = new InterfaceVpcEndpoint(this, 'SSM-VPCE', {
			service: InterfaceVpcEndpointAwsService.SSM,
			vpc: this.vpc,
			privateDnsEnabled: true,
			subnets: this.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			})
		});
	
		this.ssmMessagesVPCE = new InterfaceVpcEndpoint(this, 'SSM-Messages-VPCE', {
			service: InterfaceVpcEndpointAwsService.SSM_MESSAGES,
			vpc: this.vpc,
			privateDnsEnabled: true,
			subnets: this.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			})
		});
	
		this.ec2MessagesVPCE = new InterfaceVpcEndpoint(this, 'EC2-Messages-VPCE', {
			service: InterfaceVpcEndpointAwsService.EC2_MESSAGES,
			vpc: this.vpc,
			privateDnsEnabled: true,
			subnets: this.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			})
		});

		this.ecrMessagesVPCE = new InterfaceVpcEndpoint(this, 'ECR-VPCE', {
			service: InterfaceVpcEndpointAwsService.ECR,
			vpc: this.vpc,
			privateDnsEnabled: true,
			subnets: this.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			})
		});
	
		this.ecrDockerMessagesVPCE = new InterfaceVpcEndpoint(this, 'ECR-Docker-VPCE', {
			service: InterfaceVpcEndpointAwsService.ECR_DOCKER,
			vpc: this.vpc,
			privateDnsEnabled: true,
			subnets: this.vpc.selectSubnets({
				subnetType: SubnetType.PRIVATE
			})
		});
		
		this.s3VPCE = new GatewayVpcEndpoint(this, 'S3-VPCE', {
			service: GatewayVpcEndpointAwsService.S3,
			vpc: this.vpc
		});
		
		//	Secret used by RDS
    	this.rdsSecret = new Secret(this, 'RDS-Secret', {
    		generateSecretString: {
        		secretStringTemplate: JSON.stringify({ username: 'postgres' }),
        		generateStringKey: 'password',
        		excludePunctuation: true,
        		excludeCharacters: '/@" \'',
    		},
    	});
		
		//	Secret used by Redshift
    	this.redshiftSecret = new Secret(this, 'Redshift-Secret', {
    		generateSecretString: {
        		secretStringTemplate: JSON.stringify({ username: 'administrator' }),
        		generateStringKey: 'password',
        		excludePunctuation: true,
        		excludeCharacters: '/@" \'',
    		},
    	});

		//	S3 Bucket for Airflow
		this.airflowS3Bucket = new Bucket(this, 'Airflow-S3-Bucket', {
    		versioned: false,
    		autoDeleteObjects: true,
    		removalPolicy: RemovalPolicy.DESTROY,
    		blockPublicAccess: BlockPublicAccess.BLOCK_ALL
    	});
    	
    }
}
