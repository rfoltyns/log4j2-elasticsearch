package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscovery;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscoveryFactory;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.TestKeyAccessor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.createTestIndexTemplateBuilder;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_CONNECTION_TIMEOUT;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_IO_THREAD_COUNT;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_MAX_TOTAL_CONNECTIONS;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_POOLED_RESPONSE_BUFFERS_ENABLED;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_READ_TIMEOUT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HCHttpPluginTest {

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final String TEST_MAPPING_TYPE = "test-mapping-type";

    public static HCHttpPlugin.Builder createDefaultHttpObjectFactoryBuilder() {

        PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        return HCHttpPlugin.newBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .withServerUris(TEST_SERVER_URIS);
    }

    @Test
    public void builderSetsAllFields() {

        // given
        Auth<HttpClientFactory.Builder> auth = mock(Auth.class);
        BackoffPolicy<BatchRequest> backoffPolicy = new NoopBackoffPolicy<>();

        HCHttpPlugin.Builder builder = new HCHttpPlugin.Builder()
                .withItemSourceFactory(PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build())
                .withServerUris(TEST_SERVER_URIS)
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withPooledResponseBuffers(TEST_POOLED_RESPONSE_BUFFERS_ENABLED)
                .withPooledResponseBuffersSizeInBytes(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES)
                .withMappingType(TEST_MAPPING_TYPE)
                .withAuth(auth)
                .withBackoffPolicy(backoffPolicy);

        // when
        HCHttpPlugin plugin = builder.build();

        // then
        assertEquals(TEST_MAPPING_TYPE, ((HCBatchOperations)plugin.batchOperations).getMappingType());
        assertEquals(backoffPolicy, plugin.backoffPolicy);

        HttpClientFactory.Builder httpClientFactoryBuilder = plugin.clientProvider.getHttpClientFactoryBuilder();

        assertTrue(httpClientFactoryBuilder.serverList.contains(TEST_SERVER_URIS));
        assertEquals(TEST_CONNECTION_TIMEOUT, httpClientFactoryBuilder.connTimeout);
        assertEquals(TEST_READ_TIMEOUT, httpClientFactoryBuilder.readTimeout);
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS, httpClientFactoryBuilder.maxTotalConnections);
        assertEquals(TEST_IO_THREAD_COUNT, httpClientFactoryBuilder.ioThreadCount);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_ENABLED, httpClientFactoryBuilder.pooledResponseBuffersEnabled);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES, httpClientFactoryBuilder.pooledResponseBuffersSizeInBytes);
        assertEquals(auth, httpClientFactoryBuilder.auth);

    }

    @Test
    public void builderThrowsIfItemSourceFactoryIsNull() {

        // given
        final HCHttpPlugin.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withItemSourceFactory(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void log4j2ConfigurationBasedValueResolverIsUsedWhenConfigurationProvided()
    {

        // given
        Configuration configuration = mock(Configuration.class);
        StrSubstitutor strSubstitutor = mock(StrSubstitutor.class);
        when(strSubstitutor.replace((String)any())).thenReturn(UUID.randomUUID().toString());

        when(configuration.getStrSubstitutor()).thenReturn(strSubstitutor);

        HCHttpPlugin.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withValueResolver(null)
                .withConfiguration(configuration);

        HCHttp factory = builder.build();

        String expectedSource = UUID.randomUUID().toString();
        IndexTemplate indexTemplate = createTestIndexTemplateBuilder()
                .withName(UUID.randomUUID().toString())
                .withSource(expectedSource)
                .withPath(null)
                .build();

        // when
        factory.setupOperationFactory().create(indexTemplate);

        // then
        verify(strSubstitutor).replace(eq(expectedSource));

    }

    @Test
    public void providedValueResolverIsUsedWhenBothConfigurationAndValueResolverProvided()
    {

        // given
        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(anyString())).thenReturn(UUID.randomUUID().toString());

        HCHttpPlugin.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withConfiguration(mock(Configuration.class))
                .withValueResolver(valueResolver);

        HCHttp factory = builder.build();


        String expectedSource = UUID.randomUUID().toString();
        IndexTemplate indexTemplate = createTestIndexTemplateBuilder()
                .withName(UUID.randomUUID().toString())
                .withSource(expectedSource)
                .withPath(null)
                .build();

        // when
        factory.setupOperationFactory().create(indexTemplate);

        // then
        verify(valueResolver).resolve(eq(expectedSource));

    }

    @Test
    public void noopValueResolverIsUsedWhenBothConfigurationAndValueResolverAreNotProvided()
    {

        // given
        HCHttpPlugin.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withConfiguration(null)
                .withValueResolver(null);

        // when
        ValueResolver valueResolver = builder.getValueResolver();

        // then
        assertEquals(ValueResolver.NO_OP, valueResolver);

    }

    @Test
    public void serviceDiscoveryIsUsedIfConfigured() {

        // given
        ServiceDiscovery serviceDiscovery = mock(ServiceDiscovery.class);

        ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory = mock(ServiceDiscoveryFactory.class);
        when(serviceDiscoveryFactory.create(any())).thenReturn(serviceDiscovery);

        HCHttpPlugin.Builder builder = spy(createDefaultHttpObjectFactoryBuilder())
                .withServiceDiscoveryFactory(serviceDiscoveryFactory);

        // when
        HCHttpPlugin plugin = builder.build();

        // then
        verify(serviceDiscoveryFactory).create(any());
        assertEquals(serviceDiscovery, plugin.clientProvider.getHttpClientFactoryBuilder().serviceDiscovery);

    }

    @Test
    public void clientAPIFactoryIsUsedIfConfigured() {

        // given
        final ClientAPIFactory clientAPIFactory = mock(ClientAPIFactory.class);
        when(clientAPIFactory.batchBuilder()).thenReturn(new BatchRequest.Builder());

        final ItemSourceFactory itemSourceFactory = mock(ItemSourceFactory.class);
        when(itemSourceFactory.createEmptySource()).thenReturn(ByteBufItemSourceTest.createTestItemSource());
        final HCHttpPlugin.Builder builder = spy(createDefaultHttpObjectFactoryBuilder())
                .withClientAPIFactory(clientAPIFactory);

        verify(clientAPIFactory, never()).batchBuilder();

        // when
        final HCHttpPlugin plugin = builder.build();
        plugin.start();
        plugin.batchOperations.createBatchBuilder();

        // then
        verify(clientAPIFactory).batchBuilder();

    }

    @Test
    public void registersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key notExpectedKey = new Metric.Key(expectedComponentName, "serverTookMs", "noop");
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "serverTookMs", "max");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "noop");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "noop");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "noop");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "noop");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");
        final Metric.Key expectedKey7 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HCHttpPlugin.Builder builder = spy(createDefaultHttpObjectFactoryBuilder())
                .withName(expectedComponentName)
                .withMetricConfig(MetricConfigFactory.createMaxConfig("serverTookMs", false));

        final HCHttpPlugin plugin = builder.build();

        // when
        plugin.register(registry);

        // then
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(notExpectedKey) && TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1) && TestKeyAccessor.getMetricType(metric.getKey()).equals("max")).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey4)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey5)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey6)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey7)).size());

    }

    @Test
    public void deregistersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HCHttpPlugin.Builder builder = spy(createDefaultHttpObjectFactoryBuilder())
                .withName(expectedComponentName)
                .withMetricConfigs(BatchingClientObjectFactory.BatchingClientMetrics.createConfigs(true));

        final HCHttpPlugin plugin = builder.build();

        plugin.register(registry);
        assertEquals(7, registry.getMetrics(metric -> !TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());
        assertEquals(12, registry.getMetrics(metric -> true).size()); // pool noop metrics

        // when
        plugin.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> true).size());

    }

}
