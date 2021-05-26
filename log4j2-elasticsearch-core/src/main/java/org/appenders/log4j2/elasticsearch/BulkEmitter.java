package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import java.util.TimerTask;
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
public class BulkEmitter<BATCH_TYPE> implements BatchEmitter {

    private final AtomicInteger size = new AtomicInteger();
    private final Queue<Object> items;
    private final int maxSize;
    private final AtomicBoolean notifying = new AtomicBoolean();

    private final ScheduledExecutorService executor;
    private final int deliveryInterval;
    private final BatchOperations<BATCH_TYPE> batchOperations;
    private Function<BATCH_TYPE, Boolean> listener;
    private final AtomicReference<CountDownLatch> latchHolder = new AtomicReference<>(new CountDownLatch(1));

    private volatile State state = State.STOPPED;
    private final int shutdownDecrementMillis = Integer.parseInt(System.getProperty("appenders." + BulkEmitter.class.getSimpleName() + ".shutdownDecrementMillis", "1000"));
    final DelayedShutdown delayedShutdown = new DelayedShutdown(this::doStop)
            .decrementInMillis(shutdownDecrementMillis)
            .onDecrement(remaining -> {
                getLogger().info(
                        "Waiting for last items... {}s, {} items enqueued",
                        remaining / 1000,
                        size.get()
                );
                notifyListener();
            });

    public BulkEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations) {
        this(atSize, intervalInMillis, batchOperations, getQueueFactoryInstance().tryCreateMpmcQueue(
                BulkEmitter.class.getSimpleName(),
                Integer.parseInt(System.getProperty("appenders." + BulkEmitter.class.getSimpleName() + ".initialSize", "65536"))));
    }

    public BulkEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations, Queue<Object> queue) {
        this.maxSize = atSize;
        this.deliveryInterval = intervalInMillis;
        this.batchOperations = batchOperations;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "BatchNotifier"));
        this.items = queue;
    }

    /**
     * Delivers current batch to the listener if at least one item is waiting for delivery, no-op otherwise.
     */
    public final void notifyListener() {

        if (notifying.compareAndSet(false, true)) {

            // reset
            size.set(0);

            // get the size ONCE - size() gets costly when dealing with large linked queues
            int actualSize = items.size();

            if (actualSize == 0) {
                // scheduled notifications may have nothing to do
                notifying.set(false);
                return;
            }

            // create actual batch; there's a potential to leave some items undelivered
            // but they will be delivered eventually (on next trigger)
            BatchBuilder<BATCH_TYPE> batch = batchOperations.createBatchBuilder();
            for (int ii = 0; ii < actualSize; ii++) {
                batch.add(items.remove());
            }
            listener.apply(batch.build());

            // release other threads
            latchHolder.getAndSet(new CountDownLatch(1)).countDown();

            // switch back the gate condition
            notifying.set(false);
        } else {
            try {
                // wait until notification is completed;
                // unless there's a huge chunk of work to do on apply(batch), it should exit after a couple of millis
                latchHolder.get().await(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                getLogger().error("Interrupted while waiting for notification completion");
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public void add(Object batchItem) {

        items.add(batchItem);

        if (size.incrementAndGet() >= maxSize) {
            notifyListener();
        }


    }

    private TimerTask createNotificationTask() {
        return new TimerTask() {
            @Override
            public void run() {
                notifyListener();
            }
        };
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

    @SuppressWarnings("DuplicatedCode")
    void shutdownExecutor(long timeout) {

        executor.shutdown();

        try {

            final boolean terminated = executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            getLogger().info(
                    "{}: Executor was {}shutdown gracefully",
                    getClass().getSimpleName(),
                    terminated ? "" : "not ");
        } catch (InterruptedException e) {
            getLogger().error(
                    "{}: Executor shutdown interrupted",
                    getClass().getSimpleName());
            Thread.currentThread().interrupt();
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        long startDelayInMillis = Long.parseLong(System.getProperty(
                "appenders." + BulkEmitter.class.getSimpleName() + ".startDelay",
                Integer.toString(deliveryInterval)));

        this.executor.scheduleAtFixedRate(createNotificationTask(), startDelayInMillis, deliveryInterval, TimeUnit.MILLISECONDS);

        state = State.STARTED;
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
            getLogger().debug("Stopping {}. Flushing last batch if possible.", getClass().getSimpleName());

            notifyListener();

            shutdownExecutor(1000);

            state = State.STOPPED;

            getLogger().debug("{} stopped", getClass().getSimpleName());
        }
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
