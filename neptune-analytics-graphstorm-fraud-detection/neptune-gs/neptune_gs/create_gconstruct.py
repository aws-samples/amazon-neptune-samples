"""
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
"""

import argparse
import ast
import json
import logging
import math
import os.path as osp
import re
from collections import defaultdict
from dataclasses import dataclass
from enum import Enum
from functools import partial
from pathlib import Path
from pprint import pformat, pp
from typing import (
    Any,
    Literal,
    Mapping,
    Optional,
    Sequence,
    TypedDict,
    Union,
    cast,
)

import boto3
import botocore
import botocore.client
import pandas as pd
from pandas import Float32Dtype, Float64Dtype, Int32Dtype, Int64Dtype, StringDtype

from neptune_gs.fs_handler import FileSystemHandler

NEPTUNE_CSV_SEP = ","

# TODO: include all transforms
AVAILABLE_TRANSFORMATIONS_STR = r"""
Available transformations:
    - max_min_norm: Normalize numeric values to [0,1] range
    - standard: Standardize numeric values
    - rank_gauss: Apply rank-based Gaussian normalization
    - to_categorical: Convert to categorical values
    - tokenize_hf: Apply HuggingFace tokenization
    - bert_hf: Generate BERT embeddings
"""
SUPPORTED_TASKS_TYPE = Literal["classification", "regression", "link_prediction"]

# Always flush print to get around Jupyter stream issues
print = partial(print, flush=True)


@dataclass
class NodeEdgeInfo:
    """Schema information of a single vertex or edge type."""

    properties: list[str]
    # Property name to nullable Pandas type (includes ~id, ~label etc.)
    pandas_dtypes: dict[str, str]
    # Property name to Neptune type (only includes properties)
    neptune_types: Optional[dict[str, str]] = None

    def __post_init__(self):
        # Ensure names match for pandas and neptune types
        if self.neptune_types:
            assert set(self.neptune_types.keys()).issubset(set(self.properties)), (
                f"Element of {self.neptune_types.keys()=} was not part of "
                f"{self.properties=}."
            )


class InputFormat(Enum):
    PARQUET = "parquet"
    CSV = "csv"


@dataclass
class GraphSchema:
    """Dataclass to hold graph schema information."""

    vertex_types: set[str]
    relation_types: set[str]
    # Mapping from node type to list of files
    vertex_files: dict[str, list[str]]
    # Mapping from edge type to list of files
    edge_files: dict[str, list[str]]
    # Mapping from node type to schema definition
    vertex_schemas: dict[str, NodeEdgeInfo]
    # Mapping from edge type to schema definition
    edge_schemas: dict[str, NodeEdgeInfo]
    # Format
    input_format: InputFormat


class GConstructFeature(TypedDict):
    """Schema for GConstruct feature entries"""

    feature_col: Union[str, list[str]]
    feature_name: str
    transform: dict[str, Any]


@dataclass
class TaskInfo:
    """Dataclass to hold learning task information.

    Parameters
    ----------
        learning_task : str
            GraphStorm task name to prepare data for, by default None.
            Can be one of:
            - classification
            - regression
            - link_prediction
        target_type : str
            Node/edge type to use as target type for the learning task., by default None
        task_column : str
            Column to use as labels for the learning task, by default None.
            For link prediction the column can be omitted."""

    learning_task: str
    target_type: str
    label_col: str

    def __post_init__(self):
        assert self.learning_task in [
            "classification",
            "regression",
            "link_prediction",
        ], f"Invalid learning task: {self.learning_task}"
        if self.learning_task == "link_prediction":
            assert self.label_col is None, (
                "Link prediction does not support label column."
            )


def get_schema_from_neptune(graph_id: str) -> dict[str, Any]:
    """Get schema information from Neptune using pg_schema procedure."""
    client = boto3.client("neptune-graph")

    try:
        query = "CALL neptune.graph.pg_schema() YIELD schema RETURN schema"
        response = client.execute_query(
            graphIdentifier=graph_id, queryString=query, language="OPEN_CYPHER"
        )

        payload = response["payload"]
        schema_json = json.loads(payload.read().decode("utf-8"))

        # Validate schema structure
        if not isinstance(schema_json, dict) or "results" not in schema_json:
            raise ValueError("Invalid schema response format")

        if not schema_json["results"] or "schema" not in schema_json["results"][0]:
            raise ValueError("No schema data in response")

        neptune_schema = schema_json["results"][0]["schema"]

        # Validate required schema components
        required_keys = {"nodeLabelDetails", "edgeLabelDetails", "labelTriples"}
        missing_keys = required_keys - set(neptune_schema.keys())
        if missing_keys:
            raise ValueError(f"Schema missing required components: {missing_keys}")

        # Check if graph has a vertex index and update the schema with that information
        graph_info = client.get_graph(
            graphIdentifier=graph_id,
        )
        # Add vertex index information, or None to indicate the graph does not have an inndex
        neptune_schema["vectorSearchConfiguration"] = graph_info.get(
            "vectorSearchConfiguration", None
        )

    except botocore.client.ClientError as e:
        raise RuntimeError(f"AWS API error: {str(e)}") from e
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON in response: {str(e)}") from e
    except Exception as e:
        raise RuntimeError(f"Unexpected error getting Neptune schema: {str(e)}") from e

    return neptune_schema


NEPTUNE_TO_PD_DTYPES = {
    "Byte": pd.Int8Dtype(),
    "Short": pd.Int16Dtype(),
    "Int": Int32Dtype(),
    "Long": Int64Dtype(),
    "Float": Float32Dtype(),
    "Double": Float64Dtype(),
    "String": StringDtype(),
    "Vector": StringDtype(),  # TODO: Assuming CSV exports for now
    "vector": StringDtype(),  # TODO: Assuming CSV exports for now
}


def get_properties_from_neptune(
    neptune_schema: dict[str, Any],
) -> tuple[dict[str, NodeEdgeInfo], dict[str, NodeEdgeInfo]]:
    """Get vertex and edge properties from Neptune's pg_schema using nullable types"""

    # Process node label details
    vertex_schemas: dict[str, NodeEdgeInfo] = {}
    for node_label, details in neptune_schema["nodeLabelDetails"].items():
        ncolumns = ["~id"]  # Always include ID column
        ndtypes = {"~id": str(StringDtype())}  # Convert dtype to string

        # Gather node properties
        nproperties = details.get("properties", {})
        # TODO: Need to only do this when the embeddings are also in the files
        # Optionally add embedding:vector as a property
        # if "vectorSearchConfiguration" in neptune_schema:
        #     # We add a custom "Vector" node type although Neptune doesn't have
        #     # one currently
        #     nproperties["embedding"] = {"datatypes": ["Vector"]}
        neptune_ntypes = {}

        for prop_name, prop_info in nproperties.items():
            ncolumns.append(prop_name)
            neptune_type = prop_info["datatypes"][0]  # Take first type if multiple
            neptune_ntypes[prop_name] = neptune_type
            pandas_dtype = NEPTUNE_TO_PD_DTYPES.get(neptune_type, StringDtype())
            ndtypes[prop_name] = str(pandas_dtype)  # Convert dtype to string

        logging.debug(
            "Neptune-service discovered vertex properties for node type '%s':\n %s",
            node_label,
            pformat(neptune_ntypes),
        )
        vertex_schemas[node_label] = NodeEdgeInfo(
            properties=ncolumns, pandas_dtypes=ndtypes, neptune_types=neptune_ntypes
        )

    # Process edge label details
    edge_schemas: dict[str, NodeEdgeInfo] = {}
    for edge_label, details in neptune_schema["edgeLabelDetails"].items():
        eproperties = details.get("properties", {})
        ecolumns = ["~id", "~from", "~to"]  # Always include these columns
        edtypes = {
            "~id": str(StringDtype()),
            "~from": str(StringDtype()),
            "~to": str(StringDtype()),
        }
        neptune_etypes = {}

        for prop_name, prop_info in eproperties.items():
            ecolumns.append(prop_name)
            neptune_type = prop_info["datatypes"][0]
            neptune_etypes[prop_name] = neptune_type
            pandas_dtype = NEPTUNE_TO_PD_DTYPES.get(neptune_type, StringDtype())
            edtypes[prop_name] = str(pandas_dtype)  # Convert dtype to string

        logging.debug(
            "Neptune-service discovered edge properties for relation type '%s':\n %s",
            edge_label,
            pformat(neptune_etypes),
        )

        edge_schemas[edge_label] = NodeEdgeInfo(ecolumns, edtypes, neptune_etypes)

    return vertex_schemas, edge_schemas


