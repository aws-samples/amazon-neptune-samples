from utilities.neptune import Neptune
from strands import tool
import os
from strands.tools.mcp import MCPClient
from mcp.client.stdio import StdioServerParameters, stdio_client
import logging
logger = logging.getLogger(__name__)

class NeptuneTools:
    def __init__(self, connection_url):
        self.__connection_url = connection_url
        self.__neptune = Neptune(connection_url=connection_url)

    @tool
    def query_explain_tool(self, query: str) -> str:
        """
        Executes a Cypher query in Neptune to retrieve an explain plan

        Parameters
        ----------
        query : string
            A Cypher query to run against a Neptune graph

        Returns
        -------

        string
            The query explain plan describing its step by step performance
        
        
        """
        raw_explain = self.__neptune.run_open_cypher_explain_query(query,'detailed')
    #            logger.debug(f"query_explain_tool output -> {raw_explain}")
        if not "result" in raw_explain:
            return "No Result Returned from the tool"
        else:
            return raw_explain["result"]

    @tool
    def last_statistics_update_time_tool(self):
        """
        Retrieves the last time the statistics process completed on a Neptune cluster


        Returns
        -------

        datetime
            The timestamp of the last time the statistics process completed
        
        
        """
        response = self.__neptune.get_statistics_last_update_time()
        return response

    def create_neptune_mcp_client(self):
        """Create Neptune MCP client using stdio transport with validated credentials."""
        # Validate and sanitize Neptune endpoint
        if not self.__connection_url:
            raise ValueError("Neptune connection URL is required")
        
        # Sanitize endpoint - remove any protocol prefix if present
        connection_url = self.__connection_url.replace("https://", "").replace("http://", "")
        
        # Validate endpoint format (basic check)
        if not connection_url or ".." in connection_url or connection_url.startswith("/"):
            raise ValueError(f"Invalid Neptune endpoint format: {connection_url}")
        
        # Base environment variables
        env_vars = {
            "FASTMCP_LOG_LEVEL": "WARN",
            "NEPTUNE_ENDPOINT": f"neptune-db://{connection_url}",
            "AWS_REGION": os.getenv("AWS_REGION", "us-east-1")
        }
        
        # Validate AWS_REGION
        valid_regions = [
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "eu-west-1", "eu-west-2", "eu-west-3", "eu-central-1",
            "ap-northeast-1", "ap-northeast-2", "ap-southeast-1", "ap-southeast-2",
            "ap-south-1", "sa-east-1", "ca-central-1"
        ]
        if env_vars["AWS_REGION"] not in valid_regions:
            raise ValueError(f"Invalid AWS region: {env_vars['AWS_REGION']}")
        
        # Pass AWS credentials to MCP server subprocess
        # Check for session credentials first (temporary credentials)
        if "AWS_ACCESS_KEY_ID" in os.environ:
            env_vars["AWS_ACCESS_KEY_ID"] = os.environ["AWS_ACCESS_KEY_ID"]
        if "AWS_SECRET_ACCESS_KEY" in os.environ:
            env_vars["AWS_SECRET_ACCESS_KEY"] = os.environ["AWS_SECRET_ACCESS_KEY"]
        if "AWS_SESSION_TOKEN" in os.environ:
            env_vars["AWS_SESSION_TOKEN"] = os.environ["AWS_SESSION_TOKEN"]
        
        # Only pass AWS_PROFILE if explicitly set and no access keys present
        if "AWS_PROFILE" in os.environ and "AWS_ACCESS_KEY_ID" not in env_vars:
            profile = os.environ["AWS_PROFILE"]
            # Validate profile name - alphanumeric, hyphens, underscores only
            if profile and profile.replace("-", "").replace("_", "").isalnum():
                env_vars["AWS_PROFILE"] = profile
            else:
                raise ValueError(f"Invalid AWS_PROFILE format: {profile}")
        
        server_params = StdioServerParameters(
            command="uvx",
            args=["awslabs.amazon-neptune-mcp-server@latest"],
            env=env_vars
        )
        
        transport = lambda: stdio_client(server_params)
        return MCPClient(transport)
    
    @tool
    def validate_query_results_tool(self, original_query: str, alternative_query: str) -> dict:
        """
        Tool that executes both queries and validates their results match exactly.
        Returns:
        {
            "is_valid": bool,
            "error": str          # Only if is_valid=False
        }
        """
        def sort_nested(obj):
            if isinstance(obj, dict):
                return tuple(sorted((k, sort_nested(v)) for k, v in obj.items()))
            elif isinstance(obj, list):
                return tuple(sorted(sort_nested(item) for item in obj))
            else:
                return obj


        # Execute original query
        orig_results = self.__neptune.run_open_cypher_query(original_query)['results']

        # Execute alternative query
        alt_results = self.__neptune.run_open_cypher_query(alternative_query)['results']

        # Compare results
        try:
            
            orig_set = {sort_nested(r) for r in orig_results}
            alt_set = {sort_nested(r) for r in alt_results}
            is_valid = orig_set == alt_set
        except:
            is_valid = False

        if not is_valid:
            return {
                "is_valid": False,
                "error": "Results do not match original query"
            }

        return {
            "is_valid": True
        }