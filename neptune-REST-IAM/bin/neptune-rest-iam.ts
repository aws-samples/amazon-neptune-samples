#!/usr/bin/env node
import { App } from 'aws-cdk-lib';
import { NeptuneRestIamStack } from '../lib/neptune-rest-iam-stack';

const app = new App();
new NeptuneRestIamStack(app, 'NeptuneRestIamStack');
