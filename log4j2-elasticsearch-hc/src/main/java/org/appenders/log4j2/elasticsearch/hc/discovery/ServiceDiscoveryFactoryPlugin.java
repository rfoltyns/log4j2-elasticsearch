package org.appenders.log4j2.elasticsearch.hc.discovery;

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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.hc.ClientProviderPoliciesRegistry;
import org.appenders.log4j2.elasticsearch.hc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.hc.HttpClient;
import org.appenders.log4j2.elasticsearch.hc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.hc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.util.LinkedHashSet;
import java.util.Set;

@Plugin(name = "ServiceDiscovery", category = Node.CATEGORY, elementType = "serviceDiscovery", printObject = true)
public class ServiceDiscoveryFactoryPlugin extends ServiceDiscoveryFactory<HttpClient> {

    public static final String PLUGIN_NAME = "ServiceDiscovery";

    public ServiceDiscoveryFactoryPlugin(
            ClientProviderPolicy<HttpClient> clientProviderPolicy,
            ServiceDiscoveryRequest<HttpClient> serviceDiscoveryRequest,
            long refreshInterval) {
        super(clientProviderPolicy, serviceDiscoveryRequest, refreshInterval);
    }

    @PluginBuilderFactory
    public static ServiceDiscoveryFactoryPlugin.Builder newBuilder() {
        return new ServiceDiscoveryFactoryPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ServiceDiscoveryFactoryPlugin> {

        public static final String DEFAULT_TARGET_SCHEME = "http";
        public static final long DEFAULT_REFRESH_INTERVAL = 30000;
        public static final int DEFAULT_RESPONSE_BUFFER_SIZE = 32768;
        public static final int DEFAULT_CONN_TIMEOUT = 500;
        public static final int DEFAULT_READ_TIMEOUT = 3000;

        @PluginBuilderAttribute
        protected String targetScheme = DEFAULT_TARGET_SCHEME;

        @PluginBuilderAttribute
        protected String configPolicies = "serverList,security";

        @PluginBuilderAttribute
        protected long refreshInterval = DEFAULT_REFRESH_INTERVAL;

        @PluginBuilderAttribute
        protected String serverUris;

        @PluginBuilderAttribute
        protected int connTimeout = DEFAULT_CONN_TIMEOUT;

        @PluginBuilderAttribute
        protected int readTimeout = DEFAULT_READ_TIMEOUT;

        @PluginBuilderAttribute
        protected int pooledResponseBuffersSizeInBytes = DEFAULT_RESPONSE_BUFFER_SIZE;

        @PluginElement("auth")
        protected Auth<HttpClientFactory.Builder> auth;

        @Override
        public ServiceDiscoveryFactoryPlugin build() {

            validate();

            Set<String> policyNames = new LinkedHashSet<>(SplitUtil.split(configPolicies, ","));

            return new ServiceDiscoveryFactoryPlugin(
                    createPoliciesRegistry().get(policyNames, createClientProvider()),
                    new ElasticsearchNodesQuery(targetScheme),
                    refreshInterval
            );

        }

        protected HttpClientProvider createClientProvider() {
            return new HttpClientProvider(createClientProviderBuilder());
        }

        protected HttpClientFactory.Builder createClientProviderBuilder() {
            return new HttpClientFactory.Builder()
                    .withServerList(SplitUtil.split(serverUris))
                    .withConnTimeout(connTimeout)
                    .withReadTimeout(readTimeout)
                    .withMaxTotalConnections(1)
                    .withIoThreadCount(1)
                    .withPooledResponseBuffers(true)
                    .withPooledResponseBuffersSizeInBytes(pooledResponseBuffersSizeInBytes)
                    .withAuth(auth);
        }

        protected ClientProviderPoliciesRegistry createPoliciesRegistry() {
            return new ClientProviderPoliciesRegistry();
        }

        protected Builder validate() {

            if (targetScheme == null) {
                throw new ConfigurationException("No targetScheme provided for " + PLUGIN_NAME);
            }

            if (refreshInterval <= 0) {
                throw new ConfigurationException("refreshInterval must be higher than 0 for " + PLUGIN_NAME);
            }

            return this;

        }

        public Builder withTargetScheme(String targetScheme) {
            this.targetScheme = targetScheme;
            return this;
        }

        public Builder withRefreshInterval(long refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public Builder withConfigPolicies(String configPolicies) {
            this.configPolicies = configPolicies;
            return this;
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
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

        public Builder withPooledResponseBuffersSizeInBytes(int pooledResponseBuffersSizeInBytes) {
            this.pooledResponseBuffersSizeInBytes = pooledResponseBuffersSizeInBytes;
            return this;
        }

        public Builder withAuth(Auth<HttpClientFactory.Builder> auth) {
            this.auth = auth;
            return this;
        }

    }

}
