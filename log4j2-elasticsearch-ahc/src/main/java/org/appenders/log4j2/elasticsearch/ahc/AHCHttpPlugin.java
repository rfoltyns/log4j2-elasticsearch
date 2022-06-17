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


import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscoveryFactory;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2.
 */
@Plugin(name = AHCHttpPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class AHCHttpPlugin extends AHCHttp {

    public static final String PLUGIN_NAME = "AHCHttp";

    public AHCHttpPlugin(final AHCHttp.Builder builder) {
        super(builder);
    }

    @PluginBuilderFactory
    public static AHCHttpPlugin.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<AHCHttpPlugin> {

        @PluginConfiguration
        protected Configuration configuration;

        @PluginBuilderAttribute
        protected String serverUris;

        @PluginBuilderAttribute
        protected int connTimeout = 1000;

        @PluginBuilderAttribute
        protected int readTimeout = 0;

        @PluginBuilderAttribute
        protected int maxTotalConnections = 8;

        @PluginBuilderAttribute
        protected int ioThreadCount = Runtime.getRuntime().availableProcessors();

        @PluginBuilderAttribute
        protected Boolean gzipCompression = Boolean.FALSE;

        @PluginElement("auth")
        protected Auth<HttpClientFactory.Builder> auth;

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        protected PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory;

        @PluginElement(BackoffPolicy.NAME)
        protected BackoffPolicy<BatchRequest> backoffPolicy;

        @PluginElement("serviceDiscovery")
        protected ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory;
        @PluginBuilderAttribute
        private String name = AHCHttp.class.getSimpleName();

        @PluginElement("metricsFactory")
        private final MetricsFactory metricsFactory = new DefaultMetricsFactory(AHCHttp.metricConfigs(false));

        protected ValueResolver valueResolver;

        @PluginElement("clientAPIFactory")
        private ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> clientAPIFactory;

        @Override
        public AHCHttpPlugin build() {

            final HttpClientProvider clientProvider = createClientProvider();

            final AHCHttp.Builder builder = (AHCHttp.Builder) new AHCHttp.Builder()
                    .withBatchOperations(createBatchOperations())
                    .withOperationFactory(createOperationFactory(clientProvider))
                    .withClientProvider(clientProvider)
                    .withBackoffPolicy(backoffPolicy == null ? new NoopBackoffPolicy<>() : backoffPolicy)
                    .withName(name);

            // Don't allow factory replacement yet. Maybe in future releases?
            for (final MetricConfig metricConfig : metricsFactory.getMetricConfigs()) {
                builder.withMetricConfig(metricConfig);
            }

            return new AHCHttpPlugin(builder.validate());

        }

        /* visible for testing */
        ValueResolver getValueResolver() {

            // allow programmatic override
            if (valueResolver != null) {
                return valueResolver;
            }

            // handle XML config
            if (configuration != null) {
                return new Log4j2Lookup(configuration.getStrSubstitutor());
            }

            // fallback to no-op
            return ValueResolver.NO_OP;
        }

        protected ElasticsearchOperationFactory createOperationFactory(final HttpClientProvider clientProvider) {

            final ObjectReader objectReader = ElasticsearchBulkAPI.defaultObjectMapper()
                    .readerFor(BatchResult.class);

            final ValueResolver valueResolver = getValueResolver();

            return new ElasticsearchOperationFactory(
                    new SyncStepProcessor(clientProvider, new JacksonDeserializer<>(objectReader)),
                    valueResolver);
        }

        protected HttpClientFactory.Builder createHttpClientFactoryBuilder() {
            return new HttpClientFactory.Builder()
                    .withServerList(SplitUtil.split(serverUris, ";"))
                    .withConnTimeout(connTimeout)
                    .withReadTimeout(readTimeout)
                    .withMaxTotalConnections(maxTotalConnections)
                    .withIoThreadCount(ioThreadCount)
                    .withAuth(auth)
                    .withGzipCompression(gzipCompression);
        }

        protected HttpClientProvider createClientProvider() {

            final HttpClientFactory.Builder mainClientFactoryBuilder = createHttpClientFactoryBuilder();
            final HttpClientProvider mainClientProvider = new HttpClientProvider(mainClientFactoryBuilder);

            if (this.serviceDiscoveryFactory != null) {
                mainClientFactoryBuilder.withServiceDiscovery(serviceDiscoveryFactory.create(mainClientProvider));
            }

            return mainClientProvider;
        }

        private AHCBatchOperations createBatchOperations() {

            if (pooledItemSourceFactory == null) {
                throw new IllegalArgumentException(String.format("No %s provided for %s", PooledItemSourceFactory.class.getSimpleName(), AHCHttp.class.getSimpleName()));
            }

            if (clientAPIFactory == null) {
                clientAPIFactory = new ElasticsearchBulkAPI(null);
            }

            return new AHCBatchOperations(pooledItemSourceFactory, clientAPIFactory);
        }

        public Builder withServerUris(final String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public Builder withMaxTotalConnections(final int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder withConnTimeout(final int connTimeout) {
            this.connTimeout = connTimeout;
            return this;
        }

        public Builder withReadTimeout(final int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withIoThreadCount(final int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        public Builder withItemSourceFactory(final PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }

        public Builder withBackoffPolicy(final BackoffPolicy<BatchRequest> backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder withAuth(final Auth<HttpClientFactory.Builder> auth) {
            this.auth = auth;
            return this;
        }

        public Builder withClientAPIFactory(final ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> clientAPIFactory) {
            this.clientAPIFactory = clientAPIFactory;
            return this;
        }

        public Builder withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withValueResolver(final ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
            return this;
        }

        public Builder withServiceDiscoveryFactory(final ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory) {
            this.serviceDiscoveryFactory = serviceDiscoveryFactory;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMetricConfig(final MetricConfig metricConfig) {
            metricsFactory.configure(metricConfig);
            return this;
        }

    }

}
