import os
import tempfile
import shutil
import pandas as pd
import pytest
import boto3
from unittest.mock import patch
from moto import mock_aws

from neptune_gs.attach_gs_data import write_joined_data


@pytest.fixture
def sample_df():
    """Create a sample DataFrame for testing."""
    return pd.DataFrame(
        {
            "~id": ["1", "2", "3"],
            "feature1:String": ["a", "b", "c"],
            "feature2:Int": [1, 2, 3],
            "embedding:Vector": ["0.1;0.2", "0.3;0.4", "0.5;0.6"],
        }
    )


@pytest.fixture
def sample_dfs():
    """Create a list of sample DataFrames for testing."""
    df1 = pd.DataFrame(
        {
            "~id": ["1", "2"],
            "feature1:String": ["a", "b"],
            "feature2:Int": [1, 2],
            "embedding:Vector": ["0.1;0.2", "0.3;0.4"],
        }
    )

    df2 = pd.DataFrame(
        {
            "~id": ["3", "4"],
            "feature1:String": ["c", "d"],
            "feature2:Int": [3, 4],
            "embedding:Vector": ["0.5;0.6", "0.7;0.8"],
        }
    )

    return [df1, df2]


@pytest.fixture
def temp_dir():
    """Create a temporary directory for local file tests."""
    temp_dir = tempfile.mkdtemp()
    yield temp_dir
    shutil.rmtree(temp_dir)


@pytest.fixture(scope="function")
def aws_credentials():
    """Mocked AWS Credentials for moto."""
    os.environ["AWS_ACCESS_KEY_ID"] = "testing"
    os.environ["AWS_SECRET_ACCESS_KEY"] = "testing"
    os.environ["AWS_SECURITY_TOKEN"] = "testing"
    os.environ["AWS_SESSION_TOKEN"] = "testing"
    os.environ["AWS_DEFAULT_REGION"] = "us-east-1"


@pytest.fixture(scope="function")
def s3_client(aws_credentials):
    """Return a mocked S3 client."""
    with mock_aws():
        yield boto3.client("s3", region_name="us-east-1")


@pytest.fixture(scope="function")
def mocked_aws(aws_credentials):
    """Mock all AWS interactions."""
    with mock_aws():
        yield


# Local file system tests
def test_write_joined_data_local_csv(sample_df, temp_dir):
    """Test writing a single DataFrame to local CSV file."""
    # Call the function with local path
    output_path = os.path.join(temp_dir, "output")
    os.makedirs(output_path, exist_ok=True)

    write_joined_data(sample_df, output_path, "Person", file_format="csv")

    # Verify file was created
    output_files = os.listdir(output_path)
    assert len(output_files) == 1
    assert output_files[0].startswith("Vertex_Person_")
    assert output_files[0].endswith(".csv")


def test_write_joined_data_local_parquet(sample_df, temp_dir):
    """Test writing a single DataFrame to local Parquet file."""
    # Call the function with local path
    output_path = os.path.join(temp_dir, "output")
    os.makedirs(output_path, exist_ok=True)

    write_joined_data(sample_df, output_path, "Person", file_format="parquet")

    # Verify file was created
    output_files = os.listdir(output_path)
    assert len(output_files) == 1
    assert output_files[0].startswith("Vertex_Person_")
    assert output_files[0].endswith(".parquet")


def test_write_joined_data_local_multiple_dfs(sample_dfs, temp_dir):
    """Test writing multiple DataFrames to local files."""
    # Call the function with local path
    output_path = os.path.join(temp_dir, "output")
    os.makedirs(output_path, exist_ok=True)

    write_joined_data(sample_dfs, output_path, "Person", file_format="csv")

    # Verify files were created
    output_files = os.listdir(output_path)
    assert len(output_files) == 2
    for file in output_files:
        assert file.startswith("Vertex_Person_")
        assert file.endswith(".csv")


def test_write_joined_data_local_chunking(sample_df, temp_dir):
    """Test chunking of large DataFrames with local files."""
    # Create a larger DataFrame that will be chunked
    large_df = pd.concat([sample_df] * 3, ignore_index=True)

    # Call the function with local path and small chunk size
    output_path = os.path.join(temp_dir, "output")
    os.makedirs(output_path, exist_ok=True)

    write_joined_data(large_df, output_path, "Person", chunk_size=3)

    # Verify multiple chunk files were created
    output_files = os.listdir(output_path)
    assert len(output_files) == 3  # Should create 3 chunks


def test_write_joined_data_with_threading(sample_df, temp_dir):
    """Test writing with threading enabled."""
    output_path = os.path.join(temp_dir, "output")
    os.makedirs(output_path, exist_ok=True)

    # Call the function with threading enabled
    write_joined_data(sample_df, output_path, "Person", use_threads=True)

    # Verify file was created
    output_files = os.listdir(output_path)
    assert len(output_files) == 1


def test_write_joined_data_invalid_format():
    """Test error handling for invalid file format."""
    df = pd.DataFrame({"col1": [1, 2, 3]})

    with pytest.raises(ValueError, match="Unknown file format"):
        write_joined_data(df, "/tmp/output", "Person", file_format="invalid")  # type: ignore


# S3 tests with moto


@mock_aws
def test_write_joined_data_with_kms(sample_df, aws_credentials):
    """Test writing with KMS encryption using moto."""
    # Create bucket
    s3_client = boto3.client("s3", region_name="us-east-1")
    s3_client.create_bucket(Bucket="test-bucket")

    # Patch boto3.client to ensure it returns the moto client
    with patch("neptune_gs.fs_handler.boto3.client", return_value=s3_client):
        # Call the function with KMS key and disable threading
        write_joined_data(
            sample_df,
            "s3://test-bucket/output",
            "Person",
            file_format="csv",
            kms_key_id="my-key-id",
            use_threads=False,
        )

        # Verify the file was created with KMS encryption
        objects = s3_client.list_objects_v2(Bucket="test-bucket", Prefix="output")
        assert "Contents" in objects

        # Get the object metadata to check KMS settings
        obj_key = objects["Contents"][0]["Key"]
        metadata = s3_client.head_object(Bucket="test-bucket", Key=obj_key)

        # Verify KMS encryption was used
        assert metadata.get("ServerSideEncryption") == "aws:kms"
        assert metadata.get("SSEKMSKeyId") == "my-key-id"
