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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createDefaultTestByteBuf;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.hc.BatchRequestTest.createTestBatch;
import static org.appenders.log4j2.elasticsearch.hc.HCHttpTest.createDefaultHttpObjectFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private ArgumentCaptor<HCResultCallback> hcResultCallbackCaptor;

    @Captor
    private ArgumentCaptor<BatchResult> batchResultCaptor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @Captor
    private ArgumentCaptor<HttpAsyncResponseConsumer> asyncConsumerCaptor;

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void createClientRequestCreatesPostRequest() throws IOException {

        // given
        BatchRequest request = createDefaultTestBatchRequest();

        // when
        HttpClient client = createDefaultTestObject();
        HttpUriRequest httpRequest = client.createClientRequest(request);

        // then
        assertEquals("POST", httpRequest.getRequestLine().getMethod());

    }

    @Test
    public void createClientRequestCreatesRequestUsingUriProvidedByAction() throws IOException {

        // given
        BatchRequest request = createDefaultTestBatchRequest();

        String expectedUriPart = UUID.randomUUID().toString();
        when(request.getURI()).thenReturn(expectedUriPart);

        // when
        HttpClient client = createDefaultTestObject();
        HttpUriRequest httpRequest = client.createClientRequest(request);

        // then
        assertTrue(httpRequest.getRequestLine().getUri().contains(expectedUriPart));

    }

    @Test
    public void createClientRequestCreatesRequestWithContentTypeHeader() throws IOException {

        // given
        BatchRequest request = createDefaultTestBatchRequest();

        // when
        HttpClient client = createDefaultTestObject();
        HttpUriRequest httpRequest = client.createClientRequest(request);

        // then
        HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
        assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());

    }

    @Test
    public void createClientRequestDelegatesToRequestFactory() throws IOException {

        // given
        HCRequestFactory requestFactory = mock(HCRequestFactory.class);

        HttpClient client = createTestHttpClient(
                mock(CloseableHttpAsyncClient.class),
                mock(ServerPool.class),
                requestFactory,
                mock(HttpAsyncResponseConsumerFactory.class));

        Request request = mock(Request.class);

        // when
        client.createClientRequest(request);

        // then
        verify(requestFactory, times(1)).create(any(), eq(request));
    }

    @Test
    public void executeAsyncResponseIsNotPooledIfPoolNotConfigured() {

        // given
        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withPooledResponseBuffers(false));

        HCHttp.Builder testObjectFactoryBuilder = createDefaultHttpObjectFactoryBuilder().withClientProvider(clientProvider);

        HttpClient client = spy(testObjectFactoryBuilder.build().createClient());

        CloseableHttpAsyncClient asyncClient = mockAsyncClient(client);

        BatchRequest request = createDefaultTestBatchRequest();

        // when
        client.executeAsync(request, createMockTestResultHandler());

        // then
        verify(client).getAsyncClient();
        verify(asyncClient).execute(
                any(HttpAsyncRequestProducer.class),
                asyncConsumerCaptor.capture(),
                any(HttpContext.class),
                any(FutureCallback.class));
        assertEquals(BasicAsyncResponseConsumer.class, asyncConsumerCaptor.getValue().getClass());
    }

    @Test
    public void executeAsyncDelegatesToFailureHandlerOnCreateClientRequestIOException() throws IOException {

        // given
        RequestFactory requestFactory = mock(RequestFactory.class);
        HttpClient client = createTestHttpClient(
                mock(CloseableHttpAsyncClient.class),
                mock(ServerPool.class),
                requestFactory,
                mock(HttpAsyncResponseConsumerFactory.class)
        );

        String expectedMessage = UUID.randomUUID().toString();
        BatchRequest request = createDefaultTestBatchRequest();
        when(requestFactory.create(any(), any())).thenThrow(new IOException(expectedMessage));

        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        // when
        client.executeAsync(request, responseHandler);

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals(expectedMessage, exceptionCaptor.getValue().getMessage());

    }

    @Test
    public void executeAsyncDelegatesToConfiguredAsyncClient() {

        // given
        HttpClient client = Mockito.spy(createDefaultTestObject());
        CloseableHttpAsyncClient asyncClient = mockAsyncClient(client);

        BatchRequest request = createDefaultTestBatchRequest();

        // when
        client.executeAsync(request, createMockTestResultHandler());

        // then
        verify(client).getAsyncClient();
        verify(asyncClient).execute(
                any(HttpAsyncRequestProducer.class),
                any(HttpAsyncResponseConsumer.class),
                any(HttpContext.class),
                any(FutureCallback.class));

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerCompleted() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = mock(BatchResult.class);
        when(responseHandler.deserializeResponse(any())).thenReturn(batchResult);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        HttpEntity httpEntity = mock(HttpEntity.class);
        final ByteArrayInputStream inputStream = spy(new ByteArrayInputStream("{}".getBytes()));
        when(httpEntity.getContent()).thenReturn(inputStream);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, UUID.randomUUID().toString()));

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(responseHandler, never()).failed(any());
        verify(responseHandler).completed(any());
        verify(inputStream).close();

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerFailedOnIOException() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = mock(BatchResult.class);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        HttpEntity entity = mock(HttpEntity.class);
        String exceptionMessage = UUID.randomUUID().toString();
        when(entity.getContent()).thenThrow(new IOException(exceptionMessage));

        HttpResponse httpResponse = createDefaultTestHttpResponse();
        when(httpResponse.getEntity()).thenReturn(entity);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals(exceptionMessage, exceptionCaptor.getValue().getMessage());

        verify(responseHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerFailedOnThrowable() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);

        final BatchResult batchResult = mock(BatchResult.class);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        HttpEntity entity = mock(HttpEntity.class);
        String exceptionMessage = UUID.randomUUID().toString();
        when(entity.getContent()).thenAnswer((Answer<InputStream>) invocation -> {
            throw new Throwable(exceptionMessage);
        });

        HttpResponse httpResponse = createDefaultTestHttpResponse();
        when(httpResponse.getEntity()).thenReturn(entity);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals("Problem during response processing", exceptionCaptor.getValue().getMessage());
        assertEquals(exceptionMessage, exceptionCaptor.getValue().getCause().getMessage());

        verify(responseHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerCompletedOnInputStreamCloseException() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = mock(BatchResult.class);
        when(responseHandler.deserializeResponse(any())).thenReturn(batchResult);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        HttpEntity entity = mock(HttpEntity.class);
        String exceptionMessage = UUID.randomUUID().toString();
        ByteBufInputStream inputStream = new ByteBufInputStream(mock(ByteBuf.class)) {
            @Override
            public void close() throws IOException {
                throw new IOException(exceptionMessage);
            }
        };
        when(entity.getContent()).thenReturn(inputStream);

        HttpResponse httpResponse = createDefaultTestHttpResponse(200, UUID.randomUUID().toString());
        when(httpResponse.getEntity()).thenReturn(entity);

        mockTestLogger(); // reduce noise

        // when
        asyncCallback.completed(httpResponse);

        setLogger(null); // bring back the noise

        // then
        verify(responseHandler, times(1)).completed(any());
        verify(responseHandler, never()).failed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerWhenCancelled() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = mock(BatchResult.class);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        // when
        asyncCallback.cancelled();

        // then
        verify(responseHandler).failed(exceptionCaptor.capture());
        assertEquals("Request cancelled", exceptionCaptor.getValue().getMessage());

        verify(responseHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackDoesNotRethrowOnResponseHandlerExceptions() {

        // given
        Logger logger = mockTestLogger();

        ResponseHandler<Response> responseHandler = createMockTestResultHandler();
        RuntimeException testException = spy(new RuntimeException("test exception"));
        doThrow(testException).when(responseHandler).failed(any(Exception.class));

        HCResultCallback<Response> asyncCallback = new HCResultCallback<>(responseHandler);

        Exception exception = mock(Exception.class);

        // when
        asyncCallback.failed(exception);

        // then
        verify(logger).error(eq("Callback failed"), eq(testException));

    }

    @Test
    public void executeAsyncCallbackHandlesHttpResponse() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        final BatchResult batchResult = new BatchResult(0, false, null, 100, null);
        when(responseHandler.deserializeResponse(any())).thenReturn(batchResult);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        String reasonPhrase = UUID.randomUUID().toString();
        HttpResponse httpResponse = createDefaultTestHttpResponse(200, reasonPhrase);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(responseHandler, never()).failed(any());
        verify(responseHandler).completed(batchResultCaptor.capture());
        BatchResult result = batchResultCaptor.getValue();

        assertTrue(result.isSucceeded());
        assertEquals(200, result.getResponseCode());
        assertNull(result.getItems());

    }

    @Test
    public void executeAsyncCallbackHandlesNonSuccessfulResponse() throws IOException {

        // given
        ResponseHandler<Response> responseHandler = createMockTestResultHandler();

        BatchResult batchResult = spy(new BatchResult(0, true, null, 400, new ArrayList<>()));
        when(responseHandler.deserializeResponse(any())).thenReturn(batchResult);

        HCResultCallback asyncCallback = mockHttpResponseCallback(responseHandler);

        int expectedStatusCode = new Random().nextInt(1000) + 1000;
        String reasonPhrase = UUID.randomUUID().toString();
        HttpResponse httpResponse = createDefaultTestHttpResponse(expectedStatusCode, reasonPhrase);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(responseHandler, never()).failed(any());
        verify(responseHandler).completed(batchResultCaptor.capture());
        BatchResult result = batchResultCaptor.getValue();

        assertFalse(result.isSucceeded());
        assertEquals(expectedStatusCode, result.getResponseCode());
        assertNotEquals(httpResponse.getStatusLine().getReasonPhrase(), result.withErrorMessage("test fallback message").getErrorMessage());
        assertNotNull(result.getItems());

    }

    @Test
    public void executeDelegatesToExecuteAsync() {

        // given
        HttpClient httpClient = spy(createDefaultTestObject());
        httpClient.start();

        BlockingResponseHandler<Response> responseHandler = createDefaultTestBlockingResponseHandler();
        IndexTemplateRequest request = IndexTemplateRequestTest.createDefaultTestObjectBuilder().build();

        // when
        httpClient.execute(request, responseHandler);

        // then
        verify(httpClient).executeAsync(eq(request), eq(responseHandler));

    }

    @Test
    public void executeDelegatesToBlockingResponseHandler() {

        // given
        HttpClient httpClient = spy(createDefaultTestObject());
        httpClient.start();

        BlockingResponseHandler<Response> responseHandler = spy(createDefaultTestBlockingResponseHandler());
        IndexTemplateRequest request = IndexTemplateRequestTest.createDefaultTestObjectBuilder().build();

        // when
        httpClient.execute(request, responseHandler);

        // then
        verify(responseHandler).getResult();

    }

    @Test
    public void lifecycleStartStartsPoolOnlyOnce() {

        // given
        PoolingAsyncResponseConsumerFactory asyncResponseConsumerFactory =
                mock(PoolingAsyncResponseConsumerFactory.class);

        HttpClient httpClient = createTestHttpClient(
                mock(CloseableHttpAsyncClient.class),
                mock(ServerPool.class),
                mock(RequestFactory.class),
                asyncResponseConsumerFactory);

        // when
        httpClient.start();
        httpClient.start();

        // then
        verify(asyncResponseConsumerFactory, times(1)).start();

    }

    @Test
    public void lifecycleStopConsumerFactoryOnlyOnce() {

        // given
        PoolingAsyncResponseConsumerFactory factory = mock(PoolingAsyncResponseConsumerFactory.class);

        HttpClient httpClient = createTestHttpClient(
                mock(CloseableHttpAsyncClient.class),
                mock(ServerPool.class),
                mock(RequestFactory.class),
                factory);

        httpClient.start();

        // when
        httpClient.stop();
        httpClient.stop();

        // then
        verify(factory, times(1)).stop();

    }

    @Test
    public void lifecycleStopDoesNotRethrowIOExceptionOnClientClose() throws IOException {

        // given
        CloseableHttpAsyncClient asyncClient = mock(CloseableHttpAsyncClient.class);
        when(asyncClient.isRunning()).thenReturn(true);
        doThrow(new IOException()).when(asyncClient).close();

        HttpClient httpClient = createTestHttpClient(
                asyncClient,
                mock(ServerPool.class),
                mock(RequestFactory.class),
                mock(HttpAsyncResponseConsumerFactory.class)
        );

        httpClient.start();

        // when
        httpClient.stop();

    }

    private HttpClient createTestHttpClient(
            CloseableHttpAsyncClient asyncClient,
            ServerPool serverPool,
            RequestFactory requestFactory,
            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory) {
        return new HttpClient(asyncClient, serverPool, requestFactory, asyncResponseConsumerFactory);
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

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestObject();
    }

    private BlockingResponseHandler<Response> createDefaultTestBlockingResponseHandler() {
        return new BlockingResponseHandler<>(
                new ObjectMapper().readerFor(BatchResult.class),
                (ex) -> new BasicResponse().withErrorMessage("test_exception: " + ex)
        );
    }

    private HttpResponse createDefaultTestHttpResponse(int statusCode, String reasonPhrase) {

        HttpEntity httpEntity = mock(HttpEntity.class);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, reasonPhrase));
        return httpResponse;
    }

    private HttpResponse createDefaultTestHttpResponse() {

        HttpEntity httpEntity = mock(HttpEntity.class);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        return httpResponse;
    }

    private HCResultCallback mockHttpResponseCallback(ResponseHandler<Response> responseHandler) throws IOException {

        HttpClient client = Mockito.spy(createDefaultTestObject());
        CloseableHttpAsyncClient asyncClient = mockAsyncClient(client);

        BatchRequest request = mock(BatchRequest.class);
        when(request.getURI()).thenReturn(UUID.randomUUID().toString());
        when(request.getHttpMethodName()).thenReturn(BatchRequest.HTTP_METHOD_NAME);
        ItemSource itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(mock(ByteBuf.class));
        when(request.serialize()).thenReturn(itemSource);

        client.executeAsync(request, responseHandler);
        verify(asyncClient).execute(
                any(HttpAsyncRequestProducer.class),
                any(PoolingAsyncResponseConsumer.class),
                any(HttpClientContext.class),
                hcResultCallbackCaptor.capture());

        return hcResultCallbackCaptor.getValue();
    }

    private CloseableHttpAsyncClient mockAsyncClient(HttpClient client) {
        CloseableHttpAsyncClient asyncClient = mock(CloseableHttpAsyncClient.class);
        when(client.getAsyncClient()).thenReturn(asyncClient);
        return asyncClient;
    }

    private BatchRequest createDefaultTestBatchRequest() {
        ItemSource<ByteBuf> payload1 = createDefaultTestByteBufItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestByteBufItemSource("test2");

        return createTestBatch(new BatchRequest.Builder(), payload1, payload2);
    }

    private ResponseHandler<Response> createMockTestResultHandler() {
        return spy(new ResponseHandler<Response>() {
            @Override
            public void completed(Response result) {
            }

            @Override
            public void failed(Exception ex) {
            }

            @Override
            public Response deserializeResponse(InputStream inputStream) {
                return null;
            }

        });
    }

    private HttpClient createDefaultTestObject() {
        HCHttp.Builder testObjectFactoryBuilder =
                createDefaultHttpObjectFactoryBuilder();
        return testObjectFactoryBuilder.build().createClient();
    }

    private ItemSource<ByteBuf> createDefaultTestByteBufItemSource(String payload) {
        CompositeByteBuf buffer = createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return createTestItemSource(buffer, source -> {});
    }

}
