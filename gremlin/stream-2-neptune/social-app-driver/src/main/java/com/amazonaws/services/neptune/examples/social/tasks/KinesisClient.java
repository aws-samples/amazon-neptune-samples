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

package com.amazonaws.services.neptune.examples.social.tasks;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import groovy.transform.Synchronized;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KinesisClient implements AutoCloseable {
    public static KinesisClient create(String streamName, String region) {
        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();

        clientBuilder.setRegion(region);
        clientBuilder.setCredentials(new DefaultAWSCredentialsProviderChain());
        clientBuilder.setClientConfiguration(new ClientConfiguration());

        AmazonKinesis kinesisClient = clientBuilder.build();

        return new KinesisClient(kinesisClient, streamName);
    }

    private final AmazonKinesis kinesisClient;
    private final String streamName;
    private final Map<Class, String> errors = new HashMap<>();

    private KinesisClient(AmazonKinesis kinesisClient, String streamName) {
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
    }

    void publishToStream(Collection<EdgeInfo> edges) {

        try {

            PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
            putRecordsRequest.setStreamName(streamName);

            List<PutRecordsRequestEntry> entries = edges.stream().map(e -> {
                PutRecordsRequestEntry entry = new PutRecordsRequestEntry();
                entry.setData(e.bytes());
                entry.setPartitionKey(e.partitionKey());
                return entry;
            }).collect(Collectors.toList());

            putRecordsRequest.setRecords(entries);

            PutRecordsResult results = kinesisClient.putRecords(putRecordsRequest);

            if (results.getFailedRecordCount() == 0) {
                System.out.print(".");
            } else {
                System.out.print(results.getFailedRecordCount());
            }
        } catch (Exception e) {
            addError(e);
            System.out.print("e");
        }
    }

    @Synchronized
    private void addError(Exception e) {
        if (!errors.containsKey(e.getClass())) {
            errors.put(e.getClass(), e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        kinesisClient.shutdown();
    }

    public void printErrors() {
        for (Map.Entry<Class, String> entry : errors.entrySet()) {
            String value = entry.getValue();
            if (value.length() > 500) {
                System.out.println("[" + entry.getKey().getName() + "] " +
                        value.substring(0, 250) + "..." + value.substring(value.length() - 250));
            } else {
                System.out.println("[" + entry.getKey().getName() + "] " + value);
            }
        }
    }
}
