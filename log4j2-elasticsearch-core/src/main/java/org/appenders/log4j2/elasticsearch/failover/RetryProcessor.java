package org.appenders.log4j2.elasticsearch.failover;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ItemSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Retries items from given map. Next item key is determined by {@link KeySequence#nextReaderKey()}.
 * {@link KeySequence} instance is retrieved with {@link KeySequenceSelector#firstAvailable()}.
 * Number of retried items depends on configured {@link #maxRetryBatchSize} and {@link KeySequence#readerKeysAvailable()}.
 * <p> Each item is removed from map before {@link #retryListeners} are notified.
 * This allows the key sequence to make progress and if retried item fails again,
 * it SHOULD be put back to given map (outside of scope of this class) to be picked up again on next runs.
 */
class RetryProcessor implements Runnable {

    private static final Logger LOG = InternalLogging.getLogger();

    private final long backoffMillis;

    private final int maxRetryBatchSize;
    private final Map<CharSequence, ItemSource> items;
    private final RetryListener[] retryListeners;
    private final KeySequenceSelector keySequenceSelector;
    private final List<CharSequence> selectedKeysList;

    // TODO: add to metrics
    final AtomicLong orphanedKeyCount = new AtomicLong();
    final AtomicLong readFailureCount = new AtomicLong();

    /**
     * In order to cater for the delay between key sequence writer key being obtained and item persisted,
     * configurable delay can be specified with <i>"appenders.retry.backoff.millis"</i> (1000ms by default).
     * Pause can be disabled by setting <i>appenders.retry.backoff.millis</i> to 0.
     * 
     * @param maxRetryBatchSize maximum number of items retried on a single run
     * @param failedItems failed items store
     * @param retryListeners failed item listeners
     * @param keySequenceSelector {@link KeySequence} provider
     */
    public RetryProcessor(
            int maxRetryBatchSize,
            Map<CharSequence, ItemSource> failedItems,
            RetryListener[] retryListeners,
            KeySequenceSelector keySequenceSelector
    ) {
        this.maxRetryBatchSize = maxRetryBatchSize;
        this.items = failedItems;
        this.retryListeners = retryListeners;
        this.keySequenceSelector = keySequenceSelector;
        this.selectedKeysList = new ArrayList<>(maxRetryBatchSize);
        this.backoffMillis = TimeUnit.MILLISECONDS.toNanos(
                Long.parseLong(System.getProperty("appenders.retry.backoff.millis", "1000"))
        );
    }

    @Override
    public final void run() {
        retry();
    }

    /**
     * Delivers failed items to {@link #retryListeners}.
     * <p>Retrieves current {@link KeySequence} using {@link KeySequenceSelector#firstAvailable()}.
     * <p>Retry batch size is the lowest of {@link #maxRetryBatchSize} and {@link KeySequence#readerKeysAvailable()}
     * <p>Returns immediately, if {@link KeySequenceSelector#firstAvailable()} returns null or {@link KeySequence#readerKeysAvailable()} equals 0.
     * <p>Configurable pause ({@code env:appenders.retry.backoff.millis=1000) is applied to ensure that latest writer keys are not orphaned.
     * Pause can be disabled by setting <i>appenders.retry.backoff.millis</i> to 0.
     */
    void retry() {

        KeySequence keySequence = keySequenceSelector.firstAvailable();

        if (keySequence == null) {
            return;
        }

        long keysAvailable = keySequence.readerKeysAvailable();
        if (keysAvailable == 0) {
            return;
        }

        long remaining = Math.min(maxRetryBatchSize, keysAvailable);
        Iterator<CharSequence> keys = keySequence.nextReaderKeys(remaining);

        LOG.info("Retrying {} of {} items. Left behind: {}. Exceptions: {}",
                remaining,
                keysAvailable,
                orphanedKeyCount.get(),
                readFailureCount.get()
        );

        // collect keys
        while (keys.hasNext()) {
            selectedKeysList.add(keys.next());
        }

        // simple backoff for now
        if (backoffMillis > 0) {
            LockSupport.parkNanos(backoffMillis);
        }

        Iterator<CharSequence> selectedKeys = selectedKeysList.iterator();
        try {
            while (selectedKeys.hasNext()) {
                CharSequence next = selectedKeys.next();
                retryInternal(next);
            }
        } catch (Exception e) {
            readFailureCount.incrementAndGet();
            LOG.error("Retry failed. Item may be lost. Cause: {}", e.getMessage());
        } finally {
            selectedKeysList.clear();
        }

        // consider explicit update() on success
        keySequenceSelector.firstAvailable();

    }

    private void retryInternal(CharSequence next) {

        // consider removing on release()
        ItemSource failedItem = items.remove(next);
        // sanity check, until the code is stabilized
        if (failedItem instanceof FailedItemSource) {
            notifyListeners((FailedItemSource) failedItem);
        } else if (failedItem == null) {
            orphanedKeyCount.incrementAndGet();
        } else {
            // Invalid type. Not likely, but let's handle if it ever happens
            failedItem.release();
        }

    }

    private void notifyListeners(FailedItemSource failedItem) {
        for (int i = 0; i < retryListeners.length; i++) {
            retryListeners[i].notify(failedItem);
        }
    }

}

