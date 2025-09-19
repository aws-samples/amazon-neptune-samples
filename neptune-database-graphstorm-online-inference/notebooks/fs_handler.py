"""
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0

FileSystemHandler that provides a high-level interface for file
operations on local and S3 files.
"""

import json
import logging
import os
import re
import tempfile
from pathlib import Path
from typing import Any, Optional, Union

import boto3
import pandas as pd
import pyarrow as pa
from botocore.config import Config
from pyarrow import csv as pacsv
from pyarrow import dataset as ds
from pyarrow import fs as pa_fs


# See https://arrow.apache.org/docs/python/pandas.html#nullable-types
# for Pyarrow-Pandas interactions for nullable types
PA_TO_PD_DTYPES = {
    pa.int8(): pd.Int8Dtype(),
    pa.int16(): pd.Int16Dtype(),
    pa.int32(): pd.Int32Dtype(),
    pa.int64(): pd.Int64Dtype(),
    pa.uint8(): pd.UInt8Dtype(),
    pa.uint16(): pd.UInt16Dtype(),
    pa.uint32(): pd.UInt32Dtype(),
    pa.uint64(): pd.UInt64Dtype(),
    pa.bool_(): pd.BooleanDtype(),
    pa.float32(): pd.Float32Dtype(),
    pa.float64(): pd.Float64Dtype(),
    pa.string(): pd.StringDtype(),
}

NA_TYPES_TO_NULLABLE = {
    "Byte": pd.Int8Dtype(),
    "Short": pd.Int16Dtype(),
    "Int": pd.Int32Dtype(),
    "Long": pd.Int64Dtype(),
    "Float": pd.Float32Dtype(),
    "Double": pd.Float64Dtype(),
    "String": pd.StringDtype(),
    "String[]": pd.StringDtype(),
    "Date": pd.StringDtype(),
    "Vector": pd.ArrowDtype(pa.large_list(pa.float32())),
}

NA_TYPES_TO_PA = {
    "Byte": pa.int8(),
    "Short": pa.int16(),
    "Int": pa.int32(),
    "Long": pa.int64(),
    "Float": pa.float32(),
    "Double": pa.float64(),
    "String": pa.string(),
    "String[]": pa.string(),
    "Date": pa.string(),
    "Vector": pa.large_list(pa.float32()),
}


