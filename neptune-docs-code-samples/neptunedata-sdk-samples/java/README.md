# Java Neptune Data API Examples

## Prerequisites
Ensure you have Java 11+ and Maven installed.

Install dependencies:
```bash
mvn clean compile
```

## Configuration
Update the cluster configuration in `src/main/java/com/amazonaws/neptune/NeptuneDataClient.java`:
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
mvn exec:java
```

Or compile and run directly:
```bash
mvn clean compile
java -cp target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) com.amazonaws.neptune.NeptuneDataClient
```