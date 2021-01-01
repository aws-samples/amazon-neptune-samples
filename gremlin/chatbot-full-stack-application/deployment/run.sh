#!/bin/bash

pip install -r requirements.txt

python deploy.py ${CLUSTERNAME}
echo ${FRONTEND_PATH}
cd ${FRONTEND_PATH}
yarn start