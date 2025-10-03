# AWS Neptune Data API SDK Examples

This repository contains code examples demonstrating how to use the AWS SDK with the newer NeptuneData APIs for Amazon Neptune across multiple programming languages. These examples showcase different Neptune operations using the data plane APIs that provide direct access to Neptune's graph database capabilities.

## Overview

The examples in this repository demonstrate comprehensive Neptune operations including:
- **Gremlin Query Execution**: Property graph traversals and mutations
- **openCypher Query Execution**: Graph pattern matching and data retrieval
- **Property Graph Summaries**: Metadata and statistics about your graph
- **Bulk Loading Operations**: Starting and monitoring S3-based data loads

Each language implementation shows best practices for:
- Configuring the Neptune Data client with proper endpoints and timeouts
- Handling authentication and AWS SDK configuration
- Processing query results and error handling
- Setting up retry policies and connection management
- Monitoring long-running operations like bulk loads

## Language Examples

Each language implementation provides a comprehensive client wrapper with examples for all major Neptune operations:

### Python (`python/`)
- **Complete Neptune Client**: Full-featured client wrapper using `boto3`
- **Gremlin Queries**: Execute Gremlin traversal queries for graph operations
- **openCypher Queries**: Run openCypher pattern matching queries
- **Property Graph Summary**: Retrieve graph metadata and statistics
- **Bulk Loading**: Start and monitor bulk load jobs from S3

### JavaScript/Node.js (`javascript/`)
- **Modern ES6+ Implementation**: Uses latest `@aws-sdk/client-neptunedata` package
- **Async/Await Patterns**: Asynchronous code with proper error handling
- **Various Neptune Operations**: Gremlin, openCypher, summaries, and bulk loading
- **Flexible Configuration**: Parameterized client constructor for easy reuse

### Go (`go/`)
- **AWS SDK v2**: Uses the latest AWS SDK for Go with proper context handling
- **Comprehensive Examples**: All query types and bulk loading operations

### Java (`java/`)
- **Maven-Based Project**: Complete Maven setup with all required dependencies
- **AWS SDK v2**: Uses modern AWS SDK for Java with builder patterns
- **Jackson Integration**: JSON processing with Jackson for result handling

### .NET (`dotnet/`)
- **Modern .NET 8**: Uses latest .NET features with async/await patterns
- **AWS SDK Integration**: Full integration with AWS SDK for .NET
- **Json**: JSON processing for flexible result handling

## Prerequisites

Before running any examples, ensure you have:
- An active Amazon Neptune cluster
- Proper AWS credentials configured (IAM role, access keys, or AWS CLI profile)
- Network connectivity to your Neptune cluster (VPC configuration, security groups, and/or public endpoints configured)
- The appropriate SDK dependencies installed for your chosen language

## Configuration

Each example requires updating cluster-specific information:
- **Cluster endpoint**: Your Neptune cluster's writer or reader endpoint
- **Port**: Neptune port (typically 8182)
- **Region**: AWS region where your Neptune cluster is deployed

Look for placeholder values like `cluster-name`, `region`, and `port` in the source code that need to be replaced with your actual cluster details.

## Getting Started

1. Choose your preferred programming language from the available folders
2. Navigate to the specific language directory
3. Follow the setup instructions in that folder's README
4. Update the cluster configuration in the example code
5. Run the example following the provided instructions

Each language folder contains its own README with detailed setup and execution instructions specific to that environment.

## Neptune Data APIs

These examples utilize Neptune's data plane APIs which provide:
- Direct query execution without intermediate layers
- Support for both Gremlin and openCypher query languages
- Efficient result streaming and pagination
- Built-in retry and error handling capabilities
- Integration with AWS IAM for authentication and authorization

For more information about Neptune's capabilities and API reference, visit the [Amazon Neptune Developer Guide](https://docs.aws.amazon.com/neptune/latest/userguide/intro.html).