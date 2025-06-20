import argparse
import logging
import os
from dataclasses import dataclass

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq
import pyarrow.csv as pa_csv
import pyarrow.fs as pa_fs
from joblib import Parallel, delayed
from sklearn.model_selection import train_test_split

MISSING_DEVICE = "UNKNOWN_DEVICE"


# Create a dataclass for safer argument handling
@dataclass
class DataProcessorArgs:
    data_prefix: str
    output_prefix: str
    transactions_fname: str
    identity_fname: str
    id_cols: str
    cat_cols: str
    train_data_ratio: float
    construct_homogeneous: bool
    create_feature_vector: bool


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-prefix", type=str, default="/opt/ml/processing/input")
    parser.add_argument(
        "--output-prefix", type=str, default="/opt/ml/processing/output"
    )
    parser.add_argument(
        "--transactions-fname",
        type=str,
        default="transaction.csv",
        help="name of file with transactions",
    )
    parser.add_argument(
        "--identity-fname",
        type=str,
        default="identity.csv",
        help="name of file with identity info",
    )
    parser.add_argument(
        "--id-cols",
        type=str,
        default="",
        help="comma separated id cols in transactions table",
    )
    parser.add_argument(
        "--cat-cols",
        type=str,
        default="",
        help="comma separated categorical cols in transactions",
    )
    parser.add_argument(
        "--train-data-ratio",
        type=float,
        default=0.8,
        help="fraction of data to use in training set",
    )
    parser.add_argument(
        "--construct-homogeneous",
        action="store_true",
        default=False,
        help="use bipartite graphs edgelists to construct homogenous graph edgelist",
    )
    parser.add_argument(
        "--create-feature-vector",
        action="store_true",
        help="Create single feature vector for transaction features instead of individual columns",
    )

    return parser.parse_args()


def load_data_and_create_splits(
    raw_data_path: str,
    transactions_filename: str,
    identity_filename: str,
    train_data_ratio: float,
    splits_prefix: str,
):
    """Extract transactions and identifier DF, and write transaction train/val/test split assignments.

    Parameters
    ----------
    raw_data_path : str
        Path to input CSV files
    transactions_filename : str
        Name of the file containing the transactions.
    identity_filename : str
        Name of the file containing the transaction identifiers.
    train_data_ratio : float
        Percentage of data to assign to train set. Remainder will be equally
        split between validation and test sets.
    splits_prefix : str
        Prefix to split files output path.

    Returns
    -------
    pd.DataFrame, pd.DataFrame
        A tuple whose first element is the transactions DF and the second is the identity DF
    """

    def read_csv_with_pyarrow(file_path: str) -> pa.Table:
        """Read CSV file using PyArrow with native S3 support."""
        if file_path.startswith("s3://"):
            # Extract the region from environment or use a default
            region = os.environ.get("AWS_REGION", "us-east-1")
            s3_fs = pa_fs.S3FileSystem(region=region)
            s3_path = file_path.replace("s3://", "")
            read_options = pa_csv.ReadOptions(use_threads=True)
            parse_options = pa_csv.ParseOptions(delimiter=",")
            convert_options = pa_csv.ConvertOptions(
                timestamp_parsers=["%Y-%m-%d %H:%M:%S"]
            )
            table = pa_csv.read_csv(
                s3_fs.open_input_stream(s3_path),
                read_options=read_options,
                parse_options=parse_options,
                convert_options=convert_options,
            )
        else:
            # For local files, still use PyArrow for consistency
            table = pa_csv.read_csv(file_path)
        return table

    # Read transaction data using PyArrow
    transaction_table = read_csv_with_pyarrow(
        os.path.join(raw_data_path, transactions_filename)
    )
    transaction_df = transaction_table.to_pandas()
    logging.info("Shape of source transaction data is {}".format(transaction_df.shape))
    logging.info(
        "# Tagged transactions: {}".format(
            len(transaction_df) - transaction_df.isFraud.isnull().sum()
        )
    )

    # Read identity data using PyArrow
    identity_table = read_csv_with_pyarrow(
        os.path.join(raw_data_path, identity_filename)
    )
    identity_df = identity_table.to_pandas()
    logging.info("Shape of source identity data is {}".format(identity_df.shape))

    # extract out stratified transactions for train/val/test sets
    train_df, test_val_df = train_test_split(
        transaction_df,
        train_size=train_data_ratio,
        random_state=42,
        stratify=transaction_df["isFraud"],
    )
    test_df, val_df = train_test_split(
        test_val_df, train_size=0.5, random_state=42, stratify=test_val_df["isFraud"]
    )

    # Write id column for train/val/test sets
    logging.info("Writing transaction ids to train/val/test files")

    if not splits_prefix.startswith("s3://"):
        logging.info("Creating output directory '%s' if needed", splits_prefix)
        os.makedirs(splits_prefix, exist_ok=True)

    # Use PyArrow for writing parquet files efficiently
    for df_name, df_data in [
        ("train_ids.parquet", train_df[["TransactionID"]]),
        ("val_ids.parquet", val_df[["TransactionID"]]),
        ("test_ids.parquet", test_df[["TransactionID"]]),
    ]:
        # Reset index to avoid including it in the output
        df_data: pd.DataFrame = df_data.reset_index(drop=True)
        out_path = os.path.join(splits_prefix, df_name)
        table = pa.Table.from_pandas(df_data.rename(columns={"TransactionID": "nid"}))

        if out_path.startswith("s3://"):
            # Extract the region from environment or use a default
            region = os.environ.get("AWS_REGION", "us-east-1")
            s3_fs = pa_fs.S3FileSystem(region=region)
            s3_path = out_path.replace("s3://", "")
            pq.write_table(table, s3_path, filesystem=s3_fs)
        else:
            pq.write_table(table, out_path)

    def get_fraud_frac(series):
        return 100 * sum(series) / len(series)

    logging.info(
        "Percent fraud for train/val/test transactions: {:.2f}% / {:.2f}% / {:.2f}%".format(
            get_fraud_frac(train_df.isFraud),
            get_fraud_frac(val_df.isFraud),
            get_fraud_frac(test_df.isFraud),
        )
    )

    return transaction_df, identity_df


