import {
    NeptunedataClient,
    ExecuteGremlinQueryCommand,
    ExecuteOpenCypherQueryCommand,
    GetPropertygraphSummaryCommand,
    StartLoaderJobCommand,
    GetLoaderJobStatusCommand
} from "@aws-sdk/client-neptunedata";
import { inspect } from "util";
import { NodeHttpHandler } from "@smithy/node-http-handler";

/**
 * Main execution function
 */
async function main() {
    // Configuration - Replace with your actual Neptune cluster details
    const clusterEndpoint = 'my-cluster-name.cluster-abcdefgh1234.my-region.neptune.amazonaws.com';
    const port = 8182;
    const region = 'my-region';

    // Configure Neptune client
    // This disables retries and sets the client timeout to infinite (relying on Neptune's query timeout)
    const endpoint = `https://${clusterEndpoint}:${port}`;
    const clientConfig = {
        endpoint: endpoint,
        sslEnabled: true,
        region: region,
        maxAttempts: 1,  // do not retry
        requestHandler: new NodeHttpHandler({
            requestTimeout: 0  // no client timeout
        })
    };

    const client = new NeptunedataClient(clientConfig);

    try {
        // ========================================
        // Example 1: Execute Gremlin Query
        // ========================================
        const currentTime = new Date();
        const gremlinQuery = `
            g.mergeV([(T.label):'person']).
                option(Merge.onCreate,[name:'alice', age:10]).
                option(Merge.onMatch,[lastUpdated:single(datetime('${currentTime.toISOString()}'))])
        `;
        try {
            const command = new ExecuteGremlinQueryCommand({
                gremlinQuery: gremlinQuery,
                serializer: "application/vnd.gremlin-v2.0+json;types=false"
            });
            const response = await client.send(command);
            console.log("Gremlin Result:", inspect(response.result, { depth: null }));
        } catch (error) {
            console.log("Gremlin query failed:", error.message);
        }

        // ========================================
        // Example 2: Execute openCypher Query
        // ========================================
        const cypherQuery = "MATCH (n) RETURN n LIMIT 5";
        try {
            const command = new ExecuteOpenCypherQueryCommand({
                openCypherQuery: cypherQuery
            });
            const response = await client.send(command);
            console.log("openCypher Results:");
            response.results?.forEach(item => {
                console.log("  ", item);
            });
        } catch (error) {
            console.log("openCypher query failed:", error.message);
        }

        // ========================================
        // Example 3: Get Property Graph Summary
        // ========================================
        try {
            const command = new GetPropertygraphSummaryCommand({ mode: "basic" });
            const response = await client.send(command);
            console.log("Graph Summary:", inspect(response.payload, { depth: null }));
        } catch (error) {
            console.log("Property graph summary failed:", error.message);
        }

        // ========================================
        // Example 4: Bulk Loader Job
        // ========================================
        // Update with the S3 bucket where your bulk load files are located
        // Ensure the IAM role listed here is attached to your Neptune cluster:
        //     https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-IAM-add-role-cluster.html
        const s3Source = 's3://your-bucket/your-data/';
        const iamRole = 'arn:aws:iam::your-account:role/NeptuneLoadFromS3';

        try {
            // Start loader job
            const startCommand = new StartLoaderJobCommand({
                source: s3Source,
                format: 'csv',
                iamRoleArn: iamRole,
                s3BucketRegion: region,
                failOnError: false,
                // change to parallelism to 'OVERSUBSCRIBE' to get max load throughput
                parallelism: 'HIGH'
            });
            const loaderResponse = await client.send(startCommand);
            const loadId = loaderResponse.payload.loadId;
            console.log('Started loader job:', loadId);

            // Monitor job completion
            while (true) {
                const statusCommand = new GetLoaderJobStatusCommand({ loadId: loadId });
                const statusResponse = await client.send(statusCommand);
                const jobStatus = statusResponse.payload.overallStatus.status;
                console.log('Job status:', jobStatus);

                if (['LOAD_COMPLETED', 'LOAD_FAILED', 'LOAD_CANCELLED'].includes(jobStatus)) {
                    break;
                }

                await new Promise(resolve => setTimeout(resolve, 10000));
            }
        } catch (error) {
            console.log("Bulk loader failed:", error.message);
        }

    } catch (error) {
        console.error("Error in main execution:", error);
    }
}

// Run the main function
main().catch(console.error);