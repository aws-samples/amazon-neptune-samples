from strands import Agent
from agents.baseclass import BaseAgent
import logging
logger = logging.getLogger(__name__)

class GranularNodeGeneratorAgent(BaseAgent):
    __NAME = "granular_node_generator"
    def __init__(self, bedrock_model, neptune_mcp_tools, label):
        super().__init__(f"{GranularNodeGeneratorAgent.__NAME}_{label}")
        self.__label = label
        self._agent = Agent(
            name=f"{GranularNodeGeneratorAgent.__NAME}_{label}",
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[neptune_mcp_tools]
        )

    def _execute_agent(self, **kwargs):
        return self._agent(USER_PROMPT.format(description=kwargs["use_case"],model=kwargs["model"],label=self.__label))

SYSTEM_PROMPT = """
You specialize in generating graph data for a given use case and model. 
You will be assigned a single label and you will generate hundreds of nodes for the graph. 
You will look in the properties section of the schema for the label you were assigned. You will use the list of properties there to generate properties for your data.
First thing you will do is write a query and run it using the run_opencypher_query tool to assess whether any nodes with this label already exist. If they do, then take them into account when determining how many more you need to add. 
Next, you will use the run_opencypher_query tool to add that data into Neptune graph database, sending 20 nodes each time you run it.
Using the use case and model, generate realistic values for each of the nodes given the properties assigned for your label.
Output a summary of the number of nodes you added to Neptune and report if any errors occurred. You do not need to output every statement.

# Data Rules
- If the field is a date or datetime, you must use the `datetime('YY-MM-DDTHH:mm:SSZ')` function. If it is just a date, then still use datetime with the time set to midnight like `datetime('2025-01-01T00:00:00Z')
- Use whitespace to separate statements in the batch. Do not use a semicolon.

# Node Output Format
WITH {!!list_of_properties!!} as properties MERGE (n:!!LABEL!! {`~id`: !!ID!!}) SET n += properties
WITH {!!list_of_properties!!} as properties MERGE (n:!!LABEL!! {`~id`: !!ID!!}) SET n += properties
WITH {!!list_of_properties!!} as properties MERGE (n:!!LABEL!! {`~id`: !!ID!!}) SET n += properties
...
!!LABEL!! is the label of the node specified in the model
!!list_of_properties!! is a JSON formatted list where each property is a key and each value is a realistic value for that property.
!!ID!! is a unique identifier across all of the output with the format `{node_label}_{unique identifier across all nodes}`

<example_input>
# Use Case Description
Social media platforms generate vast amounts of interconnected data where users follow each other, share content, join groups, and interact through likes, comments, and shares. A graph database excels at modeling these complex relationships between users, posts, hashtags, and communities. The network effect is crucial for understanding influence patterns, content virality, and user engagement. Traditional relational databases struggle with the multi-hop queries needed to find connections like "friends of friends" or to analyze how information spreads through the network. Graph databases can efficiently traverse these relationships to provide real-time recommendations, detect communities, and identify influential users.

Questions:
- Which users have the most influence in spreading content about a specific topic?
- What are the shortest connection paths between two users in different geographic regions?
- Which hashtags are most likely to go viral based on the network structure of users who initially share them?

# JSON Formatted Model
{
  "nodes": ["User", "Post", "Hashtag", "Comment", "Region", "Topic"],
  "edges": [
    {
      "label": "FOLLOWS",
      "from_label": "User",
      "to_label": "User"
    },
    {
      "label": "CREATES",
      "from_label": "User",
      "to_label": "Post"
    },
    {
      "label": "CONTAINS",
      "from_label": "Post",
      "to_label": "Hashtag"
    },
    {
      "label": "COMMENTS_ON",
      "from_label": "User",
      "to_label": "Post"
    },
    {
      "label": "LIKES",
      "from_label": "User",
      "to_label": "Post"
    },
    {
      "label": "SHARES",
      "from_label": "User",
      "to_label": "Post"
    },
    {
      "label": "LOCATED_IN",
      "from_label": "User",
      "to_label": "Region"
    },
    {
      "label": "ABOUT",
      "from_label": "Post",
      "to_label": "Topic"
    },
    {
      "label": "RELATED_TO",
      "from_label": "Hashtag",
      "to_label": "Topic"
    }
  ],
  "properties": {
    "User": ["userId", "username", "fullName", "joinDate", "followerCount", "influenceScore"],
    "Post": ["postId", "content", "timestamp", "likeCount", "shareCount", "commentCount", "viralityScore"],
    "Hashtag": ["name", "occurrenceCount", "trendscore"],
    "Comment": ["commentId", "content", "timestamp"],
    "Region": ["name", "country", "population"],
    "Topic": ["name", "popularity", "category"],
    "FOLLOWS": ["sinceDate"],
    "CREATES": ["timestamp"],
    "CONTAINS": ["position"],
    "COMMENTS_ON": ["timestamp", "sentiment"],
    "LIKES": ["timestamp"],
    "SHARES": ["timestamp", "includesComment"],
    "LOCATED_IN": ["sinceDate", "isPrimary"],
    "ABOUT": ["relevanceScore"],
    "RELATED_TO": ["strength"]
  }
}

# Assigned Label
User

</example_input>
Here are just a few examples.  This isn't the exhaustive list that I expect you to generate.  You should have hundreds of nodes added.
<example_statements_added>
WITH {userId: 'user_1', username: 'tjones', fullName: 'Tom Jones', joinDate: datetime('2025-02-03T11:57:00Z'), 'followerCount': 140, influenceScore: 2.5} as properties MERGE (n:User {`~id`: 'User_01'}) SET n += properties
WITH {userId: 'user_2', username: 'djones', fullName: 'Daphne Jones', joinDate: datetime('2025-02-01T14:23:05Z'), 'followerCount': 140, influenceScore: 2.5} as properties MERGE (n:User {`~id`: 'User_02'}) SET n += properties
WITH {userId: 'user_3', username: 'social_butterfly', fullName: 'Social Butterfly', joinDate: datetime('2022-03-15T08:30:00Z'), 'followerCount': 198, influenceScore: 8.7} as properties MERGE (n:User {`~id`: 'User_03'}) SET n += properties
WITH {userId: 'user_4', username: 'tech_guru', fullName: 'Tom the Tech Guru', joinDate: datetime('2021-11-22T14:45:00Z'), 'followerCount': 513, influenceScore: 7.2} as properties MERGE (n:User {`~id`: 'User_04'}) SET n += properties
</example_statements_added>
<example_output>
I successfully created 4 User nodes
</example_output>
"""

USER_PROMPT = """
    # Use Case Description
    {description}
    # JSON Formatted Model
    {model}
    # Assigned Label
    {label}
"""