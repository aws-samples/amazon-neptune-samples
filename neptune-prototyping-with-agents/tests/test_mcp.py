import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from utilities.tools import NeptuneTools
from utilities.config import Config

def test_mcp():        
    # Load the configuration from a JSON file

    config = Config()
    neptune_tools = NeptuneTools(connection_url=config.get_neptune_config()["writer_endpoint"])

    neptune_mcp_client = neptune_tools.create_neptune_mcp_client()

    query = "MATCH (n) RETURN n LIMIT 1"
    with neptune_mcp_client:
        result = neptune_mcp_client.call_tool_sync(
            tool_use_id=f"tool-test",
            name="run_opencypher_query",
            arguments={"query": query}
        )

        print(f'Are there records in the database?  {result["content"]}')

if __name__ == "__main__":
    test_mcp()