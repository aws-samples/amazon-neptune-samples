import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.file_utilities import record_outputs
from utilities.json_utilities import extract_json
from utilities.config import Config
from utilities.tools import NeptuneTools
from utilities.statistics import StatisticsAccumulator
from agents.querygen.BulkQueryGeneratorAgent import BulkQueryGeneratorAgent
import json
import logging
logger = logging.getLogger(__name__)

def generate_queries(use_case_description_file, output_file_name = None, write_metrics_file = True):
    """
    Generates queries to answer the questions in the use case description
    
    :param use_case_description_file: the path of the use case description text file
    :param output_file_name: File name to output the text file containing the use case description (will be written in `output` directory with `.txt` extension)
    :param write_metrics_file: Description
    
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

    neptune_tools = NeptuneTools(
        connection_url=config.get_neptune_config()["writer_endpoint"]
    )
    
    neptune_mcp_client = neptune_tools.create_neptune_mcp_client()
    if output_file_name is None:
        output_file_name = "queries"

    accumulator = StatisticsAccumulator()

    with neptune_mcp_client:
        # The next agent will take the 3 questions from the model and generate a query for each.  It will inspect the Neptune database
        # to understand the model implementation and ensure the query uses the proper labels and properties, and executes successfully.
        query_gen_agent = BulkQueryGeneratorAgent(
            bedrock_model = bedrock_model,
            mcp_client = neptune_mcp_client.list_tools_sync()
        )
        use_case_response = None
        with open(use_case_description_file, 'r', encoding='utf-8') as f:
            use_case_response = f.read()   
        queries_response = query_gen_agent.execute_task(use_case = use_case_response)
        queries_statistics = query_gen_agent.get_metrics_summary()
        
        record_outputs("queries", output_file_name, queries_response, queries_statistics,'json', write_metrics_file)
        accumulator.accumulate("query_gen_agent", query_gen_agent.get_statistics())
        print(f"Queries: {queries_response}")
            
        print(f"""
            Completed.
            
            Total Accumulated Statistics: 
                {accumulator.print_summary_statistics()}
        """)

def main():
    try:
        use_case_description_file = "output/knowledge_mgmt_recommendation_system.txt"
        generate_queries(use_case_description_file)

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
