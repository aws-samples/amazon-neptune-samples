"""
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0

In this script we read the original
Neptune Analytics from S3 using pyarrow,
join on the ~id column of the nodes with
the embeddings/predictions that GraphStorm generated
(also on S3) and write back the graph data
to another location.

We do this for every node type

For the files that start with Edge_ we'll
just copy those from the source to the destination.
"""

import argparse
import logging
import os
from dataclasses import dataclass
from io import BytesIO
from typing import Literal, Optional, Union

import pandas as pd
import pyarrow as pa
from joblib import Parallel, delayed
from pyarrow import parquet as pq
from pyarrow import csv as pacsv

from neptune_gs.fs_handler import NA_TYPES_TO_NULLABLE, FileSystemHandler

# External Constants
GS_REMAP_NID_COL = "nid"
GS_REMAP_EMBED_COL = "emb"
GS_PRED_COL = "pred"
NA_CSV_EMBED_COL = "embedding:Vector"
NA_LABEL_COL = "~label"
NA_NID_COL = "~id"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Join Neptune Analytics exported data with GraphStorm data"
    )
    parser.add_argument(
        "--input",
        "-i",
        type=str,
        required=True,
        help="S3 prefix for original data exported from Neptune Analytics",
    )
    parser.add_argument(
        "--output",
        "-o",
        type=str,
        required=True,
        help="S3 prefix for output Neptune+GraphStorm enriched data",
    )
    parser.add_argument(
        "--embeddings",
        "-e",
        type=str,
        required=True,
        help="S3 prefix for GraphStorm embeddings to enrich Neptune data with.",
    )
    parser.add_argument(
        "--predictions",
        "-p",
        type=str,
        required=False,
        help="S3 prefix for GraphStorm predictions to enrich Neptune data with. "
        "Optional.",
    )
    parser.add_argument(
        "--kms-key-id",
        type=str,
        help="KMS key ID for S3 server-side encryption",
    )
    return parser.parse_args()


def validate_s3_paths(*paths: str) -> None:
    """Validate that all provided paths are S3 URIs."""
    for path in paths:
        if not path.startswith("s3://"):
            raise ValueError(f"Expected an S3 URI, got {path}")


def read_gs_data(
    handler: FileSystemHandler,
    gs_path: str,
    node_type: str,
    gs_data_kind: Literal["embeddings", "predictions"],
) -> pd.DataFrame:
    """Read embeddings or predictions for a specific node type."""
    if gs_data_kind == "embeddings":
        filename_pattern = r"embed-[0-9]{5}_[0-9]{5}\.parquet$"
    elif gs_data_kind == "predictions":
        filename_pattern = r"pred.predict-[0-9]{5}_[0-9]{5}\.parquet$"
    else:
        raise ValueError(f"Unknown GS data format: {gs_data_kind}")

    gs_files = handler.list_files(
        path=os.path.join(gs_path, node_type),
        pattern=filename_pattern,
    )
    assert len(gs_files) > 0, f"No {gs_data_kind} found for node type '{node_type}'"
    logging.info("Reading %s for node type '%s'", gs_data_kind, node_type)
    return handler.create_df_from_files(gs_files)


def attach_gs_cols_to_type(
    node_type: str,
    input_handler: FileSystemHandler,
    embedding_handler: FileSystemHandler,
    output_s3: str,
    node_files: list[str],
    predictions_handler: Optional[FileSystemHandler] = None,
    use_threads=True,
    kms_key_id: Optional[str] = None,
) -> None:
    """Process a single node type: read data, join GraphStorm columns, and write results."""
    # Read embeddings
    embeddings = read_gs_data(
        embedding_handler, embedding_handler.path, node_type, "embeddings"
    )
    embeddings.set_index(GS_REMAP_NID_COL, inplace=True)
    # Extend embeddings with predictions if present
    if predictions_handler:
        predictions = read_gs_data(
            predictions_handler, predictions_handler.path, node_type, "predictions"
        )
        if predictions.shape[0] != embeddings.shape[0]:
            raise RuntimeError(
                "Embeddings and predictions have different number of rows: "
                f"{embeddings.shape[0]:,} != {predictions.shape[0]:,} respectivelly"
            )
        predictions.set_index(GS_REMAP_NID_COL, inplace=True)
        embeddings = embeddings.join(predictions, how="left")

    # Read and process node data, using threads optionally
    # Get the minimum between cpu_count and 16. Use 8 if cpu_count returns None
    cpu_count = min(os.cpu_count() or 8, 16) if use_threads else 1
    logging.info("Reading original Neptune data for node type '%s'", node_type)
    fields = []
    # Always add the ID field
    fields.extend([pa.field("~id", pa.string()), pa.field("~label", pa.string())])
    ntype_schema = pa.schema(fields)
    with Parallel(n_jobs=cpu_count, prefer="threads", verbose=1) as parallel:
        original_node_dfs = parallel(
            delayed(input_handler.create_df_from_files)(node_file, ntype_schema)
            for node_file in node_files
        )

    # If node DF does not have ~label col include that
    for original_df in original_node_dfs:
        assert isinstance(original_df, pd.DataFrame)
        if NA_LABEL_COL not in original_df.columns:
            original_df[NA_LABEL_COL] = node_type
    assert isinstance(original_node_dfs, list)

    # Join GS columns into node data, creating a list of joined DFs
    logging.info(
        "Joining Neptune data with GraphStorm data for node type '%s'", node_type
    )
    with Parallel(n_jobs=cpu_count, prefer="threads", verbose=1) as parallel:
        joined = parallel(
            delayed(join_and_process_embeddings)(
                original_node_df, embeddings, node_type
            )
            for original_node_df in original_node_dfs
        )
    assert isinstance(joined, list)
    # Write results
    write_joined_data(
        joined, output_s3, node_type, use_threads=use_threads, kms_key_id=kms_key_id
    )


