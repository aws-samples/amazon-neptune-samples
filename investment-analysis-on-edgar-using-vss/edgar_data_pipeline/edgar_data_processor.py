# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import os
import logging
import pandas as pd
from embed_holder_node_descriptions import HolderNodeDescriptionEmbedding

class EdgarDataProcessor:
    def __init__(self, kg_data_root):
        self.kg_data_root = kg_data_root
        self.logger = self.setup_logger()

    def setup_logger(self):
        """
        Set up the logger for the data processor.

        Returns:
        - Logger: The configured logger instance.
        """
        logger = logging.getLogger(__name__)
        logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        ch = logging.StreamHandler()
        ch.setFormatter(formatter)
        logger.addHandler(ch)
        return logger

    def load_data(self, csv_path):
        """
        Load data from a CSV file.

        Args:
        - csv_path (str): The path to the CSV file.

        Returns:
        - DataFrame: A pandas DataFrame containing the loaded data.
        """
        try:
            raw_df = pd.read_csv(csv_path, delimiter=',')
            return raw_df
        except Exception as e:
            self.logger.error(f"Error loading data: {str(e)}")
            raise e

    def create_holder_vertex(self, raw_data):
        """
        Create Holder vertex data from raw data.

        Args:
        - raw_data (DataFrame): The raw data DataFrame.

        Returns:
        - DataFrame: A new DataFrame containing Holder vertex data.

        Raises:
        - Exception: If an error occurs during the data creation process.
        """
        try:
            # Extract the required columns and rename the 'HOLDER' column to 'name:string'
            holder_node_df = raw_data[['CIK', 'HOLDER']].rename(columns={'HOLDER': 'name:string'})

            # Set the data type of the 'CIK' column to string
            holder_node_df['~id'] = holder_node_df['CIK'].astype(str)

            # Create the '~label' column
            holder_node_df['~label'] = 'Holder'

            # Drop duplicates on '~id' and 'name:string'
            holder_node_df = holder_node_df[['~id', '~label', 'name:string']].drop_duplicates(subset=['~id', 'name:string'])

            # Save the new DataFrame to 'holder_node.csv'
            holder_node_df.to_csv(self.kg_data_root + 'holder_node.csv', index=False)
            self.logger.info("Holder node data created successfully.")
            return holder_node_df
        except Exception as e:
            self.logger.error(f"Error creating holder vertex: {str(e)}")
            raise e

    def get_quarter(self, month):
        """
        Get the quarter based on the given month.

        Args:
        - month (int): The month for which the quarter is to be determined.

        Returns:
        - str: The corresponding quarter ('Q1', 'Q2', 'Q3', 'Q4') or 'Invalid Month' if the month is not valid.
        """
        quarter_mapping = {1: 'Q1', 2: 'Q1', 3: 'Q1', 4: 'Q2', 5: 'Q2', 6: 'Q2', 7: 'Q3', 8: 'Q3', 9: 'Q3', 10: 'Q4', 11: 'Q4', 12: 'Q4'}
        return quarter_mapping.get(month, 'Invalid Month')

    def create_holder_quarter_vertex(self, raw_df):
        """
        Create HolderQuarter vertex data from raw data.

        Args:
        - raw_df (DataFrame): The raw data DataFrame.

        Returns:
        - DataFrame: A new DataFrame containing HolderQuarter vertex data.

        Raises:
        - Exception: If an error occurs during the data creation process.
        """
        try:
            # Create a new DataFrame with the desired columns
            holder_quarter_node_df = raw_df[['DATE_FILED', 'CIK', 'HOLDING']].copy()

            # Combine 'DATE_FILED' and 'CIK' to create the 'ID' column
            holder_quarter_node_df['~id'] = holder_quarter_node_df['DATE_FILED'].astype(str) + '_' + holder_quarter_node_df['CIK'].astype(str)

            holder_quarter_node_df['~label'] = 'HolderQuarter'

            # Rename columns to match the new CSV format
            holder_quarter_node_df = holder_quarter_node_df[['~id', '~label', 'DATE_FILED', 'CIK', 'HOLDING']].rename(columns={'DATE_FILED': 'name:string'})

            # Extract the year, month, and day from the "DATE_FILED" column
            raw_df['YEAR'] = raw_df['DATE_FILED'].apply(lambda x: int(str(x)[:4]))
            raw_df['MONTH'] = raw_df['DATE_FILED'].apply(lambda x: int(str(x)[4:6]))

            # Calculate the quarter based on the month
            holder_quarter_node_df['name:string'] = raw_df['YEAR'].astype(str) + raw_df['MONTH'].apply(self.get_quarter)

            holder_quarter_node_df = holder_quarter_node_df.drop_duplicates(subset=['~id', 'HOLDING'])

            # Save the new DataFrame to 'holder_quarter_node.csv'
            holder_quarter_node_df.to_csv(self.kg_data_root + 'holder_quarter_node.csv', index=False)
            self.logger.info("Holder quarter node data created successfully.")
            return holder_quarter_node_df
        except Exception as e:
            self.logger.error(f"Error creating holder quarter vertex: {str(e)}")
            raise e

    def create_holding_vertex(self, raw_df):
        """
        Create Holding vertex data from raw data.

        Args:
        - raw_df (DataFrame): The raw data DataFrame.

        Returns:
        - DataFrame: A new DataFrame containing Holding vertex data.

        Raises:
        - Exception: If an error occurs during the data creation process.
        """
        try:
            holding_node_df = pd.DataFrame({
                '~id': raw_df['HOLDING'].astype(str) + "_" + raw_df['CLASS'].astype(str) + "_" + raw_df['TYPE'].astype(str),
                '~label': 'Holding',
                'name:string': raw_df['HOLDING'],
                'class:string': raw_df['CLASS'],
                'type:string': raw_df['TYPE'],
                'value:float': raw_df['VALUE'].astype(float),
                'quantity:float': raw_df['QTY'].astype(float)
            })

            # Save the new DataFrame to 'holding_node.csv'
            holding_node_df.to_csv(self.kg_data_root + 'holding_node.csv', index=False)
            self.logger.info("Holding node data created successfully.")
            return holding_node_df
        except Exception as e:
            self.logger.error(f"Error creating holding vertex: {str(e)}")
            raise e

    def create_has_holderquarter_edge(self, holder_node_file, holder_quarter_vertex_file):
        """
        Create has_holderquarter_edge data from holder node and holder quarter vertex files.

        Args:
        - holder_node_file (str): The file path for the holder node data CSV file.
        - holder_quarter_vertex_file (str): The file path for the holder quarter vertex data CSV file.

        Returns:
        - DataFrame: A new DataFrame containing has_holderquarter_edge data.

        Raises:
        - Exception: If an error occurs during the data creation process.
        """
        try:
            # Read the CSV file into a DataFrame, assuming it's named 'output.csv' with a comma delimiter
            holder_node_df = pd.read_csv(holder_node_file, sep=',')
            holder_quarter_node_df = pd.read_csv(holder_quarter_vertex_file, sep=',')

            # Rename columns to match the new CSV format
            new_holder_node_df = holder_node_df[['~id']].rename(columns={'~id': '~from'})

            # Rename columns to match the new CSV format
            new_holder_quarter_node_df = holder_quarter_node_df[['~id', 'CIK']].rename(columns={'~id': '~to'})

            # Merge df1 and df2 on the specified columns
            has_holdquarter_edge_df = new_holder_node_df.merge(new_holder_quarter_node_df, left_on='~from', right_on='CIK', how='inner')

            # has_holdquarter_edge_df

            # Drop the duplicate CIK column
            has_holdquarter_edge_df = has_holdquarter_edge_df.drop(columns='CIK')

            has_holdquarter_edge_df['~label'] = "has_holderquarter"

            has_holdquarter_edge_df = has_holdquarter_edge_df.drop_duplicates(subset=['~from', '~to'])

            # Save the new DataFrame to 'has_holderquarter_edge.csv'
            has_holdquarter_edge_df.to_csv(self.kg_data_root + 'has_holderquarter_edge.csv', index=False)
            self.logger.info("has_holderquarter_edge data created successfully.")
            return has_holdquarter_edge_df
        except Exception as e:
            self.logger.error(f"Error creating has_holderquarter_edge: {str(e)}")
            raise e

    def create_owns_edge(self, holder_quarter_vertex_file, holding_vertex_file):
        """
        Create owns_edge data from holder quarter vertex and holding vertex files.

        Args:
        - holder_quarter_vertex_file (str): The file path for the holder quarter vertex data CSV file.
        - holding_vertex_file (str): The file path for the holding vertex data CSV file.

        Returns:
        - DataFrame: A new DataFrame containing owns_edge data.

        Raises:
        - Exception: If an error occurs during the data creation process.
        """
        try:
            # Read the CSV files into DataFrames
            holder_quarter_node_df = pd.read_csv(holder_quarter_vertex_file, sep=',')
            holding_node_df = pd.read_csv(holding_vertex_file, sep=',')

            # Select relevant columns for merging
            holder_quarter_node_df = holder_quarter_node_df[['~id', 'HOLDING']].rename(columns={'~id': '~from'})
            holding_node_df = holding_node_df[['~id', 'name:string', 'value:float', 'quantity:float']].rename(columns={'~id': '~to'})

            # Merge DataFrames on specified columns
            owns_df = holder_quarter_node_df.merge(holding_node_df, left_on='HOLDING', right_on='name:string', how='inner')

            # Add the edge label
            owns_df['~label'] = 'owns'

            # Drop unnecessary columns
            owns_df = owns_df.drop(columns=['name:string', 'HOLDING'])

            # Save the new DataFrame to 'owns_edge.csv'
            owns_df.to_csv(self.kg_data_root + 'owns_edge.csv', index=False)
            self.logger.info("owns_edge data created successfully.")
            return owns_df
        except Exception as e:
            self.logger.error(f"Error creating owns_edge: {str(e)}")
            raise e

    def drop_columns_holder_quarter_vertex(self, holder_quarter_vertex_file):
        """
        Process holder quarter node data by dropping specified columns and removing duplicate rows.

        Args:
        - holder_quarter_vertex_file (str): The file path for the holder quarter vertex data CSV file.

        Returns:
        - DataFrame: A new DataFrame containing processed holder quarter node data.

        Raises:
        - Exception: If an error occurs during the data processing.
        """
        try:
            # HOLDER QUARTER NODE PROCESSING
            holder_quarter_node_df = pd.read_csv(holder_quarter_vertex_file, sep=',')

            # Drop 'CIK' and 'HOLDING' columns
            holder_quarter_node_df = holder_quarter_node_df.drop(columns=['CIK', 'HOLDING'])

            # Drop duplicate rows on '~id'
            holder_quarter_node_df = holder_quarter_node_df.drop_duplicates(subset=['~id'])

            # Save the new DataFrame to 'holder_quarter_node.csv'
            holder_quarter_node_df.to_csv(self.kg_data_root + 'holder_quarter_node.csv', index=False)
            self.logger.info("holder_quarter_node data processed successfully.")
            return holder_quarter_node_df
        except Exception as e:
            self.logger.error(f"Error processing holder_quarter_node: {str(e)}")
            raise e

    def drop_columns_holding_vertex(self, holding_vertex_file):
        """
        Process holding node data by dropping specified columns and removing duplicate rows.

        Args:
        - holding_vertex_file (str): The file path for the holding vertex data CSV file.

        Returns:
        - DataFrame: A new DataFrame containing processed holding node data.

        Raises:
        - Exception: If an error occurs during the data processing.
        """
        try:
            # HOLDING NODE PROCESSING
            holding_node_df = pd.read_csv(holding_vertex_file, sep=',')

            # Drop 'value:float' column
            holding_node_df = holding_node_df.drop(columns='value:float')

            # Drop 'quantity:float' column
            holding_node_df = holding_node_df.drop(columns='quantity:float')

            # Drop duplicate rows on '~id'
            holding_node_df = holding_node_df.drop_duplicates(subset=['~id'])

            # Save the new DataFrame to 'holding_node.csv'
            holding_node_df.to_csv(self.kg_data_root + 'holding_node.csv', index=False)
            self.logger.info("holding_node data processed successfully.")
            return holding_node_df
        except Exception as e:
            self.logger.error(f"Error processing holding_node: {str(e)}")
            raise e

    def generate_holder_node_embeddings(self, holder_node_data_root, source_holder_file='holder_node.csv', emb_holder_file='holder_node_with_embeddings.csv'):
        """
        Generate embeddings for descriptions in a Holder Node DataFrame and save the result in a new DataFrame.

        Parameters:
        - holder_node_data_root (str): The root directory for Holder Node data.
        - source_holder_file (str): The source file containing Holder Node data. Default is 'holder_node.csv'.
        - emb_holder_file (str): The destination file to store Holder Node data with embeddings. Default is 'holder_node_with_embeddings.csv'.

        Returns:
        - None
        """
        edgar_description_emb = HolderNodeDescriptionEmbedding()
        edgar_description_emb.create_embeddings(holder_node_data_root, source_holder_file, emb_holder_file)

    def enrich_holder_node_with_company_description(self, holder_node_data_root, source_holder_file='holder_node.csv'):
        """
        Enriches the Holder Node DataFrame with company descriptions obtained from a language model.

        Parameters:
        - holder_node_data_root (str): The root directory for Holder Node data.
        - source_holder_file (str): The source file containing Holder Node data. Default is 'holder_node.csv'.

        Returns:
        - None
        """
        # Read the CSV file into a DataFrame
        holder_node_df = pd.read_csv(holder_node_data_root + source_holder_file, sep=',')

        # Initialize the Bedrock API for accessing language models
        profile_name ='default'
        region_name ='us-east-1'
        bedrock_api = BedrockAPI(profile_name, region_name)

        # Iterate over each row in the Holder Node DataFrame
        for index, row in holder_node_df.iterrows():
            # Extract the company name
            name = row['name:string']

            # Formulate a question for obtaining a company description
            question = f"Provide the most differentiating description of {name} with their financial mandate in 100 words"

            # Call the Bedrock API to get the description
            holder_node_df.at[index, 'description:string'] = response_text = bedrock_api.call_bedrock_model(question)

        # Display a snapshot of the updated DataFrame
        print(holder_node_df.head(5))

        # Display the description of the first entry in the DataFrame
        print(holder_node_df['description:string'][0])

        # Save the enriched DataFrame to a new CSV file
        holder_node_df.to_csv(holder_node_data_root + 'holder_node_with_description.csv', index=False)



