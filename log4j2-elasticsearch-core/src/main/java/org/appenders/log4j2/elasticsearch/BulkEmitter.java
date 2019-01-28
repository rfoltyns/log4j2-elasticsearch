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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Time- and size-based batch scheduler. Uses provided {@link BatchOperations} implementation to produce batches and
 * delivers them to provided listener.
 *
 * @param <BATCH_TYPE> type of processed batches
 */
public class BulkEmitter<BATCH_TYPE> implements BatchEmitter {

    private volatile State state = State.STOPPED;

    private final AtomicInteger size = new AtomicInteger();

    private final int maxSize;
    private final int interval;
    private final BatchOperations<BATCH_TYPE> batchOperations;

    private final AtomicReference<BatchBuilder<BATCH_TYPE>> builder;

    private final Timer scheduler = new Timer();
    private Function<BATCH_TYPE, Boolean> listener;

    private final ListenerLock listenerLock = new ListenerLock();

    public BulkEmitter(int atSize, int intervalInMillis, BatchOperations<BATCH_TYPE> batchOperations) {
        this.maxSize = atSize;
        this.interval = intervalInMillis;
        this.batchOperations = batchOperations;
        this.builder = new AtomicReference<>(batchOperations.createBatchBuilder());
        this.scheduler.scheduleAtFixedRate(createNotificationTask(), 0, interval);
    }

    /**
     * Delivers current batch to the listener if at least one item is waiting for delivery, no-op otherwise.
     */
    public final void notifyListener() {
        synchronized (listenerLock) {
            if (size.get() == 0) {
                return;
            }
            this.size.set(0);
            listener.apply(builder.getAndSet(batchOperations.createBatchBuilder()).build());
        }
    }

    @Override
    public void add(Object batchItem) {

        builder.get().add(batchItem);

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
        state = State.STARTED;
    }

    @Override
    public void stop() {
        state = State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

    /*
     * Class used as monitor to increase lock visibility in profiling tools
     */
    private class ListenerLock {
    }

}
