# Using Kiro CLI prompts

Kiro CLI supports [chat prompts](https://kiro.dev/docs/cli/chat/manage-prompts/) to allow you to interact with NACL agents.
To get started, make sure your AWS credentials are set up and type `@configure` in the CLI interface to configure your environment.

## Complete Workflow Order

1. `@configure` - Set up your Neptune cluster connection and AWS credentials
2. `@generate-usecase` - Define your use case from a topic sentence
3. `@generate-model` - Create a graph model based on your use case
4. `@generate-data` - Populate your Neptune cluster with realistic test data
5. `@generate-queries` - Generate natural language queries for your use case
6. `@generate-scenario-data` - Create targeted test data for specific query scenarios
7. `@analyze-queries` - Analyze query performance and get optimization recommendations

## Workflow Steps Explained

### Initial Setup
- **@configure**: Configures your Neptune cluster endpoint and validates connectivity

### Core Development
- **@generate-usecase**: Takes a topic sentence and creates detailed use cases with specific questions
- **@generate-model**: Designs a comprehensive graph model for your use case
- **@generate-data**: Creates thousands of nodes and edges with realistic data

### Query Development
- **@generate-queries**: Converts use case questions into valid Neptune queries
- **@generate-scenario-data**: Ensures queries have appropriate test data, generating targeted scenarios if needed
- **COMING SOON** ~~@analyze-queries**~~: Provides query optimization analysis and performance recommendations

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
