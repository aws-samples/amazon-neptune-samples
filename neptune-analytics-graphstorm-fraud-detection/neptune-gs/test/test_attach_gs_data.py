import pytest
import pandas as pd
from mock import MagicMock, patch

from neptune_gs.attach_gs_data import (
    attach_gs_data_to_na,
    attach_gs_cols_to_type,
    join_and_process_embeddings,
    validate_s3_paths,
    read_gs_data,
)
from neptune_gs.fs_handler import FileSystemHandler


@pytest.fixture
def original_df():
    """Create mock original node data."""
    return pd.DataFrame(
        {
            "~id": ["3108890", "3000848", "3011847"],
            "feature1:String": ["a", "b", "c"],
            "feature2:Int": [1, 2, 3],
        }
    )


@pytest.fixture
def embeddings_df():
    """Create a mock embeddings dataframe."""
    return pd.DataFrame(
        {
            "nid": ["3108890", "3000848", "3011847"],
            "emb": [[0.1, 0.2], [0.3, 0.4], [0.5, 0.6]],
        }
    )


@pytest.fixture
def predictions_df():
    """Create a mock predictions dataframe similar to the real data."""
    return pd.DataFrame(
        {
            "nid": ["3108890", "3000848", "3011847"],
            "pred": [[0.019738536], [0.04310186], [0.05346412]],
        }
    )


def test_attach_embeddings_invalid_s3_path():
    with pytest.raises(ValueError, match="Expected an S3 URI"):
        attach_gs_data_to_na(
            input_s3="not-s3://input",
            output_s3="s3://output",
            embeddings_s3="s3://embeddings",
        )


# Data processing tests
def test_join_and_process_data(original_df, embeddings_df):
    result = join_and_process_embeddings(original_df, embeddings_df, "Person")

    assert "embedding:Vector" in result.columns
    assert len(result) == len(original_df)
    assert isinstance(result["embedding:Vector"].iloc[0], str)
    assert result["embedding:Vector"].iloc[0].count(";") == 1


# Validation tests
def test_validate_s3_paths_valid():
    validate_s3_paths("s3://bucket1", "s3://bucket2")  # Should not raise


def test_validate_s3_paths_invalid():
    with pytest.raises(ValueError):
        validate_s3_paths("s3://bucket1", "not-s3://bucket2")


def test_read_gs_predictions(mocker):
    """Test reading prediction files."""
    mock_handler = MagicMock(spec=FileSystemHandler)
    # mock_handler = mocker.Mock(spec=FileSystemHandler)
    mock_handler.list_files.return_value = [
        "s3://bucket/Transaction/pred.predict-00000_00000.parquet",
        "s3://bucket/Transaction/pred.predict-00001_00000.parquet",
    ]

    # Mock the dataframe creation
    mock_handler.create_df_from_files.return_value = pd.DataFrame(
        {"nid": ["3108890", "3000848"], "pred": [[0.019738536], [0.04310186]]}
    )

    result = read_gs_data(
        handler=mock_handler,
        gs_path="s3://bucket",
        node_type="Transaction",
        gs_data_kind="predictions",
    )

    # Verify the correct pattern was used to find files
    mock_handler.list_files.assert_called_once_with(
        path="s3://bucket/Transaction",
        pattern=r"pred.predict-[0-9]{5}_[0-9]{5}\.parquet$",
    )

    assert isinstance(result, pd.DataFrame)
    assert "nid" in result.columns
    assert "pred" in result.columns


