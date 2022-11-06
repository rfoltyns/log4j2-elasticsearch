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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutputTest;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.TestKeyAccessor;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ClientStats;
import org.asynchttpclient.HostStats;
import org.asynchttpclient.RequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.log4j2.elasticsearch.ahc.BatchRequestTest.createTestBatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpClientTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "7");
    }

    @Captor
    private ArgumentCaptor<AHCResultCallback> hcResultCallbackCaptor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void createClientRequestCreatesPostRequest() throws Exception {

        // given
        final BatchRequest request = createDefaultTestBatchRequest();

        // when
        final HttpClient client = createDefaultTestObject();
        final org.asynchttpclient.Request httpRequest = client.createClientRequest(request).build();

        // then
        assertEquals("POST", httpRequest.getMethod());

    }

    @Test
    public void createClientRequestCreatesRequestUsingUriProvidedByAction() throws Exception {

        // given
        final BatchRequest request = createDefaultTestBatchRequest();

        final String expectedUriPart = UUID.randomUUID().toString();
        when(request.getURI()).thenReturn(expectedUriPart);

        // when
        final HttpClient client = createDefaultTestObject();
        final org.asynchttpclient.Request httpRequest = client.createClientRequest(request).build();

        // then
        assertTrue(httpRequest.getUri().toString().contains(expectedUriPart));

    }

    @Test
    public void createClientRequestCreatesRequestWithContentTypeHeader() throws Exception {

        // given
        final BatchRequest request = createDefaultTestBatchRequest();

        // when
        final HttpClient client = createDefaultTestObject();
        final RequestBuilder httpRequest = client.createClientRequest(request);

        // then
        assertEquals("application/json", httpRequest.build().getHeaders().get("Content-Type"));

    }

    @Test
    public void createClientRequestDelegatesToRequestFactory() throws Exception {

        // given
        final AHCRequestFactory requestFactory = mock(AHCRequestFactory.class);

        final HttpClient client = createTestHttpClient(
                mock(AsyncHttpClient.class),
                mock(ServerPool.class),
                requestFactory);

        final Request request = mock(Request.class);

        // when
        client.createClientRequest(request);

        // then
        verify(requestFactory, times(1)).create(any(), eq(request));
    }

    @Test
    public void executeAsyncDelegatesToFailureHandlerOnCreateClientRequestIOException() throws Exception {

        // given
        final RequestFactory requestFactory = mock(RequestFactory.class);
        final HttpClient client = createTestHttpClient(
                mock(AsyncHttpClient.class),
                mock(ServerPool.class),
                requestFactory
        );

        final String expectedMessage = UUID.randomUUID().toString();
        final BatchRequest request = createDefaultTestBatchRequest();
        when(requestFactory.create(any(), any())).thenThrow(new IOException(expectedMessage));

        final ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        // when
        client.executeAsync(request, responseHandler);

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals(expectedMessage, exceptionCaptor.getValue().getMessage());

    }

    @Test
    public void executeAsyncDelegatesToConfiguredAsyncClient() {

        // given
        final HttpClient client = spy(createDefaultTestObject());
        final AsyncHttpClient asyncClient1 = mock(AsyncHttpClient.class);
        when(client.getAsyncClient()).thenReturn(asyncClient1);
        final AsyncHttpClient asyncClient = asyncClient1;

        final BatchRequest request = createDefaultTestBatchRequest();

        // when
        client.executeAsync(request, createMockTestResultHandler());

        // then
        verify(client).getAsyncClient();
        verify(asyncClient).executeRequest(
                any(RequestBuilder.class),
                any(AHCResultCallback.class));

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerCompleted() throws Exception {

        // given
        final ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = mock(BatchResult.class);
        when(responseHandler.deserializeResponse(any())).thenReturn(batchResult);

        final AHCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        final ByteArrayInputStream inputStream = spy(new ByteArrayInputStream("{}".getBytes()));

        final org.asynchttpclient.Response httpResponse = mock(org.asynchttpclient.Response.class);
        when(httpResponse.hasResponseBody()).thenReturn(true);
        when(httpResponse.getResponseBodyAsStream()).thenReturn(inputStream);

        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusText()).thenReturn(UUID.randomUUID().toString());

        // when
        asyncCallback.onCompleted(httpResponse);

        // then
        verify(responseHandler, never()).failed(any());
        verify(responseHandler).completed(any());
        verify(inputStream).close();

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerFailedOnIOException() throws Exception {

        // given
        final String exceptionMessage = UUID.randomUUID().toString();
        final ResponseHandler<Response> responseHandler = createMockTestResultHandler();
        when(responseHandler.deserializeResponse(any())).thenThrow(new IOException(exceptionMessage));

        final AHCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        final org.asynchttpclient.Response httpResponse = createDefaultTestHttpResponse();

        // when
        asyncCallback.onCompleted(httpResponse);

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals(exceptionMessage, exceptionCaptor.getValue().getMessage());

        verify(responseHandler, never()).completed(any());

    }

    @Test
    public void executeDelegatesToExecuteAsync() {

        // given
        final AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        final HttpClient httpClient = createDefaultTestObject(asyncHttpClient);

        final BlockingResponseHandler<Response> responseHandler = mock(BlockingResponseHandler.class);
        final IndexTemplateRequest request = IndexTemplateRequestTest.createDefaultTestObjectBuilder().build();

        // when
        httpClient.execute(request, responseHandler);

        // then
        verify(asyncHttpClient).executeRequest((RequestBuilder) any(), any());

    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartStartsOnlyOnce() {

        // given
        final Logger logger = InternalLoggingTest.mockTestLogger();
        final HttpClient httpClient = spy(createDefaultTestObject());

        assertTrue(httpClient.isStopped());

        // when
        httpClient.start();
        httpClient.start();

        // then
        verify(logger).debug(eq("{}: Started"), eq(HttpClient.class.getSimpleName()));

        Mockito.reset(logger);
        InternalLogging.setLogger(null);

    }

    @Test
    public void lifecycleStopDoesNotStopClosedAsyncHttpClient() throws IOException {

        // given
        final AsyncHttpClient asyncClient = mock(AsyncHttpClient.class);
        final HttpClient httpClient = new HttpClient(
                asyncClient,
                mock(ServerPool.class),
                mock(RequestFactory.class));

        when(asyncClient.isClosed()).thenReturn(true);

        httpClient.start();

        // when
        httpClient.stop();

        // then
        verify(asyncClient, never()).close();

    }

    @Test
    public void lifecycleStopStopsOnlyOnce() {

        // given
        final AsyncHttpClient asyncClient = mock(AsyncHttpClient.class);
        final HttpClient httpClient = new HttpClient(
                asyncClient,
                mock(ServerPool.class),
                mock(RequestFactory.class));

        httpClient.start();

        // when
        httpClient.stop();
        httpClient.stop();

        // then
        verify(asyncClient).isClosed();

    }

    @Test
    public void lifecycleStopDoesNotRethrowIOExceptionOnClientClose() throws IOException {

        // given
        final AsyncHttpClient asyncClient = mock(AsyncHttpClient.class);
        when(asyncClient.isClosed()).thenReturn(false);
        Mockito.doThrow(new IOException()).when(asyncClient).close();

        final HttpClient httpClient = createTestHttpClient(
                asyncClient,
                mock(ServerPool.class),
                mock(RequestFactory.class)
        );

        httpClient.start();

        // when
        httpClient.stop();

    }

    private HttpClient createTestHttpClient(
            final AsyncHttpClient asyncClient,
            final ServerPool serverPool,
            final RequestFactory requestFactory) {
        return new HttpClient(asyncClient, serverPool, requestFactory);
    }

    @Test
    public void lifecycleStart() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

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
        final LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    // =======
    // METRICS
    // =======

    @Test
    public void registersComponentsMetricsWithDefaultComponentName() {

        // given
        final String expectedComponentName = HttpClient.class.getSimpleName();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "connectionsActive", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "connectionsIdle", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "connectionsTotal", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory.Builder builder = createTestHttpClientFactoryBuilder(mock(AsyncHttpClient.class))
                .withName(null)
                .withMetricConfigs(HttpClient.metricConfigs(false));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        // when
        provider.createClient();
        provider.register(registry);

        // then
        assertEquals(3, registry.getMetrics(metric -> TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());

    }

    @Test
    public void registersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "connectionsActive", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "connectionsIdle", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "connectionsTotal", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory.Builder builder = createTestHttpClientFactoryBuilder(mock(AsyncHttpClient.class))
                .withName(expectedComponentName)
                .withMetricConfigs(HttpClient.metricConfigs(false));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        // when
        provider.createClient();
        provider.register(registry);

        // then
        assertEquals(3, registry.getMetrics(metric -> TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());

    }

    @Test
    public void deregistersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "connectionsActive", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "connectionsIdle", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "connectionsTotal", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientFactory.Builder builder = createTestHttpClientFactoryBuilder(mock(AsyncHttpClient.class))
                .withName(expectedComponentName)
                .withMetricConfigs(HttpClient.metricConfigs(false));

        final HttpClientProvider provider = new HttpClientProvider(builder);

        final HttpClient client = provider.createClient();
        client.register(registry);
        assertEquals(3, registry.getMetrics(metric -> TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());

        // when
        client.deregister();
        client.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());

    }

    @Test
    public void providesConnectionsActive() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "connectionsActive", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        final Map<String, HostStats> expectedStats = new HashMap<>();
        final long expectedActiveConnectionCount = new Random().nextInt(100) + 1;
        expectedStats.put("testHost1", new HostStats(expectedActiveConnectionCount, 0));
        when(asyncHttpClient.getClientStats()).thenReturn(new ClientStats(expectedStats));

        final HttpClient httpClient = createTestHttpClientFactoryBuilder(asyncHttpClient)
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createCountConfig("connectionsActive")))
                .withMaxTotalConnections(2)
                .build()
                .createInstance();

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        // when
        httpClient.register(registry);
        metricProcessor.process();
        httpClient.deregister();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq(expectedActiveConnectionCount));
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals("connectionsActive")).size());

    }

    @Test
    public void providesConnectionsIdle() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "connectionsIdle", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        final Map<String, HostStats> expectedStats = new HashMap<>();
        final long expectedIdleConnectionCount = new Random().nextInt(100) + 1;
        expectedStats.put("testHost1", new HostStats(0, expectedIdleConnectionCount));
        when(asyncHttpClient.getClientStats()).thenReturn(new ClientStats(expectedStats));

        final HttpClient httpClient = createTestHttpClientFactoryBuilder(asyncHttpClient)
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createCountConfig("connectionsIdle")))
                .withMaxTotalConnections(2)
                .build()
                .createInstance();

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        // when
        httpClient.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq(expectedIdleConnectionCount));
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals("connectionsIdle")).size());

    }

    @Test
    public void providesConnectionsTotal() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "connectionsTotal", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        final Map<String, HostStats> expectedStats = new HashMap<>();
        final long expectedActiveConnectionCount = new Random().nextInt(100) + 1;
        final long expectedIdleConnectionCount = new Random().nextInt(100) + 1;
        expectedStats.put("testHost1", new HostStats(expectedActiveConnectionCount, expectedIdleConnectionCount));
        when(asyncHttpClient.getClientStats()).thenReturn(new ClientStats(expectedStats));

        final HttpClient httpClient = createTestHttpClientFactoryBuilder(asyncHttpClient)
                .withName(expectedComponentName)
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createCountConfig("connectionsTotal")))
                .withMaxTotalConnections(200)
                .build()
                .createInstance();

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        // when
        httpClient.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq(expectedActiveConnectionCount + expectedIdleConnectionCount));
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals("connectionsTotal")).size());

    }

    static HttpClientFactory.Builder createTestHttpClientFactoryBuilder(final AsyncHttpClient asyncHttpClient) {
        return new HttpClientFactory.Builder() {
            @Override
            public HttpClientFactory build() {
                return new HttpClientFactory(this) {
                    @Override
                    protected AsyncHttpClient createAsyncHttpClient() {
                        return asyncHttpClient;
                    }
                };
            }
        }.withServerList(Collections.singletonList("http://localhost:9200"));
    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestObject();
    }

    private org.asynchttpclient.Response createDefaultTestHttpResponse() {
        return mock(org.asynchttpclient.Response.class);
    }

    private AHCResultCallback mockHttpResponseCallback(final ResponseHandler<Response> responseHandler) throws Exception {

        final AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        final HttpClientFactory.Builder builder = createTestHttpClientFactoryBuilder(asyncHttpClient);
        final HttpClientProvider provider = new HttpClientProvider(builder);
        final HttpClient client = provider.createClient();

        final BatchRequest request = mock(BatchRequest.class);
        when(request.getURI()).thenReturn(UUID.randomUUID().toString());
        when(request.getHttpMethodName()).thenReturn(BatchRequest.HTTP_METHOD_NAME);
        final ItemSource itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(mock(ByteBuf.class));
        when(request.serialize()).thenReturn(itemSource);

        client.executeAsync(request, responseHandler);
        verify(asyncHttpClient).executeRequest(
                any(RequestBuilder.class),
                hcResultCallbackCaptor.capture());

        return hcResultCallbackCaptor.getValue();
    }

    private BatchRequest createDefaultTestBatchRequest() {
        final ItemSource<ByteBuf> payload1 = createDefaultTestByteBufItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestByteBufItemSource("test2");

        return createTestBatch(new BatchRequest.Builder(), payload1, payload2);
    }

    private ResponseHandler<Response> createMockTestResultHandler() {
        return spy(new ResponseHandler<Response>() {
            @Override
            public void completed(final Response result) {
            }

            @Override
            public void failed(final Exception ex) {
            }

            @Override
            public Response deserializeResponse(final InputStream inputStream) {
                return null;
            }

        });
    }

    private HttpClient createDefaultTestObject() {
        return createDefaultTestObject(mock(AsyncHttpClient.class));
    }

    private HttpClient createDefaultTestObject(final AsyncHttpClient asyncHttpClient) {

        final HttpClientFactory.Builder builder = createTestHttpClientFactoryBuilder(asyncHttpClient);
        final HttpClientProvider provider = new HttpClientProvider(builder);
        return provider.createClient();

    }

    private ItemSource<ByteBuf> createDefaultTestByteBufItemSource(final String payload) {
        final CompositeByteBuf buffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return ByteBufItemSourceTest.createTestItemSource(buffer, source -> {});
    }

}
