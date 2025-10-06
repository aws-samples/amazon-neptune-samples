package software.amazon.neptune.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.neptunedata.NeptunedataClient;
import software.amazon.awssdk.services.neptunedata.model.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.core.retry.RetryPolicy;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Neptune Data API Examples for Java
 */
public class NeptuneDataClient {

    /**
     * The AWS Java v2 SDK has specific Pojo configurations that do not work well with
     * a generally defined Jackson ObjectMapper.  The following is to assist with converting
     * the output of some response objects into JSON.
     */
    private static final ObjectMapper objectMapper = JsonMapper.builder()
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .build();

    /**
     * Convert AWS SDK Document to Jackson JsonNode
     */
    private static JsonNode documentToJsonNode(Document document) {
        try {
            String jsonString = document.toString();
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("Error converting Document to JsonNode", e);
        }
    }

    /**
     * Main execution method
     */
    public static void main(String[] args) {

        // Configuration - Replace with your actual Neptune cluster details
        String clusterEndpoint = "my-cluster-name.cluster-abcdefgh1234.my-region.neptune.amazonaws.com";
        int port = 8182;
        String region = "my-region";
        
        // Configure Neptune client
        String endpoint = String.format("https://%s:%d", clusterEndpoint, port);
        
        NeptunedataClient client = NeptunedataClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .httpClient(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ZERO) // No timeout
                        .connectionTimeout(Duration.ZERO) // No timeout
                        .build())
                .overrideConfiguration(builder -> builder
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(0) // No retries
                                .build()))
                .build();
        
        try {
            // ========================================
            // Example 1: Execute Gremlin Query
            // ========================================
            LocalDateTime currentTime = LocalDateTime.now();
            String gremlinQuery = String.format("""
                g.mergeV([(T.label):'person']).
                    option(Merge.onCreate,[name:'alice', age:10]).
                    option(Merge.onMatch,[lastUpdated:single(datetime('%s'))])
                """,currentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            try {
                ExecuteGremlinQueryRequest gremlinRequest = ExecuteGremlinQueryRequest.builder()
                        .gremlinQuery(gremlinQuery)
                        .serializer("application/vnd.gremlin-v2.0+json;types=false")
                        .build();
                
                ExecuteGremlinQueryResponse gremlinResponse = client.executeGremlinQuery(gremlinRequest);
                JsonNode gremlinResult = documentToJsonNode(gremlinResponse.result());
                System.out.println("Gremlin Result: " + gremlinResult.toPrettyString());
            } catch (Exception e) {
                System.out.println("Gremlin query failed: " + e.getMessage());
            }

            // ========================================
            // Example 2: Execute openCypher Query
            // ========================================
            String opencypherQuery = "MATCH (n) RETURN n LIMIT 5";
            try {
                ExecuteOpenCypherQueryRequest opencypherRequest = ExecuteOpenCypherQueryRequest.builder()
                        .openCypherQuery(opencypherQuery)
                        .build();
                
                ExecuteOpenCypherQueryResponse opencypherResponse = client.executeOpenCypherQuery(opencypherRequest);
                JsonNode opencypherResult = documentToJsonNode(opencypherResponse.results());
                System.out.println("openCypher Results:" + opencypherResult.toPrettyString());
            } catch (Exception e) {
                System.out.println("openCypher query failed: " + e.getMessage());
            }

            // ========================================
            // Example 3: Get Property Graph Summary
            // ========================================
            try {
                GetPropertygraphSummaryRequest summaryRequest = GetPropertygraphSummaryRequest.builder()
                        .mode(GraphSummaryType.BASIC)
                        .build();
                
                GetPropertygraphSummaryResponse summaryResponse = client.getPropertygraphSummary(summaryRequest);
                JsonNode summaryResult = objectMapper.valueToTree(summaryResponse.payload().graphSummary());
                System.out.println("Graph Summary: " + summaryResult.toPrettyString());
            } catch (Exception e) {
                System.out.println("Property graph summary failed: " + e.getMessage());
            }

            // ========================================
            // Example 4: Bulk Loader Job
            // ========================================
            // Update with the S3 bucket where your bulk load files are located
            // Ensure the IAM role listed here is attached to your Neptune cluster:
            //    https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM-add-role-cluster.html
            String s3Source = "s3://your-bucket/your-data/";
            String iamRole = "arn:aws:iam::your-account:role/NeptuneLoadFromS3";
            
            // Start loader job
            StartLoaderJobRequest loaderRequest = StartLoaderJobRequest.builder()
                    .source(s3Source)
                    .format("csv")
                    .iamRoleArn(iamRole)
                    .s3BucketRegion(region)
                    .failOnError(false)
                    .parallelism("HIGH") // change to parallelism to "OVERSUBSCRIBE" to get max load throughput
                    .build();
            
            StartLoaderJobResponse loaderResponse = client.startLoaderJob(loaderRequest);

            String loadId = loaderResponse.payload().get("loadId");
            System.out.println("Started loader job: " + loadId);
            
            // Monitor job completion
            while (true) {
                GetLoaderJobStatusRequest statusRequest = GetLoaderJobStatusRequest.builder()
                        .loadId(loadId)
                        .build();
                
                GetLoaderJobStatusResponse statusResponse = client.getLoaderJobStatus(statusRequest);
                JsonNode statusResult = documentToJsonNode(statusResponse.payload());
                String jobStatus = statusResult.get("overallStatus").get("status").asText();
                System.out.println("Job status: " + jobStatus);
                
                if ("LOAD_COMPLETED".equals(jobStatus) || 
                    "LOAD_FAILED".equals(jobStatus) || 
                    "LOAD_CANCELLED".equals(jobStatus)) {
                    break;
                }
                
                Thread.sleep(10000);
            }
            
        } catch (Exception e) {
            System.err.println("Error in main execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
            System.exit(0);
        }
    }
}