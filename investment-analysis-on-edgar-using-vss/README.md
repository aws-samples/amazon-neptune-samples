# Investment Analysis using Vector Similarity in Amazon Neptune Analytics - Demo

This repository contains a practical demo showcasing the concepts discussed in the "Investment Analysis using Vector Similarity in Amazon Neptune Analytics" blog series. The demo covers setting up a data pipeline to convert the raw EDGAR dataset to a Knowledge Graph, running Vector Similarity Search (VSS) combined with graph traversal queries, and provides a frontend for visualization.

## Source of Original Data
The original data can be accessed from the U.S. Securities and Exchange Commission's EDGAR database at SEC's EDGAR Search and Access. Link: https://www.sec.gov/edgar/search-and-access

## Sourcing Descriptions for Holder Vertex
The descriptions for the holder vertex in the knowledge graph are sourced using the BedrockAPI class. This involves generating detailed descriptions for each business entity within the holder vertex using advanced language models on Bedrock. Specifically, the Claude-v2 model is leveraged to provide rich, contextual descriptions that are then added to a new column labeled 'description:string'. These descriptions enrich the knowledge graph, aiding in deep analysis, trend identification, investment decision-making, and the discovery of unique business strategies.

## Creation of Embeddings
Embeddings within the holder vertex are created by the HolderNodeDescriptionEmbedding class. This class enhances the holder vertex by converting textual descriptions in the 'description:string' column into numerical vector form. It utilizes the "all-MiniLM-L6-v2" model from the Sentence Transformer suite to perform this encoding. The process transforms descriptive text into a format suitable for advanced computational analyses, such as similarity comparisons among top investors.

## Raw EDGAR Data Format

The raw data obtained from EDGAR is structured in a tabular format with the following headers:

| S NO. | CIK     | HOLDER                           | FORM  | DATE_FILED | HOLDING        | CLASS | CUSIP     | VALUE   | QTY   | TYPE |
|-------|---------|----------------------------------|-------|------------|----------------|-------|-----------|---------|-------|------|
| 1     | 1007295 | BRIDGES INVESTMENT MANAGEMENT INC | 13F-HR | 20231024   | ABBOTT LABS COM | COM   | 002824100 | 2005279 | 20705 | SH   |

This table includes critical information such as the company's Central Index Key (CIK), the holding company name (HOLDER), the form type (FORM), the date the form was filed (DATE_FILED), the holding details (HOLDING), the class of shares (CLASS), the Committee on Uniform Securities Identification Procedures number (CUSIP), the value of the holding (VALUE), the quantity of shares (QTY), and the type of security (TYPE).

## Preprocessed Knowledge Graph

For your convenience, we have already prepared the processed and ready-to-use EDGAR Knowledge Graph, which is stored at:

`s3://aws-neptune-customer-samples-{REGION-NAME}/sample-datasets/gremlin/edgar/`

## Steps to Spin Up the Demo

### Step 1: Install Dependencies

```bash
pip install -r requirements.txt
```

### Step 2: Create a Graph on Neptune

```bash
aws neptune-graph create-graph --graph-name 'edgar-vss'  --region us-east-1 --provisioned-memory 128 --public-connectivity --replica-count 0 --vector-search '{"dimension": 384}' --region us-east-1

```

Check the status of the graph. Once the graph status is "AVAILABLE," proceed to bulk load the Knowledge Graph with embeddings.

```bash
# check graph create status
aws neptune-graph get-graph --graph-id <GraphId> --region us-east-1
```

### Step 3: Bulk Load the EDGAR Knowledge Graph

```python
from vss_integration import VSSIntegration

source_url = "s3://aws-neptune-customer-samples-{REGION-NAME}/sample-datasets/gremlin/edgar/"
region = "us-east-1"
result = VSSIntegration(region).load_data(source=source_url, region=region)
```

### Step 4: Spin Up the Frontend to View Results in Action

```bash
cd investment-analysis-on-edgar-using-vss/edgar_frontend
streamlit run investment_analysis_interface.py
```

Feel free to explore the demo and visualize the results using the provided frontend.

For any questions or assistance, please refer to the accompanying blog series or contact the repository owner.

## Disclaimer for Data Accuracy
1. Please note that the information presented, particularly regarding the EDGAR dataset, is intended solely for demonstration purposes. The accuracy of the data cannot be guaranteed.
2. The CUSIP is not a foolproof unique identifier. It often represents a family of identifiers under a corporate structure, and the same CUSIP may be associated with different names due to the free-form entry of names in fields.
3. Filers may submit amendments to their filings, either as complete replacements or as singular records. This can lead to significant discrepancies in the aggregation of holdings values, sometimes resulting in figures vastly exceeding the overall stock market.
4. Due to the licensing fees associated with market data such as CUSIPs, some companies may resort to manual entry. This introduces the potential for errors.
5. Addressing these issues requires significant effort to clean and normalize the data. For context, companies like Bloomberg invest substantial resources, including over a hundred staff members, in cleaning and normalizing this data before making it available.

This information is provided to help users understand the limitations and challenges associated with the EDGAR dataset and should be considered when using the data for analysis or decision-making purposes.

