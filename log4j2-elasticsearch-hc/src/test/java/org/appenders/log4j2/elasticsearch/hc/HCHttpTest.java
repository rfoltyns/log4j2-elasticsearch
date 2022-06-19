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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.hc.failover.HCFailedItemOps;
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
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.hc.BatchRequestTest.createTestBatch;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
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

public class HCHttpTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    public static HCHttp.Builder createDefaultHttpObjectFactoryBuilder() {

        PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();


        return new HCHttp.Builder()
                .withOperationFactory(new ElasticsearchOperationFactory(step -> Result.SUCCESS, ValueResolver.NO_OP))
                .withBatchOperations(new HCBatchOperations(itemSourceFactory, new ElasticsearchBulkAPI(null)))
                .withClientProvider(HttpClientProviderTest.createDefaultTestClientProvider());

    }

    @Test
    public void builderThrowsIfBatchOperationsIsNotProvided() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withBatchOperations(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + BatchOperations.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderThrowsIfBackoffPolicyIsNotProvided() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withBackoffPolicy(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + BackoffPolicy.NAME + " provided"));

    }

    @Test
    public void builderThrowsIfClientProviderIsNull() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + ClientProvider.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderThrowsIfOperationFactoryIsNotProvided() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withOperationFactory(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + OperationFactory.class.getSimpleName() + " provided"));

    }

    @Test
    public void builderFallsBackToDefaultIfFailedItemOpsIsNull() {

        // given
        HCHttp.Builder builder = spy(createDefaultHttpObjectFactoryBuilder());
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
        HCHttp.Builder builder = spy(createDefaultHttpObjectFactoryBuilder());
        FailedItemOps<IndexRequest> failedItemOps = new HCFailedItemOps();
        builder.withFailedItemOps(failedItemOps);

        verify(builder, never()).createFailedItemOps();

        // when
        HCHttp objectFactory = builder.build();

        // then
        assertSame(failedItemOps, objectFactory.failedItemOps);

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withServerList(split("http://localhost:9200;http://localhost:9201;http://localhost:9202")));

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder().withClientProvider(clientProvider);
        ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();

        // then
        assertNotSame(serverUrisList, config.getServerList());

    }

    @Test
    public void returnsConfiguredBatchOperations() {

        // given
        BatchOperations<BatchRequest> expectedBatchOperations = mock(BatchOperations.class);
        HCHttp factory = spy(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(expectedBatchOperations)
                .build());

        // when
        BatchOperations<BatchRequest> batchOperation = factory.createBatchOperations();

        // then
        assertSame(expectedBatchOperations, batchOperation);

    }

    @Test
    public void clientProviderIsPassedToCreatedObject() throws IllegalArgumentException {

        // given
        HttpClientProvider expectedClientProvider = HttpClientProviderTest.createDefaultTestClientProvider();

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(expectedClientProvider);

        // when
        HCHttp objectFactory = builder.build();

        // then
        assertEquals(expectedClientProvider, objectFactory.clientProvider);

    }

    @Test
    public void authIsAppliedIfConfigured() {

        // given
        Auth<HttpClientFactory.Builder> auth = new Security(new BasicCredentials("admin", "changeme"), null);

        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withAuth(auth));

        HCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        // when
        factory.createClient();

        //then
        assertNotNull(factory.clientProvider.getHttpClientFactoryBuilder().defaultCredentialsProvider);

    }

    @Test
    public void authIsNotAppliedIfNull() {

        // given
        final HttpClientProvider clientProvider = new HttpClientProvider(createDefaultTestHttpClientFactoryBuilder()
                .withAuth(null));

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder().withClientProvider(clientProvider);

        HCHttp factory = builder.build();

        assertNull(builder.clientProvider.getHttpClientFactoryBuilder().defaultCredentialsProvider);

        // when
        factory.createClient();

        //then
        assertNull(factory.clientProvider.getHttpClientFactoryBuilder().defaultCredentialsProvider);

    }

    @Test
    public void resultHandlerUsesConfiguredResponseDeserializer() throws IOException {

        // given
        final PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        final Deserializer<BatchResult> deserializer = mock(Deserializer.class);
        final HCBatchOperations batchOperations = new HCBatchOperations(itemSourceFactory, new ElasticsearchBulkAPI(null) {
            @Override
            protected Deserializer<BatchResult> createResultDeserializer() {
                return deserializer;
            }
        });
        final HCHttp factory = new HCHttp(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(batchOperations));

        final ResponseHandler<BatchResult> resultHandler = factory.createResultHandler(
                batchOperations.createBatchBuilder().build(),
                batchRequest -> true
        );

        final InputStream inputStream = mock(InputStream.class);

        // when
        resultHandler.deserializeResponse(inputStream);

        // then
        verify(deserializer).read(eq(inputStream));
    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        ItemSource<ByteBuf> source1 = createDefaultTestItemSource(payload1);
        ItemSource<ByteBuf> source2 = createDefaultTestItemSource(payload2);
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, source1, source2);

        // when
        config.createFailureHandler(failoverPolicy).apply(request);

        // then
        ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        assertTrue(captor.getAllValues().get(0).getSource().equals(source1.getSource()));
        assertTrue(captor.getAllValues().get(1).getSource().equals(source2.getSource()));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(batchRequest);

        // then
        ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(mockedHttpClient, times(1)).executeAsync(captor.capture(), Mockito.any());

        assertEquals(batchRequest, captor.getValue());
    }

    @Test
    public void batchListenerExecutesOperationsIfOperationsAvailable() throws Exception {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        Operation operation = spy(new Operation() {
            @Override
            public void execute() {
            }
        });

        config.addOperation(operation);

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(request);

        // then
        verify(operation).execute();

    }

    @Test
    public void batchListenerOperationExceptionIsNotPropagated() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        ClientObjectFactory<HttpClient, BatchRequest> config = spy(builder.build());

        HttpClient mockedHttpClient = mock(HttpClient.class);
        when(config.createClient()).thenReturn(mockedHttpClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<BatchRequest, Boolean> listener = config.createBatchListener(failoverPolicy);

        AtomicInteger callCount = new AtomicInteger();
        Operation operation = spy(new Operation() {
            @Override
            public void execute() throws Exception {
                callCount.incrementAndGet();
                throw new Exception("test exception");
            }
        });

        config.addOperation(operation);

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        mockTestLogger();

        // when
        listener.apply(request);

        setLogger(null);

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        HCHttp config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        ResponseHandler<BatchResult> responseHandler = config.createResultHandler(request, failoverHandler);

        BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

        // then
        ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(request, times(1)).completed();

        assertEquals(request, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        HCHttp config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        ResponseHandler<BatchResult> responseHandler = config.createResultHandler(batchRequest, failoverHandler);

        BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        responseHandler.completed(result);

        // then
        verify(failoverHandler, never()).apply(Mockito.any(BatchRequest.class));
    }

    @Test
    public void failoverIsExecutedAfterFailedRequest() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        HCHttp config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestItemSource("test2");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        ResponseHandler<BatchResult> responseHandler = config.createResultHandler(batchRequest, failoverHandler);

        mockTestLogger();

        // when
        responseHandler.failed(new IOException());

        setLogger(null);

        // then
        ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(batchRequest, times(1)).completed();

        assertEquals(batchRequest, captor.getValue());

    }

    @Test
    public void failureHandlerDoesNotThrowOnFailoverException() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        HCHttp objectFactory = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        Function<BatchRequest, Boolean> failoverHandler = objectFactory.createFailureHandler(failedPayload -> {
            throw new ClassCastException("test exception");
        });

        ResponseHandler<BatchResult> responseHandler = objectFactory.createResultHandler(batchRequest, failoverHandler);

        BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        mockTestLogger();

        // when
        responseHandler.completed(result);

        setLogger(null);

        // then
        verify(batchRequest, times(1)).completed();

    }

    @Test
    public void failoverHandlerIsExecutedImmediatelyIfBackoffPolicyShouldApply() {

        // given
        TestBackoffPolicy<BatchRequest> backoffPolicy = new TestBackoffPolicy<BatchRequest>() {
            @Override
            public boolean shouldApply(BatchRequest data) {
                return true;
            }
        };

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);

        HCHttp config = spy(builder.build());
        when(config.createFailureHandler(eq(failoverPolicy))).thenReturn(failoverHandler);

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        Function<BatchRequest, Boolean> batchListener = config.createBatchListener(failoverPolicy);

        // when
        batchListener.apply(batchRequest);

        // then
        ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify(batchRequest, times(1)).completed();

        assertEquals(batchRequest, captor.getValue());

    }

    @Test
    public void failoverHandlerIsNotExecutedImmediatelyIfBackoffPolicyShouldNotApply() {

        // given
        TestBackoffPolicy<BatchRequest> backoffPolicy = spy(new TestBackoffPolicy<BatchRequest>() {
            @Override
            public boolean shouldApply(BatchRequest data) {
                return false;
            }
        });

        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        when(clientProvider.createClient()).thenReturn(mock(HttpClient.class));

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy)
                .withClientProvider(clientProvider);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);

        HCHttp config = spy(builder.build());
        when(config.createFailureHandler(eq(failoverPolicy))).thenReturn(failoverHandler);

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        Function<BatchRequest, Boolean> batchListener = config.createBatchListener(failoverPolicy);

        config.start();

        // when
        batchListener.apply(batchRequest);

        // then
        ArgumentCaptor<BatchRequest> captor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(backoffPolicy, times(1)).register(captor.capture());
        verify(batchRequest, never()).completed();

        assertEquals(batchRequest, captor.getValue());

    }


    @Test
    public void failureHandlerDeregistersRequestFromBackoffPolicyAfterException() {

        // given
        BackoffPolicy<BatchRequest> backoffPolicy = mock(BackoffPolicy.class);
        HCHttp objectFactory = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy)
                .build();

        ItemSource<ByteBuf> payload1 = createDefaultTestItemSource("test1");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<BatchRequest, Boolean> failoverHandler = objectFactory.createFailureHandler(failoverPolicy);

        ResponseHandler<BatchResult> responseHandler = objectFactory.createResultHandler(batchRequest, failoverHandler);

        BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

        // then
        verify(backoffPolicy, times(1)).deregister(eq(batchRequest));

    }

    @Test
    public void clientProviderStartMayBeDeferredUntilFirstBatch() {

        // given
        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        when(clientProvider.createClient()).thenReturn(mock(HttpClient.class));

        HCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        factory.start();

        /* sanity check */
        assertTrue(mockingDetails(clientProvider).getInvocations().isEmpty());

        Function<BatchRequest, Boolean> batchListener = factory.createBatchListener(mock(FailoverPolicy.class));

        // when
        batchListener.apply(mock(BatchRequest.class));

        // then
        verify(clientProvider, times(1)).start();

    }

    @Test
    public void setupOperationFactoryReturnsTheSameInstance() {

        // given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        // when
        OperationFactory operationFactory1 = factory.setupOperationFactory();
        OperationFactory operationFactory2 = factory.setupOperationFactory();

        // then
        assertSame(operationFactory1, operationFactory2);

    }

    private ItemSource<ByteBuf> createDefaultTestItemSource(String payload) {
        CompositeByteBuf buffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return createTestItemSource(buffer, source -> {});
    }

    @Test
    public void lifecycleStartStartBatchOperationsOnlyOnce() {

        // given
        HCBatchOperations batchOperations = mock(HCBatchOperations.class);
        when(batchOperations.isStarted()).thenAnswer(trueOnlyOnce());

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withBatchOperations(batchOperations)
                .build()
        );

        HttpClient client = mock(HttpClient.class);
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
        HCBatchOperations batchOperations = mock(HCBatchOperations.class);

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
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
        ElasticsearchOperationFactory operationFactory = mock(ElasticsearchOperationFactory.class);
        when(operationFactory.isStarted()).thenAnswer(trueOnlyOnce());

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withOperationFactory(operationFactory)
                .build()
        );

        HttpClient client = mock(HttpClient.class);
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
        ElasticsearchOperationFactory operationFactory = mock(ElasticsearchOperationFactory.class);
        when(operationFactory.isStarted()).thenAnswer(trueOnlyOnce());

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
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
        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
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
        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
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
        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        HCHttp objectFactory = createDefaultHttpObjectFactoryBuilder()
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
        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
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

        ((HCHttp)lifeCycle).createClient();

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
        public boolean shouldApply(BatchRequest data) {
            return false;
        }

        @Override
        public void register(BatchRequest data) {

        }

        @Override
        public void deregister(BatchRequest data) {

        }

    }

}
