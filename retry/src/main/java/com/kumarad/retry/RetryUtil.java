package com.kumarad.retry;

import java.util.function.Supplier;

/**
 * Util that supports synchronous retries with an exponential backoff.
 */
public class RetryUtil {

    public static<T> T retry(Supplier<T> supplier , int maxRetries, long backOffInMillis) throws Exception {
        return retryInternal(supplier, maxRetries, 0, backOffInMillis);
    }

    private static<T> T retryInternal(Supplier<T> supplier , int maxRetries, int retryCount, long backOffInMillis) throws Exception {
        try {
            return supplier.get();
        } catch (Exception e) {
            if (retryCount < maxRetries) {
                long delay = (long) (Math.pow(2, retryCount) * backOffInMillis);
                Thread.sleep(delay);

                return retryInternal(supplier, maxRetries,retryCount + 1, backOffInMillis);
            } else {
                throw e;
            }
        }
    }
}