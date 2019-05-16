/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.neptune.examples.utils.ActivityTimer;

import java.util.ArrayList;
import java.util.List;

public class Metrics {

    AmazonCloudWatchAsync cwa = AmazonCloudWatchAsyncClientBuilder.defaultClient();

    private final String clusterId;
    private final LambdaLogger logger;
    private final List<Double> batchSizes = new ArrayList<>();
    private final List<Double> durations = new ArrayList<>();

    public Metrics(String clusterId, LambdaLogger logger) {
        this.clusterId = clusterId;
        this.logger = logger;
    }

    public void add(int batchSize, long duration) {
        batchSizes.add((double) batchSize);
        durations.add((double) duration);
    }

    public void publish() {

        try (ActivityTimer timer = new ActivityTimer(logger, "Publish metrics")) {

            MetricDatum edgesSubmitted = new MetricDatum()
                    .withMetricName("EdgesSubmitted")
                    .withUnit(StandardUnit.Count)
                    .withValues(batchSizes)
                    .withStorageResolution(1)
                    .withDimensions(new Dimension().withName("clusterId").withValue(clusterId));

            MetricDatum writeDuration = new MetricDatum()
                    .withMetricName("WriteDuration")
                    .withUnit(StandardUnit.Milliseconds)
                    .withValues(durations)
                    .withStorageResolution(1)
                    .withDimensions(new Dimension().withName("clusterId").withValue(clusterId));

            try {
                cwa.putMetricData(new PutMetricDataRequest().
                        withMetricData(edgesSubmitted, writeDuration).
                        withNamespace("aws-samples/stream-2-neptune"));
            } catch (Exception e) {
                logger.log("Swallowed exception: " + e.getLocalizedMessage());
            }
        }
    }
}
