import streamlit as st
from streamlit_agraph import agraph, Node, Edge, Config

class KnowledgeGraphUpdater:
    def __init__(self, query_handler):
        self.query_handler = query_handler

    def update_knowledge_graph(self, query_type):
        nodes = []
        edges = []

        if query_type == 'Find the Top 10 Companies with the Largest Holdings':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_top_companies_table()
            
        elif query_type == 'Find the Number of Holdings by the Top Investor':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_investor_holdings_table()

        elif query_type == 'Find the Top 10 Investments of the Top Investor':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_top_investments_table()
        
        elif query_type == 'Find Investments with >$1B USD by the Top Investor':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_high_value_investments_table()
        
        elif query_type == 'Find shared investments exceeding $1 billion USD among our top investor and their competitors':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_shared_high_value_investments_table()
        
        elif query_type == 'Jaccard Similarity for Investment Portfolios: compare all stocks and investments of top 10':
            st.write(f"Executing: {query_type} Query")
            nodes, edges = self.query_handler.generate_stock_investment_comparison_graph()
            config = Config(width=1000,
                    height=750,
                    directed=True, 
                    physics=True, 
                    hierarchical=True,
                    )

            return_value = agraph(nodes=nodes, edges=edges, config=config)
        
        elif query_type == 'Jaccard Similarity for Investment Portfolios: extend our analysis to the entire dataset, we explore how all investors compare, not just the top ones':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_portfolio_comparison_table()
        
        elif query_type == 'VSS + Traversals: Identify the top 10 most similar Holder nodes to our key investor':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_similar_holders_table()
        
        elif query_type == 'VSS + Traversals: Compute the number of shared holdings exceeding $1 billion USD among the identified vector-based competitors':
            st.write(f"Executing: {query_type} Query")
            nodes, edges = self.query_handler.generate_shared_holders_graph()
            config = Config(width=1000,
                    height=750,
                    directed=True, 
                    physics=True, 
                    hierarchical=True,
                    )

            return_value = agraph(nodes=nodes, edges=edges, config=config)
        
        elif query_type == 'VSS + Traversals: Calculate the Jaccard Similarity for portfolios of the identified competitors':
            st.write(f"Executing: {query_type} Query")
            self.query_handler.display_competitor_identification_table()

        else:
            st.write("passing")