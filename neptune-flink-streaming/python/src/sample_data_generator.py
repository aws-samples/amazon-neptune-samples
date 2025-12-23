"""
Sample data generator for testing vertex and edge data processing.
"""

import json
import boto3
import time
import random
from typing import Dict, Any
import os


class SampleDataGenerator:
    """
    Generates sample vertex and edge data for testing.
    """
    
    def __init__(self, region: str = 'us-east-1'):
        self.kinesis_client = boto3.client('kinesis', region_name=region)
    
    def generate_vertex_data(self, vertex_id: str, label: str, partition_id: str, **properties) -> Dict[str, Any]:
        """
        Generate vertex data in tabular format.
        
        Args:
            vertex_id: Vertex identifier
            label: Vertex label
            partition_id: Partition identifier
            **properties: Additional vertex properties
            
        Returns:
            Dict containing vertex data
        """
        vertex_data = {
            '~id': vertex_id,
            '~label': label
        }
        vertex_data.update(properties)
        
        # Return as message with partition_id
        return {
            'data': json.dumps(vertex_data),
            'partition_id': partition_id
        }
    
    def generate_edge_data(self, edge_id: str, from_id: str, to_id: str, 
                          edge_type: str, partition_id: str, **properties) -> Dict[str, Any]:
        """
        Generate edge data in tabular format.
        
        Args:
            edge_id: Edge identifier
            from_id: Source vertex ID
            to_id: Target vertex ID
            edge_type: Edge type/label
            partition_id: Partition identifier
            **properties: Additional edge properties
            
        Returns:
            Dict containing edge data
        """
        edge_data = {
            '~id': edge_id,
            '~from': from_id,
            '~end': to_id,
            '~label': edge_type
        }
        edge_data.update(properties)
        
        # Return as message with partition_id
        return {
            'data': json.dumps(edge_data),
            'partition_id': partition_id
        }
    
    def send_to_kinesis(self, stream_name: str, data: Dict[str, Any]):
        """
        Send data to Kinesis stream.
        
        Args:
            stream_name: Kinesis stream name
            data: Data to send
        """
        try:
            response = self.kinesis_client.put_record(
                StreamName=stream_name,
                Data=json.dumps(data),
                PartitionKey=str(random.randint(1, 100))
            )
            print(f"Sent to {stream_name}: {data}")
            
        except Exception as e:
            print(f"Error sending to Kinesis: {str(e)}")
    
    def generate_sample_graph_data(self, vertex_stream: str, edge_stream: str, 
                                 num_vertices: int = 10, num_edges: int = 15):
        """
        Generate and send sample graph data to Kinesis streams.
        
        Args:
            vertex_stream: Vertex stream name
            edge_stream: Edge stream name
            num_vertices: Number of vertices to generate
            num_edges: Number of edges to generate
        """
        print(f"Generating {num_vertices} vertices and {num_edges} edges")
        
        # Generate vertices
        vertex_ids = []
        partitions = ['partition_1', 'partition_2', 'partition_3', 'partition_4']
        
        # Calculate split: 40% people, 60% companies
        num_people = int(num_vertices * 0.4)
        person_count = 0
        company_count = 0
        
        for i in range(num_vertices):
            partition_id = partitions[i % len(partitions)]
            
            if person_count < num_people:
                # Person vertices
                vertex_data = self.generate_vertex_data(
                    vertex_id=f"person_{person_count}",
                    label="Person",
                    partition_id=partition_id,
                    name=f"Person {person_count}",
                    age=random.randint(20, 60),
                    city=random.choice(["New York", "San Francisco", "Seattle", "Boston"])
                )
                person_count += 1
            else:
                # Company vertices
                vertex_data = self.generate_vertex_data(
                    vertex_id=f"company_{company_count}",
                    label="Company",
                    partition_id=partition_id,
                    name=f"Company {company_count}",
                    industry=random.choice(["Tech", "Finance", "Healthcare", "Retail"]),
                    employees=random.randint(10, 10000)
                )
                company_count += 1
            
            # Extract vertex ID from the data field
            data_dict = json.loads(vertex_data['data'])
            vertex_ids.append((data_dict['~id'], partition_id))
            self.send_to_kinesis(vertex_stream, vertex_data)
            time.sleep(0.1)
        
        # Generate edges
        edges_created = 0
        attempts = 0
        max_attempts = num_edges * 3  # Prevent infinite loop
        
        while edges_created < num_edges and attempts < max_attempts:
            attempts += 1
            from_vertex = random.choice(vertex_ids)
            to_vertex = random.choice(vertex_ids)
            
            # Use the same partition as the from_vertex
            partition_id = from_vertex[1]
            
            if from_vertex[0] != to_vertex[0]:
                edge_data = None
                if from_vertex[0].startswith('person') and to_vertex[0].startswith('company'):
                    edge_data = self.generate_edge_data(
                        edge_id=f"works_at_{edges_created}",
                        from_id=from_vertex[0],
                        to_id=to_vertex[0],
                        edge_type="WORKS_AT",
                        partition_id=partition_id,
                        start_date="2023-01-01",
                        position=random.choice(["Engineer", "Manager", "Analyst", "Director"])
                    )
                elif from_vertex[0].startswith('person') and to_vertex[0].startswith('person'):
                    edge_data = self.generate_edge_data(
                        edge_id=f"knows_{edges_created}",
                        from_id=from_vertex[0],
                        to_id=to_vertex[0],
                        edge_type="KNOWS",
                        partition_id=partition_id,
                        since=random.choice(["2020", "2021", "2022", "2023"]),
                        relationship=random.choice(["friend", "colleague", "family"])
                    )
                
                if edge_data:
                    self.send_to_kinesis(edge_stream, edge_data)
                    edges_created += 1
                    time.sleep(0.1)
        
        print(f"Created {edges_created} edges from {attempts} attempts")


def main():
    """
    Generate sample data for testing.
    """
    region = 'us-east-1'
    try:
        region = os.environ['AWS_REGION']
        print(f"region is set to {region}")
    except KeyError:
        print(f"AWS_REGION environment variable not found. Defaulting to {region}.")
    generator = SampleDataGenerator(region=region)
    
    # Generate sample data
    generator.generate_sample_graph_data(
        vertex_stream="vertex-stream",
        edge_stream="edge-stream",
        num_vertices=1000,
        num_edges=1500
    )
    
    print("Sample data generation completed")


if __name__ == "__main__":
    main()
