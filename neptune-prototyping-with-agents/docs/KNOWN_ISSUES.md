# Known Issues
- The data model often generates summary properties (e.g., number of friends, number of transactions) that can be also answered via a graph query and the results do not match.  (e.g., User.numFollowers == 50 but COUNT((Person)-[HasFollower]->(User)) == 5)
- Std Err is filled with messages like `[10/24/25 08:28:03] INFO     Processing request of type CallToolRequest  server.py:674`.  This is caused by the Neptune MCP Server (see: https://github.com/awslabs/mcp/blob/69aeccbf05964b8e3812f6fcbf6fd0c7b491d134/src/amazon-neptune-mcp-server/awslabs/amazon_neptune_mcp_server/server.py#L28).
- The scenario data generation workflow (`generate_scenario_data.py`) does not handle parameterized queries. Queries with parameters (e.g., `$userId`, `$topicId`) will return `MissingParameter` errors during validation. The workflow now detects these errors but does not attempt to substitute sample parameter values for testing.
---

## Legal

**Copyright © 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.**

**License:** MIT-0 (MIT No Attribution)

This sample code is made available under the MIT-0 license. See the [LICENSE](../LICENSE) file for details.

**Terms of Use:**
- This is sample code for demonstration and educational purposes
- Not intended for production use without proper review and testing
- No warranties or guarantees provided
- Users are responsible for testing, validating, and adapting the code for their use cases
- AWS service usage will incur costs according to AWS pricing