def analyze_neptune_export(
    input_path: str, neptune_schema: Optional[dict[str, Any]] = None
) -> GraphSchema:
    """Analyze Neptune Analytics export to discover schema.

    Parameters
    ----------
    path : str
        Path to directory containing export (local path or s3:// URL)
    neptune_schema: dict[str, Any], optional
        Optional graph schema extracted from the Neptune service

    Returns
    -------
    GraphSchema
        Container object containing discovered schema information
    """
    fs_handler = FileSystemHandler(input_path)

    # Track what we discover
    vertex_types: set[str] = set()
    relation_types: set[str] = set()
    vertex_files = defaultdict(list)
    edge_files = defaultdict(list)

    # Get all parquet/csv files
    files = fs_handler.list_files(r".+\.(parquet|csv)$")
    format = InputFormat.CSV if files[0].endswith(".csv") else InputFormat.PARQUET

    assert len(files) > 0, f"No parquet/csv files found in {input_path}"
    logging.info(f"Found {len(files)} parquet/csv files")

    # Parse filenames to discover types
    for f in files:
        fname = Path(f).name  # Get just the filename part

        if fname.startswith("Vertex_"):
            # Extract type from filename (e.g. Vertex_Actor_0.parquet -> Actor)
            # match everything between "Vertex_" and the last underscore followed by a number:
            vtype = re.match(r"Vertex_(.+?)_(\d+)\.(parquet|csv)", fname)
            if vtype:
                vertex_type = vtype.group(1)
                assert isinstance(vertex_type, str)
                vertex_types.add(vertex_type)
                # We add just the filename to use relative paths in config
                vertex_files[vtype.group(1)].append(fname)

        elif fname.startswith("Edge_"):
            # Pattern to capture edge type until the last underscore
            relation_match = re.match(r"Edge_(.+)_\d+\.(parquet|csv)", fname)
            if relation_match:
                relation_str = relation_match.group(1)
                assert isinstance(relation_str, str)
                relation_types.add(relation_str)
                # We add just the filename to use relative paths in config
                edge_files[relation_str].append(fname)

    logging.info(f"Discovered vertex types: {sorted(vertex_types)}")
    logging.info(f"Discovered relation types: {sorted(relation_types)}")

    # Analyze schema of each type
    vertex_schemas: dict[str, NodeEdgeInfo] = {}
    edge_schemas: dict[str, NodeEdgeInfo] = {}

    if neptune_schema:
        try:
            logging.info("Getting properties from Neptune Analytics service...")
            vertex_schemas, edge_schemas = get_properties_from_neptune(neptune_schema)

            # Validate that we got properties for all types
            missing_vertices = vertex_types - set(vertex_schemas.keys())
            missing_edges = relation_types - set(edge_schemas.keys())

            if missing_vertices or missing_edges:
                logging.warning(
                    "Some types missing from Neptune schema, falling back to sampling files"
                )
                if missing_vertices:
                    logging.warning(f"Missing vertex types: {missing_vertices}")
                if missing_edges:
                    logging.warning(f"Missing edge types: {missing_edges}")
                raise ValueError(
                    "Incomplete properties from Neptune "
                    f"{missing_vertices=} {missing_edges=}"
                )

        except Exception as e:
            logging.warning(
                f"Failed to get properties from Neptune, falling back to sampling: {e}"
            )
            vertex_schemas = {}
            edge_schemas = {}

    # If we didn't get properties from Neptune or if some were missing, fall back to sampling
    if not vertex_schemas or not edge_schemas:
        logging.info(
            "Sampling one file from each edge/vertex type to get properties..."
        )
        # TODO: Discover embeddings only if they exist?
        for vertex_type in vertex_types:
            logging.info(f"Sampling vertex type {vertex_type}")
            if vertex_type not in vertex_schemas:
                # TODO: For CSV, can we get types from graph schema
                # to avoid parsing errors?
                df = fs_handler.create_df_from_files(
                    osp.join(input_path, vertex_files[vertex_type][0])
                )
                # Try to get neptune types from column names
                neptune_types = {}
                pandas_type_candidates = {}

                # TODO: Only attempt this for CSV input?
                for col_name in df.columns.values.tolist():
                    if ":" in col_name:
                        neptune_type_candidate = col_name.split(":")[-1]
                        if neptune_type_candidate in NEPTUNE_TO_PD_DTYPES:
                            neptune_type = neptune_type_candidate
                            neptune_types[col_name] = neptune_type
                            pandas_type_candidates[col_name] = NEPTUNE_TO_PD_DTYPES[
                                neptune_type
                            ]
                        else:
                            continue

                # Subtract two for ~id and ~label that should not count as propertie
                if len(pandas_type_candidates) == len(df.columns) - 2:
                    pandas_types = pandas_type_candidates
                else:
                    pandas_types = df.dtypes.astype(str).to_dict()

                vertex_schemas[vertex_type] = NodeEdgeInfo(
                    list(df.columns),
                    pandas_types,
                    neptune_types if neptune_types else None,
                )

        for relation_str in relation_types:
            logging.info(f"Sampling relation type {relation_str}")
            # TODO: Implement type deduction from column names for CSV
            relation_name = relation_str
            # If ',' is in the relation name, let's check if it defines a triple
            if "," in relation_str:
                src_type, relation, dst_type = relation_str.split(",")
                if {src_type, dst_type}.issubset(vertex_types):
                    # If the relation name is in the form of "src_type,relation,dst_type",
                    # we use the relation as the name of the relation type
                    relation_name = relation

            if relation_name not in edge_schemas:
                df = fs_handler.create_df_from_files(
                    osp.join(input_path, edge_files[relation_str][0])
                )
                edge_schemas[relation_name] = NodeEdgeInfo(
                    list(df.columns),
                    df.dtypes.astype(str).to_dict(),
                )

    logging.info("Finished Neptune export analysis")
    return GraphSchema(
        vertex_types,
        relation_types,
        dict(vertex_files),
        dict(edge_files),
        vertex_schemas,
        edge_schemas,
        format,
    )