def split_dataframe(df: pd.DataFrame, num_chunks: int = 16) -> list[pd.DataFrame]:
    """Split a DataFrame into approximately equal chunks.

    Parameters
    ----------
    df : pd.DataFrame
        DataFrame to split
    num_chunks : int, optional
        Number of chunks to split into, by default 16

    Returns
    -------
    list[pd.DataFrame]
        List of DataFrames
    """
    if len(df) < num_chunks:
        # If DataFrame is smaller than requested chunks, return as single chunk
        return [df]

    # Calculate chunk size
    chunk_size = len(df) // num_chunks
    chunks = []

    # Split DataFrame into chunks
    for i in range(0, num_chunks - 1):
        chunks.append(df.iloc[i * chunk_size : (i + 1) * chunk_size])

    # Add the last chunk (which might be slightly larger)
    chunks.append(df.iloc[(num_chunks - 1) * chunk_size :])

    # Filter out empty chunks
    return [chunk for chunk in chunks if not chunk.empty]


def write_compatible_csv(df: pd.DataFrame, base_prefix: str, num_chunks: int = 16):
    """Writes CSV in the formatting that Neptune Analytics expects.

    Ensures column names follow Neptune Analytics export format with type information
    in the format ``col_name:neptune_type``. Optionally splits data into multiple files when writing.

    Uses PyArrow for improved performance, especially when writing to S3.

    Parameters
    ----------
    df : pd.DataFrame
        DataFrame to write
    base_prefix : str
        Base prefix for output files (e.g., "/path/to/Vertex_Transaction" or "/path/to/Edge_associated_with")
        The function will automatically add the index suffix and .csv extension
    num_chunks : int, optional
        Number of chunks to split the data into, by default 16
    """
    # Special columns that should not be modified
    special_cols = ["~id", "~label", "~from", "~to"]

    # Rename columns to ensure they have Neptune type suffixes if they don't already
    renamed_cols = {}
    for col in df.columns:
        # Skip special columns and columns that already have type information
        if col in special_cols or ":" in col:
            continue

        # Add default type suffix based on data type
        if pd.api.types.is_integer_dtype(df[col]):
            renamed_cols[col] = f"{col}:Int"
        elif pd.api.types.is_float_dtype(df[col]):
            renamed_cols[col] = f"{col}:Float"
        else:
            renamed_cols[col] = f"{col}:String"

    # Apply the renames if any
    if renamed_cols:
        df = df.rename(columns=renamed_cols)

    # Split the DataFrame into chunks
    df_chunks = split_dataframe(df, num_chunks)

    # Configure CSV writing options
    csv_options = pa_csv.WriteOptions(
        include_header=True,
        delimiter=",",
        quoting_style="needed",
    )

    def write_df_chunk(chunk_idx, chunk_df):
        # Convert DataFrame to PyArrow Table, dropping the index
        table = pa.Table.from_pandas(chunk_df.reset_index(drop=True))
        # Create the output path for this chunk with .csv extension
        chunk_path = f"{base_prefix}_{chunk_idx}.csv"

        if chunk_path.startswith("s3://"):
            # Extract the region from environment or use a default
            region = os.environ.get("AWS_REGION", "us-east-1")

            # Create S3 filesystem
            s3_fs = pa_fs.S3FileSystem(region=region)

            # Remove the s3:// prefix for PyArrow
            s3_path = chunk_path.replace("s3://", "")

            # Write directly to S3 using PyArrow's filesystem
            with s3_fs.open_output_stream(s3_path) as f:
                pa_csv.write_csv(table, f, write_options=csv_options)
        else:
            # For local files, still use PyArrow for consistency and performance
            with pa_fs.LocalFileSystem().open_output_stream(chunk_path) as f:
                pa_csv.write_csv(table, f, write_options=csv_options)

        logging.debug("Successfully wrote chunk to %s", chunk_path)

    # Configure parallel processing
    cpu_count = min(os.cpu_count() or 8, 16)

    with Parallel(n_jobs=cpu_count, prefer="threads", verbose=0) as parallel:
        parallel(
            delayed(write_df_chunk)(i, chunk_df) for i, chunk_df in enumerate(df_chunks)
        )

    logging.debug("Successfully wrote %d chunks for %s", len(df_chunks), base_prefix)


