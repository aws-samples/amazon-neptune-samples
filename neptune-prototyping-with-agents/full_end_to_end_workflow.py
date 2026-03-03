import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from agents.usecasedefiner.UseCaseDefinerAgent import UseCaseDefinerAgent
from agents.modelgen.ModelGeneratorAgent import ModelGeneratorAgent
from agents.querygen.BulkQueryGeneratorAgent import BulkQueryGeneratorAgent
from agents.queryrecommender.QueryExplainerAgent import QueryExplainerAgent
from agents.querytuning.QueryTuningAgent import QueryTuningAgent
from agents.datagen.GranularEdgeGeneratorAgent import GranularEdgeGeneratorAgent
from agents.datagen.GranularNodeGeneratorAgent import GranularNodeGeneratorAgent
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
import concurrent.futures
import time


def end_to_end():
    MAX_EVENT_LOOP_RETRIES = 10

    # Load the configuration from a JSON file
    print("===============Loading Configuration================\n")
    config = Config()

    # Set this to a positive number if you get a lot of ThrottlingExceptions from Strands Agent event loop via Bedrock at the start of the job process.
    STAGGER_TIME_SECONDS = 20

    use_case_topic = UseCaseTopics.CYBERSECURITY_THREAT_DETECTION_NETWORK_ANALYSIS.value
    print(use_case_topic)


    print("===============Initializing AWS SDK objects and Tools================\n")
    bedrock_model = get_bedrock_model(model_id=config.get_llm_config()["model"], region_name=config.get_llm_config()["region"])

    neptune_tools = NeptuneTools(connection_url=config.get_neptune_config()["writer_endpoint"])

    initial_statistics_timestamp = neptune_tools.last_statistics_update_time_tool() # type: ignore
    print(f"Initial Neptune statistics timestamp is {initial_statistics_timestamp}")
    neptune_mcp_client = neptune_tools.create_neptune_mcp_client()

    accumulator = StatisticsAccumulator()

    #### DEFINE TASK FUNCTIONS
    def node_agent_function(use_case, model, label, accumulator):
        agent = GranularNodeGeneratorAgent(
            bedrock_model = bedrock_model,
            neptune_mcp_tools = neptune_mcp_client.list_tools_sync(),
            label=label
        )
        output = agent.execute_task(use_case=use_case, model=model)
        statistics = agent.get_metrics_summary()
        record_outputs("datagen", f"{use_case}_{label}", output, statistics)
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
        statistics = agent.get_metrics_summary()
        record_outputs("datagen", f"{use_case}_{label}", output, statistics)
        accumulator.accumulate(f"data-gen-edge:{label}", agent.get_statistics())
        return f"""
            Output for edge label {label}:
            
            Output:
            {output if output is not None else "N/A"}
        """

    ### END TASK FUNCTIONS

    with neptune_mcp_client:

        # The first agent takes in the topic sentence of the use case topic and generates a detailed description of the use case 
        # along with 3 questions the model should answer.
        use_case_agent = UseCaseDefinerAgent(
            bedrock_model = bedrock_model,
        )
        use_case_response = use_case_agent.execute_task(use_case=use_case_topic)
        use_case_statistics = use_case_agent.get_metrics_summary()
        record_outputs("usecasedefiner", use_case_topic, f"<use_case>\n{use_case_topic}\n</use_case><response>\n{use_case_response}\n</response>",use_case_statistics)
        accumulator.accumulate("use_case_agent", use_case_agent.get_statistics())
        print(f"Use Case: {use_case_topic}\n\nResponse:\n{use_case_response}")

        # Optionally you can modify the use case and/or questions from the previous step before moving forward.

        # The next agent takes the use case and generates a graph model of nodes, edges, and properties for each.
        model_gen_agent = ModelGeneratorAgent(
            bedrock_model = bedrock_model,
        )
        model_response = model_gen_agent.execute_task(use_case=use_case_response)
        model_statistics = model_gen_agent.get_metrics_summary()
        record_outputs("modelgen", use_case_topic, model_response,model_statistics)
        accumulator.accumulate("model_gen_agent", model_gen_agent.get_statistics())
        print(f"Model: {model_response}")

        # The output of the model *should* but just JSON describing the model.  
        # If the LLM added text or markup to it, you will have to remove it before continuing.
        model_json = None
        try:
            model_json = json.loads(model_response)
        except json.JSONDecodeError as jde:
            # If there is extra junk in the LLM response, try removing it 
            print("JSON output from the model is not valid. This usually means it added markup or text. We will try to remove it...")
            try:
                model_json = extract_json(model_response, schema_name="model")
                print("It appears we were successful...")
            except json.JSONDecodeError as jde2:
                print("It appears we were unsuccessful. Raising up the original exception.")
                raise jde

        # For each node and edge label in that model, we are going to assign a worker in a thread pool to run an agent that
        # will generate realistic data specifically for that label and insert it directly into Neptune

        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            node_futures = []
            edge_futures = []
            for label in model_json['nodes']:
                node_futures.append(executor.submit(node_agent_function, use_case_topic, model_response, label, accumulator))
                if STAGGER_TIME_SECONDS > 0:
                    # Stagger the starting time of each thread to reduce the likelihood of LLM call throttling.
                    # nosemgraph: python.lang.best-practice.arbitrary-sleep
                    time.sleep(STAGGER_TIME_SECONDS)  

            for task in node_futures:
                result = task.result()
                print(f"Task Finished!\nResult:\n{result}")

            for edge in model_json['edges']:
                edge_futures.append(executor.submit(edge_agent_function, use_case_topic, model_response, edge["label"], accumulator))

            for task in edge_futures:
                result = task.result()
                print(f"Task Finished!\nResult:\n{result}")

        # Now we need to wait until the Neptune statistics process finishes so the graph schema it returns will be accurate
        current_statistics_timestamp = neptune_tools.last_statistics_update_time_tool() # type: ignore
        print(f"Current Neptune statistics timestamp is {initial_statistics_timestamp}.")

    while (current_statistics_timestamp == initial_statistics_timestamp):
        print(f"Statistics process has not completed as the initial statistics timestamp still matches. Sleeping for 30 seconds before checking again. {initial_statistics_timestamp}")
        # nosemgraph: python.lang.best-practice.arbitrary-sleep
        time.sleep(30) 
        current_statistics_timestamp = neptune_tools.last_statistics_update_time_tool() # type: ignore

    # Because of https://github.com/awslabs/mcp/issues/1580 with the Neptune MCP Server, we need to start a new instance of the MCP server so that it will refresh the graph schema in its cache.
    with neptune_mcp_client:
        # The next agent will take the 3 questions from the model and generate a query for each.  It will inspect the Neptune database
        # to understand the model implementation and ensure the query uses the proper labels and properties, and executes successfully.
        query_gen_agent = BulkQueryGeneratorAgent(
            bedrock_model = bedrock_model,
            mcp_client = neptune_mcp_client.list_tools_sync()
        )
        queries_response = query_gen_agent.execute_task(use_case = use_case_response)
        queries_statistics = query_gen_agent.get_metrics_summary()
        record_outputs("queries", use_case_topic, queries_response, queries_statistics)
        accumulator.accumulate("query_gen_agent", query_gen_agent.get_statistics())
        print(f"Queries: {queries_response}")

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
