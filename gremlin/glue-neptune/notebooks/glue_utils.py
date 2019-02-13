import boto3
import time
import os
from botocore.exceptions import ClientError

client = boto3.client('glue')

class Status:

    def __init__(self, s):
        print('Running [{}]'.format(s), end="", flush=True) 
    
    def update(self):
        print('.', end="", flush=True)
    
    def finish(self):
        print(" Finished\n")

def run_job(job_name):
    
    status = Status(job_name)
    
    job_run_id = ''
    is_starting = True
    
    while is_starting:
        try: 
            job_run_id = client.start_job_run(JobName=job_name)['JobRunId']
            is_starting = False
        except ClientError as e:
            if e.response['Error']['Code'] == 'ConcurrentRunsExceededException':
                time.sleep(5)
            else:
                raise(e)
    
    response = {}
    job_run_state = ''
    is_running = True
    
    while is_running:
        response = client.get_job_run(JobName=job_name, RunId=job_run_id)
        job_run_state = response['JobRun']['JobRunState']
        if job_run_state == 'STOPPED' or job_run_state == 'SUCCEEDED':
            is_running = False
        else:
            status.update()
            time.sleep(5)
    
    status.finish()
            
    return job_run_state

def run_crawler(crawler_name):
    
    status = Status(crawler_name)
    
    client.start_crawler(Name=crawler_name)
    
    response = {}
    crawler_state = ''
    is_running = True
    
    while is_running:
        response = client.get_crawler(Name=crawler_name)
        crawler_state = response['Crawler']['State']
        if crawler_state == 'READY':
            is_running = False
        else:
            status.update()
            time.sleep(5)
    
    status.finish()
            
    return crawler_state

def list_tables(database_name):
    
    response = client.get_tables(DatabaseName=database_name)
    for table in response['TableList']:
        yield table['Name']
        
def glue_resource(name):
    return '{}_{}'.format(os.environ['GLUE_PREFIX'], name)