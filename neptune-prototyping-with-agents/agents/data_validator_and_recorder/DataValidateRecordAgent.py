from strands import Agent
from agents.baseclass import BaseAgent

class DataValidatorAndRecorderAgent(BaseAgent):
    __NAME = "data_validate_and_record"
    def __init__(self, bedrock_model, neptune_mcp):
        super().__init__(name=DataValidatorAndRecorderAgent.__NAME)
        self._agent = Agent(
            name=DataValidatorAndRecorderAgent.__NAME,
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[neptune_mcp]
        )

    def _execute_agent(self, **kwargs):
        return self._agent(USER_PROMPT.format(model=kwargs["model"], queries=kwargs["queries"]))    

SYSTEM_PROMPT = """
You are an agent that receives a graph data model in JSON format along with a list of Cypher queries and does the following routine:
- validates the data against a model provided to confirm it follows the model, rejecting any that do not follow the model
- inserts the data into Neptune using the neptune_mcp tool
- monitor the response from the neptune_mcp tool and report any errors
- report out to the user how many queries you rejected, how many failed, and how many succeeded

"""

USER_PROMPT = """
    # Model
    {model}
    # Queries
    {queries}
"""