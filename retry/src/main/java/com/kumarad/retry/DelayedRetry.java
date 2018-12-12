package com.kumarad.retry;

import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Implements the Delayed interface to track retries using an exponential backoff.
 */
public abstract class DelayedRetry implements Delayed {
    private final int attemptedRetryCount;
    private final Long retryTimeInMillis;

    public DelayedRetry(long baseDelayInMillis, int attemptedRetryCount) {
        this.attemptedRetryCount = attemptedRetryCount;

        // Calculate the exponential backoff for the next retry attempt
        long delayInMillis = (long)(Math.pow(2, attemptedRetryCount) * baseDelayInMillis);
        retryTimeInMillis = Instant.now().plusMillis(delayInMillis).toEpochMilli();
    }

    public int getAttemptedRetryCount() {
        return attemptedRetryCount;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        // If we are past retryTimeInMillis its time to retry
        long diff = retryTimeInMillis - Instant.now().toEpochMilli();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return retryTimeInMillis.compareTo(((DelayedRetry)other).retryTimeInMillis);
    }
}
