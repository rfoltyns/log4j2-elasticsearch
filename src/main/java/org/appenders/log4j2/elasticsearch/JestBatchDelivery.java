package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;

@Plugin(name = "JestBatchDelivery", category = Node.CATEGORY, elementType = BatchDelivery.ELEMENT_TYPE, printObject = true)
public class JestBatchDelivery implements BatchDelivery<String> {

    private static String ACTION_TYPE = "index";

    private final String indexName;
    private final BulkEmitter bulkEmitter;

    public JestBatchDelivery(String indexName, int batchSize, int deliveryInterval, ClientObjectFactory<JestClient, Bulk> objectFactory, FailoverPolicy failoverPolicy) {
        this.indexName = indexName;
        this.bulkEmitter = new BulkEmitter(batchSize, deliveryInterval);
        this.bulkEmitter.addObserver(objectFactory.createBatchListener(failoverPolicy));
    }

    @Override
    public void add(String logObject) {
        this.bulkEmitter.add(
                new Index.Builder(logObject)
                .index(indexName)
                .type(ACTION_TYPE)
                .build());
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JestBatchDelivery> {

        public static final int DEFAULT_BATCH_SIZE = 1000;
        public static final int DEFAULT_DELIVERY_INTERVAL = 1000;
        public static final FailoverPolicy DEFAULT_FAILOVER_POLICY = new NoopFailoverPolicy();

        @PluginBuilderAttribute
        @Required(message = "No indexName provided for JestBatchDelivery")
        private String indexName;

        @PluginElement("elasticsearchClientFactory")
        @Required(message = "No Elasticsearch client factory [JestHttp] provided for JestBatchDelivery")
        private ClientObjectFactory clientObjectFactory;

        @PluginBuilderAttribute
        private int deliveryInterval = DEFAULT_BATCH_SIZE;

        @PluginBuilderAttribute
        private int batchSize = DEFAULT_DELIVERY_INTERVAL;

        @PluginElement("failoverPolicy")
        private FailoverPolicy failoverPolicy = DEFAULT_FAILOVER_POLICY;

        @Override
        public JestBatchDelivery build() {
            if (indexName == null) {
                throw new ConfigurationException("No indexName provided for JestClientConfig");
            }
            if (clientObjectFactory == null) {
                throw new ConfigurationException("No Elasticsearch client factory [JestHttp] provided for JestBatchDelivery");
            }
            return new JestBatchDelivery(indexName, batchSize, deliveryInterval, clientObjectFactory, failoverPolicy);
        }

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

    }

}