def get_features_and_labels(
    transactions_df: pd.DataFrame,
    transactions_id_cols: str,
    transactions_cat_cols: str,
    output_prefix: str,
    num_chunks: int = 16,
) -> None:
    """Creates transaction vertex files, processing features and labels.

    Parameters
    ----------
    transactions_df : pd.DataFrame
        DataFrame that contains all transactions
    transactions_id_cols : str
        A comma-separated string of all id columns that exist in the transactions df
    transactions_cat_cols : str
        A comma-separated string of all columns that contain categorical data in the transactions df
    output_prefix : str
        Output destination for the files.
    num_chunks : int, optional
        Number of chunks to split data into for parallel processing, by default 16
    """
    # Create a base DataFrame with ID and label columns
    logging.info("Creating transaction vertices...")
    transaction_vertices = pd.DataFrame(
        {
            "~id": transactions_df["TransactionID"].astype(str),
            "~label": "Transaction",
            "isFraud:Int": transactions_df["isFraud"],
            "TransactionDT:String": transactions_df["TransactionDT"].astype(str),
        }
    )

    # Process feature columns efficiently
    non_feature_cols = [
        "isFraud",
        "TransactionDT",
        "TransactionID",
    ] + transactions_id_cols.split(",")
    feature_cols = [
        col for col in transactions_df.columns if col not in non_feature_cols
    ]
    cat_cols = transactions_cat_cols.split(",")

    # Create a dictionary with all features at once
    feature_dict = {}
    for col in feature_cols:
        if col in cat_cols:
            feature_dict[f"{col}:String"] = transactions_df[col]
        else:
            # Check if the column contains integer values
            if pd.api.types.is_integer_dtype(transactions_df[col]):
                feature_dict[f"{col}:Int"] = transactions_df[col]
            else:
                feature_dict[f"{col}:Float"] = transactions_df[col]

    # Add all features to the transaction_vertices DataFrame in one operation
    if feature_dict:
        features_df = pd.DataFrame(feature_dict)
        transaction_vertices = pd.concat([transaction_vertices, features_df], axis=1)

    logging.info(f"Writing transaction vertices to {num_chunks} files...")
    write_compatible_csv(
        transaction_vertices,
        os.path.join(output_prefix, "Vertex_Transaction"),
        num_chunks=num_chunks,
    )


