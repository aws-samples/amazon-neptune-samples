import shutil
import tempfile
from typing import Any
from unittest.mock import patch

import pytest

from neptune_gs.create_gconstruct import (
    GConstructFeature,
    GraphSchema,
    InputFormat,
    NodeEdgeInfo,
    PropertyConfig,
    create_graphstorm_config,
    get_properties_from_neptune,
    get_relations_from_neptune,
    get_relationships_manually,
    suggest_transformations,
)


@pytest.fixture
def temp_dir():
    # Create a temporary directory
    temp_dir = tempfile.mkdtemp()
    yield temp_dir
    # Clean up the temporary directory after the test
    shutil.rmtree(temp_dir)


def test_invalid_node_edge_info():
    """Should raise if neptune type keys are not part of properties list"""

    with pytest.raises(AssertionError):
        NodeEdgeInfo(
            properties=["~id", "name:Str", "age"],
            pandas_dtypes={"~id": "object", "name": "object", "age": "int64"},
            neptune_types={"name": "String", "age": "Int"},
        )


def test_suggest_feature_csv_no_neptune_type():
    """Should throw when input is CSV, property names don't include `:`, but we don't have neptune types"""

    test_schema = GraphSchema(
        vertex_types={"Person"},
        relation_types=set(),
        vertex_files={"Person": ["test.parquet"]},
        edge_files={},
        vertex_schemas={
            # Create a node type without type annotations in names or from Neptune
            "Person": NodeEdgeInfo(
                properties=["~id", "role"],
                pandas_dtypes={"~id": "object", "role": "object"},
                neptune_types=None,  # Let's be explicit
            )
        },
        edge_schemas={
            "knows": NodeEdgeInfo(
                properties=["~from", "~to"],
                pandas_dtypes={"~from": "object", "~to": "object"},
            )
        },
        input_format=InputFormat.CSV,
    )

    relationships: dict[tuple[str, str, str], dict] = {
        ("Person", "knows", "Person"): {}
    }

    # Raise,
    with pytest.raises(AssertionError):
        _ = suggest_transformations(test_schema, relationships)


def test_feature_name_no_type():
    """Test scenario where we get property names from neptune.graph.pg_schema() with CSV input"""

    # Mock schema with property names that came from neptune.graph.pg_schema()
    # but the actual CSV columns will include the type in the column name
    test_schema = GraphSchema(
        vertex_types={"Person"},
        relation_types=set(),
        vertex_files={"Person": ["test.parquet"]},
        edge_files={},
        vertex_schemas={
            "Person": NodeEdgeInfo(
                properties=["~id", "role"],
                pandas_dtypes={"~id": "object", "role": "object"},
                neptune_types={
                    "role": "String",
                },
            )
        },
        edge_schemas={
            "knows": NodeEdgeInfo(
                properties=["~from", "~to"],
                pandas_dtypes={"~from": "object", "~to": "object"},
            )
        },
        input_format=InputFormat.CSV,
    )

    relationships: dict[tuple[str, str, str], dict] = {
        ("Person", "knows", "Person"): {}
    }

    # Get feature suggestions
    feature_suggestions = suggest_transformations(test_schema, relationships)

    # Get the GConstruct feature dict assigned to the Person node type
    assert len(feature_suggestions["vertices"]["Person"]) == 1
    gc_feature_dict = feature_suggestions["vertices"]["Person"][0]
    assert gc_feature_dict, "Feature dict should not be empty"
    assert gc_feature_dict["feature_col"] == "role:String", (
        "Type has not been added to the column name"
    )
    assert gc_feature_dict["feature_name"] == "role", (
        "Original feature name was not maintained"
    )
    assert gc_feature_dict["transform"]["name"] == "to_categorical", (
        "Wrong transformation name"
    )


