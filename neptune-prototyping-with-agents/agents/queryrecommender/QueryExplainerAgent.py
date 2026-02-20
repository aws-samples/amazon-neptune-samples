from strands import Agent
from agents.baseclass import BaseAgent
from strands_tools import http_request
from strands.types.exceptions import EventLoopException

class QueryExplainerAgent(BaseAgent):
    __NAME = "query_explainer"
    def __init__(self, bedrock_model, explain_tool):
        super().__init__(name=QueryExplainerAgent.__NAME)
        self._agent = Agent(
            name=QueryExplainerAgent.__NAME,
            system_prompt=QUERY_OPTIMIZER_SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[explain_tool, http_request]
        )

    def _execute_agent(self, **kwargs):
        return self._agent(QUERY_OPTIMIZER_USER_PROMPT.format(query=kwargs["query_text"]))

QUERY_OPTIMIZER_SYSTEM_PROMPT = """
Amazon Neptune is a graph database. You are an expert in understanding explain plans produced by Amazon Neptune and identifying what the query achieves and if there are any areas to investigate for optimizing the query. 
The text in the documentation tags is important to understand explain plans:
<documentation>
Use the HTTP Request tool to retrieve and understand the DFE Operators in the Explain plan from https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-opencypher-explain.html#access-graph-opencypher-dfe-operators
Use the HTTP Request tool to retrieve and understand the explain plan columns from https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-opencypher-explain.html#access-graph-opencypher-explain-columns
Use the HTTP Request tool to retrieve and understand the basic example of understanding explain output from https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-opencypher-explain.html#access-graph-opencypher-explain-basic-example
</documentation>

You will use the query expain plan tool to pass in the query provided to the Neptune database and return back the explain plan.
You will summarize the provided explain plan as a series of steps that are being performed by Neptune.  These steps will be listed in order.  Order is determined using these rules:
- Start with the table titled subQuery1
- Each table starts at "ID" 0. The next step in order is listed in the column "Out #1". If there is a number in "Out #2" as well, this represents a parallel execution path.  This row is the input into the row with both of those IDs.
- If the Arguments value for the row says "subQuery=", insert the table containing that same header in this location. The ID column only has the context of the current table, so start at "ID" 0 and when you see the step DFEDrain, that functions as the output of this step.
- The "Units Out" column and "Time (ms)" column are important for each step.  If the Units Out is much higher than most of the steps, or Time (ms) is much higher than most, then point this out in your summary for the step. 
- Identify the steps with many more "Units Out" or much longer "Time (ms)" than most.  Suggest ways you might modify the query to reduce the number of units out or shorten the time taken. 

** IMPORTANT **
When providing suggestions to optimize the query, under no circumstances should you suggest any of the following:

1. creating custom indexes
2. using APOC functions.
3. functions that do not exist as part of the openCypher specification
4. use of shortestPath, allShortestPath, ALL, NONE or ANY functions
5. use of bi-directional search patterns
6. use of Neptune ML functions or query

"""

QUERY_OPTIMIZER_USER_PROMPT = """
    Here is the query to analyze:
    {query}
    
"""