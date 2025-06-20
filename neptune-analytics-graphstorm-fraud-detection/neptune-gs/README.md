# NeptuneGS: Prepare and process Neptune data for GraphStorm

NeptuneGS (neptune-gs) is a Python package that streamlines the preparation and processing
of Amazon Neptune data for use with GraphStorm, allowing users of Neptune to easily
integrate graph machine learning into their workflow and enrich their graphs with
embeddings and predictions from a Graph Neural Network (GNN) model.

The package provides tools to transform Neptune graph data into GraphStorm's expected
format, handling data type conversions and feature/label annotation, along with edge and
vertex type identification.

After you train a GNN model with GraphStorm and produce graph embeddings and predictions
(optional), this package helps with enriching the original Neptune data with the GraphStorm
columns, allowing easier import of GNN-learned properties back into Neptune.

> NOTE: The package only supports Neptune Analytics.

## Repository Structure
```
neptune-gs/
├── neptune_gs/                  # Core package source code
│   ├── __init__.py              # Package initialization
│   ├── attach_gs_data.py        # Joins Neptune data with GraphStorm embeddings and predictions
│   ├── aws_utils.py             # Utilities for AWS service interactions
│   ├── create_gconstruct.py     # Generates GraphStorm configuration
│   └── fs_handler.py            # Handles local and S3 filesystem operations
├── test/                        # Test files
└── pyproject.toml               # Project dependencies and metadata
```

## Usage Instructions

### Prerequisites
- Python 3.9 or higher
- AWS role with access to:
  - Amazon Neptune Analytics
  - Amazon S3

### Installation
```bash
# Install using pip
pip install .
```

### Create GConstruct config from Neptune Analytics export

You can use a command like the below to create a GConstruct config
for data you have exported from Neptune Analytics:

```bash
create-gconstruct \
    --input-path "s3://your-bucket/neptune-export/you-graph/<export-task-id>" \
    --graph-id "g-51rmvXXXXX"
```

The tool will analyze your exported data and generate a GraphStorm configuration file that includes:
- Node and edge type definitions
- Feature transformations for numerical and categorical data
- Label configurations for graph machine learning tasks

To facilitate the automated discover of labels you can provide a
**task_name, target node/edge type, label column** triplet to `--task-info`,
separated by spaces:

```bash
create-gconstruct \
    --input-path "s3://your-bucket/neptune-export/your-graph/<export-task-id>" \
    --graph-id "g-51rmvXXXXX" \
    --task-info "classification" "Transaction" "is_fraud" # The task is "classification", the target node type is "Transaction", the target column is "is_fraud"
```

### Attach GraphStorm embeddings to Neptune Analytics output

Once you have trained a GraphStorm model on the exported data,
you can use a command like the below to create a new set of files
that match each node id to its corresponding embedding and predictions.

```bash
attach-gs-data \
    -i "s3://your-bucket/neptune-export/your-graph/<export-task-id>" \
    --embeddings "s3://bucket/graphstorm-output/embeddings/" \
    --output "s3://bucket/neptune-data/"
```

To include both embeddings and predictions:

```bash
attach-gs-data \
    -i "s3://your-bucket/neptune-export/your-graph/<export-task-id>" \
    --embeddings "s3://bucket/graphstorm-output/embeddings/" \
    --predictions "s3://bucket/graphstorm-output/predictions/" \
    --output "s3://bucket/neptune-data/"
```
