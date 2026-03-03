import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.file_utilities import record_outputs
from utilities.json_utilities import extract_json
from utilities.config import Config
from utilities.tools import NeptuneTools
from agents.datapatterngen.DataPatternGeneratorAgent import DataPatternGenerationAgent
import json
import logging
logger = logging.getLogger(__name__)

def generate_scenario_data(use_case_description_file, queries_file, output_file_name = None, write_metrics_file = True):
    """
    Checks the queries to ensure they have data, and if not, then it will generate data specifically to answer the scenario.
    
    :param use_case_topic:  
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
         # The output of the previous agent *should* but just JSON with the questions and queries.  
        # If the LLM added text or markup to it, you will have to remove it before continuing.

        use_case_response = None
        with open(use_case_description_file, 'r', encoding='utf-8') as f:
            use_case_response = f.read()  

        queries_response = None
        with open(queries_file, 'r', encoding='utf-8') as f:
            queries_response = f.read()  
        query_json = None
        try:
            query_json = json.loads(queries_response)
        except json.JSONDecodeError as jde:
            # If there is extra junk in the LLM response, try removing it 
            print("JSON output from the model is not valid. This usually means it added markup or text. We will try to remove it...")
            try:
                query_json = extract_json(queries_response, schema_name="queries")
                print("It appears we were successful...")
            except json.JSONDecodeError as jde2:
                print("It appears we were unsuccessful. Raising up the original exception.")
                raise jde

        # Now we will break into a subloop for each of the 3 questions
        queries_list = query_json.get("queries") or query_json.get("results")
        for idx, item in enumerate(queries_list):

            print(f"=========Starting the question '{item['question']}'======\n")
            query = item["query"]

            # First, let's make sure the query has data to retrieve. More complex scenarios are generally not created
            # using the data generation method from earlier.
            # Directly invoke an MCP tool
            result = neptune_mcp_client.call_tool_sync(
                tool_use_id=f"tool-{output_file_name}_q{str(idx)}",
                name="run_opencypher_query",
                arguments={"query": query}
            )

            print(f"Query Execution Result: {result}")

            # If the query has an error or does not return any data, use the Data Pattern Generation Agent to create some
            # KNOWN ISSUE: Parameterized queries (with $variables) will return MissingParameter errors.
            # This workflow currently does not handle parameter substitution for query validation.
            has_error = result.get("status") == "error"
            has_no_data = not result.get("content") or (isinstance(result.get("content"), list) and len(result["content"]) == 0)
            
            if has_error or has_no_data:
                print("===============Generating Data for this Scenario================\n")                
                data_pattern_generator_agent = DataPatternGenerationAgent(
                    bedrock_model=bedrock_model,
                    neptune_mcp_tools=neptune_mcp_client.list_tools_sync()
                )
                patterngen_response = data_pattern_generator_agent.execute_task(use_case=use_case_response, query=query)
                patterngen_statistics = data_pattern_generator_agent.get_metrics_summary()
                record_outputs("scenariogen", f"{output_file_name}_q{str(idx)}", patterngen_response, patterngen_statistics, write_metrics=write_metrics_file)
                accumulator.accumulate(f"patterngen_q{str(idx)}", data_pattern_generator_agent.get_statistics())
                print(f"{patterngen_response}")
            else:
                print("Data already exists for this scenario. Skipping data generation.")
            
        print(f"""
            Completed.
            
            Total Accumulated Statistics: 
                {accumulator.print_summary_statistics()}
        """)

def main():
    try:
        use_case_description_file = "output/knowledge_mgmt_recommendation_system.txt"
        queries_file = "output/queries/queries.json"
        generate_scenario_data(use_case_description_file, queries_file)

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
