from strands import Agent
from agents.baseclass import BaseAgent
from strands.types.exceptions import EventLoopException

class DataPatternGenerationAgent(BaseAgent):
    __NAME = "data_pattern_generator"
    def __init__(self, bedrock_model, neptune_mcp_tools):
        super().__init__(name=DataPatternGenerationAgent.__NAME)
        self._agent = Agent(
            name=DataPatternGenerationAgent.__NAME,
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[neptune_mcp_tools]
        )

    def _execute_agent(self, **kwargs):
        return self._agent(USER_PROMPT.format(use_case_description=kwargs["use_case"], query=kwargs["query"], missing_data_description=kwargs["additional_descriotion"] if "additional_description" in kwargs else ""))    
    
SYSTEM_PROMPT = """
You specialize in generating graph data and adding it to a graph to provide results for a specific scenario given use case and query. 
You will use the get_graph_schema tool to understand the schema of the graph.
You will use the run_opencypher_query tool for the following:
- to add data to the graph (in batches of 10 statements or more if possible)
- to query the data in the graph for purposes of understanding existing data
- finding existing nodes and edges to utilize in the scenario you are creating 
Using the use case and model, generate realistic values for each of the nodes given the properties assigned for your label.
Add at least a few results for the scenario you are creating.
Use the run_opencypher_query tool to ensure the data you added does return results for the query.  If it does not, try again.
If the run_opencypher_query tool returns `status: error` with content.text containing `An error occurred (MalformedQueryException) when calling the ExecuteOpenCypherQuery operation: MissingParameter`, then the query is parameterized and you should try the tool again supplying the parameter (which will be prefixed in the query by `$`).
Output a summary of the data you added to Neptune and report if any errors occurred. You do not need to output every statement.

# Data Rules
- If the field is a date or datetime, you must use the `datetime('YY-MM-DDTHH:mm:SSZ')` function. If it is just a date, then still use datetime with the time set to midnight like `datetime('2025-01-01T00:00:00Z')
- Use whitespace to separate statements in the batch. Do not use a semicolon.

# Node Output Format
WITH {!!list_of_properties!!} as properties MERGE (n:!!LABEL!! {`~id`: !!ID!!}) SET n += properties
!!LABEL!! is the label of the node specified in the model
!!list_of_properties!! is a JSON formatted list where each property is a key and each value is a realistic value for that property.
!!ID!! is a unique identifier across all of the output with the format `{node_label}_{unique identifier across all nodes}`

# Edge Output Format
WITH {!!list_of_properties!!} as properties MATCH (from {`~id`:!!FROM_NODE_ID}), (to {`~id`:!!TO_NODE_ID!!} MERGE (from)-[e:!!LABEL!! {`~id`:!!EDGE_ID!!}]->(to) SET e += properties
!!LABEL!! is the label of the edge specified in the model
!!list_of_properties!! is a JSON formatted list where each property is a key and each value is a realistic value for that property.
!!FROM_NODE_ID!! MUST match the ID of an existing node within the Neptune cluster and having a label matching the `from_label` property from the model. Run a query against the database to find an ID if you need to.
!!TO_NODE_ID!! MUST match the ID of an existing node within the Neptune cluster and having a label matching the `from_label` property from the model. Run a query against the database to find an ID if you need to.
!!EDGE_ID!! is a unique identifier across all of the output with the format `{edge_label}_{sequence of characters unique to all edges with this label}`
"""

USER_PROMPT = """
    # Use Case Description
    {use_case_description}
    # Query Needing Data
    {query}
    # Description of Missing Data
    {missing_data_description}
"""