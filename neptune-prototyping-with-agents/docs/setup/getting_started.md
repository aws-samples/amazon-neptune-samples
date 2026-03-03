# Getting Started with Neptune agent samples

## Background Knowledge

This sample uses several AWS services and technologies. Here's a brief overview:

- **[Amazon Neptune](https://docs.aws.amazon.com/neptune/latest/userguide/intro.html)** - A fully managed graph database service that supports both property graph (Gremlin) and RDF (SPARQL) query languages. Neptune stores data as nodes and edges, making it ideal for highly connected datasets.

- **[Amazon Bedrock](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html)** - A fully managed service that provides access to foundation models from leading AI companies through a unified API. This sample uses Claude models for natural language understanding and generation.

- **[AWS IAM](https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html)** - AWS Identity and Access Management controls access to AWS resources through policies, roles, and permissions.

- **[Model Context Protocol (MCP)](https://modelcontextprotocol.io/)** - An open protocol that standardizes how applications provide context to LLMs. The Neptune MCP Server enables AI agents to interact with Neptune databases.

- **Graph Databases** - Unlike relational databases, graph databases store relationships as first-class entities, enabling efficient traversal of connected data. Learn more about [graph database concepts](https://docs.aws.amazon.com/neptune/latest/userguide/feature-overview-data-model.html).

## Prerequisites

1. AWS Account Access
   - Ensure you have appropriate AWS account access
   - Configure AWS credentials
   - See [IAM Permissions](iam_permissions.md) for required permissions

2. Neptune Cluster
   - Running Amazon Neptune cluster
   - Network access to the cluster
   - Appropriate IAM permissions configured

3. uv Package Manager
   - Install from https://docs.astral.sh/uv/
   - Used to automatically run Neptune MCP Server via `uvx`
   - No separate MCP server installation required

## Installation

1. Clone the repository locally

2. Create virtual environment:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install --upgrade pip
   pip install -r requirements.txt
   ```

4. Configure environment:
   ```bash
   cp .env.example .env
   # Edit .env and set your values:
   # - NEPTUNE_WRITER_ENDPOINT: Your Neptune cluster endpoint
   # - LLM_MODEL: Your Bedrock model ID
   # - AWS_REGION: Your AWS region
   ```

## Configuration

1. Configure AWS credentials:
   ```bash
   aws configure
   ```

## Verification

1. Run basic test:
   ```bash
   python -m pytest tests/test_mcp.py
   ```

2. Try a simple workflow:
   ```python
   from step_workflows.generate_usecase_from_topic import generate_usecase
   
   result = generate_usecase("knowledge graph for customer support")
   print(result)
   ```

## Next Steps

1. Review the [Agents Documentation](../agents/index.md)
2. Use [Kiro CLI prompts](../kiro/index.md) to interact with the agents.
3. Try example [Workflows](../workflows/index.md)
4. Check [Known Issues](../KNOWN_ISSUES.md)

---

## Legal

**Copyright © 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.**

**License:** MIT-0 (MIT No Attribution)

This sample code is made available under the MIT-0 license. See the [LICENSE](../../LICENSE) file for details.

**Terms of Use:**
- This is sample code for demonstration and educational purposes
- Not intended for production use without proper review and testing
- No warranties or guarantees provided
- Users are responsible for testing, validating, and adapting the code for their use cases
- AWS service usage will incur costs according to AWS pricing
