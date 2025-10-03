# .NET Neptune Data API Examples

## Prerequisites
Ensure you have .NET 8.0+ installed.

Restore dependencies:
```bash
dotnet restore
```

## Configuration
Update the cluster configuration in `NeptuneDataClient.cs`:
- `clusterEndpoint`: Your Neptune cluster endpoint (without https:// or port)
- `port`: Neptune port (typically 8182)
- `region`: AWS region where your Neptune cluster is deployed

## Examples
The main example demonstrates direct SDK usage for:

### 1. Execute Gremlin Query
Demonstrates adding a vertex with properties using Gremlin traversal language.

### 2. Execute openCypher Query
Shows how to run openCypher pattern matching queries.

### 3. Get Property Graph Summary
Retrieves metadata and summary information about your Neptune graph.

### 4. Bulk Loader Operations
Shows how to start bulk load jobs and monitor their completion using direct API calls (requires S3 data and IAM role configuration).

## Running the Examples
```bash
dotnet run
```

Or build and run:
```bash
dotnet build
dotnet run --no-build
```