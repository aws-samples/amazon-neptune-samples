import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.usecasedefiner.UseCaseDefinerAgent import UseCaseDefinerAgent
from utilities.usecases import UseCaseTopics
from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.file_utilities import create_directory_structure, create_snake_case_file_name
from utilities.config import Config

import json
import logging
logger = logging.getLogger(__name__)

def generate_use_case(use_case_topic, output_file_name = None, write_metrics_file = True):
        """
        Generates a use case from a topic
        
        :param use_case_topic: Choose the Use Case Topic you want to use from utilities.usecases or write your own topic as a string here 
        :param output_file_name: File name to output the text file containing the use case description (will be written in `output` directory with `.txt` extension)
        :param write_metrics_file:  boolean value. If True, there will be an additional file named {output_file_name}_metrics.json containing the full metrics output from the Strands Agent.
        
        Use Case Topic Examples
        --------
        use_case_topic = UseCaseTopic.KNOWLEDGE_MGMT_RECOMMENDATION_SYSTEM.value
        use_case_topic = "Library Book Management System"

        Output File Name:
        If left as None, the file will be the use_case_topic value in snake case form.

        Write Metrics File:
        If True, there will be an additional file named {output_file_name}_metrics.json containing the full metrics output from the Strands Agent.
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
        use_case_agent = UseCaseDefinerAgent(
            bedrock_model = bedrock_model
        )

        # The first agent takes in the topic sentence of the use case topic and generates a detailed description of the use case 
        # along with 3 questions the model should answer.
        agent_response = use_case_agent.execute_task(use_case=use_case_topic)

        directory_path = "output/"
        create_directory_structure(directory_path)
        filename = create_snake_case_file_name(output_file_name if output_file_name is not None else use_case_topic, 'txt')
        with open(directory_path + filename, "w", encoding='utf-8') as f:
            f.write(f"<use_case>\n{use_case_topic}\n</use_case><response>\n{agent_response}\n</response>")

        if write_metrics_file:
            agent_metrics = use_case_agent.get_metrics_summary()
            metrics_file_name = create_snake_case_file_name(f'{output_file_name if output_file_name is not None else use_case_topic}_metrics', 'json')
            with open(directory_path + metrics_file_name, "w", encoding='utf-8') as f:
                f.write(json.dumps(agent_metrics))

        accumulator.accumulate("use_case_agent", use_case_agent.get_statistics())
        print(f"Use Case: {use_case_topic}\n\nResponse:\n{agent_response}")

        print(f"""
            Completed.
            
            Total Accumulated Statistics: 
                {accumulator.print_summary_statistics()}
        """)

def main():
    try:
         generate_use_case(UseCaseTopics.KNOWLEDGE_MGMT_RECOMMENDATION_SYSTEM.value)

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
