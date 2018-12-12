/*
 * Copyright Convey 2018. All Rights Reserved.
 */
package com.kumarad.retry;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RetryUtilTest {

    private int supplierInvocationCount;

    @Before
    public void setup() {
        supplierInvocationCount = 0;
    }

    @Test
    public void testSuccessAfterRetries() throws Exception {
        Supplier<Integer> supplier = () -> {
            supplierInvocationCount++;
            if (supplierInvocationCount == 4) {
                return supplierInvocationCount;
            } else {
                throw new RuntimeException("Expected");
            }
        };


        Instant start = Instant.now();
        assertEquals(new Integer(4), RetryUtil.retry(supplier, 5, 100));
        long timeTaken = Instant.now().minusMillis(start.toEpochMilli()).toEpochMilli();

        assertTrue(timeTaken > 700);
    }

    @Test (expected = Exception.class)
    public void testFailAfterRetries() throws Exception {
        Supplier<Integer> supplier = () -> {
            throw new RuntimeException("Expected");
        };

        RetryUtil.retry(supplier, 4, 100);
    }

}