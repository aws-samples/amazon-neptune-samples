#!/usr/bin/env bash
set -e

REGION="${1:?Usage: ./deploy.bash <region>}"

mkdir -p build

echo "Running Bundle"
npm run bundle

echo "Bundle Complete"
echo "Running CDK Synth, ensure cdk bootstrap has already been run for ${REGION}."
npx cdk synth
echo "Synth Complete, deploying Stack to ${REGION}"
npx cdk deploy NeptuneRestIamStack \
  --region "${REGION}" \
  --require-approval never
echo "Deployment Complete"
