package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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


import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

/**
 * Time- and size-based batch scheduler. Uses provided {@link BatchOperations} implementation to produce batches and
 * delivers them to provided listener.
 *
 * @param <BATCH_TYPE> type of processed batches
 */
public class AsyncBatchEmitter<BATCH_TYPE> implements BatchEmitter {

    public static final String QUEUE_FACTORY_NAME = BulkEmitter.class.getSimpleName();
    public static final int QUEUE_INITIAL_SIZE = Integer.parseInt(System.getProperty("appenders." + BulkEmitter.class.getSimpleName() + ".initialSize", "65536"));
    private final Queue<Object> items;
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSize;
    private final AtomicBoolean notifying = new AtomicBoolean();

    private volatile long lastEmittedTimestamp;
    private final int deliveryInterval;
    private final BatchOperations<BATCH_TYPE> batchOperations;
    private Function<BATCH_TYPE, Boolean> listener;

    private final EmitterLoop emitterLoop;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private final ScheduledExecutorService executor;

    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
    private final int shutdownDecrementMillis = Integer.parseInt(
            System.getProperty("appenders." + AsyncBatchEmitter.class.getSimpleName() + ".shutdownDecrementMillis", "1000")
    );

    private final DelayedShutdown delayedShutdown = new DelayedShutdown(this::doStop)
            .decrementInMillis(shutdownDecrementMillis)
            .onDecrement(remaining -> {
                getLogger().info(
                        "Waiting for last items... {}s, {} items enqueued",
                        remaining / shutdownDecrementMillis,
                        size.get()
                );
                notifyListener();
            });

    public AsyncBatchEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations) {
        this(atSize, intervalInMillis, batchOperations, getQueueFactoryInstance(QUEUE_FACTORY_NAME).tryCreateMpmcQueue(QUEUE_INITIAL_SIZE));
    }

    public AsyncBatchEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations, Queue<Object> queue) {
        this.maxSize = atSize;
        this.deliveryInterval = intervalInMillis;
        this.batchOperations = batchOperations;
        // FIXME: Switch to MpSc after batch assembly refactoring
        // FIXME: Current API makes initialSize difficult to inject
        this.items = queue;

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "BatchEmitter"));
        this.emitterLoop = new EmitterLoop(deliveryInterval, this::notifyListener);
    }

    /**
     * Delivers current batch to the listener if at least one item is waiting for delivery, no-op otherwise.
     */
    public final void notifyListener() {

        if (notifying.compareAndSet(false, true)) {

            if (size.get() == 0) {
                // scheduled notifications may have nothing to do
                notifying.set(false);
                return;
            }

            try {
                drain();
            } finally {
                emitterLoop.reset();
                // switch back the gate condition
                notifying.set(false);
            }

            getLogger().debug("{}: Notification complete", AsyncBatchEmitter.class.getSimpleName());

        }

    }

    public final boolean emit(int size) {

        // TODO: add to metrics
        lastEmittedTimestamp = System.currentTimeMillis();
        // TODO: add to metrics
        this.size.addAndGet(-size);

        // create actual batch; there's a potential to leave some items undelivered
        // but they will be delivered eventually (on next trigger)
        BatchBuilder<BATCH_TYPE> batch = batchOperations.createBatchBuilder();
        for (int ii = 0; ii < size; ii++) {
            batch.add(items.remove());
        }
        return listener.apply(batch.build());
    }

    private void drain() {

        // emit full batches
        for (int actualSize = items.size(); actualSize - maxSize >= 0 ; actualSize -= maxSize) {

            if (actualSize - maxSize < maxSize) {
                // handle items that are arriving while constructing previous batches
                // size() is costly on linked queues, so let's get it only if needed
                actualSize = items.size();
            }

            emit(maxSize);

        }

        // emit incomplete batches on scheduled notifications or on shutdown
        if (System.currentTimeMillis() - lastEmittedTimestamp > deliveryInterval || shuttingDown.get()) {
            emit(items.size());
        }

    }

    @Override
    public void add(Object batchItem) {

        final int newSize = size.incrementAndGet();

        items.add(batchItem);

        if (newSize >= maxSize) {
            emitterLoop.poke();
        }

    }

    /**
     * Sets new batch listener. Currently only one listener may be set. However, since it's an extension point, this
     * limitation can be overridden.
     *
     * @param onReadyListener batch-to-client handler
     */
    public void addListener(Function<BATCH_TYPE, Boolean> onReadyListener) {
        this.listener = onReadyListener;
    }

    /* visible for testing */
    EmitterLoop getEmitterLoop() {
        return emitterLoop;
    }

    static final class EmitterLoop implements Runnable {

        public static final String NAME = EmitterLoop.class.getSimpleName();
        private final AtomicBoolean running = new AtomicBoolean();
        private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));
        private final long interval;
        private final Runnable action;

        EmitterLoop(final long intervalMillis, final Runnable action) {
            this.interval = intervalMillis;
            this.action = action;
        }

        @Override
        public void run() {

            if (!running.compareAndSet(false, true)) {
                return;
            }

            while (running.get()) {

                try {

                    final boolean unlatched = latch.get().await(this.interval, TimeUnit.MILLISECONDS);

                    if (!running.get()) {
                        getLogger().info("{}: Ignoring wakeup while not running", NAME);
                        return;
                    }

                    getLogger().debug(
                            "{}: Executing on {}", NAME, unlatched ? "demand" : "interval");

                    action.run();

                } catch (InterruptedException e) {

                    getLogger().error("{}: Loop interrupted. Stopping", NAME);

                    stop();

                    Thread.currentThread().interrupt();

                } catch (Exception e) {
                    getLogger().error("{}: Execution failed: {}", NAME, e.getMessage());
                }

            }

            getLogger().info("{}: Stopped", EmitterLoop.class.getSimpleName());

        }

        void poke() {
            latch.get().countDown();
        }

        void reset() {
            latch.getAndSet(new CountDownLatch(1));
        }

        void stop() {

            if (running.compareAndSet(true, false)) {
                latch.get().countDown();
                getLogger().info("{}: Loop stopped", EmitterLoop.class.getSimpleName());
            }

        }

    }

    void shutdownExecutor() {

        emitterLoop.stop();
        executor.shutdown();

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        if (!isStarted()) {

            long startDelayInMillis = Long.parseLong(System.getProperty(
                    "appenders." + EmitterLoop.class.getSimpleName() + ".startDelay",
                    "0"));

            this.executor.schedule(getEmitterLoop(), startDelayInMillis, TimeUnit.MILLISECONDS);

            state.set(State.STARTED);

        }

    }

    @Override
    public void stop() {
        stop(0, false);
    }

    @Override
    public LifeCycle stop(long timeout, boolean runInBackground) {
        delayedShutdown.delay(timeout).start(runInBackground);
        return this;
    }

    private void doStop() {
        if (!isStopped()) {
            getLogger().debug("Stopping {}. Flushing last batches if possible.", getClass().getSimpleName());

            shuttingDown.compareAndSet(false, true);

            notifyListener();

            shutdownExecutor();

            state.set(State.STOPPED);

            getLogger().debug("{} stopped", getClass().getSimpleName());
        }
    }

    @Override
    public boolean isStarted() {
        return state.get() == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state.get() == State.STOPPED;
    }

}
