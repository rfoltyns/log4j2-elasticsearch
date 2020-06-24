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

import net.openhft.chronicle.hash.ChronicleHashCorruption;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.DelayedShutdown;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Uses Chronicle-Map (https://github.com/OpenHFT/Chronicle-Map) to store failed items.
 * Uses {@link RetryProcessor} to retry failed items.
 */
@Plugin(name = ChronicleMapRetryFailoverPolicy.PLUGIN_NAME, category = Node.CATEGORY, elementType = FailoverPolicy.ELEMENT_TYPE, printObject = true)
public class ChronicleMapRetryFailoverPolicy implements FailoverPolicy<ItemSource>, LifeCycle {

    public static final String PLUGIN_NAME = "ChronicleMapRetryFailoverPolicy";

    private static final Logger LOGGER = InternalLogging.getLogger();

    private volatile State state = State.STOPPED;

    private final MapProxy<CharSequence, ItemSource> failedItems;
    private final KeySequenceSelector keySequenceSelector;
    private final Supplier<KeySequence> keySequenceSupplier;

    private DelayedShutdown shutdown;

    Collection<ScheduledExecutorService> executors = new ConcurrentLinkedQueue<>();

    /**
     * Number of exceptions occurred during failed item processing.
     * Indicates potential issues with underlying storage.
     */
    protected final AtomicInteger storeFailureCount = new AtomicInteger();
    protected final int batchSize;
    protected final long retryDelay;

    protected final boolean monitored;
    protected final long monitorTaskInterval;

    protected RetryListener[] retryListeners = new RetryListener[0];

    /**
     * See {@link Builder}
     *
     * @param builder config
     */
    protected ChronicleMapRetryFailoverPolicy(Builder builder) {
        this.failedItems = builder.mapProxy;
        this.keySequenceSelector = builder.keySequenceSelector;
        this.keySequenceSupplier = keySequenceSelector.currentKeySequence();
        this.batchSize = builder.batchSize;
        this.retryDelay = builder.retryDelay;
        this.monitored = builder.monitored;
        this.monitorTaskInterval = builder.monitorTaskInterval;
    }

    /**
     * Allows to add multiple listeners. If given listener is an instance of {@link RetryListener},
     * It will be notified by {@link RetryProcessor}
     *
     * @param failoverListener item listener
     */
    @Override
    public <U extends FailoverListener> void addListener(U failoverListener) {
        if (failoverListener instanceof RetryListener) {
            List<RetryListener> listeners = new ArrayList<>(Arrays.asList(this.retryListeners));
            listeners.add((RetryListener) failoverListener);
            this.retryListeners = listeners.toArray(new RetryListener[0]);
        }
    }

    /**
     * Stores given item with key resolved from configured key sequence
     *
     * @param failedItemSource failed item
     */
    @Override
    public void deliver(ItemSource failedItemSource) {
        CharSequence key = keySequenceSupplier.get().nextWriterKey();
        tryPut(key, failedItemSource);
    }

    /**
     * Stores failed item
     *
     * @param key failed item key
     * @param failedItem failed item
     * @return true, if item was stored successfully, false otherwise
     */
    boolean tryPut(CharSequence key, ItemSource failedItem) {

        if (failedItem == null) {
            return false;
        }

        try {
            failedItems.put(key, failedItem);
            return true;
        } catch (Exception e) {
            // TODO: add to metrics
            storeFailureCount.incrementAndGet();
            LOGGER.error("Unable to store {}. Cause: {}", failedItem.getClass().getSimpleName(), e.getMessage());
            return false;
        }

    }

    RetryProcessor createRetryProcessor() {
        return new RetryProcessor(
                batchSize,
                failedItems,
                retryListeners,
                keySequenceSelector);
    }

    /*
     * temporary, until metrics are ready
     */
    MetricsPrinter createMetricPrinter() {
        return new MetricsPrinter();
    }

    ScheduledExecutorService createExecutor(String threadName) {

        ScheduledExecutorService executor =
                Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, threadName));

        executors.add(executor);

        return executor;
    }

    DelayedShutdown delayedShutdown() {
        return new DelayedShutdown(() -> executors.forEach(ExecutorService::shutdown))
                .onDecrement(remaining -> LOGGER.warn("{} ms before proceeding", remaining))
                .afterDelay(() -> {

                    int totalKeys = failedItems.size();
                    long enqueuedKeys = keySequenceSelector.currentKeySequence().get().readerKeysAvailable();

                    LOGGER.info(
                            "sequenceId: {}, total: {}, enqueued: {}",
                            keySequenceSelector.currentKeySequence().get().getConfig(true).getSeqId(),
                            totalKeys,
                            enqueuedKeys);

                    keySequenceSelector.close();
                    failedItems.close();
                });
    }

    private void schedule(ScheduledExecutorService executor, Runnable runnable, long interval) {
        executor.scheduleAtFixedRate(
                runnable,
                0,
                interval,
                TimeUnit.MILLISECONDS
        );

    }

    private void validateSetup() {
        if (retryListeners.length == 0) {
            throw new ConfigurationException(String.format(
                    "%s was not provided for %s",
                    RetryListener.class.getSimpleName(),
                    ChronicleMapRetryFailoverPolicy.class.getSimpleName())
            );
        }
    }

    @PluginBuilderFactory
    public static ChronicleMapRetryFailoverPolicy.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ChronicleMapRetryFailoverPolicy> {

        /**
         * Default entry size: 1024 bytes
         * <p/>
         * NOTE: Suitable for small logs (~300 characters)
         * )
         */
        public static final int DEFAULT_AVERAGE_VALUE_SIZE = 1024;

        /**
         * Default batch size: 1000
         */
        public static final int DEFAULT_BATCH_SIZE = 1000;

        /**
         * Default retry interval: 10 seconds
         */
        public static final int DEFAULT_RETRY_DELAY = 10000;


        @PluginBuilderAttribute("fileName")
        protected String fileName;

        @PluginBuilderAttribute("numberOfEntries")
        protected long numberOfEntries;

        @PluginBuilderAttribute(value = "averageValueSize")
        protected int averageValueSize = DEFAULT_AVERAGE_VALUE_SIZE;

        @PluginBuilderAttribute(value = "batchSize")
        protected int batchSize = DEFAULT_BATCH_SIZE;

        @PluginBuilderAttribute(value = "retryDelay")
        protected long retryDelay = DEFAULT_RETRY_DELAY;

        @PluginElement("keySequenceSelector")
        protected KeySequenceSelector keySequenceSelector;

        @PluginBuilderAttribute("monitored")
        protected boolean monitored;

        @PluginBuilderAttribute(value = "monitorTaskInterval")
        protected long monitorTaskInterval = DEFAULT_RETRY_DELAY;

        private MapProxy<CharSequence, ItemSource> mapProxy;

        @Override
        public final ChronicleMapRetryFailoverPolicy build() {

            if (keySequenceSelector == null) {
                throw new ConfigurationException(
                        KeySequenceSelector.class.getSimpleName() + " was not provided for " + ChronicleMapRetryFailoverPolicy.class.getSimpleName()
                );
            }

            if (fileName == null) {
                throw new ConfigurationException(String.format("fileName was not provided for %s",
                        ChronicleMapRetryFailoverPolicy.class.getSimpleName()));
            }

            if (averageValueSize < 1024) {
                throw new ConfigurationException("averageValueSize must be higher than or equal 1024");
            }

            if (numberOfEntries <= 2) {
                throw new ConfigurationException("numberOfEntries must be higher than 2");
            }

            if (batchSize <= 0) {
                throw new ConfigurationException("batchSize must be higher than 0");
            }

            try {
                this.mapProxy = new ChronicleMapProxy(createChronicleMap());
                this.keySequenceSelector = configuredKeySequenceSelector();
            } catch (Exception e) {
                throw new ConfigurationException("Could not initialize " +
                        ChronicleMapRetryFailoverPolicy.class.getSimpleName(), e);
            }

            return new ChronicleMapRetryFailoverPolicy(this);

        }

        protected KeySequenceSelector configuredKeySequenceSelector() {

            KeySequenceConfigRepository repository = createKeySequenceConfigRepository(this.mapProxy);
            keySequenceSelector.withRepository(repository);

            KeySequence keySequence = keySequenceSelector.firstAvailable();
            if (keySequence == null) {
                // Lack of key sequence in this spot should be terminal
                // invalid config/storage issues/concurrent updates -> it won't work properly -> just throw..
                throw new IllegalStateException("Failed to find a valid key sequence for " + ChronicleMapRetryFailoverPolicy.class);
            }

            return keySequenceSelector;
        }

        KeySequenceConfigRepository createKeySequenceConfigRepository(Map<CharSequence, ItemSource> map) {
            return new KeySequenceConfigRepository(map);
        }

        ChronicleMap<CharSequence, ItemSource> createChronicleMap() throws IOException {
            return defaultChronicleMapBuilder()
                    .averageKeySize(36)
                    .averageValueSize(averageValueSize)
                    .entries(numberOfEntries)
                    .putReturnsNull(true)
                    .removeReturnsNull(false)
                    .valueMarshaller(createItemMarshaller())
                    .createOrRecoverPersistedTo(new File(fileName), false, createCorruptionListener());
        }

        FailedItemMarshaller createItemMarshaller() {
            return new FailedItemMarshaller();
        }

        HashCorruptionListener createCorruptionListener() {
            return new HashCorruptionListener();
        }

        final ChronicleMapBuilder<CharSequence, ItemSource> defaultChronicleMapBuilder() {
            return ChronicleMap
                    .of(CharSequence.class, ItemSource.class)
                    .name(getClass().getName());
        }

        /**
         * @param fileName ChronicleMap file name. Both absolute and relative paths are allowed
         * @return this
         */
        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * @param numberOfEntries Failed item count limit.
         *                        <p>Underlying storage MAY fail if storing beyond this limit
         *                        <p>Underlying storage MAY fail before this limit is reached
         *                        if averageValueSize of stored entries was exceeded.
         *                        <p>Calculate the {@link #averageValueSize} carefully
         * @return this
         */
        public Builder withNumberOfEntries(long numberOfEntries) {
            this.numberOfEntries = numberOfEntries;
            return this;
        }

        /**
         * @param averageValueSize Average size of failed item. NOTE: metadata requires ~1kB, add payload length on top of it.
         * @return this
         */
        public Builder withAverageValueSize(int averageValueSize) {
            this.averageValueSize = averageValueSize;
            return this;
        }

        /**
         * @param batchSize max number of retried items on each retry attempt
         * @return this
         */
        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * @param retryDelay pause after each retry attempt
         * @return this
         */
        public Builder withRetryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        /**
         * @param monitored if <tt>true</tt>, execution metrics will be printed
         * @return this
         */
        public Builder withMonitored(boolean monitored) {
            this.monitored = monitored;
            return this;
        }

        /**
         * @param monitorTaskInterval interval between metrics print task
         * @return this
         */
        public Builder withMonitorTaskInterval(long monitorTaskInterval) {
            this.monitorTaskInterval = monitorTaskInterval;
            return this;
        }

        /**
         * @param keySequenceSelector key sequence resolver
         * @return this
         */
        public Builder withKeySequenceSelector(KeySequenceSelector keySequenceSelector) {
            this.keySequenceSelector = keySequenceSelector;
            return this;
        }

    }

    static class HashCorruptionListener implements ChronicleHashCorruption.Listener {

        @Override
        public void onCorruption(ChronicleHashCorruption corruption) {
            if (corruption.exception() != null) {
                LOGGER.error(corruption.message(), corruption.exception());
            } else {
                LOGGER.error(corruption.message());
            }
        }

    }

    class MetricsPrinter implements Runnable {

        @Override
        public void run() {

            int totalKeys = failedItems.size();
            long enqueuedKeys = keySequenceSelector.currentKeySequence().get().readerKeysAvailable();

            LOGGER.info(
                    "sequenceId: {}, total: {}, enqueued: {}",
                    keySequenceSelector.currentKeySequence().get().getConfig(true).getSeqId(),
                    totalKeys,
                    enqueuedKeys);
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        if (!isStarted()) {

            validateSetup();

            this.shutdown = delayedShutdown();

            schedule(createExecutor("Retry-main"), createRetryProcessor(), retryDelay);

            if (monitored) {
                schedule(createExecutor("Retry-metrics"), createMetricPrinter(), monitorTaskInterval);
            }

            state = State.STARTED;

        }
    }

    @Override
    public void stop() {

        if (!isStopped()) {
            int timeout = 0;
            boolean async = false;

            stop(timeout, async);
            state = State.STOPPED;
        }

    }

    @Override
    public LifeCycle stop(long timeout, boolean async) {
        if (!isStopped()) {
            shutdown.delay(timeout).start(async);
            state = State.STOPPED;
        }
        return this;
    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

}
