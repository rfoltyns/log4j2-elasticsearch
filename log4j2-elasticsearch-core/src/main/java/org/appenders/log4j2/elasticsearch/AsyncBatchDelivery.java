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


import org.appenders.log4j2.elasticsearch.failover.FailoverListener;
import org.appenders.log4j2.elasticsearch.failover.RetryListener;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Uses {@link BatchEmitterFactory} SPI to get a {@link BatchEmitter} instance that will hold given items until interval
 * or size conditions are met.
 */
public class AsyncBatchDelivery implements BatchDelivery<String> {

    private volatile State state = State.STOPPED;

    protected final BatchOperations batchOperations;
    protected final BatchEmitter batchEmitter;

    protected final ClientObjectFactory<Object, Object> objectFactory;
    protected final FailoverPolicy failoverPolicy;
    protected final List<OpSource> setupOpSources = new ArrayList<>();

    protected final long shutdownDelayMillis;

    protected AsyncBatchDelivery(int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, long shutdownDelayMillis, OpSource[] setupOpSources) {
        this.batchOperations = objectFactory.createBatchOperations();
        this.batchEmitter = createBatchEmitterServiceProvider()
                .createInstance(
                        batchSize,
                        deliveryInterval,
                        objectFactory,
                        failoverPolicy);
        this.objectFactory = objectFactory;
        this.failoverPolicy = failoverPolicy;
        this.shutdownDelayMillis = shutdownDelayMillis;
        this.setupOpSources.addAll(Arrays.asList(setupOpSources));
    }

    /**
     * @param builder {@link Builder} instance
     */
    protected AsyncBatchDelivery(Builder builder) {
        this(
                builder.batchSize,
                builder.deliveryInterval,
                builder.clientObjectFactory,
                builder.failoverPolicy,
                builder.shutdownDelayMillis,
                builder.setupOpSources
        );
    }

    /**
     * Transforms given items to client-specific model and adds them to provided {@link BatchEmitter}
     *
     * @param log batch item source
     */
    @Override
    public void add(String indexName, String log) {
        this.batchEmitter.add(batchOperations.createBatchItem(indexName, log));
    }

    @Override
    public void add(String indexName, ItemSource source) {
        this.batchEmitter.add(batchOperations.createBatchItem(indexName, source));
    }

    protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
        return new BatchEmitterServiceProvider();
    }

    protected FailoverListener failoverListener() {
        // TODO: consider inverting the hierarchy as it may not be appropriate in this case
        return (RetryListener) event -> {
            this.add(event.getInfo().getTargetName(), event);
            return true;
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * Default: 1000
         */
        public static final int DEFAULT_BATCH_SIZE = 1000;

        /**
         * Default: 1000 ms
         */
        public static final int DEFAULT_DELIVERY_INTERVAL = 1000;

        /**
         * Default: {@link NoopFailoverPolicy}
         */
        public static final FailoverPolicy DEFAULT_FAILOVER_POLICY = new NoopFailoverPolicy();

        /**
         * Default: 5000 ms
         */
        public static final long DEFAULT_SHUTDOWN_DELAY = 5000L;

        /**
         * Default: []
         */
        public static final OpSource[] DEFAULT_OP_SOURCES = new OpSource[0];

        protected ClientObjectFactory clientObjectFactory;
        protected int deliveryInterval = DEFAULT_BATCH_SIZE;
        protected int batchSize = DEFAULT_DELIVERY_INTERVAL;
        protected FailoverPolicy failoverPolicy = DEFAULT_FAILOVER_POLICY;
        protected Long shutdownDelayMillis = DEFAULT_SHUTDOWN_DELAY;
        protected OpSource[] setupOpSources = DEFAULT_OP_SOURCES;

        public AsyncBatchDelivery build() {
            if (clientObjectFactory == null) {
                throw new IllegalArgumentException("No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for " +
                        AsyncBatchDelivery.class.getSimpleName());
            }
            if (batchSize <= 0) {
                throw new IllegalArgumentException("No batchSize provided for " + AsyncBatchDelivery.class.getSimpleName());
            }
            if (deliveryInterval <= 0) {
                throw new IllegalArgumentException("No deliveryInterval provided for " + AsyncBatchDelivery.class.getSimpleName());
            }
            return new AsyncBatchDelivery(this);
        }

        public Builder withClientObjectFactory(ClientObjectFactory clientObjectFactory) {
            this.clientObjectFactory = clientObjectFactory;
            return this;
        }

        public Builder withDeliveryInterval(int deliveryInterval) {
            this.deliveryInterval = deliveryInterval;
            return this;
        }

        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder withFailoverPolicy(FailoverPolicy failoverPolicy) {
            this.failoverPolicy = failoverPolicy;
            return this;
        }

        /**
         * @param setupOpSources definitions of operations to execute before first batch
         * @return this
         */
        public Builder withSetupOpSources(OpSource... setupOpSources) {
            this.setupOpSources = addSetupOpSource(setupOpSources);
            return this;
        }

        public Builder withShutdownDelayMillis(long shutdownDelayMillis) {
            this.shutdownDelayMillis = shutdownDelayMillis;
            return this;
        }

        private OpSource[] addSetupOpSource(OpSource... indexTemplates) {

            List<OpSource> current = new ArrayList<>(Arrays.asList(setupOpSources));
            current.addAll(Arrays.stream(indexTemplates)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            );

            return current.toArray(new OpSource[0]);
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        if (!objectFactory.isStarted()) {
            objectFactory.start();
        }

        for (OpSource setupOpSource : setupOpSources) {
            objectFactory.addOperation(objectFactory.setupOperationFactory().create(setupOpSource));
        }

        batchEmitter.start();

        if (!LifeCycle.of(failoverPolicy).isStarted()) {
            failoverPolicy.addListener(failoverListener());
            LifeCycle.of(failoverPolicy).start();
        }

        state = State.STARTED;

    }

    @Override
    public void stop() {

        getLogger().debug("Stopping {}", getClass().getSimpleName());

        if (!LifeCycle.of(failoverPolicy).isStopped()) {
            // Shutdown MUST happen in background to allow the execution to continue
            // and allow last items flushed by emitter to fail (in case of outage downstream)
            // and get handled properly
            LifeCycle.of(failoverPolicy).stop(shutdownDelayMillis, true);
        }

        if (!batchEmitter.isStopped()) {
            batchEmitter.stop(shutdownDelayMillis, false);
        }

        if (!objectFactory.isStopped()) {
            objectFactory.stop();
        }

        state = State.STOPPED;

        getLogger().debug("{} stopped", getClass().getSimpleName());

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
