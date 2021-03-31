package org.appenders.log4j2.elasticsearch.jest;

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

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicStatusLine;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createDefaultTestByteBuf;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BufferedJestHttpClientTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "7");
    }

    public static final String TEST_INDEX_NAME = "test_index";

    @Captor
    private ArgumentCaptor<BufferedJestHttpClient.BufferedResultCallback> bufferedResultCallbackCaptor;

    @Captor
    private ArgumentCaptor<JestResult> captor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @Test
    public void prepareRequestCreatesPostRequest() throws IOException {

        // given
        BufferedBulk bulk = createDefaultTestBufferedBulk();

        // when
        BufferedJestHttpClient client = createDefaultTestHttpClient();
        HttpUriRequest request = client.prepareRequest(bulk);

        // then
        assertEquals("POST", request.getRequestLine().getMethod());

    }

    @Test
    public void prepareRequestCreatesRequestUsingUriProvidedByAction() throws IOException {

        // given
        BufferedBulk bulk = createDefaultTestBufferedBulk();

        String expectedUriPart = UUID.randomUUID().toString();
        when(bulk.getURI()).thenReturn(expectedUriPart);

        // when
        BufferedJestHttpClient client = createDefaultTestHttpClient();
        HttpUriRequest request = client.prepareRequest(bulk);

        // then
        assertTrue(request.getRequestLine().getUri().contains(expectedUriPart));

    }

    @Test
    public void prepareRequestCreatesRequestWithContentTypeHeader() throws IOException {

        // given
        BufferedBulk bulk = createDefaultTestBufferedBulk();

        // when
        BufferedJestHttpClient client = createDefaultTestHttpClient();
        HttpUriRequest request = client.prepareRequest(bulk);

        // then
        HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
        assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());

    }

    @Test
    public void prepareRequestCreatesRequestWithSerializedBulk() throws IOException {

        // given
        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        BufferedBulk.Builder builder = spy(new BufferedBulk.Builder());
        ByteBufItemSource buffer = createTestItemSource();
        builder.withBuffer(buffer);

        Bulk bulk = createTestBatch(builder, payload1, payload2);

        // when
        BufferedJestHttpClient client = createDefaultTestHttpClient();
        HttpUriRequest request = client.prepareRequest((BufferedBulk) bulk);

        // then
        ByteBufInputStream byteBufInputStream = new ByteBufInputStream(buffer.getSource());

        byte[] expectedBody = new byte[byteBufInputStream.available()];
        byteBufInputStream.read(expectedBody);
        byteBufInputStream.reset();

        HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
        ByteBufInputStream content = (ByteBufInputStream) entity.getContent();
        byte[] actualBody = new byte[content.available()];
        content.read(actualBody);
        assertEquals(new String(expectedBody), new String(actualBody));

    }

    @Test
    public void executeAsyncDelegatesToConfiguredAsyncClient() {

        // given
        BufferedJestHttpClient client = spy(createDefaultTestHttpClient());
        CloseableHttpAsyncClient asyncClient = mock(CloseableHttpAsyncClient.class);
        when(client.getAsyncClient()).thenReturn(asyncClient);

        Bulk bulk = createDefaultTestBufferedBulk();

        // when
        client.executeAsync(bulk, createMockTestResultHandler());

        // then
        verify(client).getAsyncClient();
        verify(asyncClient).execute(any(HttpUriRequest.class), any());

    }

    @Test
    public void executeAsyncDelegatesToFailureHandlerOnPrepareRequestIOException() throws IOException {

        // given
        BufferedJestHttpClient client = spy(createDefaultTestHttpClient());
        CloseableHttpAsyncClient asyncClient = mock(CloseableHttpAsyncClient.class);

        String expectedMesage = UUID.randomUUID().toString();
        BufferedBulk bulk = createDefaultTestBufferedBulk();
        when(client.prepareRequest(bulk)).thenThrow(new IOException(expectedMesage));

        JestResultHandler<JestResult> jestResultHandler = createMockTestResultHandler();

        // when
        client.executeAsync(bulk, jestResultHandler);

        // then
        verify(jestResultHandler).failed(exceptionCaptor.capture());
        assertEquals(expectedMesage, exceptionCaptor.getValue().getMessage());

        verify(client, never()).getAsyncClient();
        verify(asyncClient, never()).execute(any(HttpUriRequest.class), any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerCompleted() throws IOException {

        // given
        JestResultHandler<JestResult> resultHandler = createMockTestResultHandler();

        final BufferedBulkResult bulkResult = new BufferedBulkResult(0, false, null, 200, null);
        final BufferedBulk bulk = mock(BufferedBulk.class);
        when(bulk.deserializeResponse(any())).thenReturn(bulkResult);

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(resultHandler, bulk);

        HttpResponse httpResponse = createDefaultTestHttpResponse(200, UUID.randomUUID().toString());

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(resultHandler, never()).failed(any());
        verify(resultHandler).completed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerFailedOnIOException() throws IOException {

        // given
        JestResultHandler<JestResult> resultHandler = createMockTestResultHandler();

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(resultHandler);

        HttpEntity entity = mock(HttpEntity.class);
        String exceptionMessage = UUID.randomUUID().toString();
        when(entity.getContent()).thenThrow(new IOException(exceptionMessage));

        HttpResponse httpResponse = createDefaultTestHttpResponse(200, UUID.randomUUID().toString());
        when(httpResponse.getEntity()).thenReturn(entity);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(resultHandler).failed(exceptionCaptor.capture());
        assertEquals(exceptionMessage, exceptionCaptor.getValue().getMessage());

        verify(resultHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerFailedOnThrowable() throws IOException {

        // given
        JestResultHandler<JestResult> resultHandler = createMockTestResultHandler();

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(resultHandler);

        HttpEntity entity = mock(HttpEntity.class);
        String exceptionMessage = UUID.randomUUID().toString();
        when(entity.getContent()).thenAnswer((Answer<InputStream>) invocation -> {
            throw new Throwable(exceptionMessage);
        });

        HttpResponse httpResponse = createDefaultTestHttpResponse(200, UUID.randomUUID().toString());
        when(httpResponse.getEntity()).thenReturn(entity);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(resultHandler).failed(exceptionCaptor.capture());
        assertEquals("Problem during request processing", exceptionCaptor.getValue().getMessage());
        assertEquals(exceptionMessage, exceptionCaptor.getValue().getCause().getMessage());

        verify(resultHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackCallsResultHandlerWhenCancelled() throws IOException {

        // given
        JestResultHandler<JestResult> resultHandler = createMockTestResultHandler();

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(resultHandler);

        // when
        asyncCallback.cancelled();

        // then
        verify(resultHandler).failed(exceptionCaptor.capture());
        assertEquals("Request cancelled", exceptionCaptor.getValue().getMessage());

        verify(resultHandler, never()).completed(any());

    }

    @Test
    public void executeAsyncCallbackHandlesBufferedJestResult() throws IOException {

        // given
        JestResultHandler<JestResult> jestResultHandler = createMockTestResultHandler();

        final BufferedBulkResult bulkResult = new BufferedBulkResult(0, false, null, 200, null);
        final BufferedBulk bulk = mock(BufferedBulk.class);
        when(bulk.deserializeResponse(any())).thenReturn(bulkResult);

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(jestResultHandler, bulk);

        String reasonPhrase = UUID.randomUUID().toString();
        HttpResponse httpResponse = createDefaultTestHttpResponse(200, reasonPhrase);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(jestResultHandler, never()).failed(any());
        verify(jestResultHandler).completed(captor.capture());
        JestResult jestResult = captor.getValue();

        assertTrue(jestResult.isSucceeded());
        assertEquals(200, jestResult.getResponseCode());
        assertEquals(reasonPhrase, jestResult.getErrorMessage());
        assertNull(((BufferedJestResult)jestResult).getItems());

    }

    @Test
    public void executeAsyncCallbackHandlesNonSuccessfulBufferedJestResult() throws IOException {

        // given
        JestResultHandler<JestResult> jestResultHandler = createMockTestResultHandler();

        BufferedBulkResult bufferedBulkResult = spy(new BufferedBulkResult(0, true, null, 400, new ArrayList<>()));

        final BufferedBulk bulk = mock(BufferedBulk.class);
        when(bulk.deserializeResponse(any())).thenReturn(bufferedBulkResult);

        BufferedJestHttpClient.BufferedResultCallback asyncCallback = mockHttpResponseCallback(jestResultHandler, bulk);

        int expectedStatusCode = new Random().nextInt(1000) + 1000;
        String reasonPhrase = UUID.randomUUID().toString();
        HttpResponse httpResponse = createDefaultTestHttpResponse(expectedStatusCode, reasonPhrase);

        // when
        asyncCallback.completed(httpResponse);

        // then
        verify(jestResultHandler, never()).failed(any());
        verify(jestResultHandler).completed(captor.capture());
        JestResult jestResult = captor.getValue();

        assertFalse(jestResult.isSucceeded());
        assertEquals(expectedStatusCode, jestResult.getResponseCode());
        assertNotEquals(httpResponse.getStatusLine().getReasonPhrase(), jestResult.getErrorMessage());
        assertNotNull(((BufferedJestResult)jestResult).getItems());

    }

    private HttpResponse createDefaultTestHttpResponse(int statusCode, String reasonPhrase) {

        HttpEntity httpEntity = mock(HttpEntity.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, reasonPhrase));
        return httpResponse;
    }

    private BufferedJestHttpClient.BufferedResultCallback mockHttpResponseCallback(JestResultHandler<JestResult> jestResultHandler) throws IOException {
        return mockHttpResponseCallback(jestResultHandler, mock(BufferedBulk.class));
    }

    private BufferedJestHttpClient.BufferedResultCallback mockHttpResponseCallback(JestResultHandler<JestResult> jestResultHandler, BufferedBulk bulk) throws IOException {
        BufferedJestHttpClient client = spy(createDefaultTestHttpClient());
        CloseableHttpAsyncClient asyncClient1 = mock(CloseableHttpAsyncClient.class);
        when(client.getAsyncClient()).thenReturn(asyncClient1);
        CloseableHttpAsyncClient asyncClient = asyncClient1;

        when(bulk.getURI()).thenReturn(UUID.randomUUID().toString());
        when(bulk.serializeRequest()).thenReturn(mock(ByteBuf.class));

        client.executeAsync(bulk, jestResultHandler);
        verify(asyncClient).execute(any(HttpUriRequest.class), bufferedResultCallbackCaptor.capture());
        return bufferedResultCallbackCaptor.getValue();
    }

    private BufferedBulk createDefaultTestBufferedBulk() {
        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        return createTestBatch(payload1, payload2);
    }

    private JestResultHandler<JestResult> createMockTestResultHandler() {
        return spy(new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
            }

            @Override
            public void failed(Exception ex) {
            }
        });
    }

    private BufferedJestHttpClient createDefaultTestHttpClient() {
        BufferedJestHttpObjectFactory.Builder testObjectFactoryBuilder =
                BufferedJestHttpObjectFactoryTest.createTestObjectFactoryBuilder();
        return (BufferedJestHttpClient) testObjectFactoryBuilder.build().createClient();
    }

    private ItemSource<ByteBuf> createDefaultTestItemSource(String payload) {
        CompositeByteBuf buffer = createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return createTestItemSource(buffer, source -> {});
    }

    private BufferedBulk createTestBatch(ItemSource<ByteBuf>... payloads) {
        BufferedBulk.Builder builder = spy(new BufferedBulk.Builder());
        builder.withBuffer(createTestItemSource());

        builder.withObjectWriter(createDefaultTestObjectWriter());
        builder.withObjectReader(createDefaultTestObjectReader());

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.addAction(spy(new BufferedIndex.Builder(payload))
                    .index(TEST_INDEX_NAME)
                    .build()
            );
        }
        return spy(builder.build());
    }

    private Bulk createTestBatch(BufferedBulk.Builder builder, ItemSource<ByteBuf>... payloads) {

        builder.withObjectWriter(createDefaultTestObjectWriter());
        builder.withObjectReader(createDefaultTestObjectReader());

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.addAction(spy(new BufferedIndex.Builder(payload))
                    .index(UUID.randomUUID().toString())
                    .build());
        }
        return spy(builder.build());
    }

    private ObjectWriter createDefaultTestObjectWriter() {
        return new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredWriter();
    }

    private ObjectReader createDefaultTestObjectReader() {
        return new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredReader();
    }
}
