package com.amazonaws.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.neptunedata.NeptunedataClient;
import software.amazon.awssdk.services.neptunedata.model.ExecuteGremlinQueryRequest;
import software.amazon.awssdk.services.neptunedata.model.ExecuteGremlinQueryResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NeptuneWriter {
    private static final Logger LOG = LoggerFactory.getLogger(NeptuneWriter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final NeptunedataClient neptuneClient;
    private final List<String> vertexBatch;
    private final List<String> edgeBatch;
    private int currentObjectCount;
    private final int maxObjects;
    
    public NeptuneWriter(String region, String neptuneEndpoint) {
        LOG.warn("Initializing Neptune client for region: {}, endpoint: {}", region, neptuneEndpoint);
        this.neptuneClient = NeptunedataClient.builder()
                .region(Region.of(region))
                .endpointOverride(java.net.URI.create("https://" + neptuneEndpoint + ":8182"))
                .build();
        this.vertexBatch = new ArrayList<>();
        this.edgeBatch = new ArrayList<>();
        this.currentObjectCount = 0;
        this.maxObjects = 100;
        LOG.warn("Neptune client initialized successfully");
    }
    
    public boolean writeVertex(String vertexJson) {
        try {
            JsonNode vertexData = OBJECT_MAPPER.readTree(vertexJson);
            
            String vertexId = getFieldValue(vertexData, "~id", ":ID");
            String vertexLabel = getFieldValue(vertexData, "~label", ":LABEL");
            
            if (vertexId == null || vertexLabel == null) {
                LOG.error("Missing required vertex fields in: {}", vertexJson);
                return false;
            }
            
            int objectCount = countObjects(vertexData, List.of("~id", ":ID", "~label", ":LABEL"));
            
            if (currentObjectCount + objectCount > maxObjects) {
                flushBatches();
            }
            
            StringBuilder query = new StringBuilder();
            query.append("mergeV([(id): '").append(vertexId).append("'])");
            query.append(".option(onCreate, [(label): '").append(vertexLabel).append("'");
            
            // Add properties to onCreate
            Iterator<Map.Entry<String, JsonNode>> fields = vertexData.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if (!List.of("~id", ":ID", "~label", ":LABEL").contains(key)) {
                    query.append(", ('").append(key).append("'): '")
                         .append(field.getValue().asText()).append("'");
                }
            }
            query.append("])");
            
            // Add onMatch for property updates
            query.append(".option(onMatch, [");
            Iterator<Map.Entry<String, JsonNode>> updateFields = vertexData.fields();
            boolean first = true;
            while (updateFields.hasNext()) {
                Map.Entry<String, JsonNode> field = updateFields.next();
                String key = field.getKey();
                if (!List.of("~id", ":ID", "~label", ":LABEL").contains(key)) {
                    if (!first) query.append(", ");
                    query.append("('").append(key).append("'): '")
                         .append(field.getValue().asText()).append("'");
                    first = false;
                }
            }
            query.append("])");
            
//           LOG.warn("Wrote Vertex");
            vertexBatch.add(query.toString());
            currentObjectCount += objectCount;
//            LOG.warn("Current object count: {}", objectCount);
            
            return true;
            
        } catch (Exception e) {
            LOG.error("Error processing vertex: {}", vertexJson, e);
            return false;
        }
    }
    
    public boolean writeEdge(String edgeJson) {
        try {
            JsonNode edgeData = OBJECT_MAPPER.readTree(edgeJson);
            
            String edgeId = getFieldValue(edgeData, "~id", ":ID");
            String fromId = getFieldValue(edgeData, "~from", ":START_ID");
            String toId = getFieldValue(edgeData, "~end", ":END_ID");
            String edgeLabel = getFieldValue(edgeData, "~label", ":TYPE");
            
            if (edgeId == null || fromId == null || toId == null || edgeLabel == null) {
                LOG.error("Missing required edge fields in: {}", edgeJson);
                return false;
            }
            
            int objectCount = countObjects(edgeData, 
                List.of("~id", ":ID", "~from", ":START_ID", "~end", ":END_ID", "~label", ":TYPE"));
            
            if (currentObjectCount + objectCount > maxObjects) {
                flushBatches();
            }
            
            StringBuilder query = new StringBuilder();
            query.append("V('").append(fromId).append("')");
            query.append(".addE('").append(edgeLabel).append("')");
            query.append(".to(V('").append(toId).append("'))");
            
            // Add properties
            Iterator<Map.Entry<String, JsonNode>> fields = edgeData.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                if (!List.of("~id", ":ID", "~from", ":START_ID", "~end", ":END_ID", "~label", ":TYPE").contains(key)) {
                    query.append(".property('").append(key).append("', '")
                         .append(field.getValue().asText()).append("')");
                }
            }
            
//            LOG.warn("Wrote Edge");
            edgeBatch.add(query.toString());
            currentObjectCount += objectCount;
//            LOG.warn("Current object count: {}", currentObjectCount);
            
            return true;
            
        } catch (Exception e) {
            LOG.error("Error processing edge: {}", edgeJson, e);
            return false;
        }
    }
    
    public void flushBatches() {
        if (!vertexBatch.isEmpty()) {
            executeVertexBatch();
        }
        if (!edgeBatch.isEmpty()) {
            executeEdgeBatch();
        }
        currentObjectCount = 0;
    }
    
    private void executeVertexBatch() {
        try {
            StringBuilder batchQuery = new StringBuilder();
            if (!vertexBatch.isEmpty()) {
                batchQuery.append("g");
                for (String vertex : vertexBatch) {
                    batchQuery.append(".").append(vertex);
                }
            }
            
            String query = batchQuery.toString();
            LOG.warn("Executing vertex batch query: {}", query);
            
            ExecuteGremlinQueryRequest request = ExecuteGremlinQueryRequest.builder()
                    .gremlinQuery(query)
                    .build();
            
            ExecuteGremlinQueryResponse response = neptuneClient.executeGremlinQuery(request);

            LOG.warn("Response status is {}:{}", response.status().code(), response.status().message());
            
            LOG.warn("Successfully wrote vertex batch of {} items. Response: {}", vertexBatch.size(), response.status());
            vertexBatch.clear();
            
        } catch (Exception e) {
            LOG.error("Error writing vertex batch of {} items", vertexBatch.size(), e);
            vertexBatch.clear();
        }
    }
    
    private void executeEdgeBatch() {
        try {
            StringBuilder batchQuery = new StringBuilder();
            if (!edgeBatch.isEmpty()) {
                batchQuery.append("g");
                for (String edge : edgeBatch) {
                    batchQuery.append(".").append(edge);
                }
            }
            
            String query = batchQuery.toString();
            LOG.warn("Executing edge batch query: {}", query);
            
            ExecuteGremlinQueryRequest request = ExecuteGremlinQueryRequest.builder()
                    .gremlinQuery(query)
                    .build();
            
            ExecuteGremlinQueryResponse response = neptuneClient.executeGremlinQuery(request);
            LOG.warn("Response status is {}:{}", response.status().code(), response.status().message());
            
            LOG.warn("Successfully wrote edge batch of {} items. Response: {}", edgeBatch.size(), response.status());
            edgeBatch.clear();
            
        } catch (Exception e) {
            LOG.error("Error writing edge batch of {} items", edgeBatch.size(), e);
            edgeBatch.clear();
        }
    }
    
    private String getFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                return field.asText();
            }
        }
        return null;
    }
    
    // Number of objects is 1 for the node/edge + 1 per (populated) property (excluding ID, Start, From, and Label fields)
    private int countObjects(JsonNode node, List<String> requiredKeys) {
        int count = 1; // Base count
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (!requiredKeys.contains(key) && !field.getValue().asText().trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    public void close() {
        flushBatches();
        neptuneClient.close();
    }
}