def get_relations_from_neptune(
    edge_files: dict[str, list[str]], neptune_schema: dict[str, Any]
) -> dict[tuple, dict]:
    """Query Neptune for relation information, extracting edge types.

    Parameters
    ----------
    edge_files : dict[str, list[str]]
        A dict of RELATION to list of files.
    neptune_schema : dict[str, Any]
        A Neptune Property Graph schema. See
        https://docs.aws.amazon.com/neptune-analytics/latest/userguide/custom-algorithms-property-graph-schema.html

    Returns
    -------
    dict[str, dict]
        A mapping from edge type tuple (src, relation, dst) to a dict
        with three keys, "source_type", "relation", "dest_type".

    Raises
    ------
    ValueError
        If the queried relationships do not match the ones derived from file names.
    """
    try:
        relationships = {}
        # Process labelTriples to get relationships
        for triple in neptune_schema["labelTriples"]:
            src = triple["~from"]
            relation = triple["~type"]
            dst = triple["~to"]
            if relation in edge_files:  # Only process relations we have files for
                relationships[(src, relation, dst)] = {
                    "source_type": src,
                    "relation": relation,
                    "dest_type": dst,
                }
                logging.info(f"Found edge triple: {src}-[{relation}]->{dst}")

        # Verify we found all relationships
        missing_edges = set(edge_files.keys()) - set(
            [rel for _, rel, _ in relationships.keys()]
        )
        if missing_edges:
            raise ValueError(
                f"Could not find all relationships in Neptune schema: {edge_files.keys()=} "
                f"{relationships.keys()=}"
            )

        return relationships

    except Exception as e:
        logging.error(f"Failed to get relationships from Neptune: {str(e)}")
        raise


def get_relationships_manually(
    edge_files: dict[str, list[str]], vertex_types: set[str]
) -> dict[tuple, dict]:
    """Manually determine relationships through user input and filenames

    Parameters
    ----------
    edge_files : dict[str, list[str]]
        Set of files from relation name to list of files
    vertex_types : set[str]
        The set of available vertex types

    Returns
    -------
    dict[str, dict]
        A mapping from edge type tuple (src, relation, dst) to a dict
        with three keys, "source_type", "relation", "dest_type".

    """
    assert vertex_types, "No vertex types provided"
    for etype, efiles in edge_files.items():
        assert efiles, f"No files for relation type {etype}"
    relationships = {}

    def request_edge_type(vertex_types):
        """Ask user for src/dst vertex types"""
        src_type = ""
        while src_type not in vertex_types:
            src_type = input(
                f"What is a SOURCE vertex type for {relation}? Available types: {vertex_types}: "
            )
        dst_type = ""
        while dst_type not in vertex_types:
            dst_type = input(
                f"What is a DESTINATION vertex type for {relation}? Available types: {vertex_types}: "
            )

        return src_type, dst_type

    for relation_str in edge_files.keys():
        logging.info(f"Analyzing relation type: {relation_str}")
        # Try to get relation triple from 'src,rel,dst' string relation name,
        # otherwise ask user
        types_remaining = True
        relation = relation_str  # Default to using the full string as relation name

        if "," in relation_str:
            parts = relation_str.split(",")
            # Verify that we have exactly 3 parts and they represent a valid edge triple
            if len(parts) == 3:
                src_type, relation, dst_type = parts
                # Verify that source and destination types are valid vertex types
                if {src_type, dst_type}.issubset(vertex_types):
                    types_remaining = False
                    edge_type = (src_type, relation, dst_type)
                    relationships[edge_type] = {
                        "source_type": src_type,
                        "dest_type": dst_type,
                        "relation": relation,
                    }
                    logging.debug(
                        f"Successfully parsed edge triple: {src_type}-[{relation}]->{dst_type}"
                    )
                else:
                    logging.warning(
                        f"Invalid edge triple '{relation_str}': "
                        f"Source type '{src_type}' or destination type '{dst_type}' "
                        f"not found in vertex types {sorted(vertex_types)}. "
                        f"Treating as regular relation name."
                    )
            else:
                logging.warning(
                    f"Invalid edge triple format '{relation_str}': "
                    f"Expected 'src_type,relation,dst_type' format with exactly 3 parts. "
                    f"Treating as regular relation name."
                )

        # Ask user for all possible triples for a relation type
        while types_remaining:
            src_type, dst_type = request_edge_type(vertex_types)
            edge_type = (src_type, relation, dst_type)

            relationships[edge_type] = {
                "source_type": src_type,
                "dest_type": dst_type,
                "relation": relation,
            }

            # Ask if relation has more triples
            types_remaining = (
                input(
                    f"Are there more edge types/triples for '{relation}'? (y/n) "
                ).lower()
                == "y"
            )

    return relationships


def analyze_relations(
    schema_info: GraphSchema, neptune_schema: Optional[dict] = None
) -> dict[tuple, dict]:
    """Analyze relations in the graph to extract edge types.

    Parameters
    ----------
    schema_info : GraphSchema
        Wrapper object with graph schema derived from files
    neptune_schema : Optional[dict], optional
        Property graph schema as reported by the Neptune service, by default None

    Returns
    -------
    dict[tuple, dict]
        A mapping from edge type tuple (src, relation, dst) to a dict
        with three keys, "source_type", "relation", "dest_type".
    """
    if neptune_schema:
        try:
            return get_relations_from_neptune(schema_info.edge_files, neptune_schema)
        except Exception as e:
            logging.warning(
                f"Failed to get relations from Neptune, falling back to manual analysis: {e}"
            )

    return get_relationships_manually(schema_info.edge_files, schema_info.vertex_types)


class TransformationSuggestions(TypedDict):
    """Type definition for transformation suggestions"""

    vertices: dict[str, list[GConstructFeature]]
    edges: dict[str, list[GConstructFeature]]


