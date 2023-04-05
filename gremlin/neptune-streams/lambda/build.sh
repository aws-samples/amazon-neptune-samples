#!/bin/bash -ex

rm -rf target
mkdir target

pushd neptune-streams-demo
sh build.sh
popd



