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


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Time- and size-based batch scheduler. Uses provided {@link BatchOperations} implementation to produce batches and
 * delivers them to provided listener.
 *
 * @param <BATCH_TYPE> type of processed batches
 */
public class BulkEmitter<BATCH_TYPE> implements BatchEmitter {

    private volatile State state = State.STOPPED;

    private final AtomicInteger size = new AtomicInteger();
    private final ConcurrentLinkedQueue<Object> items = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean notifying = new AtomicBoolean();
    private final AtomicReference<CountDownLatch> latchHolder = new AtomicReference<>(new CountDownLatch(1));

    private final int maxSize;
    private final int deliveryInterval;
    private final BatchOperations<BATCH_TYPE> batchOperations;
    private final Timer scheduler;
    private final DelayedShutdown delayedShutdown = new DelayedShutdown(this::doStop)
            .onDecrement(remaining -> {
                getLogger().info(
                        "Waiting for last items... {}s, {} items enqueued",
                        remaining / 1000,
                        size.get()
                );
                notifyListener();
            });

    private Function<BATCH_TYPE, Boolean> listener;

    public BulkEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations) {
        this.maxSize = atSize;
        this.deliveryInterval = intervalInMillis;
        this.batchOperations = batchOperations;
        this.scheduler = new Timer("BatchNotifier");
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

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {
        this.scheduler.scheduleAtFixedRate(createNotificationTask(), 1000, deliveryInterval);
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
            scheduler.cancel();
            scheduler.purge();

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
