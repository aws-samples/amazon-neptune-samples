import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.modelgen.ModelGeneratorAgent import ModelGeneratorAgent
from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.file_utilities import create_directory_structure, create_snake_case_file_name
from utilities.config import Config

import json
import logging
logger = logging.getLogger(__name__)

def generate_model(use_case_description_file, output_file_name = None, write_metrics_file = True):
        """
        Docstring for generate_model
        
        :param use_case_description_file: the path of the use case description text file
        :param output_file_name: file name to output the text file containing the model. The file will be written to the 'output' directory with a ".json" extension.
        :param write_metrics_file: boolean value. If True, there will be an additional file named {output_file_name}_metrics.json containing the full metrics output from the Strands Agent.


        use_case_description_file:
        - This file was the output of the generate_usecase_from_topic.py process and resides in the output directory
        - It may be modified by hand to better reflect the desired use case.
        - It should still contain the Questions section with at least 3 questions that the model should be able to answer.

        output_file_name:
        If left as None, the file will be model.json.

        """
        # Load the configuration
        print("===============Loading Configuration================\n")
        config = Config()

        print("===============Initializing AWS SDK objects and Tools================\n")
        bedrock_model = get_bedrock_model(
            model_id=config.get_llm_config()["model"],
            region_name=config.get_llm_config()["region"]
        )

        accumulator = StatisticsAccumulator()

        print("===============Initializing Agents================\n")
        agent = ModelGeneratorAgent(
            bedrock_model = bedrock_model,
        )

        if use_case_description_file is None:
            raise TypeError("use_case_description_file must be specified.")
        
        if output_file_name is None:
            output_file_name = "model"

        use_case_response = None
        with open(use_case_description_file, 'r', encoding='utf-8') as f:
            use_case_response = f.read()        

        # This agent takes the use case and generates a graph model of nodes, edges, and properties for each.
        agent_response = agent.execute_task(use_case=use_case_response)
        directory_path = f"output/"
        print(f"Directory Path passed in as {directory_path}")
        create_directory_structure(directory_path)
        filename = create_snake_case_file_name(output_file_name, 'json')
        with open(directory_path + filename, "w", encoding='utf-8') as f:
            f.write(agent_response)

        if write_metrics_file:
            agent_metrics = agent.get_metrics_summary()
            metrics_file_name = create_snake_case_file_name(f'{output_file_name}_metrics', 'json')
            with open(directory_path + metrics_file_name, "w", encoding='utf-8') as f:
                f.write(json.dumps(agent_metrics))

        accumulator.accumulate("model_gen_agent", agent.get_statistics())
        print(f"Model: {agent_response}")

        print(f"""
            Completed.
            
            Total Accumulated Statistics: 
                {accumulator.print_summary_statistics()}
        """)

def main():
    try:
        # replace this with the output file from the generate_usecase_from_topic.py workflow.
        use_case_description_file = "output/knowledge_mgmt_recommendation_system.txt"
        generate_model(use_case_description_file)
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
