import logging
from pprint import pformat
from typing import Any, TypedDict


def _extract_val_type(gremlin_property: dict):
    """Convert Gremlin property to feature representation, with type annotation

    Parameters
    ----------
    gremlin_property : dict or str
        Can be either a string, or a gremlin property value dict, with two keys:

            '@type': A string representation of the property type, e.g. 'g:Int32'
            '@value': The property value

    Returns
    -------
        tuple(str, Any)
            The type-annotated name of the property, and its value, converted the the appropriate type
            according to the input dict. The types are modified to match the types used in Neptune CSV
            export.

    Examples
    --------

        >>> gremlin_dict = {"type@": "g:Float", "@value": 1.0}
        >>> extract_val_type(gremlin_dict)
        ("Float", 1.0)
    """
    if isinstance(gremlin_property, str):
        return ("String", gremlin_property)
    elif isinstance(gremlin_property, dict):
        val_type: str = gremlin_property["@type"].split(":")[1]
        # TODO: Add type name conversion for all possible types
        # Skip the int size annotation because the export step does not provide noe
        val_type = "Int" if val_type.startswith("Int") else val_type
        SUPPORTED_TYPES = ["String", "Float", "Int"]
        assert val_type in SUPPORTED_TYPES, (
            f"We currently only support {SUPPORTED_TYPES} for inference, got {val_type}"
        )

        # TODO: Cast values to correct type
        return (val_type, gremlin_property["@value"])


class FeatColName(TypedDict):
    feature_col: str
    feature_name: str


def extract_nodes_from_response(
    node_response,
    train_node_features: dict[str, list[str]],
    gconstruct_node_features: dict[str, list[FeatColName]],
) -> list[dict]:
    """Transform Gremlin node response to GraphStorm node format.

    Parameters
    ----------
    node_response : dict
        Gremlin response containing node data in raw format
    train_node_features : dict[str, list[str]]
        Dictionary mapping node types to lists of feature names used during training
    gconstruct_node_features : dict[str, list[FeatColName]]
        Dictionary mapping node types to lists of feature column configurations from GConstruct

    Returns
    -------
    list[dict]
        List of node dictionaries in GraphStorm format, each containing:
        - node_type: The type of the node
        - node_id: Unique identifier for the node
        - features: Dictionary of node features with their values
    """
    # Transform nodes to match GraphStorm spec
    nodes: list[dict] = []
    for node_dict in node_response["result"]["data"]["@value"]:
        node_response = node_dict["@value"]

        # node_val and prop_list are a flattened dicts as a list, with
        # values being the item after the key, e.g.
        # ['nodeType', 'nodeTypeVal', 'nodeId', 'nodeIdVal']
        # So we find the index of the key, then get the next value with +1
        ntype: str = node_response[node_response.index("nodeType") + 1]
        prop_list: list = node_response[node_response.index("properties") + 1]["@value"]

        neptune_property_dict = {}
        # Iterate with step 2 to get properties, collect them in
        # dict {prop_name_with_type: prop_value}
        for property_name_idx in range(0, len(prop_list), 2):
            prop_name: str = prop_list[property_name_idx]
            prop_dict: dict = prop_list[property_name_idx + 1]
            prop_type, prop_val = _extract_val_type(prop_dict["@value"][0])
            typed_prop_name = f"{prop_name}:{prop_type}"
            neptune_property_dict[typed_prop_name] = prop_val

        train_features_for_ntype = train_node_features.get(ntype, None)
        gcon_features_for_ntype = gconstruct_node_features.get(ntype, None)
        request_feature_dict: dict[str, Any] = {}
        if train_features_for_ntype and gcon_features_for_ntype:
            # First, go through every feature for current ntype from GConstruct,
            # and ensure it was used during training
            for typed_fname, fval in neptune_property_dict.items():
                for feat_col_name in gcon_features_for_ntype:
                    if typed_fname == feat_col_name["feature_col"]:
                        # Get the feature name GConstruct produced for training
                        transformed_fname = feat_col_name["feature_name"]
                        # Only keep feature for request if it was used during training
                        if transformed_fname in train_features_for_ntype:
                            request_feature_dict[typed_fname] = fval
                        break
                else:
                    # If break wasn't triggered, we couldn't find a
                    # matching column name for the property in GConstruct,
                    # so likely the property wasn't used during graph construction
                    logging.debug(
                        "Could not match typed property '%s' "
                        "to a GConstruct feature column for node type '%s'. "
                        "Candidates were: %s",
                        typed_fname,
                        ntype,
                        pformat(gcon_features_for_ntype),
                    )

            # Second, ensure every feature used during training gets a value,
            # even if it was missing in the Neptune graph
            for train_fname in train_features_for_ntype:
                # Get reverse match from train_fname to column name
                for feat_col_name in gcon_features_for_ntype:
                    if train_fname == feat_col_name["feature_name"]:
                        feat_column = feat_col_name["feature_col"]
                        break
                else:
                    raise ValueError(
                        f"Could not match training feature name {train_fname} "
                        f"to a GConstruct feature name for {ntype}. "
                        f"Candidates were: {gcon_features_for_ntype}"
                    )
                # If a feature was used in training, but is not in the request,
                # add it with a default value.
                # TODO: GConstruct needs a better way to indicate missing values
                GCONSTRUCT_MISSING = "nan"
                if feat_column not in request_feature_dict:
                    request_feature_dict[feat_column] = GCONSTRUCT_MISSING
        else:
            request_feature_dict = {}

        flat_node_dict = {
            "node_type": ntype,
            "node_id": node_response[node_response.index("nodeId") + 1],
            "features": request_feature_dict,
        }
        nodes.append(flat_node_dict)

    return nodes


