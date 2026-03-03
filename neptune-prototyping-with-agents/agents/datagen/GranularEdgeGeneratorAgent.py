from strands import Agent
from agents.baseclass import BaseAgent

class GranularEdgeGeneratorAgent(BaseAgent):
    __NAME = "granular_edge_generator"
    def __init__(self, bedrock_model, neptune_mcp_tools, label):
        super().__init__(f"{GranularEdgeGeneratorAgent.__NAME}_{label}")
        self.__label = label
        self._agent = Agent(
            name=f"{GranularEdgeGeneratorAgent.__NAME}_{label}",
            system_prompt=SYSTEM_PROMPT,
            model=bedrock_model,
            callback_handler=None,
            tools=[neptune_mcp_tools]
        )
    def _execute_agent(self, **kwargs):
        return self._agent(USER_PROMPT.format(description=kwargs["use_case"],model=kwargs["model"],label=self.__label))
        
SYSTEM_PROMPT = """
You specialize in generating edge data for a given use case and model. 
You will be assigned a single edge label and you will generate an appropriate number of graph edges given the number of nodes you are connecting, the use case and your knowledge of the subject. 
You will look in the edges section of the schema to find your edge label and locate the "from_label" and "to_label". You can use the Neptune tool to query the graph to see what data exists for those labels.
You will look in the properties section of the schema to find the properties that your edge should have. You will use the list of properties there to generate properties for your data.
First thing you will do is write a query and run it using the Neptune query tool to assess whether any nodes with this label already exist. If they do, then take them into account when determining how many more you need to add. 
Next, you will use the run_opencypher_query tool to add that data into Neptune graph database, sending 20 nodes each time you run it.
Using the use case and model, generate realistic values for each of the edges given the properties assigned for your label.
Output a summary of the number of edges you added to Neptune and report if any errors occurred. You do not need to output every statement.

# Data Rules
- If the field is a date or datetime, you must use the `datetime('YY-MM-DDTHH:mm:SSZ')` function. If it is just a date, then still use datetime with the time set to midnight like `datetime('2025-01-01T00:00:00Z')
- Use whitespace to separate statements in the batch. Do not use a semicolon.

# Edge Format
WITH {!!list_of_properties!!} as properties MATCH (from {`~id`:!!FROM_NODE_ID}), (to {`~id`:!!TO_NODE_ID!!} MERGE (from)-[e:!!LABEL!! {`~id`:!!EDGE_ID!!}]->(to) SET e += properties
!!LABEL!! is the label of the edge specified in the model
!!list_of_properties!! is a JSON formatted list where each property is a key and each value is a realistic value for that property.
!!FROM_NODE_ID!! MUST match the ID of an existing node within the Neptune cluster and having a label matching the `from_label` property from the model. Run a query against the database to find an ID if you need to.
!!TO_NODE_ID!! MUST match the ID of an existing node within the Neptune cluster and having a label matching the `from_label` property from the model. Run a query against the database to find an ID if you need to.
!!EDGE_ID!! is a unique identifier across all of the output with the format `{edge_label}_{sequence of characters unique to all edges with this label}`

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
LIKES

</example_input>
Here are just a few examples.  This isn't the exhaustive list that I expect you to generate.  You should have hundreds of edges added.
<example_workflow>
1. Find that LIKES has a "from_label" of "User" and a "to_label" of "Post".
2. Run the query `MATCH (u:User) RETURN COUNT(u)` to determine there are 500 Users and `MATCH (p:Post) RETURN COUNT(p)` to determine there are 1000 Posts.  You think each user might like 5 posts and decide you want to make 5000 LIKE edges.
3. You write a query to sample some User nodes and some Post nodes and retrieve their IDs (`~id`).  You generate some edges for them and then repeat until you've generated the full number of edges you decided.
</example_workflow>
<example_statements_added>
WITH {timestamp: datetime('2025-03-04T01:02:03Z)} as properties MATCH (from {`~id`:'User_abcd123'},(to {`~id`:'Post_5432a'}) MERGE (from)-[e:LIKES {`~id`:'LIKES_456345`}]->(to) SET e += properties
WITH {timestamp: datetime('2025-01-31T15:08:22Z)} as properties MATCH (from {`~id`:'User_xyz234'},(to {`~id`:'Post_best'}) MERGE (from)-[e:LIKES {`~id`:'LIKES_super123`}]->(to) SET e += properties
</example_statements_added>
<example_output>
I successfully created 2 LIKES edges
"""

USER_PROMPT = """
    # Use Case Description
    {description}
    # JSON Formatted Model
    {model}
    # Assigned Label
    {label}
"""