def test_suggest_features_from_column_names():
    """Test scenario where we get property names just from CSV cols, which include types in names."""
    test_schema = GraphSchema(
        vertex_types={"Person"},
        relation_types=set(),
        vertex_files={"Person": ["test.parquet"]},
        edge_files={},
        vertex_schemas={
            "Person": NodeEdgeInfo(
                properties=["~id", "role:String", "age:Int"],
                pandas_dtypes={
                    "~id": "object",
                    "role:String": "object",
                    "age:Int": "int64",
                },
            )
        },
        edge_schemas={
            "knows": NodeEdgeInfo(
                properties=["~from", "~to"],
                pandas_dtypes={"~from": "object", "~to": "object"},
            )
        },
        input_format=InputFormat.CSV,
    )
    relationships: dict[tuple[str, str, str], dict] = {
        ("Person", "knows", "Person"): {}
    }

    # Get feature suggestions
    feature_suggestions = suggest_transformations(test_schema, relationships)

    # role:String will be the first feature
    annotated_property = None
    for feature in feature_suggestions["vertices"]["Person"]:
        if ":" in feature["feature_col"]:
            annotated_property = feature
            break

    assert annotated_property is not None
    assert annotated_property["feature_col"] == "role:String", (
        "Original column name was not maintained"
    )
    assert annotated_property["feature_name"] == "role", (
        "Original feature name was not maintained"
    )
    assert annotated_property["transform"]["name"] == "to_categorical", (
        "Wrong transformation name"
    )


