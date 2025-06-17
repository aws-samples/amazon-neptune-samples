import textwrap
from pathlib import Path

import pandas as pd
import pyarrow as pa
import pytest

from neptune_gs.fs_handler import FileSystemHandler


def test_create_df_from_files_multiline_csv(tmp_path: Path):
    # Create a test CSV file with multiline content
    test_csv_content = textwrap.dedent("""\
        "~id","~label","name:String","bio:String"
        "1","Actor;Person","John Doe","This is a
        multiline biography
        for John"
        "2","Actor;Person","Jane Smith","Another
        multiline
        description"
        """)

    # Create directory and file
    test_dir = tmp_path / "test_data"
    test_dir.mkdir()
    csv_path = test_dir / "test_multiline.csv"
    csv_path.write_text(test_csv_content)

    # Initialize FileSystemHandler with the test directory
    handler = FileSystemHandler(str(test_dir))

    # Read the CSV file
    actual_df = handler.create_df_from_files(str(csv_path))

    # Expected DataFrame
    expected_data = {
        "~id": [1, 2],
        "~label": ["Actor;Person", "Actor;Person"],
        "name:String": ["John Doe", "Jane Smith"],
        "bio:String": [
            "This is a\nmultiline biography\nfor John",
            "Another\nmultiline\ndescription",
        ],
    }
    expected_df = pd.DataFrame(
        expected_data,
    )
    # Convert dtypes to match if needed
    expected_df = expected_df.astype(actual_df.dtypes)

    # Verify the results
    pd.testing.assert_frame_equal(
        actual_df.reset_index(drop=True),
        expected_df.reset_index(drop=True),
        check_dtype=True,
    )


def test_create_df_from_files_multiple_multiline_csvs(tmp_path):
    # Create first CSV file
    csv_content1 = textwrap.dedent("""\
        "~id","~label","name:String","bio:String"
        "1","Actor;Person","John Doe","This is a
        multiline biography
        for John"
        """)

    # Create second CSV file
    csv_content2 = textwrap.dedent("""\
        "~id","~label","name:String","bio:String"
        "2","Actor;Person","Jane Smith","Another
        multiline
        description"
        """)

    # Create directory and files
    test_dir = tmp_path / "test_data"
    test_dir.mkdir()
    csv_path1 = test_dir / "test1.csv"
    csv_path2 = test_dir / "test2.csv"
    csv_path1.write_text(csv_content1)
    csv_path2.write_text(csv_content2)

    # Initialize FileSystemHandler with the test directory
    handler = FileSystemHandler(str(test_dir))

    # Read both CSV files
    actual_df = handler.create_df_from_files([str(csv_path1), str(csv_path2)])

    # Expected DataFrame
    expected_data = {
        "~id": [1, 2],
        "~label": ["Actor;Person", "Actor;Person"],
        "name:String": ["John Doe", "Jane Smith"],
        "bio:String": [
            "This is a\nmultiline biography\nfor John",
            "Another\nmultiline\ndescription",
        ],
    }
    expected_df = pd.DataFrame(expected_data)
    # Convert dtypes to match if needed
    expected_df = expected_df.astype(actual_df.dtypes)

    # Verify the results
    pd.testing.assert_frame_equal(
        actual_df.reset_index(drop=True),
        expected_df.reset_index(drop=True),
        check_dtype=False,
    )


