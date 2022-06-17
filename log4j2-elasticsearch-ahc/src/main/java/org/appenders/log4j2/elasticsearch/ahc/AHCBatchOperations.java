package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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


import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

public class AHCBatchOperations implements BatchOperations<BatchRequest>, LifeCycle, Measured {

    private volatile State state = State.STOPPED;

    protected final PooledItemSourceFactory batchBufferFactory;
    private final ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> builderFactory;

    public AHCBatchOperations(final PooledItemSourceFactory batchBufferFactory, final ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> builderFactory) {
        this.batchBufferFactory = batchBufferFactory;
        this.builderFactory = builderFactory;
    }

    @Override
    public Object createBatchItem(final String target, final Object source) {
        throw new UnsupportedOperationException("Use ItemSource based API instead");
    }

    @Override
    public Object createBatchItem(final String target, final ItemSource payload) {
        return builderFactory.itemBuilder(target, payload).build();
    }

    @Override
    public BatchBuilder<BatchRequest> createBatchBuilder() {
        return new BatchBuilder<BatchRequest>() {

            private final BatchRequest.Builder builder = builderFactory.batchBuilder()
                    .withBuffer(batchBufferFactory.createEmptySource());

            @Override
            public void add(final Object item) {
                builder.add(item);
            }

            @Override
            public BatchRequest build() {
                return builder.build();
            }

        };
    }

    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        batchBufferFactory.start();

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        batchBufferFactory.stop();

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

    @Override
    public void register(final MetricsRegistry registry) {
        Measured.of(batchBufferFactory).register(registry);
    }

    @Override
    public void deregister() {
        Measured.of(batchBufferFactory).deregister();
    }

}