class FileSystemHandler:
    """Handler for both local and S3 filesystem operations."""

    def __init__(self, data_prefix: str):
        """Initialize filesystem handler based on path type.

        Parameters
        ----------
        path : str
            Either a local path or s3:// URL
        """
        self.path = data_prefix
        self.is_s3 = data_prefix.startswith("s3://")

        if self.is_s3:
            # Extract bucket and prefix from s3 path
            self.bucket = data_prefix.split("/")[2]
            self.key_prefix = "/".join(data_prefix.split("/")[3:])
            logging.debug("S3 bucket: %s", self.bucket)
            logging.debug("S3 prefix: %s", self.key_prefix)

            # Initialize S3 boto client for multi-threaded operations
            cpu_count = max(os.cpu_count() or 8, 32)
            max_pool_connections = cpu_count * 2
            config = Config(
                max_pool_connections=max_pool_connections, retries=dict(max_attempts=10)
            )
            self.boto_s3 = boto3.client("s3", config=config)

            # For pyarrow operations, use S3FileSystem
            response = self.boto_s3.head_bucket(Bucket=self.bucket)
            region = response["ResponseMetadata"]["HTTPHeaders"]["x-amz-bucket-region"]

            s3_metadata = None
            # Initialize S3 filesystem
            self.pa_fs = pa_fs.S3FileSystem(
                region=region, anonymous=False, default_metadata=s3_metadata
            )
        else:
            # Use local filesystem
            self.pa_fs = pa_fs.LocalFileSystem()

    def list_files(
        self, pattern: Optional[str] = None, path: Optional[str] = None
    ) -> list[str]:
        """list files in self.path, optionally matching a pattern.

        Parameters
        ----------
        pattern : str, optional
            Regex pattern to match filenames
        path: str, optional
            Alternative path to use instead of self.path

        Returns
        -------
        list[str]
            list of matching file paths
        """
        # TODO: not super happy with path being optional and using self.path
        # Let's find a stricter solution that works for both
        if path:
            filepath = path
        else:
            filepath = self.path

        def natsort(input):
            return [
                int(s) if s.isdigit() else s.lower() for s in re.split(r"(\d+)", input)
            ]

        if filepath.startswith("s3://"):
            bucket = filepath.split("/")[2]
            prefix = "/".join(filepath.split("/")[3:])
            # Use S3 pagination to handle many files
            files = []
            logging.debug("Listing S3 files under %s", f"s3://{bucket}/{prefix}")
            paginator = self.boto_s3.get_paginator("list_objects_v2")

            for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
                if "Contents" in page:
                    for obj in page["Contents"]:
                        key = obj.get("Key", None)
                        match = None
                        if key:
                            if pattern is not None:
                                match = re.match(pattern, key.split("/")[-1])
                            if pattern is None or match:
                                files.append(f"s3://{bucket}/{key}")
            return sorted(files, key=natsort)
        else:
            # Use pathlib for local files
            path_obj = Path(self.path)
            if pattern:
                return [str(f) for f in path_obj.glob(f"*{pattern}*")]
            return sorted([str(f) for f in path_obj.glob("*")], key=natsort)

    def create_pa_dataset_from_files(
        self,
        file_paths: Union[str, list[str], Path],
        external_schema: Optional[pa.Schema] = None,
    ):
        """Read parquet or csv file(s) into a PyArrow Table.

        Parameters
        ----------
        file_paths : str | list[str] | Path
            Path(s) to parquet or csv file(s)

        Returns
        -------
        pyarrow.dataset.DataSet
            Contents of file(s) as a PyArrow Dataset
        """
        if not isinstance(file_paths, list):
            file_paths = [str(file_paths).replace("s3://", "")]
        if self.is_s3:
            file_paths = [fp.replace("s3://", "") for fp in file_paths]

        def read_dataset(path_list, inner_schema=None):
            # Read CSV or parquet to a Pandas DF
            if file_paths[0].endswith(".csv"):
                # Create CSV read options for multiline support
                parse_options = pacsv.ParseOptions(
                    newlines_in_values=True, quote_char='"', double_quote=True
                )
                csv_format = ds.CsvFileFormat(parse_options=parse_options)
                # Use the options in dataset creation
                dataset = ds.dataset(
                    file_paths,
                    filesystem=self.pa_fs,
                    format=csv_format,
                    schema=inner_schema,
                )
            else:
                file_format = "parquet"
                dataset = ds.dataset(
                    file_paths,
                    filesystem=self.pa_fs,
                    format=file_format,
                    schema=inner_schema,
                )

            return dataset

        # Read dataset and fix schema if needed
        raw_dataset = read_dataset(file_paths, external_schema)
        fixed_schema = self.fix_schema(raw_dataset.schema)

        # If fixes are needed to the schema, we need to create a new dataset instance
        if not fixed_schema.equals(raw_dataset.schema):
            logging.debug(
                "Schema fix needed for files [%s, ...]: %s",
                file_paths[0],
                json.dumps(
                    {
                        "original": str(raw_dataset.schema),
                        "fixed": str(fixed_schema),
                    },
                    indent=4,
                ),
            )
            raw_dataset = read_dataset(file_paths, fixed_schema)

        return raw_dataset

    @staticmethod
    def fix_schema(input_schema: pa.Schema) -> pa.Schema:
        """Fix schema in case the input is Neptune Analytics exported files

        Parameters
        ----------
        schema : pa.Schema
            Raw schema as inferred by PyArrow

        Returns
        -------
        pa.Schema
            Fixed schema where we enforce the Neptune types from the header
        """
        new_fields = []
        for field in input_schema:
            # If field came from Neptune
            name_parts = field.name.split(":")
            if len(name_parts) == 2 and name_parts[1] in NA_TYPES_TO_PA:
                # Fix the name and type
                new_fields.append(
                    pa.field(
                        field.name,
                        NA_TYPES_TO_PA[name_parts[1]],
                        field.nullable,
                        field.metadata,
                    )
                )
            else:
                new_fields.append(
                    pa.field(
                        field.name,
                        field.type,
                        field.nullable,
                        field.metadata,
                    )
                )

        return pa.schema(new_fields)

    def create_pa_table_from_files(
        self,
        file_paths: Union[str, list[str], Path],
        schema: Optional[pa.Schema] = None,
    ) -> pa.Table:
        """Read parquet or csv file(s) into a PyArrow Table.

        Parameters
        ----------
        file_paths : str | list[str] | Path
            Path(s) to parquet or csv file(s)

        Returns
        -------
        pyarrow.Table
            Contents of file(s) as a PyArrow Table
        """
        return self.create_pa_dataset_from_files(file_paths, schema).to_table()

    def create_df_from_files(
        self,
        file_paths: Union[str, list[str], Path],
        schema: Optional[pa.Schema] = None,
    ) -> pd.DataFrame:
        """Read CSV file(s) into a pandas DataFrame, falling back to pandas if PyArrow fails.

        Parameters
        ----------
        file_paths : str | list[str] | Path
            Path(s) to CSV file(s)
        schema : Optional[pa.Schema]
            PyArrow schema if available

        Returns
        -------
        pd.DataFrame
            Contents of file(s) as a Pandas DataFrame
        """
        # Convert file paths to list if needed
        if not isinstance(file_paths, list):
            file_paths = [str(file_paths)]

        try:
            # First try with PyArrow
            s3_files = [f.replace("s3://", "") for f in file_paths]
            return self._create_df_from_files_pa(s3_files, schema)
        except Exception as e:
            logging.debug(
                "PyArrow reading failed with error: %s. Falling back to pandas", e
            )

            # Initialize empty list to store dataframes
            dfs = []

            for file_path in file_paths:
                # Read file directly with pandas (works for both local and s3 paths)
                df = pd.read_csv(
                    file_path,
                    dtype=str,  # Read everything as string first
                    na_values=["", "nan", "NaN", "NULL", "null", "None", "none"],
                    keep_default_na=True,
                )
                dfs.append(df)

            # Combine all dataframes
            final_df = pd.concat(dfs, ignore_index=True) if len(dfs) > 1 else dfs[0]

            # If we have a schema, try to convert types according to it
            if schema:
                for field in schema:
                    col_name = field.name
                    if col_name in final_df.columns:
                        try:
                            # Convert PyArrow type to pandas type
                            if pa.types.is_integer(field.type):
                                final_df[col_name] = pd.to_numeric(
                                    final_df[col_name], errors="coerce"
                                ).astype(PA_TO_PD_DTYPES.get(field.type))
                            elif pa.types.is_floating(field.type):
                                final_df[col_name] = pd.to_numeric(
                                    final_df[col_name], errors="coerce"
                                ).astype(PA_TO_PD_DTYPES.get(field.type))
                            elif pa.types.is_boolean(field.type):
                                final_df[col_name] = final_df[col_name].astype(
                                    PA_TO_PD_DTYPES.get(field.type)
                                )
                            elif pa.types.is_string(field.type):
                                final_df[col_name] = final_df[col_name].astype(
                                    PA_TO_PD_DTYPES.get(field.type)
                                )
                        except Exception as type_error:
                            logging.warning(
                                "Could not convert column %s to type %s: %s",
                                col_name,
                                field.type,
                                type_error,
                            )
            else:
                # Try to infer types intelligently
                for column in final_df.columns:
                    try:
                        # Try converting to numeric, keeping NaN values
                        numeric_series = pd.to_numeric(
                            final_df[column], errors="coerce"
                        )

                        # Check if we have any non-null values
                        if numeric_series.notna().any():
                            # Check if all non-null values are integers
                            non_null_values = numeric_series[numeric_series.notna()]
                            if (non_null_values == non_null_values.astype(int)).all():
                                final_df[column] = numeric_series.astype(
                                    pd.Int64Dtype()
                                )
                            else:
                                final_df[column] = numeric_series.astype(
                                    pd.Float64Dtype()
                                )
                        else:
                            # Keep as string if conversion to numeric failed
                            final_df[column] = final_df[column].astype(pd.StringDtype())
                    except Exception as e:
                        # If type inference fails, keep as string
                        final_df[column] = final_df[column].astype(pd.StringDtype())

            return final_df

    def _create_df_from_files_pa(
        self,
        file_paths: Union[str, list[str], Path],
        schema: Optional[pa.Schema] = None,
    ) -> pd.DataFrame:
        """Read parquet or csv file(s) into a pandas DataFrame using pyarrow

        Parameters
        ----------
        file_paths : str | list[str] | Path
            Path(s) to parquet or csv file(s)

        Returns
        -------
        pd.DataFrame
            Contents of file(s) as a Pandas DF
        """
        return self.create_pa_table_from_files(file_paths, schema).to_pandas(
            types_mapper=PA_TO_PD_DTYPES.get
        )

    def read_json(self, file_path: str) -> dict:
        """Read a JSON file.

        Parameters
        ----------
        file_path : str
            Path to JSON file

        Returns
        -------
        dict
            Contents of JSON file
        """
        if self.is_s3:
            # Try to download file from S3
            bucket = file_path.split("/")[2]
            key = "/".join(file_path.split("/")[3:])

            # Create a temporary file that won't be deleted immediately
            temp_file = tempfile.NamedTemporaryFile(delete=False)
            temp_file.flush()
            temp_path = temp_file.name
            temp_file.close()  # Close the file but don't delete it

            try:
                self.boto_s3.download_file(
                    bucket,
                    key,
                    temp_path,
                )

                with open(temp_path, "r", encoding="utf-8") as f:
                    return json.load(f)
            finally:
                # Clean up the temporary file after we're done with it
                os.unlink(temp_path)
        else:
            # For local files, just open directly
            with open(file_path, "r", encoding="utf-8") as f:
                return json.load(f)

    @staticmethod
    def create_pyarrow_schema_from_neptune(
        neptune_schema: dict[str, Any], node_type: Optional[str] = None
    ) -> pa.Schema:
        """Create a PyArrow schema from Neptune graph schema for a specific node type or all edges.

        Parameters
        ----------
        neptune_schema : Dict[str, Any]
            The Neptune graph schema as returned by neptune.graph.pg_schema()
        node_type : str, optional
            The specific node type to create schema for. If None, assumes we're
            processing edge data.

        Returns
        -------
        pa.Schema
            PyArrow schema for the specified node/edge type
        """
        # Neptune to PyArrow type mapping
        NEPTUNE_TO_ARROW = {
            "String": pa.string(),
            "Int": pa.int64(),
            "Long": pa.int64(),
            "Float": pa.float32(),
            "Double": pa.float64(),
            "Boolean": pa.bool_(),
            "Date": pa.timestamp("ms"),  # TODO: Is this the correct type?
            "Vector": pa.list_(pa.float32()),  # TODO: Use large_list?
        }

        def get_arrow_type(datatypes: list[str]) -> pa.DataType:
            """Determine the appropriate Arrow type based on Neptune datatypes."""
            if not datatypes:
                return pa.string()  # Default to string for unknown types

            # If multiple datatypes are present, use the most permissive one
            if "Vector" in datatypes:
                return NEPTUNE_TO_ARROW["Vector"]
            if "String" in datatypes:
                return NEPTUNE_TO_ARROW["String"]
            if "Double" in datatypes:
                return NEPTUNE_TO_ARROW["Double"]
            if "Float" in datatypes:
                return NEPTUNE_TO_ARROW["Float"]
            if "Long" in datatypes:
                return NEPTUNE_TO_ARROW["Long"]
            if "Int" in datatypes:
                return NEPTUNE_TO_ARROW["Int"]

            # Default to the first type we can map
            for dtype in datatypes:
                if dtype in NEPTUNE_TO_ARROW:
                    return NEPTUNE_TO_ARROW[dtype]

            return pa.string()  # Fallback

        fields = []

        # Always add the ID field
        fields.append(pa.field("~id", pa.string()))

        if node_type:
            # Processing node data
            if node_type not in neptune_schema["nodeLabelDetails"]:
                raise ValueError(f"Node type '{node_type}' not found in schema")

            node_details = neptune_schema["nodeLabelDetails"][node_type]
            if "properties" in node_details:
                for prop_name, prop_details in node_details["properties"].items():
                    arrow_type = get_arrow_type(prop_details["datatypes"])
                    # Add the property type suffix as used in Neptune Analytics
                    field_name = f"{prop_name}:{prop_details['datatypes'][0]}"
                    fields.append(pa.field(field_name, arrow_type, nullable=True))

        else:
            # Processing edge data
            # Add standard edge fields
            fields.extend(
                [
                    pa.field("~from", pa.string()),
                    pa.field("~to", pa.string()),
                    pa.field("~label", pa.string()),
                ]
            )

            # Add edge properties from all edge types
            # This creates a superset of all possible edge properties
            edge_properties = {}
            for edge_details in neptune_schema["edgeLabelDetails"].values():
                if "properties" in edge_details:
                    for prop_name, prop_details in edge_details["properties"].items():
                        if prop_name not in edge_properties:
                            edge_properties[prop_name] = prop_details["datatypes"]
                        else:
                            # Merge datatypes if property appears in multiple edge types
                            edge_properties[prop_name].extend(prop_details["datatypes"])
                            edge_properties[prop_name] = list(
                                set(edge_properties[prop_name])
                            )

            # Add edge properties to schema
            for prop_name, datatypes in edge_properties.items():
                arrow_type = get_arrow_type(datatypes)
                # Add the property type suffix as used in Neptune Analytics
                field_name = f"{prop_name}:{datatypes[0]}"
                fields.append(pa.field(field_name, arrow_type, nullable=True))

        return pa.schema(fields)
