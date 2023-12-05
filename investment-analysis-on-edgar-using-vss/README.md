# Investment Analysis using Vector Similarity in Amazon Neptune Analytics - Demo

This repository contains a practical demo showcasing the concepts discussed in the "Investment Analysis using Vector Similarity in Amazon Neptune Analytics" blog series. The demo covers setting up a data pipeline to convert the raw EDGAR dataset to a Knowledge Graph, running Vector Similarity Search (VSS) combined with graph traversal queries, and provides a frontend for visualization.

## Preprocessed Knowledge Graph

For your convenience, we have already prepared the processed and ready-to-use EDGAR Knowledge Graph, which is stored at:

`s3://aws-neptune-customer-samples-us-east-1/sample-datasets/gremlin/edgar/`

## Steps to Spin Up the Demo

### Step 1: Install Dependencies

```bash
pip install -r requirements.txt
```

### Step 2: Create a Graph on Neptune

```bash
aws neptune-graph create-graph --graph-name 'edgar-vss'  --region us-east-1 --provisioned-memory 128 --allow-from-public --replica-count 0 --vector-search '{"dimension": 384}'
```

Check the status of the graph. Once the graph status is "AVAILABLE," proceed to bulk load the Knowledge Graph with embeddings.

```bash
# check graph create status
aws neptune-graph get-graph --graph-id <GraphId> --region us-east-1
```

### Step 3: Bulk Load the EDGAR Knowledge Graph

```python
from vss_integration import VSSIntegration

source_url = "s3://aws-neptune-customer-samples-us-east-1/sample-datasets/gremlin/edgar/"
region = "us-east-1"
result = VSSIntegration().load_data(source=source_url, region=region)
```

### Step 4: Spin Up the Frontend to View Results in Action

```bash
cd investment-analysis-on-edgar-using-vss/edgar_frontend
streamlit run investment_analysis_interface.py
```

Feel free to explore the demo and visualize the results using the provided frontend.

For any questions or assistance, please refer to the accompanying blog series or contact the repository owner.