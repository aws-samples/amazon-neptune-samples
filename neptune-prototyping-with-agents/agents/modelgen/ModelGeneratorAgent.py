from strands import Agent
from agents.baseclass import BaseAgent
import logging
logger = logging.getLogger(__name__)

class ModelGeneratorAgent(BaseAgent):
    __NAME = "model_generator"
    def __init__(self, bedrock_model):
        super().__init__(ModelGeneratorAgent.__NAME)
        self._agent = Agent(
            name=ModelGeneratorAgent.__NAME,
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[]
        )

    def _execute_agent(self, **kwargs):

        return self._agent(USER_PROMPT.format(description=kwargs["use_case"]))

SYSTEM_PROMPT = """
`Instructions: The customer will send you a description of the problem they want to solve using a graph and a series of questions they want the graph to answer. 
Using the description and the questions, design a graph data model that should be able to answer their questions. 
Definitions: Reification -- the process of making abstract concepts (like statements or relationships) into concrete, identifiable objects within a graph model. It essentially gives these abstract concepts a "physical" presence in the graph, allowing them to be queried and manipulated like any other entity. Output: Three sections each encapsulated in XML tags. 

Model: 
The model shall contain: 
- a list of node labels
- a list of edge labels with each showing the types of nodes they connect as a "from label" and a "to label"
- a list of properties for each label (nodes and edges). Properties have a data type of string, integer, float, datetime, or boolean.
Important Rules that must be followed: 
- edges cannot have edges. These are refered to as hypergraphs and are not permitted. Use reification to create a node for an intermediary conceptual object and connect the edges to that conceptual object.  Example: modeling "Person buys Item from Store" should create an intermediate node labeled "Purchase" because you cannot add an edge from another edge Buys to Store. 

# OUTPUT REQUIREMENTS
A JSON formatted representation of the model. 
You MUST use this exact structure:
{
  "nodes": ["NodeLabel1", "NodeLabel2"],
  "edges": [
    {
      "label": "EDGE_LABEL",
      "from_label": "SourceNode",
      "to_label": "TargetNode"
    }
  ],
  "properties": {
    "NodeLabel1": ["property1", "property2"],
    "NodeLabel2": ["property3", "property4"],
    "EDGE_LABEL": ["edgeProperty1", "edgeProperty2"]
  }
}

# DO NOT
- Add any commentary or Markdown.
"""

USER_PROMPT = """
    Here is the description:
    {description}
    
"""