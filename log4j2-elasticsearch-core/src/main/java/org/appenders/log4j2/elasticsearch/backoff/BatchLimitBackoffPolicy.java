package org.appenders.log4j2.elasticsearch.backoff;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows to apply a limit on number of batches delivered at the same time.
 *
 * <p> NOTE: Even though the parameters are not used at the moment (as current impl is based on a simple batch count),
 * you can easily imagine a more complex implementation based on both total number of batches and
 * number of items in each batch where totalItems or totalBatches are more or less important in {@link #shouldApply(Object)} call.
 */
public class BatchLimitBackoffPolicy<T> implements BackoffPolicy<T> {

    private static final Logger LOG = StatusLogger.getLogger();

    private final AtomicInteger batchesInFlight = new AtomicInteger();
    private final int maxBatchesInFlight;

    /**
     * @param maxBatchesInFlight number of max. concurrent batches allowed
     */
    public BatchLimitBackoffPolicy(int maxBatchesInFlight) {
        this.maxBatchesInFlight = maxBatchesInFlight;
    }

    /**
     * @param request not used
     * @return true, if current number of pending batches is equal or higher than {@link #maxBatchesInFlight}, false otherwise
     */
    @Override
    public boolean shouldApply(T request) {
        LOG.debug("batchesInFlight: {}, maxBatchesInFlight: {}", batchesInFlight.get(), maxBatchesInFlight);
        return batchesInFlight.get() >= maxBatchesInFlight;
    }

    /**
     * Increments number of pending batches
     *
     * @param request not used, may be null
     */
    @Override
    public void register(T request) {
        batchesInFlight.incrementAndGet();
    }

    /**
     * Decrements number of pending batches
     *
     * @param request not used, may be null
     */
    @Override
    public void deregister(T request) {
        batchesInFlight.decrementAndGet();
    }

}
