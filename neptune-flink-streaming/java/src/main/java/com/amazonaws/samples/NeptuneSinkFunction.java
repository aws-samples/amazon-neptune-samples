package com.amazonaws.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeptuneSinkFunction extends RichSinkFunction<String> {
    private static final Logger LOG = LoggerFactory.getLogger(NeptuneSinkFunction.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final String region;
    private final String neptuneEndpoint;
    private transient NeptuneWriter neptuneWriter;
    
    public NeptuneSinkFunction(String region, String neptuneEndpoint) {
        this.region = region;
        this.neptuneEndpoint = neptuneEndpoint;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.neptuneWriter = new NeptuneWriter(region, neptuneEndpoint);
        LOG.debug("Initialized Neptune writer for region: {}, endpoint: {}", region, neptuneEndpoint);
    }
    
    @Override
    public void invoke(String value, Context context) throws Exception {
        LOG.debug("Processing record: {}", value);
        try {
            JsonNode record = OBJECT_MAPPER.readTree(value);
            String recordType = record.has("type") ? record.get("type").asText() : "unknown";
            String data = record.has("data") ? record.get("data").asText() : value;
            
            LOG.debug("Record type: {}, data: {}", recordType, data);
            
            boolean success = false;
            if ("vertex".equals(recordType)) {
                success = neptuneWriter.writeVertex(data);
                LOG.debug("Processed vertex record: {}", success);
            } else if ("edge".equals(recordType)) {
                success = neptuneWriter.writeEdge(data);
                LOG.debug("Processed edge record: {}", success);
            } else {
                // Try to determine type from data structure
                JsonNode dataNode = OBJECT_MAPPER.readTree(data);
                if (hasVertexFields(dataNode)) {
                    success = neptuneWriter.writeVertex(data);
                    LOG.debug("Processed inferred vertex record: {}", success);
                } else if (hasEdgeFields(dataNode)) {
                    success = neptuneWriter.writeEdge(data);
                    LOG.debug("Processed inferred edge record: {}", success);
                } else {
                    LOG.warn("Unknown record type, skipping: {}", value);
                }
            }
            
            if (!success) {
                LOG.error("Failed to process record: {}", value);
            }
            
        } catch (Exception e) {
            LOG.error("Error processing record: {}", value, e);
        }
    }
    
    @Override
    public void close() throws Exception {
        if (neptuneWriter != null) {
            neptuneWriter.close();
        }
        super.close();
    }
    
    private boolean hasVertexFields(JsonNode node) {
        return (node.has("~id") || node.has(":ID")) && 
               (node.has("~label") || node.has(":LABEL"));
    }
    
    private boolean hasEdgeFields(JsonNode node) {
        return (node.has("~id") || node.has(":ID")) && 
               (node.has("~from") || node.has(":START_ID")) &&
               (node.has("~end") || node.has(":END_ID")) &&
               (node.has("~label") || node.has(":TYPE"));
    }
}
