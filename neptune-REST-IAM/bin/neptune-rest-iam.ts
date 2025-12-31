#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { NeptuneRestIamStack } from '../lib/neptune-rest-iam-stack';

const app = new cdk.App();
new NeptuneRestIamStack(app, 'NeptuneRestIamStack');