def prepare_node_ids(
    original_nodes: pd.DataFrame, embeddings: pd.DataFrame
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Ensure node IDs are in compatible format for joining."""
    try:
        # Check if GS_REMAP_NID_COL is the index
        if embeddings.index.name == GS_REMAP_NID_COL:
            # Reset index to make it a regular column
            embeddings = embeddings.reset_index()

        if original_nodes[NA_NID_COL].dtypes != embeddings[GS_REMAP_NID_COL].dtype:
            logging.debug(
                "Node ID types do not match: %s != %s",
                original_nodes[NA_NID_COL].dtypes,
                embeddings[GS_REMAP_NID_COL].dtype,
            )
            mergeable_types = ["object", "string"]
            if original_nodes[NA_NID_COL].dtype not in mergeable_types:
                original_nodes[NA_NID_COL] = original_nodes[NA_NID_COL].astype(str)
                logging.debug("Converted original node IDs to string")
            if embeddings[GS_REMAP_NID_COL].dtype not in mergeable_types:
                embeddings[GS_REMAP_NID_COL] = embeddings[GS_REMAP_NID_COL].astype(str)
                logging.debug("Converted embeddings node IDs to string")
        return original_nodes, embeddings
    except KeyError as e:
        logging.error("Error getting column node IDs: %s", e)
        logging.error("Original cols: %s", original_nodes.columns)
        logging.error("Embedding cols: %s", embeddings.columns)
        raise e
    except Exception as e:
        logging.error("Error preparing node IDs: %s", e)
        raise e


def join_and_process_embeddings(
    original_nodes: pd.DataFrame, embeddings: pd.DataFrame, node_type: str
) -> pd.DataFrame:
    """Join node data with embeddings and format data for Neptune import."""

    # Make sure original_nodes[NA_NID_COL] is string type for joining
    if original_nodes[NA_NID_COL].dtype not in ["object", "string"]:
        original_nodes[NA_NID_COL] = original_nodes[NA_NID_COL].astype(str)

    # Make sure embeddings index is string type if it's the join column
    if embeddings.index.name != GS_REMAP_NID_COL:
        # Set index and convert it to string if needed
        embeddings.set_index(GS_REMAP_NID_COL, inplace=True)
    if embeddings.index.dtype not in ["object", "string"]:
        embeddings.index = embeddings.index.astype(str)

    # Join using the index
    joined = (
        original_nodes.set_index(NA_NID_COL).join(embeddings, how="left").reset_index()
    )
    joined.rename(columns={GS_REMAP_EMBED_COL: NA_CSV_EMBED_COL}, inplace=True)

    if joined[NA_CSV_EMBED_COL].isna().sum() > 0:
        logging.warning(
            "Found %s rows with missing embeddings for node type '%s'",
            joined[NA_CSV_EMBED_COL].isna().sum(),
            node_type,
        )

    # Convert embeddings to ;-delimited string format
    joined[NA_CSV_EMBED_COL] = joined[NA_CSV_EMBED_COL].apply(
        lambda x: ";".join(map(str, x)) if x is not None else ""
    )

    # Conditionally also convert predictions
    if GS_PRED_COL in joined.columns:
        joined[GS_PRED_COL] = joined[GS_PRED_COL].apply(
            lambda x: ";".join(map(str, x)) if x is not None else ""
        )
    # Rename pred col to pred:Float array
    joined.rename(columns={GS_PRED_COL: f"{GS_PRED_COL}:Float[]"}, inplace=True)

    # Convert other columns to correct types
    for col in joined.columns:
        if col in [NA_CSV_EMBED_COL, NA_NID_COL, NA_LABEL_COL]:
            continue
        if ":" in col:
            _, col_type = col.split(":")
            if col_type in NA_TYPES_TO_NULLABLE:
                joined[col] = joined[col].astype(NA_TYPES_TO_NULLABLE[col_type])

    return joined


def write_joined_data(
    dfs: Union[pd.DataFrame, list[pd.DataFrame]],
    output_s3: str,
    node_type: str,
    use_threads=False,
    file_format: Literal["csv", "parquet"] = "csv",
    chunk_size: int = 1_000_000,
    kms_key_id: Optional[str] = None,
) -> None:
    """Write the processed data to S3 with optimizations.

    Parameters
    ----------
    dfs : Union[pd.DataFrame, list[pd.DataFrame]]
        DataFrame(s) to write
    output_s3 : str
        S3 output path
    node_type : str
        Type of node being processed
    use_threads : bool
        Whether to use parallel processing
    file_format : str
        Output file format ('csv' or 'parquet')
    chunk_size : int
        Number of rows per chunk for large DataFrames.
        Adjust based on memory constraints.
    """
    if isinstance(dfs, pd.DataFrame):
        dfs = [dfs]
    assert isinstance(dfs, list)

    logging.info("Writing joined data to %s", output_s3)

    output_handler = FileSystemHandler(output_s3)

    def generate_chunks(
        dfs: list[pd.DataFrame], chunk_size: int
    ) -> list[tuple[pd.DataFrame, int]]:
        """Generate chunks from DataFrames.

        Parameters
        ----------
        dfs : list[pd.DataFrame]
            List of DataFrames to chunk
        chunk_size : int
            Size of each chunk

        Returns
        -------
        list
            List of (chunk DataFrame, chunk index) tuples
        """
        chunks = []
        chunk_counter = 0
        for df in dfs:
            if len(df) > chunk_size:
                num_chunks = (len(df) + chunk_size - 1) // chunk_size
                for _ in range(num_chunks):
                    start_idx = chunk_counter * chunk_size
                    end_idx = min((chunk_counter + 1) * chunk_size, len(df))
                    # Create a view with shallow copy
                    chunk_view = df.iloc[start_idx:end_idx].copy(deep=False)
                    chunks.append((chunk_view, chunk_counter))
                    chunk_counter += 1
            else:
                chunks.append((df, chunk_counter))
                chunk_counter += 1
        return chunks

    def write_df_chunk(chunk_data: tuple[pd.DataFrame, int]) -> None:
        cur_df, chunk_idx = chunk_data
        table = pa.Table.from_pandas(cur_df)

        # Get csv writing options
        csv_options = pacsv.WriteOptions(
            include_header=True,
            delimiter=",",
            quoting_style="needed",
        )
        out_prefix = os.path.join(
            output_s3.removeprefix("s3://"), f"Vertex_{node_type}_{chunk_idx}"
        )
        if file_format == "csv":
            outpath = out_prefix + ".csv"
        else:
            outpath = out_prefix + ".parquet"

        # If KMS encryption requested, write to buffer then upload to S3
        if kms_key_id:
            # Write to buffer first
            buffer = BytesIO()
            if file_format == "csv":
                pacsv.write_csv(table, buffer, write_options=csv_options)
            elif file_format == "parquet":
                pq.write_table(table, buffer)
            else:
                raise ValueError(f"Unsupported file format: {file_format}")

            buffer.seek(0)

            # Upload buffer to S3 with KMS encryption
            bucket = outpath.split("/")[0]
            key = "/".join(outpath.split("/")[1:])

            # TODO: Ensure region is correctly resolved here
            output_handler.boto_s3.put_object(
                Bucket=bucket,
                Key=key,
                Body=buffer.getvalue(),
                ServerSideEncryption="aws:kms",
                SSEKMSKeyId=kms_key_id,
            )
        else:
            # Write table to S3 directly
            if file_format == "csv":
                with output_handler.pa_fs.open_output_stream(outpath) as f:
                    pacsv.write_csv(table, f, write_options=csv_options)
            elif file_format == "parquet":
                pq.write_table(table, outpath, filesystem=output_handler.pa_fs)
            else:
                raise ValueError(f"Unknown file format: {file_format}")

    # Configure parallel processing
    cpu_count = min(os.cpu_count() or 8, 16) if use_threads else 1

    # Generate all chunks first to get proper progress reporting
    chunks_to_process = generate_chunks(dfs, chunk_size)
    total_chunks = len(chunks_to_process)

    with Parallel(n_jobs=cpu_count, prefer="threads", verbose=1) as parallel:
        parallel(
            delayed(write_df_chunk)(chunk_data) for chunk_data in chunks_to_process
        )

    logging.info("Finished writing %d files for node type %s", total_chunks, node_type)


def attach_gs_data_to_na(
    input_s3: str,
    output_s3: str,
    embeddings_s3: str,
    predictions_s3: Optional[str] = None,
    kms_key_id: Optional[str] = None,
):
    """Main function to attach GraphStorm embeddings and predictions to Neptune Analytics data.

    Parameters
    ----------
    input_s3 : str
        The original Neptune-exported data
    output_s3 : str
        The destination for the enriched data.
    embeddings_s3 : str
        The S3 prefix for GraphStorm-generated embeddings files.
    predictions_s3: Optional[str]
        The S3 prefix for GraphStorm-generated predictions files.
    kms_key_id: Optional[str]
        KMS key ID for S3 server-side encryption

    Raises
    ------
    ValueError
        If at least one of input_s3, output_s3, embeddings_s3 is not provided.
    """
    if not (embeddings_s3 and input_s3 and output_s3):
        raise ValueError(
            "Input, output and embeddings paths are required, got "
            f"{input_s3=}, {output_s3=}, {embeddings_s3=}"
        )

    logging.info(
        "Starting Neptune Analytics to GraphStorm embedding attachment process"
    )
    logging.info("Input path: %s", input_s3)
    logging.info("Output path: %s", output_s3)
    logging.info("Embeddings path: %s", embeddings_s3)
    if predictions_s3:
        logging.info("Predictions path: %s", predictions_s3)
    if kms_key_id:
        logging.info("Using KMS key ID: %s", kms_key_id)

    validate_s3_paths(
        input_s3,
        output_s3,
        embeddings_s3,
    )

    # Initialize handlers - only use KMS for output
    input_handler = FileSystemHandler(input_s3)
    embedding_handler = FileSystemHandler(embeddings_s3)
    predictions_handler = None
    if predictions_s3:
        validate_s3_paths(predictions_s3)
        predictions_handler = FileSystemHandler(predictions_s3)

    # Get node files and determine which node types have embeddings
    logging.info("Analyzing Neptune exported data...")
    emb_info = embedding_handler.read_json(os.path.join(embeddings_s3, "emb_info.json"))
    node_types_with_embeddings: list[str] = emb_info["emb_name"]

    node_types_with_predictions: set[str] = set()
    if predictions_s3 and predictions_handler:
        predictions_info = predictions_handler.read_json(
            os.path.join(predictions_s3, "result_info.json")
        )
        node_types_with_predictions = set(predictions_info["ntypes"])

    # Attach embeddings/predictions to exported data
    for node_type in node_types_with_embeddings:
        original_node_files = input_handler.list_files(
            pattern=f"Vertex_{node_type}_[0-9]+\\.(parquet|csv)$"
        )
        assert original_node_files, (
            f"Could not find any node files under {input_handler.path} "
            f"using pattern 'Vertex_{node_type}\\.(parquet|csv)$'"
        )
        # NOTE: We assume any node with predictions will also have embeddings
        type_predictions_handler = (
            predictions_handler if node_type in node_types_with_predictions else None
        )
        attach_gs_cols_to_type(
            node_type,
            input_handler,
            embedding_handler,
            output_s3,
            original_node_files,
            predictions_handler=type_predictions_handler,
            use_threads=True,
            kms_key_id=kms_key_id,
        )

    logging.info(f"Joined data written to {output_s3}")


@dataclass
class AttachGSDataArgs:
    """Dataclass to hold typed arguments"""

    input: str
    output: str
    embeddings: str
    predictions: Optional[str] = None
    kms_key_id: Optional[str] = None


def main():
    args = AttachGSDataArgs(**vars(parse_args()))
    logging.basicConfig(
        level=logging.INFO,
        force=True,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    attach_gs_data_to_na(
        args.input,
        args.output,
        args.embeddings,
        args.predictions,
        args.kms_key_id,
    )


if __name__ == "__main__":
    main()