def get_relations_and_edgelist(
    transactions_df: pd.DataFrame,
    identity_df: pd.DataFrame,
    transactions_ids_str: str,
    output_dir: str,
    num_chunks: int = 16,
):
    """Create vertex files and corresponding edge list files for node types other than Transaction.

    Specifically, creates vertex files for node types:
        * Card[1-6]
        * Product code (ProductCD)
        * Address[1-2]
        * EmailDomain (recipient and purchaser)

    And corresponding relation files for the ``Transaction,identified_by,<identifier-type>``
    edge triples.

    Each entity type is split into multiple files to ease parallel processing downstream.

    Parameters
    ----------
    transactions_df : pd.DataFrame
        DataFrame that contains features for all transactions
    identity_df : pd.DataFrame
        DataFrame that contains identity information for transactions
    transactions_ids_str : str
        Comma-separated string of the columns that can be used to
        identify transactions.
    output_dir : str
        Path under which we will create the output files.
    num_chunks : int, optional
        Number of chunks to split data into for parallel processing, by default 16
    """
    # Split the id columns string into a list
    id_cols = transactions_ids_str.split(",")

    # Ensure all ID columns are present in the transactions DF
    for col in id_cols:
        if col not in transactions_df.columns:
            logging.warning(f"ID column '{col}' not found in transactions table")

    # Create vertices for each card type
    logging.info("Creating vertices for card types")
    for card_num in range(1, 7):  # card1 through card6
        card_vertices = pd.DataFrame()
        card_vertices["~id"] = f"card{card_num}:" + transactions_df[
            f"card{card_num}"
        ].astype(str)
        card_vertices["~label"] = f"Card{card_num}"
        card_vertices = card_vertices.drop_duplicates()
        write_compatible_csv(
            card_vertices,
            os.path.join(output_dir, f"Vertex_Card{card_num}"),
            num_chunks=num_chunks,
        )

    # Create vertices for ProductCD
    logging.info("Creating vertices for ProductCD")
    productcd_vertices = pd.DataFrame()
    productcd_vertices["~id"] = "ProductCD:" + transactions_df["ProductCD"].astype(str)
    productcd_vertices["~label"] = "ProductCD"
    productcd_vertices = productcd_vertices.drop_duplicates()
    write_compatible_csv(
        productcd_vertices,
        os.path.join(output_dir, "Vertex_ProductCD"),
        num_chunks=num_chunks,
    )

    # Create vertices for addresses
    logging.info("Creating vertices for addresses")
    for addr_num in range(1, 3):  # addr1 and addr2
        addr_vertices = pd.DataFrame()
        addr_vertices["~id"] = f"addr{addr_num}:" + transactions_df[
            f"addr{addr_num}"
        ].astype(str)
        addr_vertices["~label"] = f"Address{addr_num}"
        addr_vertices = addr_vertices.drop_duplicates()
        write_compatible_csv(
            addr_vertices,
            os.path.join(output_dir, f"Vertex_Address{addr_num}"),
            num_chunks=num_chunks,
        )

    # Create vertices for email domains
    logging.info("Creating vertices for email domains")
    for email_type in ["P_emaildomain", "R_emaildomain"]:
        email_vertices = pd.DataFrame()
        email_vertices["~id"] = f"{email_type}:" + transactions_df[email_type].astype(
            str
        )
        email_vertices["~label"] = email_type
        email_vertices = email_vertices.drop_duplicates()
        write_compatible_csv(
            email_vertices,
            os.path.join(output_dir, f"Vertex_{email_type}"),
            num_chunks=num_chunks,
        )

    # Create edges
    logging.info("Creating edges for identity columns")
    id_cols: list[str] = ["TransactionID"] + transactions_ids_str.split(",")
    full_identity_df = transactions_df[id_cols].merge(
        identity_df, on="TransactionID", how="left"
    )

    #  Replace NaN values in the Devices columns with a meaningful placeholder
    # Count NaN values before replacement
    nan_count = full_identity_df["DeviceType"].isna().sum()

    # Replace NaN values
    full_identity_df["DeviceType"] = full_identity_df["DeviceType"].fillna(
        MISSING_DEVICE
    )

    logging.debug(f"Replaced {nan_count} NaN values with {MISSING_DEVICE}")

    # Create other edges based on identity columns
    edge_types = transactions_ids_str.split(",")
    for etype in edge_types:
        edges = pd.DataFrame()
        edges["~id"] = (
            "transaction_"
            + full_identity_df["TransactionID"].astype(str)
            + f"-identified-by-{etype}"
        )
        edges["~from"] = full_identity_df["TransactionID"].astype(str)
        edges["~to"] = f"{etype}:" + full_identity_df[etype].astype(str)

        # Map the edge type to the correct vertex label
        if etype.startswith("card"):
            # Convert card1 -> Card1, card2 -> Card2, etc.
            vertex_type = f"Card{etype[4:]}"
        elif etype == "ProductCD":
            vertex_type = "ProductCD"
        elif etype.startswith("addr"):
            # Convert addr1 -> Address1, addr2 -> Address2
            vertex_type = f"Address{etype[4:]}"
        elif etype.endswith("emaildomain"):
            vertex_type = etype
        else:
            # Default case
            vertex_type = etype.capitalize()

        associated_edge_label = f"Transaction,identified_by,{vertex_type}"
        logging.info("Creating '%s' edges", associated_edge_label)

        edges["~label"] = associated_edge_label
        edges = edges.drop_duplicates().dropna()
        write_compatible_csv(
            edges,
            os.path.join(output_dir, f"Edge_{associated_edge_label}"),
            num_chunks=num_chunks,
        )


