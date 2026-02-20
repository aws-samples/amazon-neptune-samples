# Model Generator Agent

## Overview
The Model Generator Agent designs comprehensive graph models based on detailed use cases and solution requirements.

## Input
- Detailed use case from Use Case Definer Agent
- Specific solution requirements
- Performance and scalability constraints

## Output
- Complete graph model design
- Node and edge type definitions
- Property specifications
- Relationship mappings

## Usage
Feed the output from the Use Case Definer Agent into this agent along with any specific technical requirements to generate a graph model optimized for your Neptune workload.

## Example
**Input**: Fraud detection use case with transaction monitoring requirements
**Output**: Graph model with Account, Transaction, Merchant nodes and TRANSFERS, OWNS relationships with appropriate properties.

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
