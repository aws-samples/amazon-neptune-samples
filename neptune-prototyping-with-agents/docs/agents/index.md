# Agents

## Available Agents

### Use Case Definer Agent
- **Purpose**: Converts high-level descriptions into detailed use cases
- **Input**: Topic sentence or short workload description
- **Output**: Detailed use case with specific answerable questions
- [Detailed Documentation](usecase_definer.md)

### Model Generator Agent
- **Purpose**: Creates graph models from use cases
- **Input**: Detailed use case and requirements
- **Output**: Complete graph model design
- [Detailed Documentation](model_generator.md)

### Data Generation Agents
- **Purpose**: Populates Neptune with realistic test data
- **Input**: Graph model
- **Output**: Nodes and edges with realistic data
- [Detailed Documentation](data_generator.md)

### Query Generation Agent
- **Purpose**: Converts natural language to graph queries
- **Input**: Natural language question
- **Output**: Valid graph query
- [Detailed Documentation](query_generator.md)

### Data Pattern Generation Agent
- **Purpose**: Creates specific test scenarios
- **Input**: Use case and query requirements
- **Output**: Targeted test data
- [Detailed Documentation](data_pattern_generator.md)

### Query Explainer Agent
- **Purpose**: Analyzes query performance
- **Input**: Graph query
- **Output**: Explain plan with recommendations
- [Detailed Documentation](query_explainer.md)

### Query Optimization Agent
- **Purpose**: Improves query performance
- **Input**: Query and optimization requirements
- **Output**: Optimized query variations with performance analysis
- [Detailed Documentation](query_optimizer.md)

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