def create_neptune_data(
    output_prefix: str,
    data_prefix: str,
    id_cols: str,
    cat_cols: str,
    train_data_ratio: float = 0.8,
    transactions_fname: str = "transaction.csv",
    identity_fname: str = "identity.csv",
    num_chunks: int = 16,
):
    """Create Neptune-compatible data files from raw transaction and identity data.

    Parameters
    ----------
    output_prefix : str
        Prefix for output files
    data_prefix : str
        Prefix for input data files
    id_cols : str
        Comma-separated list of ID columns
    cat_cols : str
        Comma-separated list of categorical columns
    train_data_ratio : float, optional
        Ratio of data to use for training, by default 0.8
    transactions_fname : str, optional
        Name of transactions file, by default "transaction.csv"
    identity_fname : str, optional
        Name of identity file, by default "identity.csv"
    num_chunks : int, optional
        Number of chunks to split data into for parallel processing, by default 16

    Returns
    -------
    None
        Files are written to the specified output_prefix
    """
    graph_path = os.path.join(output_prefix, "graph_data")
    splits_path = os.path.join(graph_path, "data_splits")

    if not output_prefix.startswith("s3://"):
        logging.info("Creating output directories '%s' if needed", output_prefix)
        os.makedirs(splits_path, exist_ok=True)
        os.makedirs(graph_path, exist_ok=True)

    logging.info("Loading data and creating train/val/test splits for transactions")
    transactions_df, identity_df = load_data_and_create_splits(
        data_prefix,
        transactions_fname,
        identity_fname,
        train_data_ratio,
        splits_path,
    )

    logging.info(
        f"Creating Neptune-compatible data with {num_chunks} chunks per entity type"
    )

    get_features_and_labels(
        transactions_df,
        id_cols,
        cat_cols,
        graph_path,
        num_chunks,
    )

    get_relations_and_edgelist(
        transactions_df,
        identity_df,
        id_cols,
        graph_path,
        num_chunks,
    )

    logging.info("Created Neptune-formatted data under %s", graph_path)
    logging.info("Created train/val/test split data under %s", splits_path)
    print("Converted raw graph data into Neptune Analytics export format!")


if __name__ == "__main__":
    processor_args = DataProcessorArgs(**vars(parse_args()))

    create_neptune_data(
        processor_args.output_prefix,
        processor_args.data_prefix,
        processor_args.id_cols,
        processor_args.cat_cols,
        processor_args.train_data_ratio,
        processor_args.transactions_fname,
        processor_args.identity_fname,
        num_chunks=16,  # Default to 16 chunks for parallel processing
    )
