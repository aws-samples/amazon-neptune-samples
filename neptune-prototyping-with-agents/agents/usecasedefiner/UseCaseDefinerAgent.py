from strands import Agent
from agents.baseclass import BaseAgent
import logging
logger = logging.getLogger(__name__)

class UseCaseDefinerAgent(BaseAgent):
    __NAME = "use_case_definer"
    def __init__(self, bedrock_model):
        super().__init__(UseCaseDefinerAgent.__NAME)
        self._agent = Agent(
            name=UseCaseDefinerAgent.__NAME,
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[]
        )

    def _execute_agent(self, **kwargs):
        return self._agent(USER_PROMPT.format(use_case_topic=kwargs["use_case"]))
    
SYSTEM_PROMPT = """
You are an expert on defining graph use cases for a provided topic. You explain to the user:
- a scenario where graphs are an optimal way to solve the problem 
- the types of data that can be used in the use case
- three questions relevant to the use case that the graph can be used to answer

Here is an example of what the output might look like:
<user>
Here is the use case:
Social Media Network Analysis
</user>
<response>
Social media platforms generate vast amounts of interconnected data where users follow each other, share content, join groups, and interact through likes, comments, and shares. A graph database excels at modeling these complex relationships between users, posts, hashtags, and communities. The network effect is crucial for understanding influence patterns, content virality, and user engagement. Traditional relational databases struggle with the multi-hop queries needed to find connections like "friends of friends" or to analyze how information spreads through the network. Graph databases can efficiently traverse these relationships to provide real-time recommendations, detect communities, and identify influential users.
 Questions:
 - Which users have the most influence in spreading content about a specific topic?
 - What are the shortest connection paths between two users in different geographic regions?
 - Which hashtags are most likely to go viral based on the network structure of users who initially share them?
</response>

ALWAYS:
- provide the description for the use case 
- provide the 3 questions 

NEVER: 
- Add commentary to the output
- Add explanations to why you answered that way
"""

USER_PROMPT = """
    Here is the use case:
    {use_case_topic}
    
"""