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
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.ahc.Security;
import org.appenders.log4j2.elasticsearch.ahc.SecurityTest;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscoveryFactoryPlugin.PLUGIN_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ServiceDiscoveryFactoryPluginTest {

    public static final int TEST_REFRESH_INTERVAL = 3500;
    public static final String TEST_NODES_FILTER = "test:nodes_filter";
    public static final String TEST_TARGET_SCHEME = "https";
    public static final String TEST_SERVER_URIS = "http://localhost:9200;http://localhost:9201";
    public static final int TEST_CONN_TIMEOUT = 100;
    public static final int TEST_READ_TIMEOUT = 200;

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder();

        // when
        final ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        assertNotNull(plugin);

    }

    @Test
    public void setsPropertiesIfConfigured() {

        // given
        final Security auth = SecurityTest.createDefaultTestSecurityBuilder().build();

        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withNodesFilter(TEST_NODES_FILTER)
                .withTargetScheme(TEST_TARGET_SCHEME)
                .withRefreshInterval(TEST_REFRESH_INTERVAL)
                .withServerUris(TEST_SERVER_URIS)
                .withAuth(auth)
                .withConnTimeout(TEST_CONN_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT);

        final HttpClientFactory.Builder httpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(SplitUtil.split(TEST_SERVER_URIS))
                .withConnTimeout(TEST_CONN_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withAuth(auth);

        // when
        final ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        final HttpClientFactory.Builder clientFactoryBuilder = resolveClientProvider(plugin).getHttpClientFactoryBuilder();

        assertThat(httpClientFactoryBuilder, new HttpClientFactoryTest.BuilderMatcher(clientFactoryBuilder));
        assertEquals(TEST_TARGET_SCHEME, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).resultScheme);
        assertEquals(TEST_NODES_FILTER, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).nodesFilter);
        assertEquals(TEST_REFRESH_INTERVAL, plugin.refreshInterval);

    }

    @Test
    public void setsDefaultsIfNotConfigured() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder();

        final HttpClientFactory.Builder httpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withConnTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_CONN_TIMEOUT)
                .withReadTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_READ_TIMEOUT)
                .withIoThreadCount(1)
                .withMaxTotalConnections(1)
                .withServerList(Collections.emptyList())
                .withAuth(null)
                .withServiceDiscovery(null);

        // when
        final ServiceDiscoveryFactoryPlugin plugin = builder.build();

        // then
        final HttpClientFactory.Builder clientFactoryBuilder = resolveClientProvider(plugin).getHttpClientFactoryBuilder();

        assertThat(httpClientFactoryBuilder, new HttpClientFactoryTest.BuilderMatcher(clientFactoryBuilder));
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_TARGET_SCHEME, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).resultScheme);
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_NODES_FILTER, ((ElasticsearchNodesQuery)plugin.serviceDiscoveryRequest).nodesFilter);
        assertEquals(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_REFRESH_INTERVAL, plugin.refreshInterval);

    }

    @Test
    public void setsPartialConfigPoliciesByDefault() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withServerUris(null)
                .withAuth(null);

        final Auth<HttpClientFactory.Builder> auth = SecurityTest.createDefaultTestSecurityBuilder().build();
        final HttpClientFactory.Builder parentHttpClientFactoryBuilder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(SplitUtil.split(TEST_SERVER_URIS))
                .withAuth(auth)
                .withServiceDiscovery(null)
                .withConnTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_CONN_TIMEOUT)
                .withReadTimeout(ServiceDiscoveryFactoryPlugin.Builder.DEFAULT_READ_TIMEOUT);

        final ServiceDiscoveryFactoryPlugin plugin = builder.build();

        final HttpClientProvider source = new HttpClientProvider(parentHttpClientFactoryBuilder);

        // when
        final HttpClientProvider target = resolveClientProvider(plugin, source);

        // then
        assertThat(source.getHttpClientFactoryBuilder(), new HttpClientFactoryTest.BuilderMatcher(target.getHttpClientFactoryBuilder()));

    }

    @Test
    public void setsGivenConfigPolicyIfConfigured() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withServerUris(null)
                .withAuth(null)
                .withConfigPolicies("shared");

        final ServiceDiscoveryFactoryPlugin plugin = builder.build();

        final HttpClientProvider source = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder());

        // when
        final HttpClientProvider target = resolveClientProvider(plugin, source);

        // then
        assertSame(source, target);

    }

    @Test
    public void builderThrowsIfTargetSchemeIsNull() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withTargetScheme(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No targetScheme provided for " + PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfRefreshIntervalIsZero() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withRefreshInterval(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("refreshInterval must be higher than 0 for " + PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfRefreshIntervalIsLowerThanZero() {

        // given
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withRefreshInterval(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("refreshInterval must be higher than 0 for " + PLUGIN_NAME));

    }

    // =======
    // METRICS
    // =======

    @Test
    public void registersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final MetricConfig expectedConfig = MetricConfigFactory.createCountConfig(true, "connectionsActive");
        final ServiceDiscoveryFactoryPlugin.Builder builder = createDefaultTestServiceDiscoveryConfigPluginBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(expectedConfig));

        final HttpClientFactory.Builder clientProviderBuilder = builder.createClientProviderBuilder();
        final HttpClientFactory httpClientFactory = clientProviderBuilder.build();
        final HttpClient httpClient = httpClientFactory.createInstance();

        final MetricsRegistry registry = new BasicMetricsRegistry();

        // when
        httpClient.register(registry);

        // then
        assertEquals(1, registry.getMetrics(metric -> !metric.getKey().toString().contains("noop") && metric.getKey().toString().contains("connectionsActive")).size());

    }

    @Test
    public void deregistersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final MetricConfig expectedConfig = MetricConfigFactory.createCountConfig(true, "connectionsActive");
        final ServiceDiscoveryFactoryPlugin.Builder builder = spy(new ServiceDiscoveryFactoryPlugin.Builder()
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(expectedConfig)));

        final AtomicReference<HttpClientFactory.Builder> captor = new AtomicReference<>();
        when(builder.createClientProviderBuilder()).thenAnswer((Answer<HttpClientFactory.Builder>) invocationOnMock -> {
            final HttpClientFactory.Builder httpClientFactoryBuilder = (HttpClientFactory.Builder) invocationOnMock.callRealMethod();
            captor.set(httpClientFactoryBuilder);
            return httpClientFactoryBuilder;
        });

        final MetricsRegistry registry = new BasicMetricsRegistry();

        // when
        final ServiceDiscoveryFactoryPlugin plugin = builder.build();
        final HttpClient httpClient = captor.get().build().createInstance();
        Measured.of(httpClient).register(registry);
        Measured.of(httpClient).deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().toString().contains("noop") && metric.getKey().toString().contains("connectionsActive")).size());

    }

    private HttpClientProvider resolveClientProvider(ServiceDiscoveryFactoryPlugin plugin) {
        ClientProvider<HttpClient> clientProvider = plugin.clientProviderPolicy.apply(
                new HttpClientProvider(new HttpClientFactory.Builder())); // crucial to use default, plain builder here
        return (HttpClientProvider) clientProvider;
    }

    private HttpClientProvider resolveClientProvider(final ServiceDiscoveryFactoryPlugin plugin, final HttpClientProvider source) {
        final ClientProvider<HttpClient> clientProvider = plugin.clientProviderPolicy.apply(source);
        return (HttpClientProvider) clientProvider;
    }

    private static ServiceDiscoveryFactoryPlugin.Builder createDefaultTestServiceDiscoveryConfigPluginBuilder() {
        return ServiceDiscoveryFactoryPlugin.newBuilder();
    }

}
