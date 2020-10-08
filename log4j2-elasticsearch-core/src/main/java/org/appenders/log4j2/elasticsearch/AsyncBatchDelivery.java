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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
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
@Plugin(name = "AsyncBatchDelivery", category = Node.CATEGORY, elementType = BatchDelivery.ELEMENT_TYPE, printObject = true)
public class AsyncBatchDelivery implements BatchDelivery<String> {

    private volatile State state = State.STOPPED;

    private final BatchOperations batchOperations;
    private final BatchEmitter batchEmitter;

    private final ClientObjectFactory<Object, Object> objectFactory;
    private final FailoverPolicy failoverPolicy;
    private final List<OpSource> setupOpSources = new ArrayList<>();

    private final long shutdownDelayMillis;

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

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<AsyncBatchDelivery> {

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


        @PluginElement("elasticsearchClientFactory")
        @Required(message = "No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery")
        private ClientObjectFactory clientObjectFactory;

        @PluginBuilderAttribute
        private int deliveryInterval = DEFAULT_BATCH_SIZE;

        @PluginBuilderAttribute
        private int batchSize = DEFAULT_DELIVERY_INTERVAL;

        @PluginElement("failoverPolicy")
        private FailoverPolicy failoverPolicy = DEFAULT_FAILOVER_POLICY;

        @PluginBuilderAttribute("shutdownDelayMillis")
        public long shutdownDelayMillis = DEFAULT_SHUTDOWN_DELAY;

        @PluginElement("setupOperation")
        private OpSource[] setupOpSources = new OpSource[0];

        @Override
        public AsyncBatchDelivery build() {
            if (clientObjectFactory == null) {
                throw new ConfigurationException("No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery");
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
         * @param indexTemplate index template to be configured before first batch
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withSetupOpSources(OpSource...)} instead
         */
        @Deprecated
        public Builder withIndexTemplate(IndexTemplate indexTemplate) {
            this.setupOpSources = addSetupOpSource(indexTemplate);
            return this;
        }

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
