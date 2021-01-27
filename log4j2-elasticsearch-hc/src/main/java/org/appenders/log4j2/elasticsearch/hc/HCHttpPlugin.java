package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
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
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscoveryFactory;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import static org.appenders.log4j2.elasticsearch.hc.HCHttp.Builder.DEFAULT_BACKOFF_POLICY;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2.
 */
@Plugin(name = HCHttpPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class HCHttpPlugin extends HCHttp {

    public static final String PLUGIN_NAME = "HCHttp";

    public HCHttpPlugin(HCHttp.Builder builder) {
        super(builder);
    }

    @PluginBuilderFactory
    public static HCHttpPlugin.Builder newBuilder() {
        return new HCHttpPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<HCHttpPlugin> {

        public static final int DEFAULT_RESPONSE_BUFFER_SIZE = 1024 * 1024;

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
        protected boolean pooledResponseBuffers = true;

        @PluginBuilderAttribute
        protected int pooledResponseBuffersSizeInBytes = DEFAULT_RESPONSE_BUFFER_SIZE;

        @PluginElement("auth")
        protected Auth<HttpClientFactory.Builder> auth;

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        protected PooledItemSourceFactory pooledItemSourceFactory;

        @PluginBuilderAttribute
        protected String mappingType = "_doc";

        @PluginElement(BackoffPolicy.NAME)
        protected BackoffPolicy<BatchRequest> backoffPolicy = DEFAULT_BACKOFF_POLICY;

        @PluginElement("serviceDiscovery")
        protected ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory;

        protected ValueResolver valueResolver;

        @Override
        public HCHttpPlugin build() {

            return new HCHttpPlugin(new HCHttp.Builder()
                    .withClientProvider(createClientProvider())
                    .withMappingType(mappingType)
                    .withItemSourceFactory(pooledItemSourceFactory)
                    .withBackoffPolicy(backoffPolicy)
                    .withValueResolver(getValueResolver())
                    .validate());

        }

        private ValueResolver getValueResolver() {

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

        protected HttpClientFactory.Builder createHttpClientFactoryBuilder() {
            return new HttpClientFactory.Builder()
                    .withServerList(SplitUtil.split(serverUris, ";"))
                    .withConnTimeout(connTimeout)
                    .withReadTimeout(readTimeout)
                    .withMaxTotalConnections(maxTotalConnections)
                    .withIoThreadCount(ioThreadCount)
                    .withPooledResponseBuffers(pooledResponseBuffers)
                    .withPooledResponseBuffersSizeInBytes(pooledResponseBuffersSizeInBytes)
                    .withAuth(auth);
        }

        protected HttpClientProvider createClientProvider() {

            HttpClientFactory.Builder mainClientFactoryBuilder = createHttpClientFactoryBuilder();
            HttpClientProvider mainClientProvider = new HttpClientProvider(mainClientFactoryBuilder);

            if (this.serviceDiscoveryFactory != null) {
                mainClientFactoryBuilder.withServiceDiscovery(serviceDiscoveryFactory.create(mainClientProvider));
            }

            return mainClientProvider;
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public Builder withMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder withConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
            return this;
        }

        public Builder withReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withIoThreadCount(int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        public Builder withItemSourceFactory(PooledItemSourceFactory pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }

        public Builder withBackoffPolicy(BackoffPolicy<BatchRequest> backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder withAuth(Auth<HttpClientFactory.Builder> auth) {
            this.auth = auth;
            return this;
        }

        public Builder withMappingType(String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        public Builder withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
            this.pooledResponseBuffers = pooledResponseBuffersEnabled;
            return this;
        }

        public Builder withPooledResponseBuffersSizeInBytes(int estimatedResponseSizeInBytes) {
            this.pooledResponseBuffersSizeInBytes = estimatedResponseSizeInBytes;
            return this;
        }

        public Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
            return this;
        }

        public Builder withServiceDiscoveryFactory(ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory) {
            this.serviceDiscoveryFactory = serviceDiscoveryFactory;
            return this;
        }

    }

}
