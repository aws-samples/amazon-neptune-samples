/*
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.neptune.examples.utils.EnvironmentVariableUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SocialAppNeptuneWriterStreamHandler implements RequestHandler<KinesisEvent, Void> {

    private final Random random = new Random(DateTime.now().getMillis());
    private final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

    @Override
    public Void handleRequest(KinesisEvent event, Context context) {

        LambdaLogger logger = context.getLogger();

        String neptuneEndpoint = EnvironmentVariableUtils.getMandatoryEnv("neptune_endpoint");
        int percentError = Integer.parseInt(EnvironmentVariableUtils.getOptionalEnv("percent_error", "0"));
        int batchWriteSize = Integer.parseInt(EnvironmentVariableUtils.getOptionalEnv("batch_write_size", "100"));
        boolean conditionalCreate = Boolean.parseBoolean(EnvironmentVariableUtils.getOptionalEnv("conditional_create", "true"));
        String clusterId = EnvironmentVariableUtils.getMandatoryEnv("cluster_id");

        boolean wait = Boolean.parseBoolean(EnvironmentVariableUtils.getOptionalEnv("wait", "true"));

        if (wait) {
            throw new RuntimeException("Waiting... [Set 'wait' Lambda environment variable to 'false' to continue processing]");
        }

        logger.log("Neptune endpoint  : " + neptuneEndpoint);
        logger.log("Percent error     : " + percentError);
        logger.log("Batch write size  : " + batchWriteSize);
        logger.log("Conditional create: " + conditionalCreate);
        logger.log("ClusterId           : " + clusterId);


        try (NeptuneClient neptuneClient = new NeptuneClient(neptuneEndpoint, logger)) {
            List<KinesisEvent.KinesisEventRecord> records = event.getRecords();
            logger.log("Number of records: " + records.size());
            List<List<KinesisEvent.KinesisEventRecord>> batches = partition(records, batchWriteSize);
            logger.log("Number of batches: " + batches.size());
            for (List<KinesisEvent.KinesisEventRecord> batch : batches) {
                writeBatch(batch, logger, percentError, neptuneClient, conditionalCreate, clusterId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return null;
    }

    private void writeBatch(List<KinesisEvent.KinesisEventRecord> batch,
                            LambdaLogger logger,
                            int percentError,
                            NeptuneClient neptuneClient,
                            boolean conditionalCreate,
                            String clusterId) throws IOException {

        BatchAddEdges query = new BatchAddEdges(neptuneClient, logger, conditionalCreate);

        for (KinesisEvent.KinesisEventRecord record : batch) {

            JsonNode json = new ObjectMapper().readTree(record.getKinesis().getData().array());

            String fromVertexId = json.path("fromId").asText();
            String toVertexId = json.path("toId").asText();
            String creationDate = json.path("creationDate").asText();
            long insertDateTime = DateTime.now().getMillis();

            query.addEdge(fromVertexId, toVertexId, creationDate, insertDateTime);
        }

        if (random.nextInt(100) < percentError) {
            query.provokeError();
        }

        long duration = query.execute();

        publishCloudWatchMetric(batch.size(), duration, clusterId);
    }

    private void publishCloudWatchMetric(int batchSize, long duration, String clusterId) {

        cw.putMetricData(new PutMetricDataRequest().
                withMetricData(new MetricDatum()
                        .withMetricName("EdgesSubmitted")
                        .withUnit(StandardUnit.Count)
                        .withValues()
                        .withValue((double) batchSize)
                        .withStorageResolution(1)
                .withDimensions(new Dimension().withName("clusterId").withValue(clusterId))).
                withNamespace("aws-samples/stream-2-neptune"));

        cw.putMetricData(new PutMetricDataRequest().
                withMetricData(new MetricDatum()
                        .withMetricName("WriteDuration")
                        .withUnit(StandardUnit.Milliseconds)
                        .withValues()
                        .withValue((double) duration)
                        .withStorageResolution(1)
                .withDimensions(new Dimension().withName("clusterId").withValue(clusterId))).
                withNamespace("aws-samples/stream-2-neptune"));
    }

    private List<List<KinesisEvent.KinesisEventRecord>> partition(List<KinesisEvent.KinesisEventRecord> list, int batchSize) {
        List<List<KinesisEvent.KinesisEventRecord>> parts = new ArrayList<>();
        int originalListSize = list.size();
        for (int i = 0; i < originalListSize; i += batchSize) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(originalListSize, i + batchSize)))
            );
        }
        return parts;
    }
}
