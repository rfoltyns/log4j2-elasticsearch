package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;

/**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@Plugin(name = AsyncBatchDeliveryPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = BatchDelivery.ELEMENT_TYPE, printObject = true)
public class AsyncBatchDeliveryPlugin extends AsyncBatchDelivery {

    public static final String PLUGIN_NAME = "AsyncBatchDelivery";

    protected AsyncBatchDeliveryPlugin(AsyncBatchDelivery.Builder builder) {
        super(builder);
    }

    @PluginFactory
    public static AsyncBatchDeliveryPlugin createAsyncBatchDelivery(
            @PluginElement("objectFactory") ClientObjectFactory clientObjectFactory,
            @PluginAttribute("batchSize") int batchSize,
            @PluginAttribute("deliveryInterval") int deliveryInterval,
            @PluginElement("failoverPolicy") FailoverPolicy failoverPolicy,
            @PluginAttribute("shutdownDelayMillis") long shutdownDelayMillis,
            @PluginElement("setupOperation") OpSource[] setupOpSources,
            @PluginElement("MetricsProcessor") MetricsProcessor metricsProcessor) {

        if (clientObjectFactory == null) {
            throw new ConfigurationException("No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for AsyncBatchDelivery");
        }

        AsyncBatchDelivery.Builder builder = new AsyncBatchDelivery.Builder()
                .withClientObjectFactory(clientObjectFactory)
                .withDeliveryInterval(deliveryInterval <= 0 ? Builder.DEFAULT_DELIVERY_INTERVAL : deliveryInterval)
                .withBatchSize(batchSize <= 0 ? Builder.DEFAULT_BATCH_SIZE : batchSize)
                .withFailoverPolicy(failoverPolicy == null ? Builder.DEFAULT_FAILOVER_POLICY : failoverPolicy)
                .withSetupOpSources(setupOpSources.length == 0 ? Builder.DEFAULT_OP_SOURCES : setupOpSources)
                .withShutdownDelayMillis(shutdownDelayMillis < 0 ? Builder.DEFAULT_SHUTDOWN_DELAY : shutdownDelayMillis)
                .withMetricProcessor(metricsProcessor == null ? new MetricsProcessor(new BasicMetricsRegistry(), new BasicMetricOutputsRegistry()) : metricsProcessor);

        return new AsyncBatchDeliveryPlugin(builder);

    }

}
