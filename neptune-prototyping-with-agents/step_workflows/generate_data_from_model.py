import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.datagen.GranularEdgeGeneratorAgent import GranularEdgeGeneratorAgent
from agents.datagen.GranularNodeGeneratorAgent import GranularNodeGeneratorAgent
from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.tools import NeptuneTools
from utilities.file_utilities import create_directory_structure, create_snake_case_file_name
from utilities.json_utilities import extract_json
from utilities.config import Config

import json
import time
import concurrent.futures
import logging
logger = logging.getLogger(__name__)

def generate_data(use_case_description_file, model_file, output_file_prefix = None, write_metrics_file = True):
        """
        Docstring for generate_data
        
        :param use_case_description_file: path of the use case description text file
        :param model_file: path of the model json file
        :param output_file_prefix: prefix of the set of text files describing what the agents created in Neptune. file name will be `{output_file_prefix}_{label}.txt`
        :param write_metrics_file: boolean value. If True, there will be an additional file named {output_file_prefix}_{label}_metrics.json containing the full metrics output from the Strands Agent.
        
        use_case_description_file:
        This file was the output of the `generate_usecase_from_topic` process and resides in the output directory
        This should remain the same version used in `generate_model_from_usecase` for consistency. If you change it, you should run that step again first.
        It should still contain the Questions section with at least 3 questions that the model should be able to answer.

        model_file:
        This file was the output of the `generate_model_from_usecase` process and resides in the output directory
        The model may be modified by hand to better reflect the desired outcome.

        output_file_prefix:
        If left as None, the file prefix will be `data`.
        """
        if output_file_prefix is None:
            output_file_prefix = "data"

        """
        If True, there will be an additional file named {output_file_name}_metrics.json containing the full metrics output from the Strands Agent.
        """
        write_metrics_file = True

        model_file_path = model_file
        """
        Each agent generates the data for a single label.  First all of the nodes are created, then all of the edges.
        To speed this up, you can parallelize the process by using multiple concurrent threads. 
        The variable MAX_THREADS sets the maximum parallelization.
        NOTE:  The higher this value, the more likely you'll run into LLM throttling unless your requests per minute quota is high.
        """
        MAX_THREADS = 4

        """
        All of the threads will execute at once. The beginning of the process involves sending a lot of requests to the LLM and is where throttling frequently occurs.
        Setting STAGGER_TIME_SECONDS to a value greater than 0 introduces a delay between when each label is added to the pool and therefore staggers the start, possibly reducing
        the number of throttling events at the outset.
        """
        STAGGER_TIME_SECONDS = 20

        # Load the configuration
        print("===============Loading Configuration================\n")
        config = Config()

        print("===============Initializing AWS SDK objects and Tools================\n")
        bedrock_model = get_bedrock_model(
            model_id=config.get_llm_config()["model"],
            region_name=config.get_llm_config()["region"]
        )

        neptune_tools = NeptuneTools(
            connection_url=config.get_neptune_config()["writer_endpoint"]
        )

        neptune_mcp_client = neptune_tools.create_neptune_mcp_client()
        with neptune_mcp_client:

            accumulator = StatisticsAccumulator()

            #### DEFINE TASK FUNCTIONS
            def node_agent_function(use_case, model, label, accumulator):
                agent = GranularNodeGeneratorAgent(
                    bedrock_model = bedrock_model,
                    neptune_mcp_tools = neptune_mcp_client.list_tools_sync(),
                    label=label
                )
                output = agent.execute_task(use_case=use_case, model=model)
                filename = create_snake_case_file_name(f"{output_file_prefix}_{label}", 'txt')
                directory_path = f"output/"
                create_directory_structure(directory_path)
                with open(directory_path + filename, "w", encoding='utf-8') as f:
                    f.write(output)
                if write_metrics_file:
                    agent_metrics = agent.get_metrics_summary()
                    metrics_file_name = create_snake_case_file_name(f"{output_file_prefix}_{label}_metrics", 'json')
                    with open(directory_path + metrics_file_name, "w", encoding='utf-8') as f:
                        f.write(json.dumps(agent_metrics))                    
                accumulator.accumulate(f"data-gen-node:{label}", agent.get_statistics())
                return f"""
                        Output for node label {label}:
                        
                        Output:
                        {output if output is not None else "N/A"}
                    """   
            
            def edge_agent_function(use_case, model, label, accumulator):
                agent = GranularEdgeGeneratorAgent(
                    bedrock_model = bedrock_model,
                    neptune_mcp_tools = neptune_mcp_client.list_tools_sync(),
                    label=label
                )
                output = agent.execute_task(use_case=use_case, model=model)
                filename = create_snake_case_file_name(f"{output_file_prefix}_{label}", 'txt')
                directory_path = f"output/"
                create_directory_structure(directory_path)
                with open(directory_path + filename, "w", encoding='utf-8') as f:
                    f.write(output)
                if write_metrics_file:
                    agent_metrics = agent.get_metrics_summary()
                    metrics_file_name = create_snake_case_file_name(f"{output_file_prefix}_{label}_metrics", 'json')
                    with open(directory_path + metrics_file_name, "w", encoding='utf-8') as f:
                        f.write(json.dumps(agent_metrics))                    
                accumulator.accumulate(f"data-gen-edge:{label}", agent.get_statistics())
                return f"""
                    Output for edge label {label}:
                    
                    Output:
                    {output if output is not None else "N/A"}
                """

            ### END TASK FUNCTIONS
    
            if model_file_path is None:
                raise TypeError("model_file_path must be specified.")

            model = None
            with open(model_file_path, 'r', encoding='utf-8') as f:
                model = f.read()        

                # The output of the model *should* be just JSON describing the model.  
                # If the LLM added text or markup to it, you will have to remove it before continuing.
                model_json = None
                try:
                    model_json = json.loads(model)
                except json.JSONDecodeError as jde:
                    # If there is extra junk in the LLM response, try removing it 
                    print("JSON output from the model is not valid. This usually means it added markup or text. We will try to remove it...")
                    try:
                        model_json = extract_json(model, schema_name="model")
                        print("It appears we were successful...")
                    except json.JSONDecodeError as jde2:
                        print("It appears we were unsuccessful. Raising up the original exception.")
                        raise jde

                # For each node and edge label in that model, we are going to assign a worker in a thread pool to run an agent that
                # will generate realistic data specifically for that label and insert it directly into Neptune
                with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_THREADS) as executor:
                    node_futures = []
                    edge_futures = []
                    for label in model_json['nodes']:
                        node_futures.append(executor.submit(node_agent_function, use_case_description_file, model, label, accumulator))
                        if STAGGER_TIME_SECONDS > 0:
                            # nosemgraph: python.lang.best-practice.arbitrary-sleep
                            time.sleep(STAGGER_TIME_SECONDS) 

                    for task in node_futures:
                        result = task.result()
                        print(f"Task Finished!\nResult:\n{result}")

                    for edge in model_json['edges']:
                        edge_futures.append(executor.submit(edge_agent_function, use_case_description_file, model, edge["label"], accumulator))

                    for task in edge_futures:
                        result = task.result()
                        print(f"Task Finished!\nResult:\n{result}")

            print(f"""
                Completed.
                
                Total Accumulated Statistics: 
                    {accumulator.print_summary_statistics()}
            """)


def main():
    try:
        use_case_description_file = "output/financial_transaction_monitoring.txt"
        model_file = "output/financial_transaction_monitoring_model.json"

        generate_data(use_case_description_file, model_file)
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