if __name__ == "__main__":
    # Example usage
    kg_data_root = '/path/to/kg_data/'
    processor = EdgarDataProcessor(kg_data_root)

    # Call the methods as needed
    raw_data = processor.load_data()
    processor.create_holder_vertex(raw_data)
    processor.create_holder_quarter_vertex(raw_data)
    processor.create_holding_vertex(raw_data)
    
    # Call the methods as needed
    holder_node_file = os.path.join(kg_data_root, 'holder_node.csv')
    holder_quarter_node_file = os.path.join(kg_data_root, 'holder_quarter_node.csv')
    processor.create_has_holderquarter_edge(holder_node_file, holder_quarter_node_file)

    # Call the methods as needed
    holder_quarter_node_file = os.path.join(kg_data_root, 'holder_quarter_node.csv')
    holding_node_file = os.path.join(kg_data_root, 'holding_node.csv')
    processor.create_owns_edge(holder_quarter_node_file, holding_node_file)

    # Call the methods as needed
    holder_quarter_node_file = os.path.join(kg_data_root, 'holder_quarter_node.csv')
    processor.drop_columns_holder_quarter_node(holder_quarter_node_file)

    # Call the methods as needed
    holding_node_file = os.path.join(kg_data_root, 'holding_node.csv')
    processor.drop_columns_holding_node(holding_node_file)

    # Enriches the Holder Node with company descriptions obtained from a language model.
    holder_node_data_root = '/path/to/holder_node_root/'
    source_holder_file = 'holder_node.csv'
    processor.enrich_holder_node_with_company_description(holder_node_data_root, source_holder_file)

    # generate embeddings for descriptions in a Holder Node.
    holder_node_data_root = '/path/to/holder_node_root/'
    source_holder_file = 'holder_node.csv'
    emb_holder_file = 'holder_node_with_embeddings.csv'
    processor.generate_holder_node_embeddings(holder_node_data_root, source_holder_file, emb_holder_file)