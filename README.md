# neptune-aws-services-price-graph
## Neptune AWS Regions, Services and Prices Graph
[Amazon Neptune](https://aws.amazon.com/neptune/) is a fast, reliable, fully managed graph database service that makes it easy to build and run applications that work with highly connected datasets. The core of Neptune is a purpose-built, high-performance graph database engine that is optimized for storing billions of relationships and querying the graph with milliseconds latency.

## Running the example

### Option-1 - Loading from CSV files
If you want to quickly load the data, experiment with Amazon Neptune and are not worried if the data is current, please follow the steps below. 
1. Create an EC2 instance as defined in the [documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-launch-ec2-instance.html). Follow the steps in the [documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-gremlin-console.html) to install and configure the Gremlin console. The EC2 instance will be used to load the data and execute Gremlin queries.
1. Ensure you have setup the prerequisites for loading data from Amazon S3 to Amazon Neptune as defined in the [documentation](https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM.html).
1. Download and extract the csv archive from [here](./csv/export.tar.gz).
1. Copy the extracted CSV files to the S3 bucket.
    ```
    cd export/
    aws s3 sync ./ s3://<S3-BUCKET-NAME>
    ```
1. Load the CSV files to Neptune
    ```
    curl -X POST -H 'Content-Type: application/json' http://<NEPTUNE-ENDPOINT>:8182/loader -d '
    {
        "source" : "s3://<S3-BUCKET-NAME>/ ",
        "format" : "csv",
        "iamRoleArn" : "arn:aws:iam::<ACCOUNT-ID>:role/<ROLE-NAME>",
        "region" : "us-east-1",
        "failOnError" : "FALSE"
    }'
    ```
1. To verify the status of your data loading, execute the below command using the **loadId** returned from the previous command.
    ```
    curl -G 'http://<NEPTUNE-ENDPOINT>:8182/loader/<LOAD-ID>’
    ```

### Option-2 - Fetch latest data and load to Neptune
1. Launch an EC2 instance in the same VPC as your Neptune cluster using the  **Amazon Linux AMI**. Assign an Instance Role with **AmazonEC2ReadOnlyAccess** permission. You will need to SSH into this instance to run the code.
1. Clone this repository to your EC2 instance.
1. Install Python
    ```
    sudo yum install -y git python36 python36-pip.noarch
    ```
1. Execute the script to fetch data and load to your Neptune cluster.
    ```
    cd neptune-aws-services-price-graph
    sudo pip-3.6 install -r requirements.txt
    python3 main.py <Neptune-endpoint> <Neptune-port>
    ```