def suggest_transformations(
    schema_info: GraphSchema, relationships: dict[tuple, dict]
) -> TransformationSuggestions:
    """Suggest feature transformations based on column data types

    Parameters
    ----------
    schema_info : GraphSchema
        Object that describes the derived graph schema.
    relationships: dict[tuple, dict]
        Mapping from (src,relation,dst) tuples to a dict

    Returns
    -------
    Mapping[str, Mapping[str, GConstructFeature]]
        Mapping with "vertices" and "edges" keys, each containing a mapping of
        node/edge type to lists of GConstruct feature dicts.
    """
    feature_suggestions: TransformationSuggestions = {
        "vertices": defaultdict(list),
        "edges": defaultdict(list),
    }

    def build_feature_dict(
        property: str, dtype: str, type_info: NodeEdgeInfo
    ) -> Optional[GConstructFeature]:
        """Builds GConstruct feature dict for a single edge/node property

        Parameters
        ----------
        property : str
            Property name. This might include a type like <name:type>
        dtype : str
            String representation of a nullable Pandas type
        type_info : NodeEdgeInfo
            Schema information for node/edge type.

        Returns
        -------
        dict | None
            A GConstruct feature dict or None if the feature type is not supported by GraphStorm,
            i.e. type is something other than numeric or string.
        """
        # Create a feature name that removes `:` for compatibility with graphstorm
        if ":" in property:
            # Property is already type-annotated, column name remains the same,
            # feature name excludes the type
            feature_col = property
            feature_name = get_safe_name(property)
        else:
            # If the input is CSV, but the property has no type, add it
            if schema_info.input_format == InputFormat.CSV:
                assert type_info.neptune_types is not None, (
                    "Neptune types are required for CSV input when property name has no type. "
                    "Please provide a graph ID to get types from."
                )
                neptune_type = type_info.neptune_types[property]
                feature_col = f"{property}:{neptune_type}"
            else:
                # Otherwise, the input was Parquet, for which feature_col == feature_name
                feature_col = property
            feature_name = property

        # By default apply min-max to all numerical features
        if pd.api.types.is_numeric_dtype(dtype):
            return {
                "feature_col": feature_col,
                "feature_name": feature_name,
                "transform": {"name": "max_min_norm"},
            }
        # TODO: This assumes Neptune-exported data is CSV, need to figure parquet export later
        # elif (
        #     pd.api.types.is_string_dtype(dtype)
        #     and feature_col.split(":")[-1].lower() == "vector"
        # ):
        #     # When a feature is an array-like we just parse the vector directly (no-op transform)
        #     # for now let's ensure the data cam from a csv file and is of type 'embedding'
        #     assert schema_info.input_format == InputFormat.CSV, (
        #         "Array-like features are only supported for CSV input. "
        #         f"Please use a CSV file for feature '{feature_col}'."
        #     )
        #     return {
        #         "feature_col": feature_col,
        #         "feature_name": feature_name,
        #         "transform": {"name": "no-op", "separator": ";"},
        #     }
        # By default apply categorical transformations to all string features
        elif pd.api.types.is_string_dtype(dtype):
            return {
                "feature_col": feature_col,
                "feature_name": feature_name,
                "transform": {"name": "to_categorical"},
            }

        # If no type matched, we skip the feature
        logging.warning(
            "Skipping feature '%s', cannot process feature with type '%s'",
            property,
            dtype,
        )
        return None

    # Analyze vertex features
    for vtype, ntype_info in schema_info.vertex_schemas.items():
        for col, dtype in ntype_info.pandas_dtypes.items():
            # Skip ID columns
            if col in ["~id", "~label"]:
                continue

            suggestion = build_feature_dict(col, dtype, ntype_info)
            if suggestion:
                feature_suggestions["vertices"][vtype].append(suggestion)

    # Build a mapping from relation name to edge schema
    # This handles both cases where edge files are named with just the relation
    # or with the full triple (src,relation,dst)
    relation_to_schema = {}
    for relation_str, schema in schema_info.edge_schemas.items():
        # Always add the full string as a key to handle cases where comma-separated format isn't a valid triple
        relation_to_schema[relation_str] = schema

        # If the relation string contains commas, try to extract the relation part
        if "," in relation_str:
            parts = relation_str.split(",")
            # Verify that we have exactly 3 parts and they represent a valid edge triple
            if len(parts) == 3:
                src_type, relation, dst_type = parts
                # Verify that source and destination types are valid vertex types
                if {src_type, dst_type}.issubset(schema_info.vertex_types):
                    # Add the extracted relation as another key pointing to the same schema
                    relation_to_schema[relation] = schema
                    logging.info(
                        f"Mapped relation '{relation}' from triple '{relation_str}'"
                    )
                else:
                    logging.warning(
                        f"Not extracting relation from '{relation_str}': "
                        f"Source type '{src_type}' or destination type '{dst_type}' "
                        f"not found in vertex types {sorted(schema_info.vertex_types)}"
                    )
            else:
                logging.warning(
                    f"Not extracting relation from '{relation_str}': "
                    f"Expected 'src_type,relation,dst_type' format with exactly 3 parts"
                )

    # Analyze edge features
    for etype, _ in relationships.items():
        _, relation, _ = etype
        # Get canonical etype from triple
        etype_str = ",".join(etype)

        # Try to find the edge schema using the relation name
        if relation in relation_to_schema:
            etype_info = relation_to_schema[relation]
        elif relation in schema_info.edge_schemas:
            etype_info = schema_info.edge_schemas[relation]
        else:
            # If we can't find the schema, log a warning and skip this edge type
            logging.warning(
                f"Could not find schema for relation '{relation}'. "
                f"Available schemas: {list(schema_info.edge_schemas.keys())}"
            )
            continue

        for col, dtype in etype_info.pandas_dtypes.items():
            # Skip structural columns
            if col in ["~id", "~from", "~to", "~label"]:
                continue

            suggestion = build_feature_dict(col, dtype, etype_info)
            if suggestion:
                feature_suggestions["edges"][etype_str].append(suggestion)

    # Convert defaultdict to regular dicts to ensure we trigger KeyErrors if encountered
    feature_suggestions["vertices"] = dict(feature_suggestions["vertices"])
    feature_suggestions["edges"] = dict(feature_suggestions["edges"])

    return feature_suggestions


def print_feature_instructions() -> None:
    """Print usage instructions for the feature configuration tool."""

    print(
        r"""
        Reviewing features for potential transformations and labels...

        For each feature you can:
        1. Use as a regular feature with default transformation (press Enter)
        2. Change transformation type (enter new transformation name)
        3. Use as a label (enter 'label')
        4. Skip this feature (enter 'skip')

        """
        + AVAILABLE_TRANSFORMATIONS_STR
    )


def get_split_configuration() -> dict[str, Any]:
    """Handle user input for data split configuration.

    Returns
    -------
    dict[str, Any]
        Split configuration dictionary containing either custom_split_filenames or split_pct
    """
    split_config: dict[str, Any] = {}
    use_custom = input("\nUse custom split files? (y/n): ").lower().strip() == "y"

    if use_custom:
        print("\nEnter paths for split files (leave blank if not used):")
        train_path = input("Training split file path: ").strip()
        valid_path = input("Validation split file path: ").strip()
        test_path = input("Test split file path: ").strip()

        split_config["custom_split_filenames"] = {
            "train": train_path if train_path else None,
            "valid": valid_path if valid_path else None,
            "test": test_path if test_path else None,
        }
    else:
        custom_percent = (
            input("\nUse custom split percentages? (default is 80/10/10) (y/n): ")
            .lower()
            .strip()
            == "y"
        )
        if custom_percent:
            train_pct = float(input("Enter training percentage [0.0-1.0]: ").strip())
            val_pct = float(input("Enter validation percentage [0.0-1.0]: ").strip())
            test_pct = float(input("Enter test percentage [0.0-1.0]: ").strip())
        else:
            train_pct, val_pct, test_pct = 0.8, 0.1, 0.1

        split_sum = math.fsum([train_pct, val_pct, test_pct])
        if split_sum > 1.0:
            logging.warning(f"Split percentages do not sum up to 1.0, got {split_sum=}")
        split_config["split_pct"] = [train_pct, val_pct, test_pct]

    return split_config


def handle_label_configuration(feature_name: str) -> dict[str, Any]:
    """Configure label settings through user interaction.

    Parameters
    ----------
    feature_name : str
        Name of the feature being configured as a label

    Returns
    -------
    dict[str, Any]
        Label configuration dictionary
    """
    task_map = {
        "1": "classification",
        "2": "regression",
        "3": "reconstruct_node_feat",
        "4": "reconstruct_edge_feat",
    }

    print("\nConfiguring label settings:")
    print("\nAvailable task types:")
    pp(task_map, indent=4)

    # Get task type
    while True:
        task_type = input("Enter task type number: ").strip()
        if task_type in task_map:
            break
        print(f"Invalid task type. Please provide one of {list(task_map.keys())}.")

    split_config = get_split_configuration()

    return {"task_type": task_type, "label_col": feature_name, **split_config}


