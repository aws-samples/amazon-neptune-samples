# Neptune Database GraphStorm Online Inference

This project demonstrates how to set up and use Amazon Neptune with GraphStorm for online inference. The project consists of a CDK infrastructure deployment and a series of Jupyter notebooks that guide you through the process of data preparation, model training, and endpoint deployment.

## Run the Notebooks

The notebooks should be executed in order:

1. `0-Data-Preparation.ipynb`: Prepare the data for training
2. `1-Load-Data-Into-Neptune-DB.ipynb`: Load the prepared data into Neptune
3. `2-Model-Training.ipynb`: Train the GraphStorm model
4. `3-GraphStorm-Endpoint-Deployment.ipynb`: Deploy the trained model
5. `4-Sample-graph-and-invoke-endpoint.ipynb`: Test the deployed endpoint
