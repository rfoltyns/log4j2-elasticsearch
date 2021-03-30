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
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.hc.HttpClient;
import org.appenders.log4j2.elasticsearch.hc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest;
import org.appenders.log4j2.elasticsearch.hc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.hc.Security;
import org.appenders.log4j2.elasticsearch.hc.SecurityTest;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscoveryFactoryPlugin.PLUGIN_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceDiscoveryFactoryPluginTest {

    public static final int TEST_REFRESH_INTERVAL = 3500;
    public static final String TEST_NODES_FILTER = "test:nodes_filter";
    public static final String TEST_TARGET_SCHEME = "https";
    public static final String TEST_SERVER_URIS = "http://localhost:9200;http://localhost:9201";
    public static final int TEST_CONN_TIMEOUT = 100;
    public static final int TEST_READ_TIMEOUT = 200;
    public static final int TEST_BUFFER_SIZE_IN_BYTES = 512;

    @Test
    public void builderBuildsSuccessfully() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder();

        // when
        ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        assertNotNull(plugin);

    }

    @Test
    public void setsPropertiesIfConfigured() {

        // given
        Security auth = SecurityTest.createDefaultTestSecurityBuilder().build();

        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withNodesFilter(TEST_NODES_FILTER)
                .withTargetScheme(TEST_TARGET_SCHEME)
                .withRefreshInterval(TEST_REFRESH_INTERVAL)
                .withServerUris(TEST_SERVER_URIS)
                .withAuth(auth)
                .withConnTimeout(TEST_CONN_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withPooledResponseBuffersSizeInBytes(TEST_BUFFER_SIZE_IN_BYTES);

        HttpClientFactory.Builder httpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(SplitUtil.split(TEST_SERVER_URIS))
                .withConnTimeout(TEST_CONN_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withAuth(auth)
                .withPooledResponseBuffers(true)
                .withPooledResponseBuffersSizeInBytes(TEST_BUFFER_SIZE_IN_BYTES);

        // when
        ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        HttpClientFactory.Builder clientFactoryBuilder = resolveClientProvider(plugin).getHttpClientFactoryBuilder();

        assertThat(httpClientFactoryBuilder, new HttpClientFactoryTest.BuilderMatcher(clientFactoryBuilder));
        assertEquals(TEST_TARGET_SCHEME, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).resultScheme);
        assertEquals(TEST_NODES_FILTER, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).nodesFilter);
        assertEquals(TEST_REFRESH_INTERVAL, plugin.refreshInterval);

    }

    @Test
    public void setsDefaultsIfNotConfigured() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder();

        HttpClientFactory.Builder httpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withConnTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_CONN_TIMEOUT)
                .withReadTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_READ_TIMEOUT)
                .withPooledResponseBuffersSizeInBytes(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_RESPONSE_BUFFER_SIZE)
                .withPooledResponseBuffers(true)
                .withIoThreadCount(1)
                .withMaxTotalConnections(1)
                .withServerList(Collections.emptyList())
                .withAuth(null)
                .withServiceDiscovery(null);

        // when
        ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        HttpClientFactory.Builder clientFactoryBuilder = resolveClientProvider(plugin).getHttpClientFactoryBuilder();

        assertThat(httpClientFactoryBuilder, new HttpClientFactoryTest.BuilderMatcher(clientFactoryBuilder));
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_TARGET_SCHEME, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).resultScheme);
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_NODES_FILTER, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).nodesFilter);
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_REFRESH_INTERVAL, plugin.refreshInterval);

    }

    @Test
    public void setsPartialConfigPoliciesByDefault() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withServerUris(null)
                .withAuth(null);

        Auth<HttpClientFactory.Builder> auth = SecurityTest.createDefaultTestSecurityBuilder().build();
        HttpClientFactory.Builder parentHttpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(SplitUtil.split(TEST_SERVER_URIS))
                .withAuth(auth)
                .withServiceDiscovery(null)
                .withPooledResponseBuffers(true)
                .withPooledResponseBuffersSizeInBytes(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_RESPONSE_BUFFER_SIZE)
                .withConnTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_CONN_TIMEOUT)
                .withReadTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_READ_TIMEOUT);

        ServiceDiscoveryFactoryPlugin plugin = builder.build();

        HttpClientProvider source = new HttpClientProvider(parentHttpClientFactoryBuilder);

        // when
        HttpClientProvider target = resolveClientProvider(plugin, source);

        // then
        assertThat(source.getHttpClientFactoryBuilder(), new HttpClientFactoryTest.BuilderMatcher(target.getHttpClientFactoryBuilder()));

    }

    @Test
    public void setsGivenConfigPolicyIfConfigured() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withServerUris(null)
                .withAuth(null)
                .withConfigPolicies("shared");

        ServiceDiscoveryFactoryPlugin plugin = builder.build();

        HttpClientProvider source = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder());

        // when
        HttpClientProvider target = resolveClientProvider(plugin, source);

        // then
        assertSame(source, target);

    }

    @Test
    public void builderThrowsIfTargetSchemeIsNull() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withTargetScheme(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No targetScheme provided for " + PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfRefreshIntervalIsZero() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withRefreshInterval(0);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("refreshInterval must be higher than 0 for " + PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfRefreshIntervalIsLowerThanZero() {

        // given
        ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withRefreshInterval(-1);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("refreshInterval must be higher than 0 for " + PLUGIN_NAME));

    }

    private HttpClientProvider resolveClientProvider(ServiceDiscoveryFactoryPlugin plugin) {
        ClientProvider<HttpClient> clientProvider = plugin.clientProviderPolicy.apply(
                new HttpClientProvider(new HttpClientFactory.Builder())); // crucial to use default, plain builder here
        return (HttpClientProvider) clientProvider;
    }

    private HttpClientProvider resolveClientProvider(ServiceDiscoveryFactoryPlugin plugin, HttpClientProvider source) {
        ClientProvider<HttpClient> clientProvider = plugin.clientProviderPolicy.apply(source);
        return (HttpClientProvider) clientProvider;
    }

    private static ServiceDiscoveryFactoryPlugin.Builder createDefaultTestServiceDiscoveryConfigPluginBuilder() {
        return ServiceDiscoveryFactoryPlugin.newBuilder();
    }

}