def handle_transformation(
    feature: GConstructFeature, response: str
) -> GConstructFeature:
    """Handle feature transformation configuration.

    Parameters
    ----------
    feature : GConstructFeature
        Original feature dictionary
    response : str
        User's transformation choice

    Returns
    -------
    GConstructFeature
        Updated feature dictionary with new transformation
    """
    feature_name = feature["feature_name"]

    while True:
        if response == "bert_hf":
            model = input(
                "Enter HuggingFace model name (Default: bert-base-uncased): "
            ).strip()
            model = model if model else "bert-base-uncased"
            seq_len_in = input("Enter max sequence length (default: 128): ").strip()
            seq_len = int(seq_len_in) if seq_len_in else 128

            return {
                "feature_col": feature["feature_col"],
                "feature_name": feature_name,
                "transform": {
                    "name": "bert_hf",
                    "bert_model": model,
                    "max_seq_length": seq_len,
                },
            }
        elif response in ["max_min_norm", "standard", "rank_gauss", "to_categorical"]:
            return {
                "feature_col": feature["feature_col"],
                "feature_name": feature_name,
                "transform": {"name": response},
            }
        else:
            print(f"Invalid transformation: {response}")
            response = ""
            print(AVAILABLE_TRANSFORMATIONS_STR)
            while response == "":
                response = input(
                    "Enter a transformation name or press Enter to skip: "
                ).strip()


def do_feature_aggregation(
    type_name: str,
    features_list: Sequence[GConstructFeature],
    aggregate_categorical: bool = False,
) -> list[GConstructFeature]:
    """Aggregate numerical and (optionally) categorical features into one transformation.

    Parameters
    ----------
    features_list : Sequence[GConstructFeature]
        List of features to aggregate
    aggregate_categorical: bool
        When true will aggregate categorical features into one transformation.
        False by default because GConstruct does not support multi-column categorical
        transformations as of GraphStorm 0.4.1.

    Returns
    -------
    Sequence[GConstructFeature]
        List of aggregated features
    """
    logging.info("Aggregating features for type '%s'", type_name)
    # TODO: Support more individual numerical transforms
    numerical_features: GConstructFeature = {
        "feature_col": [],
        "feature_name": f"{type_name}-numerical_feats",
        "transform": {
            "name": "max_min_norm",
            "min_val": 0.0,
            "max_val": 1.0,
        },
    }
    categorical_features: GConstructFeature = {
        "feature_col": [],
        "feature_name": f"{type_name}-categorical_feats",
        "transform": {"name": "to_categorical"},
    }
    all_features: list[GConstructFeature] = []
    # Type-checker assertions
    assert isinstance(numerical_features["feature_col"], list)
    assert isinstance(categorical_features["feature_col"], list)

    for confirmed_feature in features_list:
        assert isinstance(confirmed_feature["feature_col"], str)
        suggested_transform = confirmed_feature["transform"].get("name", "")
        # Gather all numerical/categorical features under one transformation
        if suggested_transform == "max_min_norm":
            numerical_features["feature_col"].append(confirmed_feature["feature_col"])
        elif aggregate_categorical and suggested_transform == "to_categorical":
            categorical_features["feature_col"].append(confirmed_feature["feature_col"])
        else:
            # For other types append feature as-is
            all_features.append(confirmed_feature)

    # After done iterating through all features, add all numerical and categorical features
    # into single transformations
    if len(numerical_features["feature_col"]) > 0:
        logging.info(
            "Aggregated %s numerical features into one transformation: '%s'",
            len(numerical_features["feature_col"]),
            numerical_features["feature_name"],
        )
        all_features.append(numerical_features)
    if len(categorical_features["feature_col"]) > 0:
        logging.info(
            "Aggregated %s categorical features into one transformation: '%s'",
            len(categorical_features["feature_col"]),
            categorical_features["feature_name"],
        )
        all_features.append(categorical_features)

    return all_features


def get_safe_name(property_name: str) -> str:
    if ":" in property_name:
        return property_name.split(":")[0]
    return property_name


def process_properties_for_type(
    type_name: str,
    feature_candidates: list[GConstructFeature],
    is_vertex: bool,
    aggregate_features: bool = False,
    task_info: Optional[TaskInfo] = None,
    cols_to_skip: Optional[list[str]] = None,
    cols_to_keep: Optional[list[str]] = None,
    masks_subpath: Optional[str] = None,
) -> tuple[list[GConstructFeature], list[dict[str, Any]]]:
    """Process features for a specific vertex or edge type.

    Parameters
    ----------
    type_name : str
        Name of the vertex or edge type
    feature_candidates : list[dict[str, Any]]
        List of feature dicts for this type
    is_vertex : bool
        True if processing vertex features, False for edge features
    aggregate_features: bool, optional
        Whether to aggregate numerical and categorical features
        to one transformation for faster processing. Default is False.
    task_info: TaskInfo, optional
        Combined information for a learning task, including learning
        task name, target node/edge type, label column.
    cols_to_skip: Optional[list[str]]
        List of columns to skip for type
    cols_to_keep: Optional[list[str]]
        List of columns to keep for type
    masks_subpath: Optional[str]
        Subpath to [train|val|test]_mask.parquet files. We assume this is under the
        main input prefix.

    Returns
    -------
    tuple[list[GConstructFeature], list[dict[str, Any]]]
        A tuple whose first element are confirmed features and the
        second the confirmed labels for this type.
    """

    confirmed_features = []
    labels_list = []

    # Handle edge-specific label processing
    if not is_vertex:
        labels_list.extend(process_edge_labels(type_name, task_info))

    # Process features either automatically or manually
    if task_info:
        logging.info(
            "Automatically processing features for '%s' type",
            type_name,
        )

        # Get features and labels without user interaction
        for gc_feature_dict in feature_candidates:
            # Check if we should skip column
            logging.debug("Processing feature candidate '%s'", gc_feature_dict)
            should_skip = (
                cols_to_skip and gc_feature_dict["feature_col"] in cols_to_skip
            )
            should_not_keep = (
                cols_to_keep and gc_feature_dict["feature_col"] not in cols_to_keep
            )
            if should_skip or should_not_keep:
                logging.debug(
                    "Skipping column '%s' for type '%s'",
                    gc_feature_dict["feature_col"],
                    type_name,
                )
                continue

            # If column is a label column
            if (
                type_name == task_info.target_type
                and gc_feature_dict["feature_col"] == task_info.label_col
            ):
                logging.info(
                    "Found task column '%s' in target type '%s', creating label config...",
                    task_info.label_col,
                    task_info.target_type,
                )
                if masks_subpath:
                    logging.info(
                        "Creating split config for label '%s' using custom masks under '%s'",
                        task_info.label_col,
                        masks_subpath,
                    )
                    split_config = {
                        "custom_split_filenames": {
                            "train": osp.join(masks_subpath, "train_ids.parquet"),
                            "valid": osp.join(masks_subpath, "val_ids.parquet"),
                            "test": osp.join(masks_subpath, "test_ids.parquet"),
                            "column": ["nid"],
                        }
                    }
                else:
                    split_config = {"split_pct": [0.8, 0.1, 0.1]}  # type: ignore
                    logging.info(
                        "No masks subpath provided, using default split configuration: %s",
                        split_config,
                    )

                # Add as label if this is the target column in the target type
                labels_list.append(
                    {
                        "task_type": task_info.learning_task,
                        "label_col": task_info.label_col,
                        "label_name": get_safe_name(task_info.label_col),
                        **split_config,
                    }
                )
                logging.debug(
                    "Added col: '%s' as label for %s",
                    gc_feature_dict["feature_col"],
                    type_name,
                )

            else:
                # Add as feature for all other cases
                logging.debug(
                    "Added col(s): '%s' as feature for %s",
                    gc_feature_dict["feature_col"],
                    type_name,
                )
                confirmed_features.append(gc_feature_dict)

    else:
        # Ask for user input
        print(
            f"\n{'=' * 80}"
            f"\nProcessing features for {'vertex' if is_vertex else 'edge'} type: {type_name}"
            f"\n{'=' * 80}"
        )

        process_features_manually(feature_candidates, confirmed_features, labels_list)

    if aggregate_features:
        confirmed_features = do_feature_aggregation(type_name, confirmed_features)

    logging.info(
        "Created %s feature(s) for '%s' type", len(confirmed_features), type_name
    )
    logging.debug("Confirmed feature(s): %s", confirmed_features)

    return confirmed_features, labels_list