def extract_edges_from_response(edge_response, add_reverse) -> list[dict]:
    """Transform Gremlin edge response to GraphStorm edge format.

    Parameters
    ----------
    edge_response : dict
        Gremlin response containing edge data in raw format
    add_reverse : bool
        Whether to add reverse edges to the output. If True, for each edge A->B,
        a reverse edge B->A will be added with "-rev" suffix to the edge type

    Returns
    -------
    list[dict]
        List of edge dictionaries in GraphStorm format, each containing:
        - edge_type: List of [source_type, edge_type, target_type]
        - src_node_id: ID of the source node
        - dest_node_id: ID of the destination node
        - features: Dictionary of edge features (currently empty)
    """
    # Transform edges to match GraphStorm spec
    edges: list[dict] = []
    for edge_dict in edge_response["result"]["data"]["@value"]:
        edge_val = edge_dict["@value"]
        edge = {
            "edge_type": [
                edge_val[edge_val.index("fromType") + 1],
                edge_val[edge_val.index("edgeType") + 1],
                edge_val[edge_val.index("toType") + 1],
            ],
            "src_node_id": edge_val[edge_val.index("fromId") + 1],
            "dest_node_id": edge_val[edge_val.index("toId") + 1],
            "features": {},
        }
        # Add forward edge
        edges.append(edge)
        # TODO: Support edge property to feature conversion

        # Add reverse edge if needed
        if add_reverse:
            rev_edge = {
                "edge_type": [
                    edge_val[edge_val.index("toType") + 1],
                    f"{edge_val[edge_val.index('edgeType') + 1]}-rev",
                    edge_val[edge_val.index("fromType") + 1],
                ],
                "src_node_id": edge_val[edge_val.index("toId") + 1],
                "dest_node_id": edge_val[edge_val.index("fromId") + 1],
                "features": {},
            }
            edges.append(rev_edge)

    return edges


def extract_features_from_train_config(train_config_dict: dict) -> dict[str, list[str]]:
    """Extract node feature names from training configuration.

    Parameters
    ----------
    train_config_dict : dict
        Dictionary containing the training configuration, typically loaded from a YAML file.
        Expected to have a 'gsf' section with either 'runtime' or 'gnn' subsections
        containing 'node_feat_name'.

    Returns
    -------
    dict[str, list[str]]
        Dictionary mapping node types to lists of feature names used during training.
        Each feature string is parsed from format "node_type:feat1,feat2" into
        {'node_type': ['feat1', 'feat2']}.
    """
    train_node_features: dict[str, list[str]] = {}
    train_nfeature_list: list[str] = []
    train_config_dict = train_config_dict["gsf"]
    # Try to get feature list from runtime, fallback to "gnn" section
    if "runtime" in train_config_dict:
        if "node_feat_name" in train_config_dict["runtime"]:
            train_nfeature_list = train_config_dict["gnn"]["node_feat_name"]
    # If we didn't find feature list in runtime, try getting from gsf
    if len(train_nfeature_list) == 0:
        if "gnn" in train_config_dict:
            if "node_feat_name" in train_config_dict["gnn"]:
                train_nfeature_list = train_config_dict["gnn"]["node_feat_name"]

    # Each feature_str is of the format node_type:feat1,feat2
    for feature_str in train_nfeature_list:
        node_type, features = feature_str.split(":")
        train_node_features[node_type] = features.split(",")

    return train_node_features


