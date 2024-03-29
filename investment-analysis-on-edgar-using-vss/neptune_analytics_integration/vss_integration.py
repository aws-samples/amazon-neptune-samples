# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import boto3
import json
import pandas as pd

from settings import graphId

class VSSIntegration:
    client = None

    def __init__(self, region_name="us-east-1") -> None:
        """
        Initializes the VSSIntegration instance with a Neptune client and graphId.

        Parameters:
        - region_name (str): The AWS region name. Defaults to "us-east-1".
        """
        self.client = boto3.client(
            "neptune-graph",
            region_name=region_name,
        )
        # self.lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        self.lst = []
        self.graphId = graphId
        pass

    def load_data(self, source, region):
        """
        Loads data into the graph from a specified S3 source.

        Args:
        - source (str): The S3 source URL.
        - region (str): The AWS region of the S3 bucket.

        Returns:
        - str: JSON representation of the query results.
        """
        query = f"""
        CALL neptune.load({{
            format: "csv", 
            source: "{source}", 
            region: "{region}"
        }})
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the company nodes")
    
    def create_neptune_graph(self, graph_name, region, replica_count, source_type, vector_search_config,
                            min_provisioned_memory, max_provisioned_memory, allow_from_public,
                            data_format, source_location, role_arn):
        """
        Creates a Neptune graph using the specified parameters.

        Args:
            graph_name (str): The name of the graph to be created.
            region (str): The AWS region in which the graph will be created.
            replica_count (int): The number of replicas for the graph.
            source_type (str): The type of source for the graph (e.g., S3).
            vector_search_config (str): Configuration for vector search (e.g., "dimension=384").
            min_provisioned_memory (int): Minimum provisioned memory for the graph.
            max_provisioned_memory (int): Maximum provisioned memory for the graph.
            allow_from_public (bool): Whether to allow access from public networks.
            data_format (str): The format of the data (e.g., "csv").
            source_location (str): The S3 location of the data source.
            role_arn (str): The ARN of the role for graph execution.

        Returns:
            str: JSON representation of the response from the Neptune graph creation.

        Raises:
            subprocess.CalledProcessError: If the command returns a non-zero exit code.

        Note:
            This method assumes that the AWS CLI is installed and configured with the necessary credentials.

        Example:
        create_neptune_graph(
            graph_name="p8-test-edgar-007",
            region="us-east-1",
            replica_count=0,
            source_type="S3",
            vector_search_config="dimension=384",
            min_provisioned_memory=768,
            max_provisioned_memory=768,
            allow_from_public=True,
            data_format="csv",
            source_location="s3://neptune-benchmark-artifacts-us-east-1/data/CP/edger/EDGAR-owns-edge-file/",
            role_arn="arn:aws:iam::451235071234:role/GraphExecutionRole"
        )
        """
        command = [
            "aws",
            "neptune-graph",
            "create-graph-using-import-task",
            "--graph-name", graph_name,
            "--region", region,
            "--replica-count", str(replica_count),
            "--source-type", source_type,
            "--vector-search-configuration", vector_search_config,
            "--min-provisioned-memory", str(min_provisioned_memory),
            "--max-provisioned-memory", str(max_provisioned_memory),
            "--allow-from-public" if allow_from_public else "",
            "--format", data_format,
            "--source", source_location,
            "--role-arn", role_arn
        ]

        try:
            result = subprocess.run(command, capture_output=True, check=True, text=True)
            response_json = json.loads(result.stdout)
            return json.dumps(response_json)
        except subprocess.CalledProcessError as e:
            print(f"Error executing command: {e}")
            print(f"Command output: {e.output}")

    # Query 1: Find the Top 10 Companies with the Largest Holdings
    def fetch_top_ten_companies_with_largest_holding(self):
        """
        Query to find the Top 10 Companies with the Largest Holdings.

        Returns:
        - str: JSON representation of the query results.
        """
        query = """
        MATCH (h:Holder)-->(hq:HolderQuarter)-[o:owns]->(holdings:Holding)
        WHERE hq.name = '2023Q4'
        RETURN h.name, id(hq) as hq_id, sum(o.value) as total_value 
        ORDER BY total_value DESC LIMIT 10
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            self.lst = resp["results"]
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the company nodes")

    # Query 2: Find the Number of Holdings by the Top Investor
    def fetch_investor_holdings(self):
        """
        Fetches the number of holdings owned by the top investor.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)-[:owns]->(h:Holding)
        WHERE id(hq) = $hq_id
        RETURN count(h)
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")
    
    # Query 3: Find the Top 10 Investments of the Top Investor
    def fetch_top_ten_investments(self):
        """
        Fetches the top 10 investments of the top investor.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)-[o:owns]->(h:Holding)
        WHERE id(hq) = $hq_id
        RETURN h.name, o.value
        ORDER by o.value DESC LIMIT 10
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")
    
    # Query 4: Find Investments with >$1B USD by the Top Investor
    def fetch_investments_greater_than_billion(self):
        """
        Fetches investments with a value greater than $1 billion USD by the top investor.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)-[o:owns]->(h:Holding)
        WHERE id(hq) = $hq_id
        AND o.value > 1000000000
        RETURN h.name, o.value
        ORDER by o.value DESC
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")

    # Comparing Our Top Investor Against Competitors
    # Query 5: Find shared investments exceeding $1 billion USD among our top investor and their competitors.
    def fetch_shared_investments_greater_than_billion(self):
        """
        Finds shared investments exceeding $1 billion USD among our top investor and their competitors.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)-[o:owns]->(h:Holding)
        WHERE id(hq) = $hq_id
        AND o.value > 1000000000
        WITH h
        MATCH (h)<-[o:owns]-(co_hq)<--(coholder:Holder)
        WHERE id(co_hq) IN $competitors
        AND o.value > 1000000000
        WITH coholder.name as name, collect(DISTINCT h.name) as coholdings
        RETURN name, size(coholdings) as number_shared, coholdings
        ORDER BY number_shared DESC
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")
    
    # Query 6: Jaccard Similarity for Investment Portfolios: compare all stocks and investments of top 10
    def js_compare_stocks_and_investments_for_top_ten(self):
        """
        Computes the Jaccard Similarity for investment portfolios by comparing all stocks and investments of the top 10 investors.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)<--(holder:Holder)
        WHERE id(hq) = $hq_id
        MATCH (co_hq:HolderQuarter)<--(coholder:Holder)
        WHERE id(co_hq) IN $competitors
        CALL neptune.algo.jaccardSimilarity(hq, co_hq)
        YIELD score
        RETURN holder.name, coholder.name, score
        ORDER BY score DESC
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")
    
    # Query 7: Jaccard Similarity for Investment Portfolios: extend our analysis to the entire dataset, we explore how all investors compare, not just the top ones.
    def js_compare_stocks_and_investments_for_entire_portfolio(self):
        """
        Computes the Jaccard Similarity for investment portfolios, extending the analysis to the entire dataset. Compares how all investors compare, not just the top ones.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (hq:HolderQuarter)<--(holder:Holder)
        WHERE id(hq) = $hq_id
        MATCH (co_hq:HolderQuarter)<--(coholder:Holder)
        WHERE NOT id(co_hq) = $hq_id
        CALL neptune.algo.jaccardSimilarity(hq, co_hq)
        YIELD score
        RETURN holder.name, coholder.name, score
        ORDER BY score DESC LIMIT 10
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")

    # Vector Similarity Search (VSS) + Graph Traversals
    # Query 8: identify the top 10 most similar Holder nodes to our key investor based on vector embeddings derived from company descriptions.
    def fetch_top_ten_similar_holders(self):
        """
        Identifies the top 10 most similar Holder nodes to our key investor based on vector embeddings derived from company descriptions.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (n:Holder)-->(hq)
        WHERE id(hq) = $hq_id
        CALL neptune.algo.vectors.topKByNode(n)
        YIELD node, score
        RETURN node.name
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")

    # Vector Similarity Search (VSS) + Graph Traversals
    # Query 9: assess the number of shared holdings exceeding $1 billion USD among the identified vector-based competitors.
    def fetch_shared_holders_greater_than_billion(self):
        """
        Assesses the number of shared holdings exceeding $1 billion USD among the identified vector-based competitors.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (h:Holder)-->(hq)
        WHERE id(hq) = $hq_id
        WITH h
        CALL neptune.algo.vectors.topKByNode(h)
        YIELD node, score
        WHERE score > 0
        WITH node as coholder LIMIT 10
        MATCH (holding)<-[o:owns]-(hq)<--(coholder:Holder)
        WHERE hq.name = '2023Q4' AND o.value > 10000000000
        WITH coholder.name as name, collect(DISTINCT holding.name) as coholdings
        RETURN name, size(coholdings) as number_shared, coholdings
        ORDER BY number_shared DESC
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")

    # Vector Similarity Search (VSS) + Graph Traversals
    # Query 10: To Conclude: we calculate the Jaccard Similarity for portfolios of the identified vector-based competitors.
    def js_on_identified_competitors(self):
        """
        Calculates the Jaccard Similarity for portfolios of the identified vector-based competitors.

        Returns:
        - str: JSON representation of the query results.
        """
        params = {'hq_id':  self.lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in  self.lst[-9:]]}
        query = """
        MATCH (holder:Holder)-->(hq)
        WHERE id(hq) = $hq_id
        WITH holder, hq
        CALL neptune.algo.vectors.topKByNode(holder)
        YIELD node, score as s
        WHERE s > 0
        MATCH (node)-->(co_hq)
        WHERE co_hq.name = '2023Q4'
        CALL neptune.algo.jaccardSimilarity(hq, co_hq)
        YIELD score
        RETURN holder.name, node.name, score
        ORDER BY score DESC LIMIT 10
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, parameters=json.dumps(params), graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the vss")