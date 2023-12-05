# Import necessary libraries and modules
import streamlit as st
import numpy as np
import sys
import os

# Add the project root to sys.path
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
sys.path.append(project_root)

import json
import uuid
import pandas as pd
from streamlit_agraph import agraph, Node, Edge, Config
from kg_query_data_handler import QueryHandler
from knowledge_graph_updater import KnowledgeGraphUpdater

# Set the title for the Streamlit app
st.markdown("<h1 style='text-align: center;'>Investment Analysis Using VSS in Neptune Analytics</h1>", unsafe_allow_html=True)

# Create instances of QueryHandler and KnowledgeGraphUpdater
query_handler = QueryHandler()
knowledge_graph_updater = KnowledgeGraphUpdater(query_handler)

# Show user table header
colms = st.columns((2, 30, 4))
fields = ["№", 'Query', 'Action']
for col, field_name in zip(colms, fields):
    col.write(field_name)

# Define queries and their corresponding descriptions
queries = ['Find the Top 10 Companies with the Largest Holdings',
           'Find the Number of Holdings by the Top Investor',
           'Find the Top 10 Investments of the Top Investor',
           'Find Investments with >$1B USD by the Top Investor',
           'Find shared investments exceeding $1 billion USD among our top investor and their competitors',
           'Jaccard Similarity for Investment Portfolios: compare all stocks and investments of top 10',
           'Jaccard Similarity for Investment Portfolios: extend our analysis to the entire dataset, we explore how all investors compare, not just the top ones',
           'VSS + Traversals: Identify the top 10 most similar Holder nodes to our key investor',
           'VSS + Traversals: Compute the number of shared holdings exceeding $1 billion USD among the identified vector-based competitors',
           'VSS + Traversals: Calculate the Jaccard Similarity for portfolios of the identified competitors']

# Define descriptions for the corresponding queries
complete_queries = ['Print Query 1',
                    'Print Query 2',
                    'Print Query 3',
                    'Print Query 4',
                    'Print Query 5',
                    'Print Query 6',
                    'Print Query 7',
                    'Print Query 8',
                    'Print Query 9',
                    'Print Query 10']

# Iterate through queries
for i in range(0, len(queries)):
    col1, col2, col3 = st.columns((2, 30, 4))
    col1.write(i + 1)  # Display index
    col2.write(queries[i])  # Display query
    button_type = "Run"
    button_phold = col3.empty()
    do_action = button_phold.button(button_type, key=i)
    
    # Check if the "Run" button is pressed
    if do_action:
        # Process the row's data by updating the knowledge graph
        knowledge_graph_updater.update_knowledge_graph(queries[i])
        st.write(f"Running: {complete_queries[i]}")  # Display running message
