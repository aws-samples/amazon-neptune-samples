import numpy as np
from sentence_transformers import SentenceTransformer
import pandas as pd

class HolderNodeDescriptionEmbedding:
    """
    A class to generate embeddings for descriptions in a Holder Node DataFrame.

    Attributes:
    - None

    Methods:
    - create_embeddings(holder_node_data_root, source_holder_file='holder_node.csv', emb_holder_file='holder_node_with_embeddings.csv'):
        Creates embeddings for descriptions in a Holder Node DataFrame and saves the result in a new DataFrame.

    Usage:
    - Instantiate an object of HolderNodeDescriptionEmbedding.
    - Call create_embeddings method to process the Holder Node DataFrame.

    Example:
    ```
    edgar_description_emb = HolderNodeDescriptionEmbedding()
    edgar_description_emb.create_embeddings()
    ```

    Note: This class relies on the 'sentence_transformers' library for generating embeddings.
    """

    def __init__(self):
        """
        Initializes an instance of HolderNodeDescriptionEmbedding.

        Parameters:
        - None
        """
        pass

    def create_embeddings(self, holder_node_data_root, source_holder_file='holder_node.csv', emb_holder_file='holder_node_with_embeddings.csv'):
        """
        Creates embeddings for descriptions in a Holder Node DataFrame and saves the result in a new DataFrame.

        Parameters:
        - holder_node_data_root (str): The root directory for Holder Node data.
        - source_holder_file (str): The source file containing Holder Node data. Default is 'holder_node.csv'.
        - emb_holder_file (str): The destination file to store Holder Node data with embeddings. Default is 'holder_node_with_embeddings.csv'.

        Returns:
        - None
        """
        holder_node_df = pd.read_csv(holder_node_data_root + source_holder_file, sep=',')
        description_column = []

        # Extract description column from the Holder Node DataFrame
        for index, row in holder_node_df.iterrows():
            description = row['description:string']
            description_column.append(description)

        # Load the Sentence Transformer model
        model = SentenceTransformer("all-MiniLM-L6-v2")

        # Encode descriptions to obtain embeddings
        embeddings = model.encode(description_column, normalize_embeddings=True)

        # Create a new DataFrame with embeddings
        data = []

        for index, row in holder_node_df.iterrows():
            row_data = {'~id': row['~id'], '~label': row['~label'], 'name:string': row['name:string'],
                        'description:string': row['description:string'], 'embedding:vector': embeddings[index]}
            data.append(row_data)

        df = pd.DataFrame(data)

        # Convert the embedding vectors to a semicolon-separated string
        def convert_to_semicolon(embedding):
            return ';'.join(map(str, embedding))

        df['embedding:vector'] = df['embedding:vector'].apply(convert_to_semicolon)

        # Clean up description column by replacing commas and newlines
        df["description:string"] = df["description:string"].str.replace(',', ' ')
        df["description:string"] = df["description:string"].str.replace('\n', '')

        # Save the new DataFrame to the specified file
        df.to_csv(holder_node_data_root + emb_holder_file, index=False)


def main():
    """
    Entry point for the script. Executes HolderNodeDescriptionEmbedding to generate embeddings for Holder Node descriptions.
    """
    edgar_description_emb = HolderNodeDescriptionEmbedding()
    holder_node_data_root = '/path/to/holder_node_root/'
    source_holder_file='holder_node.csv'
    emb_holder_file='holder_node_with_embeddings.csv'
    edgar_description_emb.create_embeddings()


if __name__ == "__main__":
    main()