def extract_features_from_gconstruct_config(
    gconstruct_config_dict: dict,
) -> dict[str, list[FeatColName]]:
    """Extract feature column configurations from GConstruct configuration.

    Parameters
    ----------
    gconstruct_config_dict : dict
        Dictionary containing the GConstruct configuration, typically loaded from a JSON file.
        Expected to have a 'nodes' section containing node configurations with 'features'
        and optional 'labels' subsections.

    Returns
    -------
    dict[str, list[FeatColName]]
        Dictionary mapping node types to lists of feature column configurations.
        Each configuration contains:
        - feature_col: The column name in the source data
        - feature_name: The transformed feature name used in training

    Notes
    -----
    The function also processes label fields to avoid missing key errors.
    Multi-column features are not supported for online inference.
    """
    node_configs: list[dict] = gconstruct_config_dict["nodes"]

    gconstruct_node_features: dict[str, list[FeatColName]] = {}
    for node_config in node_configs:
        ntype = node_config["node_type"]
        feat_list_for_type: list[FeatColName] = []
        for feat_config in node_config.get("features", []):
            assert isinstance(feat_config, dict)
            feat_col = feat_config["feature_col"]
            if isinstance(feat_col, list):
                assert len(feat_col) == 1, (
                    "We do not support multi-col features for online inference, got "
                    f"{feat_col} for node type: {ntype}"
                )
                feat_col = feat_col[0]

            assert isinstance(feat_col, str), (
                f"'feat_col' should be a list or str, got {feat_col}, {type(feat_col)=}"
            )

            feat_name = feat_config.get("feature_name", feat_col)
            feat_list_for_type.append(
                {"feature_col": feat_col, "feature_name": feat_name}
            )
        # Also add label fields if any to avoid missing key errors
        for label_config in node_config.get("labels", []):
            label_col = label_config["label_col"]
            feat_list_for_type.append(
                {"feature_col": label_col, "feature_name": label_col}
            )

        gconstruct_node_features[ntype] = feat_list_for_type

    return gconstruct_node_features


def prepare_payload(
    node_response: dict,
    edge_response: dict,
    tx_id: str,
    gconstruct_config_dict: dict,
    train_config_dict: dict,
    target_node_type: str,
    add_reverse: bool = True,
) -> dict:
    """Convert Gremlin node/edge responses to GraphStorm payload format.

    Parameters
    ----------
    node_response : dict
        Gremlin response for sampled nodes, containing raw node data in Gremlin format
    edge_response : dict
        Gremlin response for sampled edges, containing raw edge data in Gremlin format
    tx_id : str
        Transaction ID to predict for, used as the target node in the inference request
    gconstruct_config_dict : dict
        Dict representation of runtime GConstruct config JSON used when building the graph.
        Contains feature column mappings and node/edge type configurations.
    train_config_dict : dict
        Dict representation of runtime train config YAML used when training the model.
        Contains information about which features were used during training.
    target_node_type : str
        Type of the target node for inference.
    add_reverse : bool, optional
        Whether to add reverse edges to the graph, by default True


    Returns
    -------
    dict
        GraphStorm inference request payload containing:
        - version: API version string
        - gml_task: Task type (node_classification)
        - graph: Dictionary with nodes and edges in GraphStorm format
        - targets: List of target nodes for inference

    Notes
    -----
    The function performs several transformations:
    1. Extracts features from training and GConstruct configs
    2. Converts nodes to GraphStorm format with proper feature mapping
    3. Converts edges to GraphStorm format, optionally adding reverse edges
    4. Packages everything in the required payload structure
    """
    train_node_features = extract_features_from_train_config(train_config_dict)
    gconstruct_node_features = extract_features_from_gconstruct_config(
        gconstruct_config_dict
    )
    # TODO: Do we need to handle type names for labels as well?

    # Transform nodes to match GraphStorm spec
    nodes = extract_nodes_from_response(
        node_response, train_node_features, gconstruct_node_features
    )
    # Transform edges to match GraphStorm spec
    edges = extract_edges_from_response(edge_response, add_reverse)

    return {
        "version": "gs-realtime-v0.1",
        "gml_task": "node_classification",
        "graph": {"nodes": nodes, "edges": edges},
        "targets": [{"node_type": target_node_type, "node_id": tx_id}],
    }
