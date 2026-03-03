# NACL Workflows

## Standard Workflows

### Complete End-to-End Graph Solution Development
1. **Define use case** with Use Case Definer Agent
2. **Generate graph model** with Model Generator Agent
3. **Create test data** with Data Generation Agents
4. **Generate queries** with Query Generation Agent
5. **Create scenario data** with Data Pattern Generation Agent (if needed)
6. **Analyze and optimize queries** with Query Explainer and Optimization Agents

### Query Development and Optimization Workflow
1. Generate initial queries with Query Generation Agent
2. Validate queries have appropriate test data with Data Pattern Generation Agent
3. Analyze performance with Query Explainer Agent
4. Optimize queries with Query Optimization Agent
5. Validate optimized results with test data

### Data Pattern Testing Workflow
1. Define test scenario requirements
2. Generate specific test patterns with Data Pattern Generation Agent
3. Validate queries against test patterns
4. Analyze and optimize performance

## Kiro CLI Workflow

The complete workflow using Kiro CLI prompts:

1. `@configure` - Set up Neptune connection
2. `@generate-usecase` - Create detailed use case from topic
3. `@generate-model` - Design graph model
4. `@generate-data` - Populate with realistic data
5. `@generate-queries` - Create queries for use case questions
6. `@generate-scenario-data` - Ensure queries have test data
7. **COMING SOON** ~~`@analyze-queries`~~ - Optimize query performance

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
