package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/neptunedata"
	"github.com/aws/aws-sdk-go-v2/service/neptunedata/types"
)

func main() {
	// Configuration - Replace with your actual Neptune cluster details
	clusterEndpoint := "my-cluster-name.cluster-abcdefgh1234.my-region.neptune.amazonaws.com"
	port := 8182
	region := "my-region"

	// Configure Neptune client
	// Configure HTTP client with no timeout
	endpoint := fmt.Sprintf("https://%s:%d", clusterEndpoint, port)
	// Load AWS SDK configuration
	sdkConfig, _ := config.LoadDefaultConfig(
		context.TODO(),
		config.WithRegion(region),
		config.WithHTTPClient(&http.Client{Timeout: 0}),
	)
	
	// Create Neptune client with custom endpoint
	client := neptunedata.NewFromConfig(sdkConfig, func(o *neptunedata.Options) {
		o.BaseEndpoint = aws.String(endpoint)
		o.Retryer = aws.NopRetryer{} // Do not retry calls if they fail
	})

	// ========================================
	// Example 1: Execute Gremlin Query
	// ========================================
	currentTime := time.Now().Format(time.RFC3339)
	gremlinQuery := fmt.Sprintf(`g.mergeV([(T.label):'person']).
            option(Merge.onCreate,[name:'alice', age:10]).
            option(Merge.onMatch,[lastUpdated:single(datetime('%s'))])`, currentTime)
	serializer := "application/vnd.gremlin-v2.0+json;types=false"
	
	gremlinInput := &neptunedata.ExecuteGremlinQueryInput{
		GremlinQuery: &gremlinQuery,
		Serializer:   &serializer,
	}
	gremlinResult, err := client.ExecuteGremlinQuery(context.TODO(), gremlinInput)
	if err != nil {
		fmt.Printf("Gremlin query failed: %v\n", err)
	} else {
		var resultMap map[string]interface{}
		err = gremlinResult.Result.UnmarshalSmithyDocument(&resultMap)
		if err != nil {
			fmt.Printf("Error unmarshaling Gremlin result: %v\n", err)
		} else {
			resultJSON, _ := json.MarshalIndent(resultMap, "", "  ")
			fmt.Printf("Gremlin Result: %s\n", string(resultJSON))
		}
	}

	// ========================================
	// Example 2: Execute openCypher Query
	// ========================================
	opencypherQuery := "MATCH (n) RETURN n LIMIT 5"
	
	opencypherInput := &neptunedata.ExecuteOpenCypherQueryInput{
		OpenCypherQuery: &opencypherQuery,
	}
	opencypherResult, err := client.ExecuteOpenCypherQuery(context.TODO(), opencypherInput)
	if err != nil {
		fmt.Printf("openCypher query failed: %v\n", err)
	} else {
		var resultSlice []interface{}
		err = opencypherResult.Results.UnmarshalSmithyDocument(&resultSlice)
		if err != nil {
			fmt.Printf("Error unmarshaling openCypher result: %v\n", err)
		} else {
			resultJSON, _ := json.MarshalIndent(resultSlice, "", "  ")
			fmt.Printf("openCypher Results: %s\n", string(resultJSON))
		}
	}

	// ========================================
	// Example 3: Get Property Graph Summary
	// ========================================
	mode := types.GraphSummaryTypeBasic
	
	summaryInput := &neptunedata.GetPropertygraphSummaryInput{
		Mode: mode,
	}
	summaryResult, err := client.GetPropertygraphSummary(context.TODO(), summaryInput)
	if err != nil {
		fmt.Printf("Property graph summary failed: %v\n", err)
	} else {
    	var summaryJSON []byte
		summaryJSON, err = json.MarshalIndent(summaryResult.Payload, "", "  ")
		if err != nil {
			fmt.Printf("Error unmarshaling summary payload: %v\n", err)
		} else {
			fmt.Printf("Graph Summary: %s\n", string(summaryJSON))
		}
	}
	
	// ========================================
	// Example 4: Bulk Loader Job
	// ========================================
	// Update with the S3 bucket where your bulk load files are located
	// Ensure the IAM role listed here is attached to your Neptune cluster:
    //     https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM-add-role-cluster.html
	s3Source := "s3://your-bucket/your-data/"
	iamRole := "arn:aws:iam::your-account:role/NeptuneLoadFromS3"
	
	// Start loader job
	loaderInput := &neptunedata.StartLoaderJobInput{
		Source:      &s3Source,
		Format:      "csv",
		IamRoleArn:  &iamRole,
		S3BucketRegion: types.S3BucketRegion(region),
		FailOnError: aws.Bool(false),
		Parallelism: "HIGH", //change to parallelism to 'OVERSUBSCRIBE' to get max load throughput
	}
	
	loaderResult, err := client.StartLoaderJob(context.TODO(), loaderInput)
	if err != nil {
		fmt.Printf("Error starting loader job: %v\n", err)
	} else {
		loadID := loaderResult.Payload["loadId"]
		fmt.Printf("Started loader job: %s\n", loadID)
		
		// Monitor job completion
		for {
			var statusResultMap map[string]interface{}
			statusInput := &neptunedata.GetLoaderJobStatusInput{LoadId: &loadID}
			statusResponse, err := client.GetLoaderJobStatus(context.TODO(), statusInput)
			if err != nil {
				fmt.Printf("Error getting job status: %v\n", err)
				break
			}
			statusResponse.Payload.UnmarshalSmithyDocument(&statusResultMap)
			jobStatus := statusResultMap["overallStatus"].(map[string]interface{})["status"].(string)
			fmt.Printf("Job status: %s\n", jobStatus)
			
			if jobStatus == "LOAD_COMPLETED" || jobStatus == "LOAD_FAILED" || jobStatus == "LOAD_CANCELLED" {
				break
			}
			
			time.Sleep(10 * time.Second)
		}
	}
}