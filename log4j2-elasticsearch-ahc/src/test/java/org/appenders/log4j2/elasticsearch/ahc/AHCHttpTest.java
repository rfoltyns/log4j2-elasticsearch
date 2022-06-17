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

import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonSerializerTest;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.ahc.failover.HCFailedItemOps;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.log4j2.elasticsearch.ahc.BatchRequestTest.createTestBatch;
import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.trueOnlyOnce;
import static org.appenders.log4j2.elasticsearch.util.SplitUtil.split;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AHCHttpTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    public static AHCHttp.Builder createDefaultHttpObjectFactoryBuilder() {

        final PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        return new AHCHttp.Builder()
                .withOperationFactory(new ElasticsearchOperationFactory(step -> Result.SUCCESS, ValueResolver.NO_OP))
                .withBatchOperations(new AHCBatchOperations(itemSourceFactory, new ElasticsearchBulkAPI(null)))
                .withClientProvider(HttpClientProviderTest.createDefaultTestClientProvider());

    }

    @Test
    public void builderThrowsIfBatchOperationsIsNotProvided() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withBatchOperations(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + BatchOperations.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderThrowsIfBackoffPolicyIsNotProvided() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withBackoffPolicy(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + BackoffPolicy.NAME + " provided"));

    }

    @Test
    public void builderThrowsIfClientProviderIsNull() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + ClientProvider.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderThrowsIfOperationFactoryIsNotProvided() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withOperationFactory(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + OperationFactory.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderFallsBackToDefaultIfFailedItemOpsIsNull() {

        // given
        final AHCHttp.Builder builder = spy(createDefaultHttpObjectFactoryBuilder());
        builder.withFailedItemOps(null);

        verify(builder, never()).createFailedItemOps();

        // when
        builder.build();

        // then
        verify(builder).createFailedItemOps();

    }

    @Test
    public void builderUsesProvidedFailedItemOpsIfNotNull() {

        // given
        final AHCHttp.Builder builder = spy(createDefaultHttpObjectFactoryBuilder());
        final FailedItemOps<IndexRequest> failedItemOps = new HCFailedItemOps();
        builder.withFailedItemOps(failedItemOps);

        verify(builder, never()).createFailedItemOps();

        // when
        final AHCHttp objectFactory = builder.build();

        // then
        assertSame(failedItemOps, objectFactory.failedItemOps);

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withServerList(split("http://localhost:9200;http://localhost:9201;http://localhost:9202")));

        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder().withClientProvider(clientProvider);
        final ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        // when
        final Collection<String> serverUrisList = config.getServerList();

        // then
        assertNotSame(serverUrisList, config.getServerList());

    }

    @Test
    public void returnsConfiguredBatchOperations() {

        // given
        final BatchOperations<BatchRequest> expectedBatchOperations = mock(BatchOperations.class);
        final AHCHttp factory = spy(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(expectedBatchOperations)
                .build());

        // when
        final BatchOperations<BatchRequest> batchOperation = factory.createBatchOperations();

        // then
        assertSame(expectedBatchOperations, batchOperation);

    }

    @Test
    public void clientProviderIsPassedToCreatedObject() throws IllegalArgumentException {

        // given
        final HttpClientProvider expectedClientProvider = HttpClientProviderTest.createDefaultTestClientProvider();

        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(expectedClientProvider);

        // when
        final AHCHttp objectFactory = builder.build();

        // then
        assertEquals(expectedClientProvider, objectFactory.clientProvider);

    }

    @Test
    public void authIsAppliedIfConfigured() {

        // given
        final Auth<HttpClientFactory.Builder> auth = new Security(new BasicCredentials("admin", "changeme"), null);

        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withAuth(auth));

        final AHCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        // when
        factory.createClient();

        //then
        assertNotNull(factory.clientProvider.getHttpClientFactoryBuilder().realm);

    }

    @Test
    public void authIsNotAppliedIfNull() {

        // given
        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withAuth(null));

        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder().withClientProvider(clientProvider);

        final AHCHttp factory = builder.build();

        assertNull(builder.clientProvider.getHttpClientFactoryBuilder().realm);

        // when
        factory.createClient();

        //then
        assertNull(factory.clientProvider.getHttpClientFactoryBuilder().realm);

    }

    @Test
    public void resultHandlerUsesConfiguredDeserializer() throws IOException {

        // given
        final PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        final ObjectReader mockedObjectReader = mock(ObjectReader.class);
        final AHCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(new AHCBatchOperations(itemSourceFactory,
                        new ElasticsearchBulkAPI(
                                null,
                                JacksonSerializerTest.createDefaultTestBuilder().build(),
                                new JacksonDeserializer<>(mockedObjectReader))))
                .build();

        final BatchRequest batchRequest = factory.createBatchOperations().createBatchBuilder().build();
        final ResponseHandler<BatchResult> resultHandler = factory.createResultHandler(
                batchRequest,
                request -> true
        );

        final InputStream inputStream = mock(InputStream.class);

        // when
        resultHandler.deserializeResponse(inputStream);

        // then
        verify(mockedObjectReader).readValue(eq(inputStream));

    }

    @Test
    public void resultHandlerDoesNotThrowOnNullResponseBody() throws IOException {

        // given
        final PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        final ObjectReader mockedObjectReader = mock(ObjectReader.class);
        final AHCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(new AHCBatchOperations(itemSourceFactory,
                        new ElasticsearchBulkAPI(
                                null,
                                JacksonSerializerTest.createDefaultTestBuilder().build(),
                                new JacksonDeserializer<>(mockedObjectReader))))
                .build();

        final BatchRequest batchRequest = factory.createBatchOperations().createBatchBuilder().build();
        final ResponseHandler<BatchResult> resultHandler = factory.createResultHandler(
                batchRequest,
                request -> true
        );

        final InputStream inputStream = mock(InputStream.class);

        // when
        final BatchResult batchResult = resultHandler.deserializeResponse(null);

        // then
        assertNotNull(batchResult);
        assertFalse(batchResult.isSucceeded());
        verify(mockedObjectReader, never()).readValue(eq(inputStream));

    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        final FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());

        final String payload1 = "test1";
        final String payload2 = "test2";
        final ItemSource<ByteBuf> source1 = createDefaultTestItemSource(payload1);
        final ItemSource<ByteBuf> source2 = createDefaultTestItemSource(payload2);
        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest request = createTestBatch(batchBuilder, source1, source2);

        // when
        config.createFailureHandler(failoverPolicy).apply(request);

        // then
        final ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        assertTrue(captor.getAllValues().get(0).getSource().equals(source1.getSource()));
        assertTrue(captor.getAllValues().get(1).getSource().equals(source2.getSource()));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        final HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        final FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        final Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(batchRequest);

        // then
        final ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(mockedHttpClient, times(1)).executeAsync(captor.capture(), Mockito.any());

        assertEquals(batchRequest, captor.getValue());
    }

    @Test
    public void batchListenerExecutesOperationsIfOperationsAvailable() throws Exception {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        final HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        final FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        final Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        final Operation operation = spy(new Operation() {
            @Override
            public void execute() {
            }
        });

        config.addOperation(operation);

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(request);

        // then
        verify(operation).execute();

    }

    @Test
    public void batchListenerOperationExceptionIsNotPropagated() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        final HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        final FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        final Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        final AtomicInteger callCount = new AtomicInteger();
        final Operation operation = spy(new Operation() {
            @Override
            public void execute() throws Exception {
                callCount.incrementAndGet();
                throw new Exception("test exception");
            }
        });

        config.addOperation(operation);

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        InternalLoggingTest.mockTestLogger();

        // when
        listener.apply(request);

        setLogger(null);

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final AHCHttp config = spy(builder.build());

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        final Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        final ResponseHandler<BatchResult> responseHandler = config.createResultHandler(request, failoverHandler);

        final BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

        // then
        final ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(request, times(1)).completed();

        assertEquals(request, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final AHCHttp config = spy(builder.build());

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        final Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        final ResponseHandler<BatchResult> responseHandler = config.createResultHandler(batchRequest, failoverHandler);

        final BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        responseHandler.completed(result);

        // then
        verify(failoverHandler, never()).apply(Mockito.any(BatchRequest.class));
    }

    @Test
    public void failoverIsExecutedAfterFailedRequest() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final AHCHttp config = spy(builder.build());

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        final ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        final Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        final ResponseHandler<BatchResult> responseHandler = config.createResultHandler(batchRequest, failoverHandler);

        InternalLoggingTest.mockTestLogger();

        // when
        responseHandler.failed(new IOException());

        setLogger(null);

        // then
        final ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(batchRequest, times(1)).completed();

        assertEquals(batchRequest, captor.getValue());

    }

    @Test
    public void failureHandlerDoesNotThrowOnFailoverException() {

        // given
        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        final AHCHttp objectFactory = spy(builder.build());

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        final Function<BatchRequest, Boolean> failoverHandler = objectFactory.createFailureHandler(failedPayload -> {
            throw new ClassCastException("test exception");
        });

        final ResponseHandler<BatchResult> responseHandler = objectFactory.createResultHandler(batchRequest, failoverHandler);

        final BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        InternalLoggingTest.mockTestLogger();

        // when
        responseHandler.completed(result);

        setLogger(null);

        // then
        verify(batchRequest, times(1)).completed();

    }

    @Test
    public void failoverHandlerIsExecutedImmediatelyIfBackoffPolicyShouldApply() {

        // given
        final TestBackoffPolicy<BatchRequest> backoffPolicy = new TestBackoffPolicy<BatchRequest>() {
            @Override
            public boolean shouldApply(final BatchRequest data) {
                return true;
            }
        };

        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy);

        final FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        final Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);

        final AHCHttp config = spy(builder.build());
        when(config.createFailureHandler(eq(failoverPolicy))).thenReturn(failoverHandler);

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        final Function<BatchRequest, Boolean> batchListener = config.createBatchListener(failoverPolicy);

        // when
        batchListener.apply(batchRequest);

        // then
        final ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(batchRequest, times(1)).completed();

        assertEquals(batchRequest, captor.getValue());

    }

    @Test
    public void failoverHandlerIsNotExecutedImmediatelyIfBackoffPolicyShouldNotApply() {

        // given
        final TestBackoffPolicy<BatchRequest> backoffPolicy = spy(new TestBackoffPolicy<BatchRequest>() {
            @Override
            public boolean shouldApply(final BatchRequest data) {
                return false;
            }
        });

        final HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        when(clientProvider.createClient()).thenReturn(mock(HttpClient.class));

        final AHCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy)
                .withClientProvider(clientProvider);

        final FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        final Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);

        final AHCHttp config = spy(builder.build());
        when(config.createFailureHandler(eq(failoverPolicy))).thenReturn(failoverHandler);

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        final Function<BatchRequest, Boolean> batchListener = config.createBatchListener(failoverPolicy);

        config.start();

        // when
        batchListener.apply(batchRequest);

        // then
        final ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(backoffPolicy, times(1)).register(captor.capture());
        verify(batchRequest, never()).completed();

        assertEquals(batchRequest, captor.getValue());

    }


    @Test
    public void failureHandlerDeregistersRequestFromBackoffPolicyAfterException() {

        // given
        final BackoffPolicy<BatchRequest> backoffPolicy = mock(BackoffPolicy.class);
        final AHCHttp objectFactory = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy)
                .build();

        final ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        final BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        final BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        final FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        final Function<BatchRequest, Boolean> failoverHandler = objectFactory.createFailureHandler(failoverPolicy);

        final ResponseHandler<BatchResult> responseHandler = objectFactory.createResultHandler(batchRequest, failoverHandler);

        final BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

        // then
        verify(backoffPolicy, times(1)).deregister(eq(batchRequest));

    }

    @Test
    public void clientProviderStartMayBeDeferredUntilFirstBatch() {

        // given
        final HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        when(clientProvider.createClient()).thenReturn(mock(HttpClient.class));

        final AHCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        factory.start();

        /* sanity check */
        assertTrue(mockingDetails(clientProvider).getInvocations().isEmpty());

        final Function<BatchRequest, Boolean> batchListener = factory.createBatchListener(mock(FailoverPolicy.class));

        // when
        batchListener.apply(mock(BatchRequest.class));

        // then
        verify(clientProvider, times(1)).start();

    }

    @Test
    public void setupOperationFactoryReturnsTheSameInstance() {

        // given
        final AHCHttp factory = spy(AHCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        // when
        final OperationFactory operationFactory1 = factory.setupOperationFactory();
        final OperationFactory operationFactory2 = factory.setupOperationFactory();

        // then
        assertSame(operationFactory1, operationFactory2);

    }

    private ItemSource<ByteBuf> createDefaultTestItemSource(final String payload) {
        final CompositeByteBuf buffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return ByteBufItemSourceTest.createTestItemSource(buffer, source -> {});
    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartStartBatchOperationsOnlyOnce() {

        // given
        final AHCBatchOperations batchOperations = mock(AHCBatchOperations.class);
        when(batchOperations.isStarted()).thenAnswer(trueOnlyOnce());

        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(batchOperations)
                .build()
        );

        final HttpClient client = mock(HttpClient.class);
        when(objectFactory.createClient()).thenReturn(client);

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        verify(batchOperations).start();

    }

    @Test
    public void lifecycleStopStopsBatchOperationsOnlyOnce() {

        // given
        final AHCBatchOperations batchOperations = mock(AHCBatchOperations.class);

        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(batchOperations)
                .build());

        objectFactory.start();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(batchOperations).stop();

    }

    @Test
    public void lifecycleStartStartOperationFactoryOnlyOnce() {

        // given
        final ElasticsearchOperationFactory operationFactory = mock(ElasticsearchOperationFactory.class);
        when(operationFactory.isStarted()).thenAnswer(trueOnlyOnce());

        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withOperationFactory(operationFactory)
                .build()
        );

        final HttpClient client = mock(HttpClient.class);
        when(objectFactory.createClient()).thenReturn(client);

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        verify(operationFactory).start();

    }

    @Test
    public void lifecycleStopStopsOperationFactoryOnlyOnce() {

        // given
        final ElasticsearchOperationFactory operationFactory = mock(ElasticsearchOperationFactory.class);
        when(operationFactory.isStarted()).thenAnswer(trueOnlyOnce());

        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withOperationFactory(operationFactory)
                .build());

        objectFactory.start();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(operationFactory).stop();

    }

    @Test
    public void lifecycleStarStartsExtensions() {

        // given
        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(mock(HttpClientProvider.class))
                .build());

        // when
        objectFactory.start();

        // then
        verify(objectFactory).startExtensions();

    }

    @Test
    public void lifecycleStopStopsExtensions() {

        // given
        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(mock(HttpClientProvider.class))
                .build());

        objectFactory.start();

        // when
        objectFactory.stop();

        // then
        verify(objectFactory).stopExtensions();

    }

    @Test
    public void lifecycleStartDoesntStartClientProvider() {

        // given
        final HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        final AHCHttp objectFactory = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        assertEquals(0, mockingDetails(clientProvider).getInvocations().size());

    }

    @Test
    public void lifecycleStopStopsClientProviderOnlyOnce() {

        // given
        final HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        final AHCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build());

        objectFactory.start();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(clientProvider).stop();

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

        ((AHCHttp)lifeCycle).createClient();

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultHttpObjectFactoryBuilder().build();
    }

    private static class TestBackoffPolicy<T> implements BackoffPolicy<BatchRequest> {

        @Override
        public boolean shouldApply(final BatchRequest data) {
            return false;
        }

        @Override
        public void register(final BatchRequest data) {

        }

        @Override
        public void deregister(final BatchRequest data) {

        }

    }

}
