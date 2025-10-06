using Amazon;
using Amazon.Neptunedata;
using Amazon.Neptunedata.Model;
using System.Text.Json;

namespace NeptuneDataplaneSamples
{

    public class Program
    {
        /*
         * Helper method to convert query response objects from Document objects
         * into a stringifiable JSON object.
         */
        private static object ConvertDocument(Amazon.Runtime.Documents.Document doc)
        {
            if (doc.IsNull()) return null!;
            if (doc.IsBool()) return doc.AsBool();
            if (doc.IsString()) return doc.AsString();
            if (doc.IsInt()) return doc.AsInt();
            if (doc.IsLong()) return doc.AsLong();
            if (doc.IsDouble()) return doc.AsDouble();
            
            if (doc.IsList())
            {
                var list = doc.AsList();
                return list.Select(ConvertDocument).ToList();
            }
            
            if (doc.IsDictionary())
            {
                var dict = doc.AsDictionary();
                return dict.ToDictionary(kvp => kvp.Key, kvp => ConvertDocument(kvp.Value));
            }
            
            return doc.ToString();
        }

        public static async Task Main(string[] args)
        {
            // Configuration - Replace with your actual Neptune cluster details
            const string clusterEndpoint = "my-cluster-name.cluster-abcdefgh1234.my-region.neptune.amazonaws.com";
            const int port = 8182;
            const string region = "my-region";

            // Configure Neptune client
            var endpoint = $"https://{clusterEndpoint}:{port}";
            var config = new AmazonNeptunedataConfig
            {
                ServiceURL = endpoint,
                MaxErrorRetry = 0, // No retries
                Timeout = Timeout.InfiniteTimeSpan // No timeout
            };

            using var client = new AmazonNeptunedataClient(config);

            try
            {
                // ========================================
                // Example 1: Execute Gremlin Query
                // ========================================
                var currentTime = DateTime.Now.ToString("O");
                var gremlinQuery = $"""
                    g.mergeV([(T.label):'person']).
                    option(Merge.onCreate,[name:'alice', age:10]).
                    option(Merge.onMatch,[lastUpdated:single(datetime('{currentTime}'))])
                    """;
                try
                {
                    var gremlinRequest = new ExecuteGremlinQueryRequest
                    {
                        GremlinQuery = gremlinQuery,
                        Serializer = "application/vnd.gremlin-v2.0+json;types=false"
                    };

                    var gremlinResponse = await client.ExecuteGremlinQueryAsync(gremlinRequest);
                    var convertedResult = ConvertDocument(gremlinResponse.Result);
                    var gremlinJsonString = JsonSerializer.Serialize(convertedResult, new JsonSerializerOptions { WriteIndented = true });
                    Console.WriteLine($"Gremlin Result: {gremlinJsonString}");

                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Gremlin query failed: {ex.Message}");
                }

                // ========================================
                // Example 2: Execute openCypher Query
                // ========================================
                const string opencypherQuery = "MATCH (n) RETURN n LIMIT 5";
                try
                {
                    var opencypherRequest = new ExecuteOpenCypherQueryRequest
                    {
                        OpenCypherQuery = opencypherQuery
                    };

                    var opencypherResponse = await client.ExecuteOpenCypherQueryAsync(opencypherRequest);
                    var convertedResult = ConvertDocument(opencypherResponse.Results);
                    var opencypherJsonString = JsonSerializer.Serialize(convertedResult, new JsonSerializerOptions { WriteIndented = true });
                    Console.WriteLine($"openCypher Results: {opencypherJsonString}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"openCypher query failed: {ex.Message}");
                }

                // ========================================
                // Example 3: Get Property Graph Summary
                // ========================================
                try
                {
                    var summaryRequest = new GetPropertygraphSummaryRequest
                    {
                        Mode = GraphSummaryType.Basic
                    };

                    var summaryResponse = await client.GetPropertygraphSummaryAsync(summaryRequest);
                    var summaryJsonString = JsonSerializer.Serialize(summaryResponse.Payload, new JsonSerializerOptions { WriteIndented = true });
                    Console.WriteLine($"Graph Summary: {summaryJsonString}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Property graph summary failed: {ex.Message}");
                }

                // ========================================
                // Example 4: Bulk Loader Job
                // ========================================
                // Update with the S3 bucket where your bulk load files are located
                // Ensure the IAM role listed here is attached to your Neptune cluster:
                //     https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM-add-role-cluster.html
                const string s3Source = "s3://your-bucket/your-data/";
                const string iamRole = "arn:aws:iam::your-account:role/NeptuneLoadFromS3";

                try
                {
                    // Start loader job
                    var loaderRequest = new StartLoaderJobRequest
                    {
                        Source = s3Source,
                        Format = "csv",
                        IamRoleArn = iamRole,
                        S3BucketRegion = region,
                        FailOnError = false,
                        Parallelism = "HIGH" // change to parallelism to 'OVERSUBSCRIBE' to get max load throughput
                    };
                    
                    var loaderResponse = await client.StartLoaderJobAsync(loaderRequest);
                    var loadId = loaderResponse.Payload["loadId"].ToString();
                    Console.WriteLine($"Started loader job: {loadId}");
                    
                    // Monitor job completion
                    while (true)
                    {
                        var statusRequest = new GetLoaderJobStatusRequest { LoadId = loadId };
                        var statusResponse = await client.GetLoaderJobStatusAsync(statusRequest);
                        var jobStatus = statusResponse.Payload.AsDictionary()["overallStatus"].AsDictionary()["status"];
                        Console.WriteLine($"Job status: {jobStatus}");
                        
                        if (jobStatus == "LOAD_COMPLETED" || jobStatus == "LOAD_FAILED" || jobStatus == "LOAD_CANCELLED")
                        {
                            break;
                        }
                        
                        await Task.Delay(10000);
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Bulk loader failed: {ex.Message}");
                }
                Console.WriteLine();;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error in main execution: {ex.Message}");
            }
        }
    }
}