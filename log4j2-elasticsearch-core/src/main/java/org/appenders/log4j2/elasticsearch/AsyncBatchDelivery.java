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


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;

/**
 * Uses {@link BatchEmitterFactory} SPI to get a {@link BatchEmitter} instance that will hold given items until interval
 * or size conditions are met.
 */
@Plugin(name = "AsyncBatchDelivery", category = Node.CATEGORY, elementType = BatchDelivery.ELEMENT_TYPE, printObject = true)
public class AsyncBatchDelivery implements BatchDelivery<String> {

    private static final Logger LOG = StatusLogger.getLogger();

    private volatile State state = State.STOPPED;

    private final BatchOperations batchOperations;
    private final BatchEmitter batchEmitter;

    private final IndexTemplate indexTemplate;
    private final ClientObjectFactory<Object, Object> objectFactory;

    public AsyncBatchDelivery(int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, IndexTemplate indexTemplate) {
        this.batchOperations = objectFactory.createBatchOperations();
        this.batchEmitter = createBatchEmitterServiceProvider()
                .createInstance(
                        batchSize,
                        deliveryInterval,
                        objectFactory,
                        failoverPolicy);
        this.indexTemplate = indexTemplate;
        this.objectFactory = objectFactory;
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
         * Default: 1000
         */
        public static final int DEFAULT_DELIVERY_INTERVAL = 1000;

        /**
         * Default: {@link NoopFailoverPolicy}
         */
        public static final FailoverPolicy DEFAULT_FAILOVER_POLICY = new NoopFailoverPolicy();

        @PluginElement("elasticsearchClientFactory")
        @Required(message = "No Elasticsearch client factory [JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery")
        private ClientObjectFactory clientObjectFactory;

        @PluginBuilderAttribute
        private int deliveryInterval = DEFAULT_BATCH_SIZE;

        @PluginBuilderAttribute
        private int batchSize = DEFAULT_DELIVERY_INTERVAL;

        @PluginElement("failoverPolicy")
        private FailoverPolicy failoverPolicy = DEFAULT_FAILOVER_POLICY;

        @PluginElement("indexTemplate")
        private IndexTemplate indexTemplate;

        @Override
        public AsyncBatchDelivery build() {
            if (clientObjectFactory == null) {
                throw new ConfigurationException("No Elasticsearch client factory [JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery");
            }
            return new AsyncBatchDelivery(batchSize, deliveryInterval, clientObjectFactory, failoverPolicy, indexTemplate);
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

        public Builder withIndexTemplate(IndexTemplate indexTemplate) {
            this.indexTemplate = indexTemplate;
            return this;
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

        if (indexTemplate != null) {
            objectFactory.addOperation(() -> objectFactory.execute(indexTemplate));
        }

        batchEmitter.start();

        state = State.STARTED;
    }

    @Override
    public void stop() {

        LOG.debug("Stopping {}", getClass().getSimpleName());

        if (!batchEmitter.isStopped()){
            batchEmitter.stop();
        }

        if (!objectFactory.isStopped()) {
            objectFactory.stop();
        }

        state = State.STOPPED;

        LOG.debug("{} stopped", getClass().getSimpleName());

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