def process_edge_labels(
    type_name: str,
    task_info: Optional[TaskInfo] = None,
) -> list[dict[str, Any]]:
    """Process labels for edge types."""
    default_split_config = {"split_pct": [0.8, 0.1, 0.1]}

    # Handle pre-configured link prediction
    if task_info and task_info.learning_task == "link_prediction":
        if task_info.target_type == type_name:
            logging.info(
                "Found link prediction task for edge type '%s', creating label config...",
                type_name,
            )
            return [
                {
                    "task_type": "link_prediction",
                    "label_col": "",
                    **default_split_config,
                }
            ]

    # Handle interactive link prediction configuration
    if (
        not task_info
        and input(f"\nRun link prediction for edge type '{type_name}'? (y/n): ").lower()
        == "y"
    ):
        split_config = get_split_configuration()
        return [{"task_type": "link_prediction", "label_col": "", **split_config}]

    return []


def process_features_manually(
    features: Sequence[GConstructFeature],
    confirmed_list: list[GConstructFeature],
    labels_list: list[dict[str, Any]],
):
    """Process features through user interaction.

    Modifies the input lists in-place to append features and labels.

    """
    for feature in features:
        feature_name = feature["feature_name"]
        current_transform = feature["transform"]["name"]

        print(f"\nFeature: {feature_name}")
        print(f"Suggested transformation: {current_transform}")

        response = input(
            "Enter 'label' to use as label, 'skip' to skip, a transformation name "
            "to change to, or press Enter to accept: "
        ).strip()

        if response == "skip":
            continue
        elif response == "label":
            labels_list.append(handle_label_configuration(feature_name))
        elif response == "":
            confirmed_list.append(feature)
        else:
            transformed_feature = handle_transformation(feature, response)
            if transformed_feature:
                confirmed_list.append(transformed_feature)


def print_configuration_summary(
    confirmed_features: Mapping[str, Mapping[str, list]],
    labels_config: Mapping[str, Mapping[str, list]],
) -> None:
    """Print summary of the configuration choices.

    Parameters
    ----------
    confirmed_features : dict[str, dict[str, list]]
        Dictionary of confirmed features
    labels_config : dict[str, dict[str, list]]
        Dictionary of label configurations
    """
    print("\nGraph Feature Transformations:")
    for entity_type in ["vertices", "edges"]:
        print(f"\n\t{entity_type.title()} features:")
        for type_name, features in confirmed_features[entity_type].items():
            print(f"\n\t\t{type_name}:")
            for feature in features:
                print(
                    f"\t\t\t  - {feature['feature_name']}: {feature['transform']['name']}"
                )

    print("\nGraph Labels:")
    for entity_type in ["vertices", "edges"]:
        print(f"\n\t{entity_type.title()} Labels:")
        for type_name, labels in labels_config[entity_type].items():
            print(f"\n\t\t{type_name}:")
            for label in labels:
                print(f"\t\t\t  - {label['label_name']}: {label['task_type']}")
                if "custom_split_filenames" in label:
                    print(
                        f"\t\t\t    Custom split files: {label['custom_split_filenames']}"
                    )
                elif "split_pct" in label:
                    print(f"\t\t\t    Split percentages: {label['split_pct']}")


@dataclass
class PropertyConfig:
    """Dataclass for feature/label configurations.

    confirmed_features: dict[str, dict[str, list]]
        Dict with two top-level keys, "vertices" and "edges", each containing a dict
        of features for vertices and edges. Each inner dict has canonical type names as keys,
        and a list of GConstruct config feature dicts as values.
    labels_config: dict[str, dict[str, list]]
        Same structure as confirmed_features, with lists of GConstruct config label dictionaries
        as values for the inner dicts.
    """

    confirmed_features: dict[str, dict[str, list[GConstructFeature]]]
    labels_config: dict[str, dict[str, list[dict[str, Any]]]]


def analyze_properties(
    schema_info: GraphSchema,
    relationships: dict[tuple, dict[str, str]],
    aggregate_features: bool = False,
    task_info: Optional[TaskInfo] = None,
    cols_to_skip_dict: Optional[dict[str, list[str]]] = None,
    cols_to_keep_dict: Optional[dict[str, list[str]]] = None,
    masks_subpath: Optional[str] = None,
) -> PropertyConfig:
    """Suggest feature transformations and labels based on data types and allow user override.

    Parameters
    ----------
    schema_info : dict[str, Any]
        Dictionary containing schema information
    relationships: dict[tuple, dict[str, str]]
        Mapping from (src,relation,dst) tuples to a dict
    task_info: Optional[TaskInfo]
        Task information dataclass, optional
    cols_to_skip_dict: Optional[dict[str, list[str]]]
        Per-type column lists to skip during feature parsing.
    cols_to_keep_dict: Optional[dict[str, list[str]]]
        Per-type column lists to keep during feature parsing.
    masks_subpath:
        Subpath under input prefix to train_mask.parquet, val_mask.parquet, test_mask.parquet.
        Each file should contain the node ids for train, validation, test
        masks.

    Returns
    -------
    PropertyConfig
        Confirmed features and label configurations. A dict with two top-level keys,
        "vertices" and "edges" corresponding to dicts of features for vertices and
        edges. Each inner dict is a list of
    """
    cols_to_skip_dict = cols_to_skip_dict or {}
    cols_to_keep_dict = cols_to_keep_dict or {}
    # Get suggested transformations for every graph property, might include labels
    property_suggestions = suggest_transformations(schema_info, relationships)
    # We only ask for user input when we don't have a learning task provided
    if not task_info:
        print_feature_instructions()

    # Make into a typed dict?
    confirmed_features: dict[str, dict] = {
        "vertices": defaultdict(list),
        "edges": defaultdict(list),
    }

    labels_config: dict[str, dict] = {
        "vertices": defaultdict(list),
        "edges": defaultdict(list),
    }

    # Process vertex features
    for vtype, properties in sorted(property_suggestions["vertices"].items()):
        confirmed_feats, labels = process_properties_for_type(
            vtype,
            properties,
            is_vertex=True,
            aggregate_features=aggregate_features,
            task_info=task_info,
            cols_to_skip=cols_to_skip_dict.get(vtype, None),
            cols_to_keep=cols_to_keep_dict.get(vtype, None),
            masks_subpath=masks_subpath,
        )
        if confirmed_feats:
            confirmed_features["vertices"][vtype] = confirmed_feats
        if labels:
            labels_config["vertices"][vtype] = labels

    # Process edge features
    for canonical_etype, properties in sorted(property_suggestions["edges"].items()):
        confirmed_feats, labels = process_properties_for_type(
            canonical_etype,
            properties,
            is_vertex=False,
            aggregate_features=aggregate_features,
            task_info=task_info,
            cols_to_skip=cols_to_skip_dict.get(canonical_etype, None),
            masks_subpath=masks_subpath,
        )
        if confirmed_feats:
            confirmed_features["edges"][canonical_etype] = confirmed_feats
        if labels:
            labels_config["edges"][canonical_etype] = labels

    return PropertyConfig(confirmed_features, labels_config)


