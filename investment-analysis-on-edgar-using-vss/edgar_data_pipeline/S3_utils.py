import boto3
import os
import numpy as np
import pandas as pd

class S3Utils:
    def __init__(self, s3_bucket):
        """
        Initializes the S3Utils class with the specified S3 bucket.

        Args:
        - s3_bucket (str): The name of the S3 bucket.

        Initializes the S3 client using the 'benchmark' profile from the AWS credentials file.
        """
        self.s3_bucket = s3_bucket

        # Initialize the S3 client
        self.session = boto3.Session(profile_name='benchmark')
        self.s3 = self.session.client(service_name='s3')

    def upload_to_s3(self, local_file, s3_object_key):
        """
        Uploads a local file to the specified location in the S3 bucket.

        Args:
        - local_file (str): The local file path to be uploaded.
        - s3_object_key (str): The object key specifying the location in the S3 bucket.

        Prints a success message upon successful upload.

        Raises:
        - Exception: If an error occurs during the file upload process.
        """
        try:
            # Upload the local file to the S3 bucket
            self.s3.upload_file(local_file, self.s3_bucket, s3_object_key)
            print(f'Successfully uploaded {local_file} to {self.s3_bucket}/{s3_object_key}')
        except Exception as e:
            print(f'Error uploading file to S3: {str(e)}')

    def download_from_s3(self, s3_object_key, local_file):
        """
        Downloads a file from the specified location in the S3 bucket to the local directory.

        Args:
        - s3_object_key (str): The object key specifying the location in the S3 bucket.
        - local_file (str): The local file path where the S3 file will be downloaded.

        Prints a success message upon successful download.

        Raises:
        - Exception: If an error occurs during the file download process.
        """
        try:
            # Download the file from S3 to the local directory
            self.s3.download_file(self.s3_bucket, s3_object_key, local_file)
            print(f'Successfully downloaded {s3_object_key} to {local_file}')
        except Exception as e:
            print(f'Error downloading file from S3: {str(e)}')

    def list_files_with_keyword(self, keyword):
        """
        Lists all files in the S3 bucket with a specified keyword in their object key.

        Args:
        - keyword (str): The keyword to search for in S3 object keys.

        Returns:
        - list: A list of filenames containing the specified keyword.

        If no matching files are found, an empty list is returned.

        Raises:
        - Exception: If an error occurs during the file listing process.
        """
        try:
            # List all objects in the S3 bucket
            objects = self.s3.list_objects_v2(Bucket=self.s3_bucket, Prefix="data/CP/SemOpenAlex/v6/")
            if 'Contents' in objects:
                matching_files = [obj['Key'] for obj in objects['Contents'] if keyword in obj['Key']]
                csv_filenames = [path.split('/')[-1] for path in matching_files]
                return csv_filenames
            else:
                return []
        except Exception as e:
            print(f'Error listing files in S3: {str(e)}')
            return

    def list_all_files(self):
        """
        Lists all files in the specified S3 bucket and directory.

        Returns:
        - list: A list of filenames in the specified S3 directory.

        If no files are found, an empty list is returned.

        Raises:
        - Exception: If an error occurs during the file listing process.
        """
        try:
            # List all objects in the S3 bucket
            objects = self.s3.list_objects_v2(Bucket=self.s3_bucket, Prefix="neptune-formatted/imdb-minimized/")
            if 'Contents' in objects:
                matching_files = [obj['Key'] for obj in objects['Contents']]
                csv_filenames = [path.split('/')[-1] for path in matching_files]
                return csv_filenames
            else:
                return []
        except Exception as e:
            print(f'Error listing files in S3: {str(e)}')
            return

    def delete_file_from_local(self, local_folder, filename):
        """
        Deletes a file from the local directory.

        Args:
        - local_folder (str): The local folder path where the file is located.
        - filename (str): The name of the file to be deleted.

        Prints a success message upon successful file deletion.

        If the file does not exist, it prints a message indicating its non-existence.

        Raises:
        - None: No exceptions are explicitly raised during this method.
        """
        file_path = local_folder + filename
        if os.path.exists(file_path):
            os.remove(file_path)
            print(f"File '{file_path}' has been deleted.")
        else:
            print(f"File '{file_path}' does not exist.")

    def move_files_between_buckets(self, local_download_folder, upload_s3_key_folder, download_s3_key_folder, filenames_list):
        """
        Moves files between S3 buckets by downloading, uploading, and deleting local copies.

        Args:
        - local_download_folder (str): The local folder where files are downloaded before uploading.
        - upload_s3_key_folder (str): The S3 object key folder for uploading files.
        - download_s3_key_folder (str): The S3 object key folder for downloading files.
        - filenames_list (list): A list of filenames to be moved between S3 buckets.

        Downloads each file from the source S3 bucket, uploads it to the destination S3 bucket,
        and deletes the local copy after successful upload.

        Prints error messages if any exceptions occur during the download, upload, or deletion process.

        Raises:
        - None: No exceptions are explicitly raised during this method.
        """
        for filename in filenames_list:
            download_s3_key = download_s3_key_folder + filename # Replace with the desired object key in your S3 bucket
            # dowload from S3 API call
            dowloaded_file_location = local_download_folder + filename
            try:
                # Upload the local file to S3
                self.download_from_s3(download_s3_key, dowloaded_file_location)
            except Exception as e:
                print(f'Error in main method: {str(e)}')

            # Local file to upload
            local_file = local_download_folder + filename  # Replace with the path to your local file
            
            upload_s3_key = upload_s3_key_folder + filename # Replace with the desired object key in your S3 bucket

            # upload to S3 API call
            try:
                # Upload the local file to S3
                self.upload_to_s3(local_file, upload_s3_key)
            except Exception as e:
                print(f'Error in main method: {str(e)}')
            
            self.delete_file_from_local(local_download_folder, filename)