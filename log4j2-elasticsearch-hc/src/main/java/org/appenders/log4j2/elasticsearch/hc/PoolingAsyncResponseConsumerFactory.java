package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.http.HttpResponse;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

/**
 * Allows to create pooled response consumer
 */
public class PoolingAsyncResponseConsumerFactory implements HttpAsyncResponseConsumerFactory, LifeCycle, Measured {

    private volatile State state = State.STOPPED;

    private final GenericItemSourcePool<SimpleInputBuffer> pool;
    private final PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics metrics;

    public PoolingAsyncResponseConsumerFactory(final GenericItemSourcePool<SimpleInputBuffer> pool) {
        this(pool, PoolingAsyncResponseConsumer.class.getSimpleName(), new DefaultMetricsFactory(PoolingAsyncResponseConsumer.metricConfigs(false)));
    }

    public PoolingAsyncResponseConsumerFactory(
            final GenericItemSourcePool<SimpleInputBuffer> pool,
            final String name,
            final MetricsFactory metricsFactory) {
        this.pool = pool;
        this.metrics = new PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics(name, metricsFactory);
    }

    @Override
    public HttpAsyncResponseConsumer<HttpResponse> create() {
        return new PoolingAsyncResponseConsumer(metrics, pool);
    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        if (!isStarted()) {
            pool.start();
            state = State.STARTED;
        }

    }

    @Override
    public void stop() {

        if (!isStopped()) {
            pool.stop();
            deregister();
            state = State.STOPPED;
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

    @Override
    public void register(final MetricsRegistry registry) {
        pool.register(registry);
        metrics.register(registry);
    }

    @Override
    public void deregister() {
        pool.deregister();
        metrics.deregister();
    }

}
