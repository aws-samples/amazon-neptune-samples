import os
import re
import json
import logging
logger = logging.getLogger(__name__)

def create_snake_case_file_name(text, extension="txt"):
    # Convert to lowercase
    text = text.lower()
    # Replace spaces with underscores
    text = text.replace(" ", "_")
    # Remove special characters
    text = re.sub(r'[^a-z0-9_]', '', text)
    text = text + "." + extension
    return text

def create_directory_structure(file_path):
#    print(f"File path: {file_path}")
    directory = os.path.dirname(file_path)
#    print(f"Directory is '{directory}'")
#    print(f"Path exists? {os.path.exists(directory)}")
    if not os.path.exists(directory):
        os.makedirs(directory)

def record_outputs(folder_name,file_name,agent_response,agent_metrics,agent_response_extension = 'txt',write_metrics=True):
    directory_path = f"output/{folder_name}/"
    create_directory_structure(directory_path)
    filename = create_snake_case_file_name(file_name, agent_response_extension)
    with open(directory_path + filename, "w", encoding='utf-8') as f:
        f.write(agent_response)
    if (write_metrics):
        metrics_file_name = create_snake_case_file_name(f'{file_name}_metrics', 'json')
        with open(directory_path + metrics_file_name, "w", encoding='utf-8') as f:
            f.write(json.dumps(agent_metrics))    
