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

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.hc.discovery.HCServiceDiscovery;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.TestKeyAccessor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class HttpClientProviderTest {

    private static final Random RANDOM = new Random();

    public static final int TEST_CONNECTION_TIMEOUT = RANDOM.nextInt(1000) + 10;
    public static final int TEST_READ_TIMEOUT = RANDOM.nextInt(1000) + 10;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    public static final int TEST_MAX_TOTAL_CONNECTIONS = RANDOM.nextInt(1000) + 10;
    public static final int TEST_IO_THREAD_COUNT = RANDOM.nextInt(1000) + 10;
    public static final boolean TEST_POOLED_RESPONSE_BUFFERS_ENABLED = true;
    public static final int TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES = 34;
    public static final Security TEST_AUTH = SecurityTest.createDefaultTestSecurityBuilder().build();

    public static HttpClientFactory.Builder createDefaultTestBuilder() {
        return createDefaultTestHttpClientFactoryBuilder()
                .withServerList(Collections.singletonList(TEST_SERVER_URIS))
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withPooledResponseBuffers(TEST_POOLED_RESPONSE_BUFFERS_ENABLED)
                .withPooledResponseBuffersSizeInBytes(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES);
    }

    public static HttpClientProvider createDefaultTestClientProvider() {
        return new HttpClientProvider(createDefaultTestBuilder());
    }

    @Test
    public void paramsAreSetCorrectly() {

        // given
        HttpClientFactory.Builder httpClientFactoryBuilder = createDefaultTestBuilder()
                .withAuth(TEST_AUTH);

        // when
        HttpClientProvider clientProvider = new HttpClientProvider(httpClientFactoryBuilder);

        // then
        assertSame(httpClientFactoryBuilder, clientProvider.getHttpClientFactoryBuilder());
        assertEquals(httpClientFactoryBuilder.serverList, Collections.singletonList(TEST_SERVER_URIS));
        assertSame(httpClientFactoryBuilder.auth, TEST_AUTH);
        assertEquals(httpClientFactoryBuilder.connTimeout, TEST_CONNECTION_TIMEOUT);
        assertEquals(httpClientFactoryBuilder.readTimeout, TEST_READ_TIMEOUT);
        assertEquals(httpClientFactoryBuilder.ioThreadCount, TEST_IO_THREAD_COUNT);
        assertEquals(httpClientFactoryBuilder.maxTotalConnections, TEST_MAX_TOTAL_CONNECTIONS);
        assertEquals(httpClientFactoryBuilder.pooledResponseBuffersEnabled, TEST_POOLED_RESPONSE_BUFFERS_ENABLED);
        assertEquals(httpClientFactoryBuilder.pooledResponseBuffersSizeInBytes, TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES);

        assertThat(clientProvider.toString(), containsString(Integer.toString(TEST_MAX_TOTAL_CONNECTIONS)));

    }

    @Test
    public void createClientReturnsSameInstance() {

        // given
        HttpClientProvider clientProvider = createDefaultTestClientProvider();

        // when
        HttpClient httpClient = clientProvider.createClient();

        // then
        assertSame(httpClient, clientProvider.createClient());

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    @Test
    public void lifecycleStartStartsComponentsOnlyOnce() {

        // given
        HttpClient client = mock(HttpClient.class);
        when(client.isStarted()).thenAnswer(falseOnlyOnce());

        HCServiceDiscovery<HttpClient> serviceDiscovery = mock(HCServiceDiscovery.class);
        when(serviceDiscovery.isStarted()).thenAnswer(falseOnlyOnce());

        HttpClientFactory.Builder clientFactoryBuilder = createDefaultTestBuilder()
                .withServiceDiscovery(serviceDiscovery);

        LifeCycle lifeCycle = new HttpClientProvider(clientFactoryBuilder) {
            @Override
            public HttpClient createClient() {
                return client;
            }
        };

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

        verify(serviceDiscovery).start();
        verify(client).start();

    }

    @Test
    public void lifecycleStopStopsComponentsOnlyOnce() {

        // given
        HttpClient httpClient = mock(HttpClient.class);

        HttpClientFactory.Builder clientFactoryBuilder = new HttpClientFactory.Builder() {
            @Override
            public HttpClientFactory build() {
                super.build();
                return new HttpClientFactory(this) {
                    @Override
                    protected HttpClient createConfiguredClient(
                            CloseableHttpAsyncClient asyncHttpClient,
                            ServerPool serverPool,
                            RequestFactory requestFactory,
                            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory) {
                        return httpClient;
                    }
                };
            }
        };

        HCServiceDiscovery<HttpClient> serviceDiscovery = mock(HCServiceDiscovery.class);
        clientFactoryBuilder.withServiceDiscovery(serviceDiscovery);

        LifeCycle lifeCycle = new HttpClientProvider(clientFactoryBuilder);

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        assertTrue(lifeCycle.isStopped());
        assertFalse(lifeCycle.isStarted());

        verify(serviceDiscovery).stop();
        verify(httpClient).stop();

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestClientProvider();
    }

    // =======
    // METRICS
    // =======

    @Test
    public void registersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "responseBytes", "count");

        final MetricsRegistry registry = mock(MetricsRegistry.class);
        final HttpClientFactory.Builder builder = createDefaultTestBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(PoolingAsyncResponseConsumer.metricConfigs(true));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        // when
        provider.createClient();
        provider.register(registry);

        // then
        final ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
        verify(registry, times(6)).register(captor.capture());

        assertEquals(expectedKey1, captor.getValue().getKey());

    }

    @Test
    public void deregistersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory.Builder builder = createDefaultTestBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(PoolingAsyncResponseConsumer.metricConfigs(true));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        provider.createClient();
        provider.register(registry);

        assertEquals(1, registry.getMetrics(metric -> !TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());

        // when
        provider.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> true).size());

    }

    @Test
    public void doesNotRegisterMetricsUntilCreateInstance() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "responseBytes", "count");

        final MetricsRegistry registry = mock(MetricsRegistry.class);
        final HttpClientFactory.Builder builder = createDefaultTestBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(PoolingAsyncResponseConsumer.metricConfigs(true));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        // when
        provider.register(registry);
        verifyNoInteractions(registry);

        provider.createClient();
        provider.register(registry);

        // then
        final ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
        verify(registry, times(6)).register(captor.capture());

        assertEquals(expectedKey1, captor.getValue().getKey());

    }

}
