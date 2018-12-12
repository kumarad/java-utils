/*
 * Copyright Convey 2018. All Rights Reserved.
 */
package com.kumarad.retry;

import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetryManagerTest {

    private static class TestDelayedRetry extends DelayedRetry {

        private long created;
        private long retried;

        public TestDelayedRetry(int retryCount) {
            super(1000, retryCount);
            created = Instant.now().toEpochMilli();
        }

        public void setRetried() {
            retried = Instant.now().toEpochMilli();
        }

        public long getDelta() {
            return retried - created;
        }

        @Override
        public String toString() {
            return "TestDelayedRetry toString";
        }
    }

    @Test (timeout = 5000)
    public void testRetries() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);

        RetryManager<TestDelayedRetry> retryManager = new RetryManager<>(retry -> {
            retry.setRetried();
            retryCount.incrementAndGet();
            return null;
        });

        TestDelayedRetry retry1 = new TestDelayedRetry(0);
        TestDelayedRetry retry2 = new TestDelayedRetry(1);
        retryManager.addRetry(retry1);
        retryManager.addRetry(retry2);

        while(retryCount.get() < 2) {
            Thread.sleep(100);
        }

        assertTrue("Retry Delta: " + retry1.getDelta(), retry1.getDelta() >= 1000 & retry1.getDelta() <= 1500);
        assertTrue("Retry Delta: " + retry2.getDelta(), retry2.getDelta() >= 2000 & retry2.getDelta() <= 2500);

        retryManager.shutdown();

        while(retryManager.shutdownCompleted()) {
            Thread.sleep(100);
        }
    }

    @Test (timeout = 5000)
    public void testMultipleRetriesWithRetryFailures() throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);

        RetryManager<TestDelayedRetry> retryManager = new RetryManager<>(retry -> {
            retry.setRetried();
            if (retryCount.incrementAndGet() == 2) {
                throw new RuntimeException("Expected failure");
            }
            return null;
        });

        TestDelayedRetry retry1 = new TestDelayedRetry(0);
        TestDelayedRetry retry2 = new TestDelayedRetry(0);
        TestDelayedRetry retry3 = new TestDelayedRetry(0);
        retryManager.addRetry(retry1);
        retryManager.addRetry(retry2);
        retryManager.addRetry(retry3);

        while(retryCount.get() < 3) {
            Thread.sleep(100);
        }

        assertTrue("Retry Delta: " + retry1.getDelta(), retry1.getDelta() >= 1000 & retry1.getDelta() <= 1500);
        assertTrue("Retry Delta: " + retry3.getDelta(), retry3.getDelta() >= 1000 & retry2.getDelta() <= 1500);

        retryManager.shutdown();

        while(retryManager.shutdownCompleted()) {
            Thread.sleep(100);
        }

        assertFalse(retryManager.addRetry(retry3));
    }
}