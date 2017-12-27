package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

    private final String indexName;
    private final BatchOperations batchOperations;
    private final BatchEmitter batchEmitter;

    public AsyncBatchDelivery(String indexName, int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, IndexTemplate indexTemplate) {
        this.indexName = indexName;
        this.batchOperations = objectFactory.createBatchOperations();
        this.batchEmitter = createBatchEmitterServiceProvider()
                .createInstance(
                        batchSize,
                        deliveryInterval,
                        objectFactory,
                        failoverPolicy);
        if (indexTemplate != null) {
            objectFactory.execute(indexTemplate);
        }
    }

    /**
     * Transforms given items to client-specific model and adds them to provided {@link BatchEmitter}
     *
     * @param log batch item source
     *
     * @deprecated will use configured indexName for backwards compatibility
     */
    @Override
    public void add(String log) {
        if (indexName == null) {
            throw new ConfigurationException("No indexName provided for AsyncBatchDelivery");
        }
        add(this.indexName, log);
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

        @PluginBuilderAttribute
        private String indexName;

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
            if (indexName != null) {
                LOG.warn("AsyncBatchDelivery.indexName attribute has been deprecated and will be removed in 1.3. " +
                        "It will NOT be used in AsyncBatchDelivery.add(String indexName,  T logObject) calls. " +
                        "Please use IndexName instead.");
            }

            if (clientObjectFactory == null) {
                throw new ConfigurationException("No Elasticsearch client factory [JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery");
            }
            return new AsyncBatchDelivery(indexName, batchSize, deliveryInterval, clientObjectFactory, failoverPolicy, indexTemplate);
        }

        /**
         * @deprecated  As of release 1.3, replaced by {@link IndexNameFormatter}
         *
         * @param indexName target index name
         * @return this
         */
        @Deprecated
        public Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
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

}
