package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.reactor.IOReactorException;
import org.appenders.log4j2.elasticsearch.hc.discovery.HCServiceDiscovery;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscovery;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.createDefaultTestClientProvider;
import static org.appenders.log4j2.elasticsearch.hc.discovery.HCServiceDiscoveryTest.createNonSchedulingServiceDiscovery;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpClientFactoryTest {

    private static final Random RANDOM = new Random();

    private static final Collection<String> TEST_SERVER_LIST = Collections.singletonList("http://localhost:9200");

    private static final int TEST_CONNECTION_TIMEOUT = RANDOM.nextInt(1000) + 10;
    private static final int TEST_READ_TIMEOUT = RANDOM.nextInt(1000) + 10;

    private static final int TEST_MAX_TOTAL_CONNECTIONS = RANDOM.nextInt(1000) + 10;
    private static final int TEST_IO_THREAD_COUNT = RANDOM.nextInt(1000) + 10;
    private static final boolean TEST_POOLED_RESPONSE_BUFFERS_ENABLED = true;
    private static final int TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES = 34;

    @Test
    public void toStringDoesNotPrintSensitiveInfo() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        String randomString = UUID.randomUUID().toString();

        Security security = SecurityTest.createDefaultTestSecurityBuilder()
                .withCredentials(new BasicCredentials(randomString, randomString))
                .withCertInfo(new PEMCertInfo(randomString, randomString, randomString, randomString))
                .build();

        builder.withServerList(TEST_SERVER_LIST)
                .withAuth(security);

        // when
        String toString = builder.toString();

        // then
        assertThat(toString, not(containsString(randomString)));
        assertThat(toString, containsString("auth=true"));

    }

    @Test
    public void toStringDoesNotPrintFullServiceDiscoveryInfo() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        ServiceDiscovery serviceDiscovery = createNonSchedulingServiceDiscovery(createDefaultTestClientProvider(), (client, callback) -> {});

        builder.withServerList(TEST_SERVER_LIST)
                .withServiceDiscovery(serviceDiscovery);

        // when
        String toString = builder.toString();

        // then
        assertThat(toString, containsString("serviceDiscovery=true"));
        assertEquals(toString.indexOf("serviceDiscovery"), toString.lastIndexOf("serviceDiscovery"));

    }

    @Test
    public void builderSetsAllFields() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        ConnectionSocketFactory plainSocketFactory = mock(ConnectionSocketFactory.class);
        LayeredConnectionSocketFactory sslSocketFactory = mock(LayeredConnectionSocketFactory.class);
        SchemeIOSessionStrategy httpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy httpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider credentialsProvider = mock(CredentialsProvider.class);
        ServiceDiscovery serviceDiscovery = mock(ServiceDiscovery.class);

        builder.withServerList(TEST_SERVER_LIST)
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withPooledResponseBuffers(TEST_POOLED_RESPONSE_BUFFERS_ENABLED)
                .withPooledResponseBuffersSizeInBytes(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES)
                .withServiceDiscovery(serviceDiscovery)
                .withPlainSocketFactory(plainSocketFactory)
                .withSslSocketFactory(sslSocketFactory)
                .withHttpIOSessionStrategy(httpIOSessionStrategy)
                .withHttpsIOSessionStrategy(httpsIOSessionStrategy)
                .withDefaultCredentialsProvider(credentialsProvider);

        // when
        HttpClientFactory httpClientFactory = builder.build();

        // then
        assertEquals(TEST_SERVER_LIST, httpClientFactory.serverList);
        assertEquals(TEST_CONNECTION_TIMEOUT, httpClientFactory.connTimeout);
        assertEquals(TEST_READ_TIMEOUT, httpClientFactory.readTimeout);
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS, httpClientFactory.maxTotalConnections);
        assertEquals(TEST_IO_THREAD_COUNT, httpClientFactory.ioThreadCount);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_ENABLED, httpClientFactory.pooledResponseBuffersEnabled);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES, httpClientFactory.pooledResponseBuffersSizeInBytes);
        assertEquals(serviceDiscovery, httpClientFactory.serviceDiscovery);
        assertEquals(plainSocketFactory, httpClientFactory.plainSocketFactory);
        assertEquals(sslSocketFactory, httpClientFactory.sslSocketFactory);
        assertEquals(httpIOSessionStrategy, httpClientFactory.httpIOSessionStrategy);
        assertEquals(httpsIOSessionStrategy, httpClientFactory.httpsIOSessionStrategy);
        assertEquals(credentialsProvider, httpClientFactory.defaultCredentialsProvider);

    }

    @Test
    public void builderSetsDefaultFields() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        // when
        HttpClientFactory httpClientFactory = builder.build();

        // then
        assertNull(httpClientFactory.serviceDiscovery);
        assertNotNull(httpClientFactory.plainSocketFactory);
        assertNotNull(httpClientFactory.sslSocketFactory);
        assertNotNull(httpClientFactory.httpIOSessionStrategy);
        assertNotNull(httpClientFactory.httpsIOSessionStrategy);

    }

    @Test
    public void builderAppliesAuthIfConfigured() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        ConnectionSocketFactory plainSocketFactory = mock(ConnectionSocketFactory.class);
        LayeredConnectionSocketFactory sslSocketFactory = mock(LayeredConnectionSocketFactory.class);
        SchemeIOSessionStrategy httpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy httpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider credentialsProvider = mock(CredentialsProvider.class);

        Security security = spy(SecurityTest.createDefaultTestSecurityBuilder().build());

        builder.withAuth(security)
                .withPlainSocketFactory(plainSocketFactory)
                .withSslSocketFactory(sslSocketFactory)
                .withHttpIOSessionStrategy(httpIOSessionStrategy)
                .withHttpsIOSessionStrategy(httpsIOSessionStrategy)
                .withDefaultCredentialsProvider(credentialsProvider);

        // when
        HttpClientFactory httpClientFactory = builder.build();

        // then
        verify(security).configure(builder);

        assertEquals(plainSocketFactory, httpClientFactory.plainSocketFactory);
        assertNotEquals(sslSocketFactory, httpClientFactory.sslSocketFactory);
        assertEquals(httpIOSessionStrategy, httpClientFactory.httpIOSessionStrategy);
        assertNotEquals(httpsIOSessionStrategy, httpClientFactory.httpsIOSessionStrategy);
        assertNotEquals(credentialsProvider, httpClientFactory.defaultCredentialsProvider);

    }

    @Test
    public void throwsIllegalStateOnReactorException() throws IOReactorException {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();
        HttpClientFactory factory = spy(builder.build());

        String expectedMessage = UUID.randomUUID().toString();
        when(factory.createIOReactor()).thenThrow(new IOReactorException(expectedMessage));

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, factory::createInstance);

        // then
        assertThat(exception.getMessage(), containsString(expectedMessage));

    }

    @Test
    public void createInstanceConfiguresAsyncHttpClient() {

        // given
        HttpClientFactory factory = Mockito.spy(createDefaultTestHttpClientFactory());
        HttpClient client = spy(factory.createConfiguredClient(any(), any(), any()));
        when(factory.createConfiguredClient(any(), any(), any())).thenReturn(client);

        NHttpClientConnectionManager connectionManager = mock(NHttpClientConnectionManager.class);
        when(factory.getAsyncConnectionManager()).thenReturn(connectionManager);

        CloseableHttpAsyncClient httpAsyncClient = mock(CloseableHttpAsyncClient.class);
        when(factory.createAsyncHttpClient(any())).thenReturn(httpAsyncClient);

        // when
        factory.createInstance();

        // then
        verify(factory, times(1)).createAsyncHttpClient(eq(connectionManager));

    }

    @Test
    public void disablingPooledResponseBuffersInitializesClientWithBasicAsyncResponseConsumerFactory() {

        // given
        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();
        builder.withServerList(TEST_SERVER_LIST)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withPooledResponseBuffers(false);

        HttpClientFactory factory = spy(builder.build());

        // when
        factory.createInstance();

        // then
        ArgumentCaptor<HttpAsyncResponseConsumerFactory> captor = ArgumentCaptor.forClass(HttpAsyncResponseConsumerFactory.class);
        verify(factory).createConfiguredClient(any(), any(), captor.capture());
        assertEquals(BasicAsyncResponseConsumer.class, captor.getValue().create().getClass());

    }

    @Test
    public void configuresServerPoolUrlsFromGivenServiceDiscovery() throws IOException {

        // given
        String expectedAddress = "http://expected:9234";

        HCServiceDiscovery<HttpClient> serviceDiscovery = new HCServiceDiscovery<>(
                createDefaultTestClientProvider(),
                (client, callback) -> callback.onSuccess(Collections.singletonList(expectedAddress)),
                Integer.MAX_VALUE);

        HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(Collections.emptyList())
                .withServiceDiscovery(serviceDiscovery);

        HttpClientFactory httpClientFactory = spy(builder.build());
        when(httpClientFactory.createAsyncHttpClient(any())).thenReturn(mock(CloseableHttpAsyncClient.class));

        RequestFactory<HttpUriRequest> requestFactory = spy(new HCRequestFactory());
        when(httpClientFactory.createRequestFactory()).thenReturn(requestFactory);

        String expectedPath = "test";
        Request request = new GenericRequest("GET", expectedPath, null);

        HttpClient httpClient = httpClientFactory.createInstance();
        serviceDiscovery.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));

        // when
        httpClient.execute(request, mock(BlockingResponseHandler.class));

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestFactory).create(captor.capture(), any());
        assertEquals(expectedAddress + "/" + expectedPath, captor.getValue());

    }

    @Test
    public void enablesAllPooledConsumerMetricsWithMetricRegistry() throws IOException {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "responseBytes", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory config = spy(new HttpClientFactory.Builder()
                .withServerList(TEST_SERVER_LIST)
                .withMaxTotalConnections(1)
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createCountConfig("responseBytes")))
                .withPooledResponseBuffers(true)
                .withMaxTotalConnections(2)
                .build());

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        final CloseableHttpAsyncClient closeableHttpAsyncClient = mock(CloseableHttpAsyncClient.class);
        final HttpClient httpClient = spy(config.createInstance());
        when(httpClient.getAsyncClient()).thenReturn(closeableHttpAsyncClient);


        final HttpEntity httpEntity = new ByteArrayEntity(new byte[405]);

        final HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null));
        httpResponse.setEntity(httpEntity);

        // when
        httpClient.register(registry); // metrics are registered before first batch; that's the lazy registration

        httpClient.executeAsync(new GenericRequest("GET", "http://localhost:9200", null), new TestResponseHandler());
        ArgumentCaptor<HttpAsyncResponseConsumer> captor = ArgumentCaptor.forClass(HttpAsyncResponseConsumer.class);
        verify(closeableHttpAsyncClient).execute(any(HttpAsyncRequestProducer.class), captor.capture(), any(HttpClientContext.class), any(FutureCallback.class));

        final HttpAsyncResponseConsumer asyncResponseConsumer = captor.getValue();
        ((PoolingAsyncResponseConsumer)asyncResponseConsumer).onResponseReceived(httpResponse);
        ((PoolingAsyncResponseConsumer)asyncResponseConsumer).onEntityEnclosed(httpEntity, ContentType.APPLICATION_JSON);

        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(405L));

    }

    @Test
    public void configuresSubSetOfMetricsWithMetricRegistry() throws IOException {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "responseBytes", "max");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory config = createDefaultTestHttpClientFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createMaxConfig("responseBytes", false)))
                .withPooledResponseBuffers(true)
                .withMaxTotalConnections(2)
                .build();

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        final HttpAsyncResponseConsumerFactory consumerFactory = config.createHttpAsyncResponseConsumerFactory();

        Measured.of(consumerFactory).register(registry);

        final PoolingAsyncResponseConsumer httpResponseHttpAsyncResponseConsumer = (PoolingAsyncResponseConsumer) consumerFactory.create();
        final HttpEntity httpEntity = new ByteArrayEntity(new byte[405]);

        final HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null));
        httpResponse.setEntity(httpEntity);

        // when
        httpResponseHttpAsyncResponseConsumer.onResponseReceived(httpResponse);
        httpResponseHttpAsyncResponseConsumer.onEntityEnclosed(httpEntity, ContentType.APPLICATION_JSON);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(405L));

    }

    @Test
    public void deregistersAllMetricsWithMetricRegistry() throws IOException {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory config = createDefaultTestHttpClientFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createMaxConfig("responseBytes", false)))
                .withPooledResponseBuffers(true)
                .withMaxTotalConnections(2)
                .build();

        final HttpAsyncResponseConsumerFactory consumerFactory = config.createHttpAsyncResponseConsumerFactory();

        Measured.of(consumerFactory).register(registry);

        // when
        Measured.of(consumerFactory).deregister();

        // then
        assertEquals(0, registry.getMetrics(m -> true).size());

    }

    private HttpClientFactory createDefaultTestHttpClientFactory() {
        return createDefaultTestHttpClientFactoryBuilder().build();
    }

    public static HttpClientFactory.Builder createDefaultTestHttpClientFactoryBuilder() {
        return new HttpClientFactory.Builder()
                .withServerList(TEST_SERVER_LIST)
                .withMaxTotalConnections(1);
    }

    public static class BuilderMatcher extends BaseMatcher<HttpClientFactory.Builder> {

        private final HttpClientFactory.Builder current;
        private HttpClientFactory.Builder other;

        public BuilderMatcher(HttpClientFactory.Builder current) {
            this.current = current;
        }

        @Override
        public boolean matches(Object item) {

            if (item == null) {
                return false;
            }

            if (!(item instanceof HttpClientFactory.Builder)) {
                return false;
            }

            other = (HttpClientFactory.Builder) item;

            if (!current.serverList.equals(other.serverList)) {
                return false;
            }

            if (current.readTimeout != other.readTimeout) {
                return false;
            }

            if (current.connTimeout != other.connTimeout) {
                return false;
            }

            if (current.maxTotalConnections != other.maxTotalConnections) {
                return false;
            }

            if (current.ioThreadCount != other.ioThreadCount) {
                return false;
            }

            if (current.pooledResponseBuffersEnabled != other.pooledResponseBuffersEnabled) {
                return false;
            }

            if (current.pooledResponseBuffersSizeInBytes != other.pooledResponseBuffersSizeInBytes) {
                return false;
            }

            if (current.auth != other.auth) {
                return false;
            }

            if (current.serviceDiscovery != other.serviceDiscovery) {
                return false;
            }

            return true;

        }

        @Override
        public void describeTo(Description description) {
            description.appendText(current.toString());
        }

    }

    private static class TestResponseHandler implements ResponseHandler<Response> {
        @Override
        public void completed(Response result) {

        }

        @Override
        public void failed(Exception ex) {

        }

        @Override
        public Response deserializeResponse(InputStream inputStream) throws IOException {
            return null;
        }
    }
}
