package com.amazonaws.services.neptune.examples.social.tasks;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class AddEdgesTask extends TimerTask {

    private final Timer timer;
    private final Iterator<String> edgesIterator;
    private final KinesisClient kinesisClient;
    private final CountDownLatch waitHandle;
    private final int batchSize;

    public AddEdgesTask(Timer timer,
                        Iterator<String> edgesIterator,
                        KinesisClient kinesisClient,
                        CountDownLatch waitHandle,
                        int batchSize,
                        int numberOfWorkers) {

        this.timer = timer;
        this.edgesIterator = edgesIterator;
        this.kinesisClient = kinesisClient;
        this.waitHandle = waitHandle;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {

        List<EdgeInfo> edges = new ArrayList<>();
        int i = 0;

        while (edgesIterator.hasNext() && i < batchSize) {

            String line = edgesIterator.next();

            String[] columns = line.split(",");
            String fromVertexId = clean(columns[2]);
            String toVertexId = clean(columns[3]);
            String creationDate = clean(columns[4]);

            EdgeInfo edgeInfo = new EdgeInfo(fromVertexId, toVertexId, creationDate);
            edges.add(edgeInfo);

            i++;
        }

        if (edges.isEmpty()) {
            timer.cancel();
            waitHandle.countDown();
        } else {
            kinesisClient.publishToStream(edges);
        }
    }

    private String clean(String s){
        return s.trim().replace("\"", "");
    }

}