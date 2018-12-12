package com.kumarad.retry;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 *
 * Starts up a thread to attempt retries that implement the Delayed
 * interface.
 *
 * Once a shutdown is requested, new retry requests will be rejected.
 *
 */
public class RetryManager<T extends DelayedRetry> {

    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);

    private volatile boolean shutdown = false;
    private volatile boolean shutdownCompleted = false;

    private final Function<T, Void> retryFunction;
    private final DelayQueue<T> queue;
    private final ExecutorService executorService;

    public RetryManager(Function<T, Void> retryFunction) {
        this.retryFunction = retryFunction;
        this.queue = new DelayQueue<>();

        executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("retry-manager-%d").build());
        executorService.submit(this::run);
    }

    /**
     * Will reject retries once the manager has been shutdown.
     */
    public boolean addRetry(T retry) {
        if (executorService.isShutdown()) {
            logger.info("Rejecting retry request because manager has been shutdown.");
            return false;
        } else {
            queue.add(retry);
            return true;
        }
    }

    /**
     * This method will initiate a shutdown. Note that the thread attempting retries
     * will not truly shutdown till all queued up retries have been attempted.
     * Invoke isShutdown to determine when all the retries have been attempted.
     *
     * The client of this class is responsible for deciding when to give up on the retries
     * and just shutdown regardless of the state of queued up retries. RetryManager will abide by the
     * InterruptedException contract and force shutdown when necessary.
     *
     * In Dropwizard, this class should be added to the managed lifecycle as an example.
     */
    public void shutdown() {
        logger.info("Shutting down RetryManager.");
        shutdown = true;

        executorService.shutdown();
    }

    public boolean shutdownCompleted() {
        return shutdownCompleted;
    }

    private void run() {
        List<T> drainedList = new LinkedList<>();

        while (!shutdown) {
            drainedList.clear();
            queue.drainTo(drainedList);

            drainedList.forEach(retry -> {
                try {
                    retryFunction.apply(retry);
                } catch (Exception e) {
                    // Don't let one rogue retry kill the manager.
                }
            });

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // JVM is going down. Time to shutdown regardless of whether we have stuff
                // queued up for retries.
                return;
            }
        }

        shutdownCompleted = true;
    }
}

