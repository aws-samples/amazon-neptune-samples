import boto3
import logging
logger = logging.getLogger(__name__)

from botocore.config import Config

BOTO3_CONFIG = Config(
    retries = {
        'max_attempts': 1
    }
)

class Neptune:
    def __init__(self, connection_url):
        logger.debug(f"Connecting to Neptune cluster at 'https://{connection_url}:8182'")
        self.neptune_data_client = boto3.client('neptunedata', endpoint_url=f"https://{connection_url}:8182", region_name='us-east-1', config=BOTO3_CONFIG)

    def run_open_cypher_query(self, query):
        response = ""

        try:
            response = self.neptune_data_client.execute_open_cypher_query(
                openCypherQuery=query
            )
#            logger.debug(f"Query Result is {response}")
        except Exception as e:
            response = {"error": type(e), "message": str(e)}
           
        return response
    
    def run_open_cypher_explain_query(self, query, explain_level):
        response = ""

        try:
            raw_response = self.neptune_data_client.execute_open_cypher_explain_query(
                openCypherQuery=query,
                explainMode=explain_level
            )

            response = {"result": raw_response["results"].read().decode('utf-8')}
        except Exception as e:
            response = {"error": type(e), "message": str(e)}
           
        return response

    def get_statistics_last_update_time(self):
        response = ""

        try:
            graph_summary = self.neptune_data_client.get_propertygraph_summary()
            response = graph_summary["payload"]["lastStatisticsComputationTime"]
        except Exception as e:
            response = {"error": type(e), "message": str(e)}

        return response
    
    def get_schema(self):
        schema_summary = self.neptune_data_client.get_propertygraph_summary()
        nodeLabels = schema_summary["payload"]["graphSummary"]["nodeLabels"]
        edgeLabels = schema_summary["payload"]["graphSummary"]["edgeLabels"]

        types = {
            "str": "STRING",
            "float": "DOUBLE",
            "int": "INTEGER",
            "list": "LIST",
            "dict": "MAP",
            "bool": "BOOLEAN",
        }
        node_properties = self.__get_node_properties(nodeLabels, types)
        edge_properties = self.__get_edge_properties(edgeLabels, types)
        triple_schema = self.__get_triples(edgeLabels)

        return  f"""
                Node properties are the following:
                {node_properties}
                Relationship properties are the following:
                {edge_properties}
                The relationships are the following:
                {triple_schema}
                """

    def __get_node_properties(self, n_labels, types):
        node_properties_query = """
        MATCH (a:`{n_label}`)
        RETURN properties(a) AS props
        LIMIT 100
        """
        node_properties = []
        for label in n_labels:
            q = node_properties_query.format(n_label=label)
            data = {"label": label, "properties": self.neptune_data_client.execute_open_cypher_query(openCypherQuery=q)["results"]}
            s = set({})
            for p in data["properties"]:
                for k, v in p["props"].items():
                    s.add((k, types[type(v).__name__]))
            properties = []
            for k,v in s:
                property_dict = {}
                property_dict["property"] = k
                property_dict["type"] = v
                if (property_dict["type"] == "STRING"):
                    categories = []
                    if (self.__count_distinct_properties(label, property_dict["property"])[0]["result_count"] < 10):
                        category_data = self.__get_categories_for_property(label, property_dict["property"])
                        for item in category_data:
                            categories.append(item["categories"])
                        property_dict["values"] = categories
                properties.append(property_dict)
            np = {
                "properties": properties,
                "labels": label,
            }
            node_properties.append(np)

        return node_properties
        
    def __get_edge_properties(self, e_labels, types):
        edge_properties_query = """
        MATCH ()-[e:`{e_label}`]->()
        RETURN properties(e) AS props
        LIMIT 100
        """
        edge_properties = []
        for label in e_labels:
            q = edge_properties_query.format(e_label=label)
            data = {"label": label, "properties": self.neptune_data_client.execute_open_cypher_query(openCypherQuery=q)["results"]}
            s = set({})
            for p in data["properties"]:
                for k, v in p["props"].items():
                    s.add((k, types[type(v).__name__]))
            ep = {
                "type": label,
                "properties": [{"property": k, "type": v} for k, v in s],
            }
            edge_properties.append(ep)

        return edge_properties
        
    def __get_triples(self,e_labels):
        triple_query = """
        MATCH (a)-[e:`{e_label}`]->(b)
        WITH a,e,b LIMIT 3000
        RETURN DISTINCT labels(a) AS from, type(e) AS edge, labels(b) AS to
        LIMIT 10
        """

        triple_template = "(:`{a}`)-[:`{e}`]->(:`{b}`)"
        triple_schema = []
        for label in e_labels:
            q = triple_query.format(e_label=label)
            data = self.neptune_data_client.execute_open_cypher_query(openCypherQuery=q)["results"]
            for d in data:
                triple = triple_template.format(
                    a=d["from"][0], e=d["edge"], b=d["to"][0]
                )
                triple_schema.append(triple)

        return triple_schema
    def __count_distinct_properties(self,n_label, n_property):
        count_properties_distinct_query = """
        MATCH (a:`{n_label}`)
        WITH DISTINCT a.{n_property} as dist_props
        RETURN COUNT(dist_props) as result_count
        """
        q = count_properties_distinct_query.format(n_label=n_label, n_property=n_property)
        result = self.neptune_data_client.execute_open_cypher_query(openCypherQuery=q)["results"]
        return result
        
    def __get_categories_for_property(self, n_label, n_property):
        categories_for_property_query = """
        MATCH (a:`{n_label}`)
        RETURN DISTINCT a.{n_property} as categories    
        """
        q = categories_for_property_query.format(n_label=n_label, n_property=n_property)
        return self.neptune_data_client.execute_open_cypher_query(openCypherQuery=q)["results"]
