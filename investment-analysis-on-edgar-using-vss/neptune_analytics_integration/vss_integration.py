import boto3
import json
import pandas as pd

# from settings import graphId
graphId = "g-sqmear9347" #alpha-9 with VSS WORKING GRAPH


# Create a Graph
# alpha environment

# Working Graph creation
'''
aws neptune-graph create-graph --graph-name 'edgar-vss-3'  --region us-east-1 --provisioned-memory 128 --allow-from-public --replica-count 0 --vector-search '{"dimension": 384}' --endpoint https://svc.us-east-1-alpha.p8.neptune.aws.dev --region us-east-1
'''

# check graph create status
'''
aws neptune-graph get-graph \
--graph-id g-sqmear9347 \
--endpoint https://svc.us-east-1-alpha.p8.neptune.aws.dev \
--region us-east-1
'''

# Load KG using:
'''
CALL neptune.load({format: "csv", 
source: "s3://aws-neptune-customer-samples-us-east-1/sample-datasets/gremlin/edgar/", 
region : "us-east-1"})
'''

class VSSIntegration:
    client = None

    def __init__(self) -> None:
        """
        Initializes the VSSIntegration instance with a Neptune client and graphId.

        Parameters:
        - graphId (str): The ID of the graph.
        """
        self.client = boto3.client(
            "neptune-graph",
            endpoint_url="https://data.us-east-1-alpha.p8.neptune.aws.dev",
            region_name="us-east-1",
        )
        self.graphId = graphId
        pass

    def load_data(self):
        """
        Loads data into the graph from a specified S3 source.

        Returns:
        - str: JSON representation of the query results.
        """
        query = """
        CALL neptune.load({format: "csv", 
        source: "s3://aws-neptune-customer-samples-us-east-1/sample-datasets/gremlin/edgar/", 
        region : "us-east-1"})
        """
        resp = self.client.execute_open_cypher_query(
            openCypherQuery=query, graphId=self.graphId
        )
        if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
            return json.dumps(resp["results"])
        else:
            print("An error occurred fetching the company nodes")

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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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
        lst = [{"h.name": "RAYMOND JAMES & ASSOCIATES", "hq_id": "20231024_1084208", "total_value": 1170740000000.0}, {"h.name": "Raymond James Financial Services Advisors, Inc.", "hq_id": "20231024_1462284", "total_value": 1151900000000.0}, {"h.name": "CONCOURSE FINANCIAL GROUP SECURITIES, INC.", "hq_id": "20231025_752798", "total_value": 1111670000000.0}, {"h.name": "PATHSTONE FAMILY OFFICE, LLC", "hq_id": "20231024_1511137", "total_value": 1102170000000.0}, {"h.name": "BROWN ADVISORY INC", "hq_id": "20231025_1345929", "total_value": 1099560000000.0}, {"h.name": "Stratos Wealth Partners, LTD.", "hq_id": "20231024_1612865", "total_value": 1065569999999.9999}, {"h.name": "FinTrust Capital Advisors, LLC", "hq_id": "20231024_1622001", "total_value": 1052160000000.0}, {"h.name": "MACKENZIE FINANCIAL CORP", "hq_id": "20231025_919859", "total_value": 1028419999999.9999}, {"h.name": "Concord Wealth Partners", "hq_id": "20231025_1814214", "total_value": 1008690000000.0001}, {"h.name": "PINNACLE ASSOCIATES LTD", "hq_id": "20231024_743127", "total_value": 993753000000.0}]
        params = {'hq_id': lst[0]['hq_id'], 'competitors': [l['hq_id'] for l in lst[-9:]]}
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