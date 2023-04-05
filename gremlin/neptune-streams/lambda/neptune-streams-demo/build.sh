#!/bin/bash -ex

pip install virtualenv
rm -rf temp
virtualenv temp --python=python3.8
source temp/bin/activate
pushd temp
pip install redis
cd lib/python3.8/site-packages
rm -rf certifi-*
rm -rf easy_install.py
rm -rf six.py
cp -r ../../../../*.py .
cp -r ../../../../*.zip .
unzip -o neptune_python_utils.zip
zip -r neptune_streams_demo.zip ./* -x "*.zip" -x "*pycache*" -x "*.so" -x "*dist-info*" -x "*.virtualenv" -x "pip*" -x "pkg_resources*" -x "setuptools*" -x "wheel*" -x "certifi*"
mv neptune_streams_demo.zip ../../../../../target/neptune_streams_demo.zip
deactivate
popd
rm -rf temp



