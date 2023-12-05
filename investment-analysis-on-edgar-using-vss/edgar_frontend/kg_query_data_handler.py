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
from neptune_analytics_integration.vss_integration import VSSIntegration


class QueryHandler:

    def __init__(self) -> None:
        self.graph = VSSIntegration()

    def generate_top_companies_graph(self):
        """
        Fetches and processes data for the top ten companies with the largest holdings,
        returning nodes and edges for a graph.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_top_ten_companies_with_largest_holding()
        data_list = json.loads(query_result)

        nodes = []
        edges = []

        for item in data_list:
            label = item["h.name"]
            hq_id = item["hq_id"]
            
            node = Node(id=hq_id, label=label, size=50)
            nodes.append(node)

        return nodes, edges

    def display_top_companies_table(self):
        """
        Fetches and displays data for the top ten companies with the largest holdings in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_top_ten_companies_with_largest_holding()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"h.name": "Holder Name", "hq_id": "Id", "total_value": "Total Value"})
        st.table(df)


    def display_investor_holdings_table(self):
        """
        Fetches and displays data for investor holdings in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_investor_holdings()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"count(h)": "Count"})
        st.table(df)

    def display_top_investments_table(self):
        """
        Fetches and displays data for the top ten investments in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_top_ten_investments()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"h.name": "Investment", "o.value": "Value"})
        st.table(df)


    def display_high_value_investments_table(self):
        """
        Fetches and displays data for investments greater than one billion in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_investments_greater_than_billion()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"h.name": "Investment", "o.value": "Value"})
        st.table(df)

    def display_shared_high_value_investments_table(self):
        """
        Fetches and displays data for shared investments greater than one billion in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_shared_investments_greater_than_billion()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"name": "Investment Firm", "coholdings": "Holdings"})
        st.table(df)

    def generate_stock_investment_comparison_graph(self):
        """
        Fetches and processes data to generate a graph comparing stocks and investments for the top ten holders.
        """
        graph = VSSIntegration()
        query_result = graph.js_compare_stocks_and_investments_for_top_ten()
        data_list = json.loads(query_result)

        # Use a set to remove duplicates
        unique_data = list({item["coholder.name"]: item for item in data_list}.values())
        grouped_data = {}
        
        for entry in unique_data:
            holder_name = entry['holder.name']
            
            if holder_name not in grouped_data:
                grouped_data[holder_name] = {'holder.name': holder_name, 'coholder.names': [], 'scores': []}
            
            grouped_data[holder_name]['coholder.names'].append(entry['coholder.name'])
            grouped_data[holder_name]['scores'].append(entry['score'])

        # Convert the grouped data back to a list
        result = list(grouped_data.values())
        nodes = []
        edges = []

        for item in result:
            holder_name = item["holder.name"]
            node = Node(id=holder_name, label=holder_name, size=20, color="red")
            nodes.append(node)
            
            for idx, ch_name in enumerate(item["coholder.names"]):
                ch_id = "score:" + str(item["scores"][idx])
                node = Node(id=ch_id, label=ch_name, size=20, color="yellow")
                nodes.append(node)
                edges.append(Edge(source=holder_name, label="coholding", target=ch_id))

        return nodes, edges


    def display_portfolio_comparison_table(self):
        """
        Fetches and displays data for comparing stocks and investments across the entire portfolio in a table.
        """
        graph = VSSIntegration()
        query_result = graph.js_compare_stocks_and_investments_for_entire_portfolio()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"holder.name": "Holder Name", "coholder.name": "Coholder Name", "score": "Score"})
        st.table(df)

    def generate_similar_holders_graph(self):
        """
        Fetches and processes data to generate a graph of the top ten similar holders.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_top_ten_similar_holders()
        data_list = json.loads(query_result)

        # Use a set to remove duplicates
        colors = ["red", "blue", "green", "yellow", "orange", "purple", "pink", "brown", "gray", "cyan"]
        unique_data = list({item["node.name"]: item for item in data_list}.values())    
        nodes = []
        edges = []

        for idx, item in enumerate(unique_data):
            label = item["node.name"]
            node_id = uuid.uuid4()
            node = Node(id=str(node_id), label=label, size=50, color=colors[idx])
            nodes.append(node)

        return nodes, edges


    def display_similar_holders_table(self):
        """
        Fetches and displays data for the top ten similar holders in a table.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_top_ten_similar_holders()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"node.name": "Investment Firm"})
        st.table(df)

    def generate_shared_holders_graph(self):
        """
        Fetches and processes data to generate a graph of shared holders with holdings greater than one billion.
        """
        graph = VSSIntegration()
        query_result = graph.fetch_shared_holders_greater_than_billion()
        data_list = json.loads(query_result)

        nodes = []
        edges = []

        for name_idx, item in enumerate(data_list):
            name = item["name"]
            name_id = uuid.uuid4()
            node = Node(id=(name + str(name_id)), label=name, size=30, color="red")
            nodes.append(node)

            for ch_idx, coholding in enumerate(item["coholdings"]):
                ch_id = uuid.uuid4()
                node = Node(id=(coholding + str(ch_id)), label=coholding, size=30, color="yellow")
                nodes.append(node)
                edges.append( Edge(source=(name + str(name_id)), label="owns", target=(coholding + str(ch_id))))

        return nodes, edges


    def display_competitor_identification_table(self):
        """
        Fetches and displays data on identified competitors in a table.
        """
        graph = VSSIntegration()
        query_result = graph.js_on_identified_competitors()
        data_list = json.loads(query_result)

        df = pd.DataFrame(data_list)
        df = df.rename(columns={"holder.name": "Holder Name", "node.name": "Node Name", "score": "Score"})
        st.table(df)

    def generate_default_graph(self):
        """
        Generates default graph data with nodes and edges.
        """
        nodes = []
        edges = []
        nodes.append(Node(id="Spiderman2", label="Spiderman2", size=50))
        nodes.append(Node(id="Captain_Marvel2", label="Captain Marvel2", size=50))
        edges.append(Edge(source="Captain_Marvel2", label="friend_of", target="Spiderman2"))
        return nodes, edges