def create_graphstorm_config(
    input_path: str,
    graph_id: Optional[str] = None,
    learning_task: Optional[SUPPORTED_TASKS_TYPE] = None,
    target_type: Optional[str] = None,
    label_column: Optional[str] = None,
    masks_prefix: Optional[str] = None,
    cols_to_skip_dict: Optional[dict[str, list[str]]] = None,
    cols_to_keep_dict: Optional[dict[str, list[str]]] = None,
    aggregate_features: bool = True,
    verbose: bool = False,
):
    """Create a GraphStorm data processing configuration by Neptune Analytics exported data and graph schema.

    Parameters
    ----------
    input_path : str
        Path to Neptune Analytics exported data
    graph_id : Optional[str], optional
        Neptune Analytics Graph ID, by default None. Allows automated relation extraction.
    learning_task : Optional[str],
        GraphStorm task name to prepare data for, by default None.
        Can be one of:
          - classification
          - regression
          - link_prediction
    target_type : Optional[str]
        Node/edge type to use as target type for the learning task, by default None
    label_column : Optional[str],
        Column to use as labels for the learning task, by default None.
        For link prediction the column can be omitted.
    mask_prefix: Optional[str]
        Prefix path to files train_mask.parquet, val_mask.parquet, test_mask.parquet,
        that contain the node ids for the train, validation, and test masks respectively.
        Needs to share a prefix with input_path.
    cols_to_skip_dict: Optional[dict[str, list[str]]]
        Marks which columns to skip during parsing features. Optional
        Format is a dict with keys being type names and values are list of columns to skip when parsing features.
    cols_to_keep_dict: Optional[dict[str, list[str]]]
        Specify which columns to keep during parsing features. All other columns are ignored.
        Format is a dict with keys being type names and values are lists of columns to keep when parsing features.
    aggregate_features: bool
        When true will aggregate same-type features into a single transformation to help
        with graph-building efficiency. Default: True
    verbose : bool
        Create verbose output, by default False

    Returns
    -------
    dict
        A dictionary that conforms to GraphStorm's graph construction schema.
        See https://graphstorm.readthedocs.io/en/latest/cli/graph-construction/single-machine-gconstruct.html
    """
    neptune_schema = None
    if graph_id:
        try:
            neptune_schema = get_schema_from_neptune(graph_id)
        except Exception as e:
            logging.warning(f"Failed to get schema from Neptune: {e}")

    # Step 1: Analyze the export
    logging.info("Analyzing Neptune Analytics exported data/schema")
    schema_info = analyze_neptune_export(input_path, neptune_schema)

    # Step 2: Get relationship information
    logging.info("Analyzing relationships")
    relationships = analyze_relations(schema_info, neptune_schema)

    # Step 3: Get feature suggestions and confirmations
    logging.info("Analyzing node/edge properties to get features/labels")
    task_info = None
    if learning_task and target_type:
        if learning_task == "link_prediction":
            logging.info("Link prediction task selected, will not use label column")
            label_column = ""
        else:
            assert label_column, (
                "Label column must be specified for classification/regression tasks"
            )

        task_info = TaskInfo(learning_task, target_type, label_column)

    masks_subpath = None
    if masks_prefix:
        # We assume mask_prefix and input_prefix share a common prefix, e.g.
        # s3://bucket/data/graph_data, s3://bucket/data/mask_data

        # Ensure masks_prefix and input_prefix share a prefix
        assert osp.commonprefix([input_path, masks_prefix]), (
            "input_path and masks_prefix need to have a common prefix, got "
            f"{input_path=} and {masks_prefix=}"
        )
        # TODO: See if using a relative ../ path works here
        masks_subpath = osp.relpath(masks_prefix, input_path)

    # print(f"{cols_to_keep_dict=}")
    property_configs = analyze_properties(
        schema_info,
        relationships,
        aggregate_features=aggregate_features,
        task_info=task_info,
        cols_to_skip_dict=cols_to_skip_dict,
        cols_to_keep_dict=cols_to_keep_dict,
        masks_subpath=masks_subpath,
    )
    features = property_configs.confirmed_features
    labels = property_configs.labels_config

    if verbose:
        print_configuration_summary(features, labels)

    # Step 4: Create config
    GSConfig = TypedDict("GSConfig", {"version": str, "nodes": list, "edges": list})
    gs_config = GSConfig(version="gconstruct-v0.1", nodes=[], edges=[])

    def get_format_config(file_list: list[str]) -> dict[str, Any]:
        """Get format config for a given list of files.

        Parameters
        ----------
        file_list : str
            Name of the format

        Returns
        -------
        dict[str, Any]
            Format config dictionary
        """
        format_name = osp.splitext(file_list[0])[-1][1:]
        if format_name == "csv":
            return {"name": format_name, "separator": NEPTUNE_CSV_SEP}
        else:
            return {"name": format_name}

    # Add vertex configs
    for vtype in schema_info.vertex_types:
        # Split on extension and ignore the first character '.'
        format_dict = get_format_config(schema_info.vertex_files[vtype])
        vertex_config: dict[str, Any] = {
            "node_type": vtype,
            "format": format_dict,
            "files": schema_info.vertex_files[vtype],
            "node_id_col": "~id",
        }

        # Add confirmed features
        if features["vertices"].get(vtype):
            vertex_config["features"] = features["vertices"][vtype]

        # Add labels
        if labels["vertices"].get(vtype):
            vertex_config["labels"] = labels["vertices"][vtype]

        gs_config["nodes"].append(vertex_config)

    # Add edge configs
    for src, rel, dst in relationships.keys():
        canonical_etype = ",".join([src, rel, dst])

        # Try to get edge files using canonical edge type first, then fall back to relation name
        if canonical_etype in schema_info.edge_files:
            efiles = schema_info.edge_files[canonical_etype]
        elif rel in schema_info.edge_files:
            efiles = schema_info.edge_files[rel]
        else:
            raise KeyError(
                f"Could not find edge files for relation '{rel}' or canonical edge type '{canonical_etype}'"
            )

        format_dict = get_format_config(efiles)
        edge_config = {
            "relation": [
                src,
                rel,
                dst,
            ],
            "format": format_dict,
            "files": efiles,
            "source_id_col": "~from",
            "dest_id_col": "~to",
        }

        # Add confirmed features
        if features["edges"].get(canonical_etype):
            edge_config["features"] = features["edges"][canonical_etype]

        # Add labels
        if labels["edges"].get(canonical_etype):
            edge_config["labels"] = labels["edges"][canonical_etype]

        gs_config["edges"].append(edge_config)

    print("Created GraphStorm configuraction dictionary")
    return gs_config


