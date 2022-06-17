package org.appenders.log4j2.elasticsearch.ahc.discovery;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ahc.ClientProviderPoliciesRegistry;
import org.appenders.log4j2.elasticsearch.ahc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Plugin(name = "ServiceDiscovery", category = Node.CATEGORY, elementType = "serviceDiscovery", printObject = true)
public class ServiceDiscoveryFactoryPlugin extends ServiceDiscoveryFactory<HttpClient> {

    public static final String PLUGIN_NAME = "ServiceDiscovery";

    public ServiceDiscoveryFactoryPlugin(
            final ClientProviderPolicy<HttpClient> clientProviderPolicy,
            final ServiceDiscoveryRequest<HttpClient> serviceDiscoveryRequest,
            final long refreshInterval) {
        super(clientProviderPolicy, serviceDiscoveryRequest, refreshInterval);
    }

    @PluginBuilderFactory
    public static ServiceDiscoveryFactoryPlugin.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ServiceDiscoveryFactoryPlugin> {

        public static final String DEFAULT_TARGET_SCHEME = "http";
        public static final String DEFAULT_NODES_FILTER = "_all";
        public static final long DEFAULT_REFRESH_INTERVAL = 30000;
        public static final int DEFAULT_CONN_TIMEOUT = 500;
        public static final int DEFAULT_READ_TIMEOUT = 3000;

        @PluginBuilderAttribute
        protected String nodesFilter = DEFAULT_NODES_FILTER;

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

        @PluginElement("auth")
        protected Auth<HttpClientFactory.Builder> auth;

        @PluginBuilderAttribute
        private String name = ServiceDiscovery.class.getSimpleName();

        @SuppressWarnings("FieldMayBeFinal")
        @PluginElement("metricsFactory")
        private MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());

        @Override
        public ServiceDiscoveryFactoryPlugin build() {

            validate();

            final Set<String> policyNames = new LinkedHashSet<>(SplitUtil.split(configPolicies, ","));

            return new ServiceDiscoveryFactoryPlugin(
                    createPoliciesRegistry().get(policyNames, createClientProvider()),
                    new ElasticsearchNodesQuery(targetScheme, nodesFilter),
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
                    .withAuth(auth)
                    .withName(name)
                    .withMetricConfigs(metricsFactory.getMetricConfigs());
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

        public Builder withNodesFilter(final String nodesFilter) {
            this.nodesFilter = nodesFilter;
            return this;
        }

        public Builder withTargetScheme(final String targetScheme) {
            this.targetScheme = targetScheme;
            return this;
        }

        public Builder withRefreshInterval(final long refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public Builder withConfigPolicies(final String configPolicies) {
            this.configPolicies = configPolicies;
            return this;
        }

        public Builder withServerUris(final String serverUris) {
            this.serverUris = serverUris;
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

        public Builder withAuth(final Auth<HttpClientFactory.Builder> auth) {
            this.auth = auth;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withMetricConfigs(final List<MetricConfig> metricConfigs) {
            this.metricsFactory.configure(metricConfigs);
            return this;
        }

    }

}
