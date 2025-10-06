# JavaScript/Node.js Neptune Data API Examples

Before you run any examples, you need to install the required dependencies:
```bash
npm install
```

## Configuration
Update the cluster configuration in `src/neptune_client.js`:
- `clusterEndpoint`: Your Neptune cluster endpoint (without https:// or port)
- `port`: Neptune port (typically 8182)
- `region`: AWS region where your Neptune cluster is deployed

## Examples
The main example (`src/neptune_client.js`) demonstrates direct SDK usage for:

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
node src/neptune_client.js
```