def test_attach_gs_cols_with_predictions(embeddings_df, predictions_df, original_df):
    """Test attaching both embeddings and predictions to node data."""
    # Mock handlers
    input_handler = MagicMock(spec=FileSystemHandler)
    embedding_handler = MagicMock(spec=FileSystemHandler)
    predictions_handler = MagicMock(spec=FileSystemHandler)

    # Setup paths
    embedding_handler.path = "s3://fake/embeddings"
    predictions_handler.path = "s3://fake/predictions"
    input_handler.path = "s3://fake/input"

    # Mock the data reading behavior
    embedding_handler.create_df_from_files.return_value = embeddings_df
    predictions_handler.create_df_from_files.return_value = predictions_df
    input_handler.create_df_from_files.return_value = original_df

    # Mock file listing
    embedding_handler.list_files.return_value = ["embed-00000_00000.parquet"]
    predictions_handler.list_files.return_value = ["pred.predict-00000_00000.parquet"]

    with patch("neptune_gs.attach_gs_data.write_joined_data") as mock_write:
        attach_gs_cols_to_type(
            node_type="TestNode",
            input_handler=input_handler,
            embedding_handler=embedding_handler,
            output_s3="s3://fake/output",
            node_files=["node_file.csv"],
            predictions_handler=predictions_handler,
            use_threads=False,
        )

    # Verify data was processed correctly
    mock_write.assert_called_once()
    # The first argument to write_joined_data should be a list of DataFrames
    written_data = mock_write.call_args[0][0]
    assert isinstance(written_data, list)
    assert len(written_data) > 0
    assert isinstance(written_data[0], pd.DataFrame)
    # Verify the data contains both embeddings and predictions
    assert "embedding:Vector" in written_data[0].columns
    assert "pred:Float[]" in written_data[0].columns


def test_attach_gs_cols_mismatched_predictions(
    embeddings_df, predictions_df, original_df
):
    """Test error handling when predictions and embeddings have different sizes."""

    # Create mismatched predictions
    mismatched_predictions = predictions_df.iloc[:-1]  # Remove last row

    # Create MagicMocks
    input_handler = MagicMock(spec=FileSystemHandler)
    embedding_handler = MagicMock(spec=FileSystemHandler)
    predictions_handler = MagicMock(spec=FileSystemHandler)

    # Setup handler paths
    input_handler.path = "s3://fake/input"
    embedding_handler.path = "s3://fake/embeddings"
    predictions_handler.path = "s3://fake/predictions"

    # Setup handler returns
    input_handler.create_df_from_files.return_value = original_df
    embedding_handler.create_df_from_files.return_value = embeddings_df
    predictions_handler.create_df_from_files.return_value = mismatched_predictions
    input_handler.list_files.return_value = ["Vertex_TestNode_0.csv"]
    embedding_handler.list_files.return_value = ["embed-00000_00000.parquet"]
    predictions_handler.list_files.return_value = ["pred.predict-00000_00000.parquet"]

    with pytest.raises(RuntimeError) as exc_info:
        attach_gs_cols_to_type(
            node_type="TestNode",
            input_handler=input_handler,
            embedding_handler=embedding_handler,
            output_s3="s3://fake/output",
            node_files=["node_file.csv"],
            predictions_handler=predictions_handler,
        )

    assert "different number of rows" in str(exc_info.value)
    assert "3 != 2" in str(exc_info.value)


def test_attach_gs_cols_without_predictions(embeddings_df, original_df):
    """Test attaching only embeddings without predictions."""

    # Mock handlers
    input_handler = MagicMock(spec=FileSystemHandler)
    embedding_handler = MagicMock(spec=FileSystemHandler)

    # Setup handler returns
    embedding_handler.create_df_from_files.return_value = embeddings_df
    embedding_handler.path = "s3://fake/embeddings"
    embedding_handler.list_files.return_value = ["embed-00000_00000.parquet"]
    input_handler.create_df_from_files.return_value = original_df
    input_handler.path = "s3://fake/input"

    with patch("neptune_gs.attach_gs_data.write_joined_data") as mock_write:
        attach_gs_cols_to_type(
            node_type="TestNode",
            input_handler=input_handler,
            embedding_handler=embedding_handler,
            output_s3="s3://fake/output",
            node_files=["node_file.csv"],
            predictions_handler=None,
        )

    # Verify data was processed correctly
    mock_write.assert_called_once()
    written_data = mock_write.call_args[0][0]
    # We call the write function with a list of DataFrames
    assert isinstance(written_data, list)
    assert len(written_data) > 0
    assert isinstance(written_data[0], pd.DataFrame)
    # Verify the data contains embeddings but not predictions
    assert "embedding:Vector" in written_data[0].columns
    assert "pred" not in written_data[0].columns
