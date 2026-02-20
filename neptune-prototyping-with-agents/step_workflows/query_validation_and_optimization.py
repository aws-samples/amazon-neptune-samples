import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.queryrecommender.QueryExplainerAgent import QueryExplainerAgent
from agents.querytuning.QueryTuningAgent import QueryTuningAgent
from agents.datapatterngen.DataPatternGeneratorAgent import DataPatternGenerationAgent
from strands.types.exceptions import EventLoopException
from utilities.usecases import UseCaseTopics
from utilities.statistics import StatisticsAccumulator
from utilities.bedrock import get_bedrock_model
from utilities.tools import NeptuneTools
from utilities.file_utilities import record_outputs, create_directory_structure, create_snake_case_file_name
from utilities.json_utilities import extract_json
from utilities.config import Config

import json
import logging
logger = logging.getLogger(__name__)

def end_to_end():
    # Load the configuration
    print("===============Loading Configuration================\n")
    config = Config()

    use_case_topic = UseCaseTopics.CYBERSECURITY_THREAT_DETECTION_NETWORK_ANALYSIS.value
    print(use_case_topic)

    print("===============Initializing AWS SDK objects and Tools================\n")
    bedrock_model = get_bedrock_model(
        model_id=config.get_llm_config()["model"],
        region_name=config.get_llm_config()["region"]
    )

    neptune_tools = NeptuneTools(
        connection_url=config.get_neptune_config()["writer_endpoint"]
    )

    neptune_mcp_client = neptune_tools.create_neptune_mcp_client()

    accumulator = StatisticsAccumulator()

    ### END TASK FUNCTIONS

    with neptune_mcp_client:

        use_case_response = None
        queries_response = None

        with open('output/usecasedefiner/cybersecurity_threat_detection_and_network_analysis.txt', 'r', encoding='utf-8') as file:
            use_case_response = file.read()
        print(use_case_response)

        with open('output/queries/cybersecurity_threat_detection_and_network_analysis.txt', 'r', encoding='utf-8') as file:
            queries_response = file.read()
        print(queries_response)

        # The output of the previous agent *should* but just JSON with the questions and queries.  
        # If the LLM added text or markup to it, you will have to remove it before continuing.
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
        for idx, item in enumerate(query_json["results"]):

            print(f"=========Starting the question '{item['question']}'======\n")
            query = item["query"]

            # First, let's make sure the query has data to retrieve. More complex scenarios are generally not created
            # using the data generation method from earlier.
            # Directly invoke an MCP tool
            result = neptune_mcp_client.call_tool_sync(
                tool_use_id=f"tool-{use_case_topic}_{str(idx)}",
                name="run_opencypher_query",
                arguments={"query": query}
            )

            print(f"Query Execution Result: {result}")

            # If the query does not return any data, use the Data Pattern Generation Agent to create some
            if not result["content"]:
                print("===============Generating Data for this Scenario================\n")                
                data_pattern_generator_agent = DataPatternGenerationAgent(
                    bedrock_model=bedrock_model,
                    neptune_mcp_tools=neptune_mcp_client.list_tools_sync()
                )
                patterngen_response = data_pattern_generator_agent.execute_task(use_case=use_case_response, query=query)
                patterngen_statistics = data_pattern_generator_agent.get_metrics_summary()
                record_outputs("scenariogen", f"{use_case_topic}_q{str(idx)}", patterngen_response, patterngen_statistics)
                accumulator.accumulate(f"patterngen_q{str(idx)}", data_pattern_generator_agent.get_statistics())
                print(f"{patterngen_response}")

            print(f"=========================== Analyzing the query =======================\n")

            # This agent will generate a Neptune explain plan for the query and analyze any potential issues
            query_explain_agent = QueryExplainerAgent(
                bedrock_model=bedrock_model,
                explain_tool=neptune_tools.query_explain_tool
            )
            explain_response = query_explain_agent.execute_task(query_text=query)
            explain_statistics = query_explain_agent.get_metrics_summary()
            record_outputs("explain", f"{use_case_topic}_{str(idx)}", explain_response, explain_statistics)
            accumulator.accumulate(f"explain_q{str(idx)}", query_explain_agent.get_statistics())
            print(f"Explain for {query}: \n{explain_response}")

            print(f"=========================== Tuning the query =======================\n")

            # This agent will take in the query and analysis from the last agent and generate several alternate queries that may perform better.
            # It will test each variant and return the best performing queries that give the same answer.
            query_tuning_agent = QueryTuningAgent(
                bedrock_model=bedrock_model,
                neptune_mcp=neptune_mcp_client.list_tools_sync(),
                explain_tool=neptune_tools.query_explain_tool,
                validate_query_results_tool=neptune_tools.validate_query_results_tool
            )
            tuning_response = query_tuning_agent.execute_task(query=query, optimization_text=explain_response)
            tuning_statistics = query_tuning_agent.get_metrics_summary()
            record_outputs("tuning", f"{use_case_topic}_{str(idx)}", tuning_response, tuning_statistics)
            accumulator.accumulate(f"tuning_q{str(idx)}", query_tuning_agent.get_statistics())
            print(f"Tuning for {query}: \n{tuning_response}")

        print(f"""
            Use Case {use_case_topic} Finished.
            
            Total Accumulated Statistics: 
                {accumulator.print_summary_statistics()}
        """)

        statistics_directory_path = "output/stats/"
        create_directory_structure(statistics_directory_path)
        stats_filename = create_snake_case_file_name(f"{use_case_topic}", "json")
        with open(statistics_directory_path + stats_filename, "w", encoding='utf-8') as f:
            f.write(f"{accumulator.print_all_statistics()}")

def main():
    try:
        end_to_end()
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