@pytest.fixture(scope="module", name="schema_json")
def test_create_pyarrow_schema_from_neptune():
    # Example schema from Neptune
    # Source:
    # https://docs.aws.amazon.com/neptune-analytics/latest/userguide/custom-algorithms-property-graph-schema.html
    schema_json = {
        "edgeLabelDetails": {
            "route": {
                "properties": {
                    "weight": {"datatypes": ["Int"]},
                    "dist": {"datatypes": ["Int"]},
                }
            },
            "contains": {"properties": {"weight": {"datatypes": ["Int"]}}},
        },
        "edgeLabels": ["route", "contains"],
        "nodeLabels": ["version", "airport", "continent", "country"],
        "labelTriples": [
            {"~type": "route", "~from": "airport", "~to": "airport"},
            {"~type": "contains", "~from": "country", "~to": "airport"},
            {"~type": "contains", "~from": "continent", "~to": "airport"},
        ],
        "nodeLabelDetails": {
            "continent": {
                "properties": {
                    "type": {"datatypes": ["String"]},
                    "code": {"datatypes": ["String"]},
                    "desc": {"datatypes": ["String"]},
                }
            },
            "airport": {
                "properties": {
                    "type": {"datatypes": ["String"]},
                    "city": {"datatypes": ["String"]},
                    "icao": {"datatypes": ["String"]},
                    "code": {"datatypes": ["String"]},
                    "country": {"datatypes": ["String"]},
                    "lat": {"datatypes": ["Double"]},
                    "longest": {"datatypes": ["Int"]},
                    "runways": {"datatypes": ["Int"]},
                    "desc": {"datatypes": ["String"]},
                    "lon": {"datatypes": ["Double"]},
                    "region": {"datatypes": ["String"]},
                    "elev": {"datatypes": ["Int"]},
                }
            },
            "country": {
                "properties": {
                    "type": {"datatypes": ["String"]},
                    "code": {"datatypes": ["String"]},
                    "desc": {"datatypes": ["String"]},
                }
            },
            "version": {
                "properties": {
                    "date": {"datatypes": ["String"]},
                    "desc": {"datatypes": ["String"]},
                    "author": {"datatypes": ["String"]},
                    "type": {"datatypes": ["String"]},
                    "code": {"datatypes": ["String"]},
                }
            },
        },
    }

    yield schema_json


def test_node_schema(schema_json: dict):
    # Test airport node schema
    airport_schema = FileSystemHandler.create_pyarrow_schema_from_neptune(
        schema_json, "airport"
    )

    # Check that all fields are present
    expected_fields = {
        "~id": pa.string(),
        "type:String": pa.string(),
        "city:String": pa.string(),
        "icao:String": pa.string(),
        "code:String": pa.string(),
        "country:String": pa.string(),
        "lat:Double": pa.float64(),
        "longest:Int": pa.int64(),
        "runways:Int": pa.int64(),
        "desc:String": pa.string(),
        "lon:Double": pa.float64(),
        "region:String": pa.string(),
        "elev:Int": pa.int64(),
    }

    assert len(airport_schema) == len(expected_fields)
    for field in airport_schema:
        assert field.name in expected_fields
        assert field.type == expected_fields[field.name]
        assert field.nullable


def test_edge_schema(schema_json: dict):
    # Test edge schema
    edge_schema = FileSystemHandler.create_pyarrow_schema_from_neptune(schema_json)

    # Check required edge fields
    required_fields = {"~from", "~to", "~label"}
    schema_fields = {field.name for field in edge_schema}
    assert required_fields.issubset(schema_fields)

    # Check edge properties
    expected_properties = {"weight:Int": pa.int64(), "dist:Int": pa.int64()}

    for field in edge_schema:
        if field.name in expected_properties:
            assert field.type == expected_properties[field.name]
            assert field.nullable


def test_invalid_node_type(schema_json: dict):
    # Test that requesting an invalid node type raises an error
    with pytest.raises(ValueError):
        FileSystemHandler.create_pyarrow_schema_from_neptune(
            schema_json, "invalid_type"
        )


def test_schema_with_vectors(schema_json: dict):
    # Test schema with vector properties
    schema_with_vectors = schema_json.copy()
    schema_with_vectors["nodeLabelDetails"]["airport"]["properties"]["embedding"] = {
        "datatypes": ["Vector"]
    }

    airport_schema = FileSystemHandler.create_pyarrow_schema_from_neptune(
        schema_with_vectors, "airport"
    )
    vector_field = None
    for field in airport_schema:
        if field.name == "embedding:Vector":
            vector_field = field
            break

    assert vector_field is not None
    assert isinstance(vector_field.type, pa.ListType)
    assert vector_field.type.value_type == pa.float32()