def test_create_graphstorm_config_with_vertex_features_and_edge_labels():
    """
    Test create_graphstorm_config with vertex features, edge labels, edge features
    but no vertex labels.
    """
    # Mock the dependencies
    with (
        patch(
            "neptune_gs.create_gconstruct.analyze_neptune_export"
        ) as mock_analyze_export,
        patch(
            "neptune_gs.create_gconstruct.analyze_relations"
        ) as mock_analyze_relationships,
        patch(
            "neptune_gs.create_gconstruct.analyze_properties"
        ) as mock_suggest_features,
    ):
        # Set up mock return values
        mock_analyze_export.return_value = GraphSchema(
            vertex_types={"Person", "Movie"},
            relation_types={"ACTED_IN"},
            vertex_files={
                "Person": ["Vertex_Person_0.parquet"],
                "Movie": ["Vertex_Movie_0.parquet"],
            },
            edge_files={"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]},
            vertex_schemas={},
            edge_schemas={},
            input_format=InputFormat.PARQUET,
        )

        mock_analyze_relationships.return_value = {
            ("Person", "ACTED_IN", "Movie"): {
                "source_type": "Person",
                "dest_type": "Movie",
                "relation": "ACTED_IN",
            }
        }

        mock_suggest_features.return_value = PropertyConfig(
            confirmed_features={
                "vertices": {
                    "Person": [
                        {
                            "feature_col": "age",
                            "feature_name": "age",
                            "transform": {"name": "max_min_norm"},
                        }
                    ]
                },
                "edges": {
                    "Person,ACTED_IN,Movie": [
                        {
                            "feature_col": "role",
                            "feature_name": "role",
                            "transform": {"name": "to_categorical"},
                        }
                    ]
                },
            },
            labels_config={
                "vertices": {},
                "edges": {
                    "Person,ACTED_IN,Movie": [
                        {
                            "task_type": "link_prediction",
                            "label_col": "",
                            "split_pct": [0.8, 0.1, 0.1],
                        }
                    ]
                },
            },
        )

        # Call the function under test
        result = create_graphstorm_config("dummy_path")

        # Assertions
        assert result["version"] == "gconstruct-v0.1"
        assert len(result["nodes"]) == 2
        assert len(result["edges"]) == 1

        # Check Person node config
        person_node = next(
            node for node in result["nodes"] if node["node_type"] == "Person"
        )
        assert person_node["features"] == [
            {
                "feature_col": "age",
                "feature_name": "age",
                "transform": {"name": "max_min_norm"},
            }
        ]
        assert "labels" not in person_node

        # Check Movie node config
        movie_node = next(
            node for node in result["nodes"] if node["node_type"] == "Movie"
        )
        assert "features" not in movie_node
        assert "labels" not in movie_node

        # Check ACTED_IN edge config
        edge_config = result["edges"][0]
        assert edge_config["relation"] == ["Person", "ACTED_IN", "Movie"]
        assert edge_config["features"] == [
            {
                "feature_col": "role",
                "feature_name": "role",
                "transform": {"name": "to_categorical"},
            }
        ]
        assert edge_config["labels"] == [
            {
                "task_type": "link_prediction",
                "label_col": "",
                "split_pct": [0.8, 0.1, 0.1],
            }
        ]

        # Verify that the mocked functions were called
        mock_analyze_export.assert_called_once_with("dummy_path", None)
        mock_analyze_relationships.assert_called_once()
        mock_suggest_features.assert_called_once()


def test_create_graphstorm_config_with_vertex_features_and_labels():
    """
    Test create_graphstorm_config when vertex features and labels are present,
    but edge features are not present and edge labels are present.
    """
    # Mock the dependencies
    with (
        patch(
            "neptune_gs.create_gconstruct.analyze_neptune_export"
        ) as mock_analyze_export,
        patch(
            "neptune_gs.create_gconstruct.analyze_relations"
        ) as mock_analyze_relationships,
        patch(
            "neptune_gs.create_gconstruct.analyze_properties"
        ) as mock_suggest_features,
    ):
        # Set up mock return values
        mock_analyze_export.return_value = GraphSchema(
            vertex_types={"Person", "Movie"},
            relation_types={"ACTED_IN"},
            vertex_files={
                "Person": ["Vertex_Person_0.parquet"],
                "Movie": ["Vertex_Movie_0.parquet"],
            },
            edge_files={"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]},
            vertex_schemas={},
            edge_schemas={},
            input_format=InputFormat.PARQUET,
        )

        mock_analyze_relationships.return_value = {
            ("Person", "ACTED_IN", "Movie"): {
                "source_type": "Person",
                "dest_type": "Movie",
                "relation": "ACTED_IN",
            }
        }

        mock_suggest_features.return_value = PropertyConfig(
            confirmed_features={
                "vertices": {
                    "Person": [
                        {
                            "feature_col": "age",
                            "feature_name": "age",
                            "transform": {"name": "max_min_norm"},
                        }
                    ],
                    "Movie": [],
                },
                "edges": {
                    "Person,ACTED_IN,Movie": [],
                },
            },
            labels_config={
                "vertices": {
                    "Person": [
                        {
                            "task_type": "1",
                            "label_col": "gender",
                            "split_pct": [0.8, 0.1, 0.1],
                        }
                    ],
                    "Movie": [],
                },
                "edges": {
                    "Person,ACTED_IN,Movie": [
                        {
                            "task_type": "link_prediction",
                            "label_col": "",
                            "split_pct": [0.8, 0.1, 0.1],
                        }
                    ]
                },
            },
        )

        # Call the function under test
        result = create_graphstorm_config("dummy_input_path")

        # Assertions
        assert result["version"] == "gconstruct-v0.1"
        assert len(result["nodes"]) == 2
        assert len(result["edges"]) == 1

        # Check Person node config
        person_node = next(
            node for node in result["nodes"] if node["node_type"] == "Person"
        )
        assert "features" in person_node
        assert "labels" in person_node

        # Check Movie node config
        movie_node = next(
            node for node in result["nodes"] if node["node_type"] == "Movie"
        )
        assert "features" not in movie_node
        assert "labels" not in movie_node

        # Check ACTED_IN edge config
        acted_in_edge = result["edges"][0]
        assert "features" not in acted_in_edge
        assert "labels" in acted_in_edge

        # Verify that the mocked functions were called
        mock_analyze_export.assert_called_once_with("dummy_input_path", None)
        mock_analyze_relationships.assert_called_once()
        mock_suggest_features.assert_called_once()


def test_create_graphstorm_config_with_cols_to_skip_dict():
    """
    Test create_graphstorm_config with cols_to_skip_dict parameter to ensure
    specified columns are skipped during feature extraction.
    """
    # Mock the dependencies
    with (
        patch(
            "neptune_gs.create_gconstruct.analyze_neptune_export"
        ) as mock_analyze_export,
        patch(
            "neptune_gs.create_gconstruct.analyze_relations"
        ) as mock_analyze_relations,
        patch(
            "neptune_gs.create_gconstruct.analyze_properties"
        ) as mock_analyze_properties,
    ):
        # Set up mock return values
        mock_analyze_export.return_value = GraphSchema(
            vertex_types={"Person", "Movie"},
            relation_types={"ACTED_IN"},
            vertex_files={
                "Person": ["Vertex_Person_0.parquet"],
                "Movie": ["Vertex_Movie_0.parquet"],
            },
            edge_files={"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]},
            vertex_schemas={},
            edge_schemas={},
            input_format=InputFormat.PARQUET,
        )

        mock_analyze_relations.return_value = {
            ("Person", "ACTED_IN", "Movie"): {
                "source_type": "Person",
                "dest_type": "Movie",
                "relation": "ACTED_IN",
            }
        }

        # Mock analyze_properties to return different features based on cols_to_skip_dict
        def mock_analyze_properties_func(
            schema_info,
            relationships,
            aggregate_features=False,
            task_info=None,
            cols_to_skip_dict=None,
            cols_to_keep_dict=None,
            masks_subpath=None,
        ):
            # Create properly typed features
            name_feature: GConstructFeature = {
                "feature_col": "name",
                "feature_name": "name",
                "transform": {"name": "to_categorical"},
            }

            age_feature: GConstructFeature = {
                "feature_col": "age",
                "feature_name": "age",
                "transform": {"name": "max_min_norm"},
            }

            address_feature: GConstructFeature = {
                "feature_col": "address",
                "feature_name": "address",
                "transform": {"name": "to_categorical"},
            }

            salary_feature: GConstructFeature = {
                "feature_col": "salary",
                "feature_name": "salary",
                "transform": {"name": "max_min_norm"},
            }

            role_feature: GConstructFeature = {
                "feature_col": "role",
                "feature_name": "role",
                "transform": {"name": "to_categorical"},
            }

            # Base features that should always be included
            confirmed_features: dict[str, dict[str, list[GConstructFeature]]] = {
                "vertices": {"Person": [name_feature, age_feature]},
                "edges": {"Person,ACTED_IN,Movie": [salary_feature]},
            }

            # Add features that should be skipped if cols_to_skip_dict is provided
            if (
                not cols_to_skip_dict
                or "Person" not in cols_to_skip_dict
                or "address" not in cols_to_skip_dict["Person"]
            ):
                confirmed_features["vertices"]["Person"].append(address_feature)

            if (
                not cols_to_skip_dict
                or "Person,ACTED_IN,Movie" not in cols_to_skip_dict
                or "role" not in cols_to_skip_dict["Person,ACTED_IN,Movie"]
            ):
                confirmed_features["edges"]["Person,ACTED_IN,Movie"].append(
                    role_feature
                )

            # Create empty labels config with proper typing
            labels_config: dict[str, dict[str, list[dict[str, Any]]]] = {
                "vertices": {},
                "edges": {},
            }

            return PropertyConfig(
                confirmed_features=confirmed_features, labels_config=labels_config
            )

        mock_analyze_properties.side_effect = mock_analyze_properties_func

        # Define cols_to_skip_dict
        cols_to_skip_dict = {"Person": ["address"], "Person,ACTED_IN,Movie": ["role"]}

        # Call the function with columns to skip
        result = create_graphstorm_config(
            "dummy_input_path", cols_to_skip_dict=cols_to_skip_dict
        )

        # Get the Person node and ACTED_IN edge configs
        person_node = next(
            node for node in result["nodes"] if node["node_type"] == "Person"
        )
        edge_config = result["edges"][0]

        # Check that address feature is not in Person node features
        person_features = [f["feature_name"] for f in person_node["features"]]
        assert "address" not in person_features, "Address feature should be skipped"
        assert "name" in person_features, "Name feature should be present"
        assert "age" in person_features, "Age feature should be present"

        # Check that role feature is not in ACTED_IN edge features
        edge_features = [f["feature_name"] for f in edge_config["features"]]
        assert "role" not in edge_features, "Role feature should be skipped"
        assert "salary" in edge_features, "Salary feature should be present"


def test_get_properties_from_neptune():
    """Test property extraction from Neptune schema"""
    neptune_schema = {
        "nodeLabelDetails": {
            "airport": {
                "properties": {
                    "city": {"datatypes": ["String"]},
                    "lat": {"datatypes": ["Double"]},
                    "runways": {"datatypes": ["Int"]},
                }
            }
        },
        "edgeLabelDetails": {
            "route": {
                "properties": {
                    "weight": {"datatypes": ["Int"]},
                    "dist": {"datatypes": ["Int"]},
                }
            }
        },
    }

    vertex_schemas, edge_schemas = get_properties_from_neptune(neptune_schema)

    # Check vertex schema
    assert "airport" in vertex_schemas
    airport_schema = vertex_schemas["airport"]
    assert set(airport_schema.properties) == {"~id", "city", "lat", "runways"}
    assert airport_schema.pandas_dtypes["city"] == "string"
    assert airport_schema.pandas_dtypes["lat"] == "Float64"
    assert airport_schema.pandas_dtypes["runways"] == "Int32"

    # Check edge schema
    assert "route" in edge_schemas
    route_schema = edge_schemas["route"]
    assert set(route_schema.properties) == {"~id", "~from", "~to", "weight", "dist"}
    assert route_schema.pandas_dtypes["weight"] == "Int32"
    assert route_schema.pandas_dtypes["dist"] == "Int32"


def test_get_relationships_from_neptune():
    """Test relationship extraction from Neptune schema"""
    neptune_schema = {
        "labelTriples": [
            {"~type": "route", "~from": "airport", "~to": "airport"},
            {"~type": "contains", "~from": "country", "~to": "airport"},
            {"~type": "contains", "~from": "country", "~to": "city"},
        ]
    }
    edge_files = {
        "route": ["Edge_route_0.parquet"],
        "contains": ["Edge_contains_0.parquet"],
    }

    relationships = get_relations_from_neptune(edge_files, neptune_schema)

    assert len(relationships) == 3
    assert relationships[("airport", "route", "airport")] == {
        "source_type": "airport",
        "relation": "route",
        "dest_type": "airport",
    }
    assert relationships[("country", "contains", "airport")] == {
        "source_type": "country",
        "relation": "contains",
        "dest_type": "airport",
    }
    assert relationships[("country", "contains", "city")] == {
        "source_type": "country",
        "relation": "contains",
        "dest_type": "city",
    }


def test_get_relationships_from_neptune_missing_edge():
    """Test handling of missing edges in Neptune schema"""
    neptune_schema = {
        "labelTriples": [{"~type": "route", "~from": "airport", "~to": "airport"}]
    }
    edge_files = {
        "route": ["Edge_route_0.parquet"],
        "contains": ["Edge_contains_0.parquet"],  # This edge type isn't in schema
    }

    with pytest.raises(
        ValueError, match="Could not find all relationships in Neptune schema"
    ):
        get_relations_from_neptune(edge_files, neptune_schema)


def test_get_relationships_manually_invalid():
    """
    Test get_relationships_manually when src_type is not in vertex_types.
    """
    edge_files = {"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]}
    vertex_types = {"Person", "Movie"}

    # Mock user inputs: first an invalid src_type, then valid src_type and dst_type
    with patch("builtins.input") as mock_input:
        mock_input.side_effect = [
            "InvalidType",  # invalid source type
            "Person",  # valid source type
            "Movie",  # valid dst type
            "n",  # no more edge types for relation
        ]
        result = get_relationships_manually(edge_files, vertex_types)

    expected = {
        ("Person", "ACTED_IN", "Movie"): {
            "source_type": "Person",
            "dest_type": "Movie",
            "relation": "ACTED_IN",
        }
    }

    assert result == expected, (
        "The function should return the correct relationship after prompting for a valid source type"
    )


def test_get_relationships_manually_valid():
    """
    Test get_relationships_manually with valid inputs.
    """
    edge_files = {"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]}
    vertex_types = {"Person", "Movie"}

    with patch("builtins.input") as mock_input:
        mock_input.side_effect = [
            "Person",  # source type
            "Movie",  # destination type
            "n",  # no more edge types for relation
        ]

        result = get_relationships_manually(edge_files, vertex_types)

    expected = {
        ("Person", "ACTED_IN", "Movie"): {
            "source_type": "Person",
            "dest_type": "Movie",
            "relation": "ACTED_IN",
        }
    }

    assert result == expected, f"Expected {expected}, but got {result}"


@pytest.mark.parametrize(
    "edge_files, vertex_types",
    [
        ({"ACTED_IN": []}, {"Person", "Movie"}),
        ({"ACTED_IN": ["Edge_ACTED_IN_0.parquet"]}, set()),
    ],
)
def test_get_relationships_manually_empty_input(edge_files, vertex_types):
    """
    Test get_relationships_manually with empty input for edge files and vertex types.
    """

    print(edge_files, vertex_types)
    # Should raise when either edge files of vertex types is empty
    with pytest.raises(AssertionError):
        get_relationships_manually(edge_files, vertex_types)