def extract_features_from_gconstruct_config(config_dict):
    """Extract node and edge features from a GConstruct configuration.

    Parameters
    ----------
    config_dict : dict
        Dictionary containing the GConstruct configuration

    Returns
    -------
    tuple[list[str], list[str]]
        (node_features, edge_features) where:
            - node_features is a list of strings in format "ntype:feat1,feat2"
            - edge_features is a list of strings in format "srctype,edgetype,dsttype:feat1,feat2"
    """
    node_features = []
    edge_features = []

    # Process node features
    if "nodes" in config_dict:
        for node in config_dict["nodes"]:
            if "features" in node and node["features"]:
                ntype = node["node_type"]
                feat_names = []

                for feat in node["features"]:
                    if "feature_name" in feat:
                        feat_names.append(feat["feature_name"])
                    elif "feature_col" in feat:
                        feat_names.append(feat["feature_col"])

                if feat_names:
                    node_features.append(f"{ntype}:{','.join(feat_names)}")

    # Process edge features
    if "edges" in config_dict:
        for edge in config_dict["edges"]:
            if "features" in edge and edge["features"]:
                # Edge type is defined as [src_type, rel_type, dst_type]
                if "relation" in edge:
                    src_type, rel_type, dst_type = edge["relation"]
                    feat_names = []

                    for feat in edge["features"]:
                        if "feature_name" in feat:
                            feat_names.append(feat["feature_name"])
                        elif "feature_col" in feat:
                            feat_names.append(feat["feature_col"])

                    if feat_names:
                        edge_features.append(
                            f"{src_type},{rel_type},{dst_type}:{','.join(feat_names)}"
                        )

    return node_features, edge_features


def parse_dict(dict_string):
    """Parse a JSON string or literal Python dict string into a dictionary.

    Parameters
    ----------
    dict_string : str
        String to parse

    Returns
    -------
    dict:
        A dict representation of the input string.
    """
    try:
        # Try to parse as JSON first
        return json.loads(dict_string)
    except json.JSONDecodeError:
        try:
            # If JSON fails, try using ast.literal_eval which can handle Python dict syntax
            return ast.literal_eval(dict_string)
        except (ValueError, SyntaxError):
            raise argparse.ArgumentTypeError(
                f"Invalid dictionary format: {dict_string}"
            )


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate GraphStorm config from Neptune Analytics export"
    )
    parser.add_argument(
        "--input-path",
        "-i",
        help="Path to Neptune Analytics export location (local path or s3:// URI). Required.",
        required=True,
    )
    parser.add_argument(
        "--graph-id",
        "-g",
        help="Graph identifier to help with resolving graph properties. Required.",
        # TODO: Make optional when we allow input of multiple triples per relation?
        required=True,
    )
    parser.add_argument(
        "--output-filename",
        "-o",
        help="File name for GConstruct config file. Will be created under --input-path."
        "Default: gconstruct_config.json'",
        default="gconstruct_config.json",
    )
    parser.add_argument(
        "--verbose",
        "-v",
        action="store_true",
        help="Print verbose output",
    )
    parser.add_argument(
        "--task-info",
        nargs=3,
        metavar=("task", "target_type", "label_col"),
        default=None,
        help="Provide task type (e.g. classification, link_prediction), "
        "target node/edge type, and label column. When provided all other columns "
        "in the data are parsed as features automatically.",
    )
    col_set_group = parser.add_mutually_exclusive_group()
    col_set_group.add_argument(
        "--type-cols-to-skip",
        type=parse_dict,
        help=(
            "Mark which columns to skip per node/edge type during feature extraction. "
            'Expected value is a dictionary in JSON/Python format: {"type_name": ["col1", "col2"]} or '
            "{'type_name': '['col1', 'col2']'}. "
            "Edge type names should be in the 'src,rel,dst' format."
            'Example: --type-cols-to-skip {"ntype1": ["n_col"], "ntype1,rel,ntype1": ["e_col]}'
        ),
    )
    col_set_group.add_argument(
        "--type-cols-to-keep",
        type=parse_dict,
        help=(
            "Specify which columns to keep per node/edge type during feature extraction. All other columns as ignored. "
            'Expected value is a dictionary in JSON/Python format: {"type_name": ["col1", "col2"]} or '
            "{'type_name': '['col1', 'col2']'}. "
            "Edge type names should be in the 'src,rel,dst' format."
            'Example: --type-cols-to-keep {"ntype1": ["n_col"], "ntype1,rel,ntype1": ["e_col]}'
        ),
    )
    parser.add_argument(
        "--masks-prefix",
        type=str,
        help=(
            "Prefix path to files train_mask.parquet, val_mask.parquet, test_mask.parquet, "
            "that contain the node ids for the train, validation, and test masks respectively. "
            "Needs to have a common prefix with --input-path."
        ),
    )
    args = parser.parse_args()

    return args


@dataclass
class CreateGSConfigArgs:
    """Typed runtime arguments."""

    input_path: str
    output_filename: str
    verbose: bool
    graph_id: Optional[str]
    type_cols_to_skip: Optional[dict[str, list[str]]]
    task_info: Optional[list[str]]
    masks_prefix: Optional[str]


def main():
    """Main function to generate GraphStorm config."""

    args = CreateGSConfigArgs(**vars(parse_args()))
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO)
    logging.getLogger("botocore").setLevel(logging.WARNING)

    task, target_type, label_col = [None] * 3
    if args.task_info:
        task, target_type, label_col = args.task_info
        if task not in [
            "classification",
            "regression",
            "link_prediction",
        ]:
            raise ValueError(
                f"Invalid task type: {task}. "
                "Must be one of: classification, regression, link_prediction"
            )
    # This only indicates type to the type checker
    task = cast(SUPPORTED_TASKS_TYPE, task)

    # Create the config
    gs_config = create_graphstorm_config(
        args.input_path,
        args.graph_id,
        task,
        target_type,
        label_col,
        masks_prefix=args.masks_prefix,
        cols_to_skip_dict=args.type_cols_to_skip,
        verbose=args.verbose,
    )

    assert isinstance(gs_config, dict)

    input_no_protocol: str = args.input_path.replace("s3://", "")

    fs_handler = FileSystemHandler(args.input_path)

    # Write the config locally and at the input location
    with open(args.output_filename, "w") as f:
        json.dump(gs_config, f, indent=2)

    with fs_handler.pa_fs.open_output_stream(
        f"{osp.join(input_no_protocol, args.output_filename)}"
    ) as f:
        f.write(json.dumps(gs_config, indent=2).encode("utf-8"))

    print(
        f"\nConfiguration written to ./{args.output_filename}"
        f"\nand to {osp.join(args.input_path, args.output_filename)}"
    )


if __name__ == "__main__":
    main()
