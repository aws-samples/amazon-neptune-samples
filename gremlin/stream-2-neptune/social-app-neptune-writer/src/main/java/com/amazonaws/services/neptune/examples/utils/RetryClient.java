/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

package com.amazonaws.services.neptune.examples.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class RetryClient {

    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(RetryClient.class);

    public int retry(Runnable task, int retryCount, int baseMillis, RetryCondition... conditions) throws Exception {

        int count = 0;
        boolean allowContinue = true;
        Exception lastException = null;


        while (allowContinue) {

            if (count > 0) {
                try {
                    int delayMillis = baseMillis + ((2 ^ count) * 15) + random.nextInt(100);
                    logger.info(String.format("Retry – count %s delay %s", count, delayMillis));
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }

            try {
                task.run();
                allowContinue = false;
            } catch (Exception e) {
                if (count < retryCount) {
                    boolean isRetriable = false;
                    for (RetryCondition condition : conditions) {
                        if (condition.allowRetry(e)) {
                            count++;
                            isRetriable = true;
                            logger.info(String.format("Retriable exception: %s", e.getMessage()));
                            break;
                        }
                    }
                    if (!isRetriable) {
                        lastException = e;
                        allowContinue = false;
                    }
                } else {
                    lastException = e;
                    allowContinue = false;
                }
            }
        }


        if (lastException != null) {
            throw lastException;
        }

        return count;
    }

    public int retry(Runnable task, int retryCount, RetryCondition... conditions) throws Exception {
        return retry(task, retryCount, 500, conditions);
    }
}
