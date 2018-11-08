from __future__  import print_function
from ipywidgets import widgets
from IPython.display import display

'''
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import subprocess

button = widgets.Button(description='Update notebooks')
display(button)

def on_button_clicked(b):
    notebook_dir = '/home/ec2-user/SageMaker/Neptune'
    
    print('Updating helper modules...')
    result = subprocess.check_output(['aws', 's3', 'sync', 's3://aws-neptune-customer-samples/neptune-sagemaker/notebooks/', notebook_dir, '--exclude', '"*"', '--include', '"util/*"', '--delete']).decode("utf-8")
    print(result + 'Updating content...')
    result = subprocess.check_output(['sudo', '../../../sync.sh']).decode("utf-8")
    print(result + 'Done\n')

    
button.on_click(on_button_clicked)