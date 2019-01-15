package com.amazonaws.services.neptune.examples.social.util;

import org.joda.time.DateTime;

public class ActivityTimer implements AutoCloseable {

    private final long start = System.currentTimeMillis();

    @Override
    public void close() throws Exception {
        System.err.println();
        System.err.println(String.format("[%s] Completed in %s seconds", DateTime.now(), (System.currentTimeMillis() - start) / 1000));
    }
}
