package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.neptune.examples.social.tasks.AddEdgesTask;
import com.amazonaws.services.neptune.examples.social.tasks.AddEdgesTaskMulithreaded;
import com.amazonaws.services.neptune.examples.social.tasks.KinesisClient;
import com.amazonaws.services.neptune.examples.social.util.ActivityTimer;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class SocialAppDriver {
    private static final String FILENAME = "follows.csv";

    public static void main(String args[]) throws Exception {

        String streamName = getArg("streamName", args, 0);
        String region = getArg("region", args, 1);
        String period = getArg("period", args, 2);
        String batchSize = getArg("batchSize", args, 3);

        int numberOfWorkers = Runtime.getRuntime().availableProcessors() * 2;

        System.out.println("streamName     : " + streamName);
        System.out.println("region         : " + region);
        System.out.println("period         : " + period);
        System.out.println("batchSize      : " + batchSize);
        System.out.println("workers        : " + numberOfWorkers);

        File csvFile = new File(FILENAME);

        if (!csvFile.exists()) {
            System.err.println(csvFile.getAbsolutePath() + " does not exist");
            System.exit(1);
        }

        try (Stream<String> edgesStream = Files.lines(csvFile.toPath());
             KinesisClient kinesisClient = KinesisClient.create(streamName, region);
             ActivityTimer activityTimer = new ActivityTimer()) {

            Iterator<String> edgesIterator = edgesStream.iterator();

            if (edgesIterator.hasNext()){
                // Skip headers
                edgesIterator.next();
            }

            CountDownLatch waitHandle = new CountDownLatch(1);

            Timer timer = new Timer();
            AddEdgesTaskMulithreaded task;
            task = new AddEdgesTaskMulithreaded(timer, edgesIterator, kinesisClient, waitHandle, Integer.parseInt(batchSize), numberOfWorkers);

            timer.schedule(task, 0, Long.parseLong(period));

            waitHandle.await();

            kinesisClient.printErrors();
        }
    }

    private static String getArg(String name, String[] args, int index) {
        if (args.length < index + 1) {
            throw new IllegalStateException("Expected arg[" + index + "]: " + name);
        }
        return args[index];
    }
}
