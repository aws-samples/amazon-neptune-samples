package com.amazonaws.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kinesis.source.KinesisStreamsSource;
import org.apache.flink.connector.kinesis.source.config.KinesisStreamsSourceConfigConstants;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.RichCoMapFunction;
import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class FlinkNeptuneStreamingJob {
    private static final Logger LOG = LoggerFactory.getLogger(FlinkNeptuneStreamingJob.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    public static void main(String[] args) throws Exception {
        LOG.debug("=== STARTING FLINK NEPTUNE STREAMING APPLICATION ===");
        
        // Create execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Get application properties
        Map<String, Properties> applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties();
        LOG.debug("Retrieved application properties: {}", applicationProperties.keySet());
        
        // Debug: Print all property groups and their contents
/*        for (Map.Entry<String, Properties> entry : applicationProperties.entrySet()) {
            LOG.debug("Property group '{}' contains: {}", entry.getKey(), entry.getValue());
        }
*/        
        // Get Neptune configuration
        Properties neptuneConfig = applicationProperties.get("neptune.config");
        if (neptuneConfig == null) {
            LOG.error("Neptune configuration property group 'neptune.config' not found!");
            LOG.error("Available property groups: {}", applicationProperties.keySet());
            throw new IllegalArgumentException("Neptune endpoint must be configured via neptune.endpoint property");
        }
        
        String region = neptuneConfig.getProperty("AWS_REGION", "us-east-1");
        String neptuneEndpoint = neptuneConfig.getProperty("neptune.endpoint");
        
        LOG.debug("Neptune configuration - Region: {}, Endpoint: {}", region, neptuneEndpoint);
        
        if (neptuneEndpoint == null || neptuneEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Neptune endpoint must be configured via neptune.endpoint property");
        }
        
        // Get Kinesis configuration
        Properties kinesisConfig = applicationProperties.get("kinesis.config");
        String vertexStream = kinesisConfig.getProperty("VERTEX_SOURCE", "vertex-stream");
        String edgeStream = kinesisConfig.getProperty("EDGE_SOURCE", "edge-stream");
        int parallelism = Integer.parseInt(kinesisConfig.getProperty("PARALLELISM", "4"));
        
        LOG.debug("Kinesis configuration - Vertex stream: {}, Edge stream: {}, Parallelism: {}", 
                vertexStream, edgeStream, parallelism);
        
        // Get AWS account ID for building stream ARNs
        String accountId;
        // Get account ID from application properties
        accountId = neptuneConfig.getProperty("AWS_ACCOUNT_ID");
        if (accountId == null || accountId.isEmpty()) {
            // Fallback to environment variable
            accountId = System.getenv("AWS_ACCOUNT_ID");
            if (accountId == null || accountId.isEmpty()) {
                throw new IllegalArgumentException("AWS account must be configured via AWS_ACCOUNT_ID property");
            }
        }
        
        // Create configuration for Kinesis source
        Configuration sourceConfig = new Configuration();
        sourceConfig.set(KinesisStreamsSourceConfigConstants.STREAM_INITIAL_POSITION, 
                        KinesisStreamsSourceConfigConstants.InitialPosition.LATEST);
        sourceConfig.setString("aws.region", region);
        
        // Build stream ARNs
        String vertexStreamArn = String.format("arn:aws:kinesis:%s:%s:stream/%s", region, accountId, vertexStream);
        String edgeStreamArn = String.format("arn:aws:kinesis:%s:%s:stream/%s", region, accountId, edgeStream);
        
        // Create data streams using new Source API
        DataStream<String> vertexStreamData = env.fromSource(
            KinesisStreamsSource.<String>builder()
                .setStreamArn(vertexStreamArn)
                .setSourceConfig(sourceConfig)
                .setDeserializationSchema(new SimpleStringSchema())
                .build(),
            WatermarkStrategy.noWatermarks(),
            "Vertex Source"
        ).returns(String.class);
        
        DataStream<String> edgeStreamData = env.fromSource(
            KinesisStreamsSource.<String>builder()
                .setStreamArn(edgeStreamArn)
                .setSourceConfig(sourceConfig)
                .setDeserializationSchema(new SimpleStringSchema())
                .build(),
            WatermarkStrategy.noWatermarks(),
            "Edge Source"
        ).returns(String.class);
        LOG.debug("Created edge stream data source");
        
        // Key both streams by partition_id
        DataStream<String> vertexKeyed = vertexStreamData.keyBy(FlinkNeptuneStreamingJob::extractPartitionId);
        DataStream<String> edgeKeyed = edgeStreamData.keyBy(FlinkNeptuneStreamingJob::extractPartitionId);
        LOG.debug("Keyed streams by partition_id");
        
        // Connect streams for partition-aware processing
        ConnectedStreams<String, String> connectedStreams = vertexKeyed.connect(edgeKeyed);
        
        // Process with CoMapFunction (writes directly to Neptune)
        DataStream<String> results = connectedStreams.map(new PartitionProcessor(region, neptuneEndpoint));
        LOG.debug("Connected streams and added partition processor");
        
        // Log processing results
        results.print("Neptune Processing Results");
        LOG.debug("Added result logging");
        
        // Execute the job
        LOG.debug("=== STARTING FLINK JOB EXECUTION ===");
        env.execute("Flink Neptune Streaming Application");
    }
    
    private static String extractPartitionId(String record) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(record);
            JsonNode partitionId = node.get("partition_id");
            return partitionId != null ? partitionId.asText() : "default";
        } catch (Exception e) {
            LOG.warn("Failed to extract partition_id from record: {}", record, e);
            return "default";
        }
    }
    
    public static class PartitionProcessor extends RichCoMapFunction<String, String, String> {
        private static final Logger LOG = LoggerFactory.getLogger(PartitionProcessor.class);
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        
        private final String region;
        private final String neptuneEndpoint;
        private transient NeptuneWriter neptuneWriter;
        
        public PartitionProcessor(String region, String neptuneEndpoint) {
            this.region = region;
            this.neptuneEndpoint = neptuneEndpoint;
        }
        
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            this.neptuneWriter = new NeptuneWriter(region, neptuneEndpoint);
        }
        
        @Override
        public void close() throws Exception {
            if (neptuneWriter != null) {
                neptuneWriter.close();
            }
            super.close();
        }
        
        @Override
        public String map1(String vertexRecord) throws Exception {
            LOG.warn("Processing vertex record: {}", vertexRecord);
            try {
                JsonNode record = OBJECT_MAPPER.readTree(vertexRecord);
                JsonNode dataNode = record.get("data");
                
                if (dataNode == null) {
                    LOG.error("Missing 'data' field in vertex record: {}", vertexRecord);
                    return "VERTEX_ERROR: Missing data field";
                }
                
                String vertexData = dataNode.asText();
                LOG.debug("Extracted vertex data: {}", vertexData);
                
                boolean success = neptuneWriter.writeVertex(vertexData);
                LOG.debug("Neptune write result: {}", success);
                
                JsonNode vertexJson = OBJECT_MAPPER.readTree(vertexData);
                String vertexId = getVertexId(vertexJson);
                
                String result = success ? "VERTEX_SUCCESS: " + vertexId : "VERTEX_FAILED: " + vertexId;
                LOG.debug("Returning result: {}", result);
                return result;
                
            } catch (Exception e) {
                LOG.error("Error processing vertex record: {}", vertexRecord, e);
                return "VERTEX_ERROR: " + e.getMessage();
            }
        }
        
        @Override
        public String map2(String edgeRecord) throws Exception {
            try {
                JsonNode record = OBJECT_MAPPER.readTree(edgeRecord);
                JsonNode dataNode = record.get("data");
                
                if (dataNode == null) {
                    LOG.error("Missing 'data' field in edge record: {}", edgeRecord);
                    return "EDGE_ERROR: Missing data field";
                }
                
                String edgeData = dataNode.asText();
                boolean success = neptuneWriter.writeEdge(edgeData);
                
                JsonNode edgeJson = OBJECT_MAPPER.readTree(edgeData);
                String edgeId = getEdgeId(edgeJson);
                
                return success ? "EDGE_SUCCESS: " + edgeId : "EDGE_FAILED: " + edgeId;
                
            } catch (Exception e) {
                LOG.error("Error processing edge record: {}", edgeRecord, e);
                return "EDGE_ERROR: " + e.getMessage();
            }
        }
        
        private String getVertexId(JsonNode node) {
            JsonNode id = node.get("~id");
            if (id == null) {
                id = node.get(":ID");
            }
            return id != null ? id.asText() : "unknown";
        }
        
        private String getEdgeId(JsonNode node) {
            JsonNode id = node.get("~id");
            if (id == null) {
                id = node.get(":ID");
            }
            return id != null ? id.asText() : "unknown";
        }
    }
}
