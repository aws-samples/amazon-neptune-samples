#!/usr/bin/env bash
set -e

mkdir -p build

echo "Running Bundle"
npm run bundle

echo "Bundle Complete"
echo "Running CDK Synth, ensure cdk bootstrap has already been run for your deployment region."
./node_modules/aws-cdk/bin/cdk synth
echo "Synth Complete, deploying Stack"
./node_modules/aws-cdk/bin/cdk deploy NeptuneRestIamStack \
--region eu-west-1 \
--require-approval never \
--stack-name neptune-rest-iam-stack
echo "Deployment Complete"
