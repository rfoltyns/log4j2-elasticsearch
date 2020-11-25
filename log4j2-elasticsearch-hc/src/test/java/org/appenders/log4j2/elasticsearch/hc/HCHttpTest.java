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
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.OperationFactoryDispatcher;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.createTestIndexTemplateBuilder;
import static org.appenders.log4j2.elasticsearch.hc.BatchRequestTest.createTestBatch;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.trueOnlyOnce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final Random RANDOM = new Random();

    private static final int TEST_CONNECTION_TIMEOUT = RANDOM.nextInt(1000) + 10;
    private static final int TEST_READ_TIMEOUT = RANDOM.nextInt(1000) + 10;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final int TEST_MAX_TOTAL_CONNECTIONS = RANDOM.nextInt(1000) + 10;
    private static final int TEST_IO_THREAD_COUNT = RANDOM.nextInt(1000) + 10;
    private static final String TEST_MAPPING_TYPE = UUID.randomUUID().toString();
    private static final boolean TEST_POOLED_RESPONSE_BUFFERS_ENABLED = true;
    private static final int TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES = 34;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @After
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    public static HCHttp.Builder createDefaultHttpObjectFactoryBuilder() {

        PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        HCHttp.Builder builder = HCHttp.newBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    @Test
    public void builderThrowsIfSourceFactoryIsNotProvided() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withItemSourceFactory(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No " + PooledItemSourceFactory.class.getSimpleName() + " provided");

        // when
        builder.build();

    }

    @Test
    public void builderThrowsIfBackoffPolicyIsNotProvided() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withBackoffPolicy(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No " + BackoffPolicy.NAME + " provided");

        // when
        builder.build();

    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsIfServerUrisStringIsNull() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        String serverUris = null;

        // when
        builder.withServerUris(serverUris);
        builder.build();

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        builder.withServerUris("http://localhost:9200;http://localhost:9201;http://localhost:9202");
        ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList, config.getServerList());

    }


    @Test
    public void createsHCBatchOperationsByDefault() {

        // given
        HCHttp factory = spy(createDefaultHttpObjectFactoryBuilder().build());

        // when
        BatchOperations<BatchRequest> batchOperation = factory.createBatchOperations();

        // then
        assertEquals(HCBatchOperations.class, batchOperation.getClass());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        HCHttp factory = spy(createDefaultHttpObjectFactoryBuilder().build());

        // when
        HttpClient client1 = factory.createClient();
        HttpClient client2 = factory.createClient();

        // then
        assertEquals(client1, client2);

    }

    @Test
    public void httpParamsArePassedToCreatedObject() throws IllegalArgumentException, IllegalAccessException {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withMappingType(TEST_MAPPING_TYPE)
                .withPooledResponseBuffers(TEST_POOLED_RESPONSE_BUFFERS_ENABLED)
                .withPooledResponseBuffersSizeInBytes(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES);

        // when
        HCHttp config = builder.build();

        // then
        assertEquals(TEST_CONNECTION_TIMEOUT, config.connTimeout);
        assertEquals(TEST_READ_TIMEOUT, config.readTimeout);
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS, config.maxTotalConnections);
        assertEquals(TEST_IO_THREAD_COUNT, config.ioThreadCount);
        assertEquals(TEST_MAPPING_TYPE, config.mappingType);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_ENABLED, config.pooledResponseBuffers);
        assertEquals(TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES, config.pooledResponseBuffersSizeInBytes);

    }

    @Test
    public void authIsAppliedIfConfigured() {

        // given
        Auth auth = mock(Auth.class);

        HCHttp factory = createDefaultHttpObjectFactoryBuilder()
                .withAuth(auth)
                .build();

        // when
        factory.createClient();

        //then
        verify(auth).configure(any());
    }

    @Test
    public void defaultValueResolverIsUsedWheNoConfigurationOrValueResolverProvided()
    {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withValueResolver(null);

        // when
        HCHttp factory = builder.build();

        // then
        assertSame(ValueResolver.NO_OP, factory.valueResolver());

    }

    @Test
    public void log4j2ConfigurationBasedValueResolverIsUsedWhenConfigurationProvided()
    {

        // given
        Configuration configuration = mock(Configuration.class);
        StrSubstitutor strSubstitutor = mock(StrSubstitutor.class);
        when(strSubstitutor.replace((String)any())).thenReturn(UUID.randomUUID().toString());

        when(configuration.getStrSubstitutor()).thenReturn(strSubstitutor);

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
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
        assertTrue(factory.valueResolver() instanceof Log4j2Lookup);
        verify(strSubstitutor).replace(eq(expectedSource));

    }

    @Test
    public void providedValueResolverIsUsedWhenBothConfigurationAndValueResolverProvided()
    {

        // given
        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(anyString())).thenReturn(UUID.randomUUID().toString());

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withConfiguration(mock(Configuration.class))
                .withValueResolver(valueResolver);

        HCHttp factory = builder.build();

        assertEquals(valueResolver, factory.valueResolver());

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
    public void resultHandlerUsesGivenObjectReader() throws IOException {

        // given
        ObjectReader mockedObjectReader = mock(ObjectReader.class);
        HCHttp factory = new HCHttp(createDefaultHttpObjectFactoryBuilder()) {
            @Override
            protected ObjectReader configuredReader() {
                return mockedObjectReader;
            }
        };

        ResponseHandler<BatchResult> resultHandler = factory.createResultHandler(
                BatchRequestTest.createDefaultTestObjectBuilder().build(),
                batchRequest -> true
        );

        InputStream inputStream = mock(InputStream.class);

        // when
        resultHandler.deserializeResponse(inputStream);

        // then
        verify(mockedObjectReader).readValue(any(InputStream.class));
    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        ClientObjectFactory<HttpClient, BatchRequest> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        ItemSource<ByteBuf> source1 = createDefaultTestBuffereItemSource(payload1);
        ItemSource<ByteBuf> source2 = createDefaultTestBuffereItemSource(payload2);
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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");

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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(request);

        // then
        verify(operation).execute();

    }

    @Test
    public void batchListenerOperationExceptionIsNotPropagated() throws Exception {

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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest request = createTestBatch(batchBuilder, payload1, payload2);

        // when
        listener.apply(request);

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder();
        HCHttp config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1, payload2);

        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);
        ResponseHandler<BatchResult> responseHandler = config.createResultHandler(batchRequest, failoverHandler);

        // when
        responseHandler.failed(new IOException());

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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");

        BatchRequest.Builder batchBuilder = spy(new BatchRequest.Builder());
        BatchRequest batchRequest = createTestBatch(batchBuilder, payload1);

        Function<BatchRequest, Boolean> failoverHandler = objectFactory.createFailureHandler(failedPayload -> {
            throw new ClassCastException("test exception");
        });

        ResponseHandler<BatchResult> responseHandler = objectFactory.createResultHandler(batchRequest, failoverHandler);

        BatchResult result = mock(BatchResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");

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

        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<BatchRequest, Boolean> failoverHandler = mock(Function.class);

        HCHttp config = spy(builder.build());
        when(config.createFailureHandler(eq(failoverPolicy))).thenReturn(failoverHandler);

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");

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

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");

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
    public void passesIndexTemplateToClient() {

        //given
        HttpClient httpClient = mock(HttpClient.class);

        HCHttp factory = Mockito.spy(createTestObjectFactory(httpClient));

        AtomicReference<ByteBuf> argCaptor = new AtomicReference<>();
        responseMock(httpClient, true, argCaptor);

        IndexTemplate indexTemplate = spy(createTestIndexTemplateBuilder()
                .build());

        String expectedPayload = indexTemplate.getSource();

        // when
        factory.execute(indexTemplate);

        // then
        ArgumentCaptor<IndexTemplateRequest> requestArgumentCaptor = ArgumentCaptor.forClass(IndexTemplateRequest.class);
        verify(httpClient).execute(requestArgumentCaptor.capture(), any());

        assertEquals(argCaptor.get().toString(StandardCharsets.UTF_8), expectedPayload);

    }

    @Test
    public void executeLogsExceptions() {

        //given
        HttpClient httpClient = mock(HttpClient.class);
        HCHttp factory = Mockito.spy(createTestObjectFactory(httpClient));

        Logger logger = mockTestLogger();

        String expectedErrorMessage = UUID.randomUUID().toString();
        Exception testException = new IOException(expectedErrorMessage);

        when(httpClient.execute(any(), any())).thenAnswer(
                (Answer<Response>) invocationOnMock -> {
                    throw testException;
                });

        IndexTemplate indexTemplate = spy(IndexTemplateTest.createTestIndexTemplateBuilder()
                .withPath("classpath:indexTemplate-7.json")
                .build());

        factory.start();

        // when
        factory.execute(indexTemplate);

        // then
        verify(logger).error(eq("IndexTemplate not added"), eq(testException));

    }

    @Test
    public void errorMessageIsNotLoggedIfTemplateActionHasSucceeded() {

        //given
        HttpClient httpClient = mock(HttpClient.class);

        HCHttp factory = createTestObjectFactory(httpClient);

        Response responseMock = responseMock(httpClient, true);

        IndexTemplate indexTemplate = spy(createTestIndexTemplateBuilder().build());

        // when
        factory.execute(indexTemplate);

        // then
        verify(responseMock, never()).getErrorMessage();

    }

    @Test
    public void errorMessageIsRetrievedIfTemplateActionNotSucceeded() {

        //given
        HttpClient httpClient = mock(HttpClient.class);

        HCHttp factory = createTestObjectFactory(httpClient);

        Response responseMock = responseMock(httpClient, false);

        IndexTemplate indexTemplate = spy(createTestIndexTemplateBuilder().build());

        // when
        factory.execute(indexTemplate);

        // then
        verify(responseMock).getErrorMessage();

    }

    @Test
    public void executeDoesNotRethrowOnIndexTemplateOperationException() {

        //given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        when(factory.setupOperationFactory()).thenReturn(new OperationFactoryDispatcher() {
            {
                register(IndexTemplate.TYPE_NAME, new OperationFactory() {
                    @Override
                    public <T extends OpSource> Operation create(T opSource) {
                        return () -> {
                            throw new RuntimeException("test exception");
                        };
                    }
                });
            }
        });

        IndexTemplate indexTemplate = spy(createTestIndexTemplateBuilder().build());

        Logger logger = mockTestLogger();

        // when
        factory.execute(indexTemplate);

        // then
        verify(logger).error(eq("IndexTemplate not added"), any());

    }

    @Test
    public void executeUsesBlockingResponseHandler() {

        //given
        HttpClient httpClient = mock(HttpClient.class);

        BlockingResponseHandler<BasicResponse> blockingResponseHandler = spy(new BlockingResponseHandler<>(
                new ObjectMapper().readerFor(Response.class), response -> null));

        HCHttp factory = createTestObjectFactory(httpClient, blockingResponseHandler);

        Response responseMock = responseMock(httpClient, false);

        IndexTemplate indexTemplate = spy(createTestIndexTemplateBuilder().build());

        // when
        factory.execute(indexTemplate);

        // then
        verify(httpClient).execute(any(), eq(blockingResponseHandler));

    }

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithExceptionMessage() {

        //given
        HCHttp factory = createTestObjectFactory(mock(HttpClient.class));

        Function<Exception, BasicResponse> blockingResponseExceptionHandler = factory.createBlockingResponseFallbackHandler();

        String expectedMessage = UUID.randomUUID().toString();

        // when
        BasicResponse basicResponse = blockingResponseExceptionHandler.apply(new Exception(expectedMessage));

        // then
        assertEquals(expectedMessage, basicResponse.getErrorMessage());

    }

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithNoExceptionMessage() {

        //given
        HCHttp factory = createTestObjectFactory(mock(HttpClient.class));

        Function<Exception, BasicResponse> blockingResponseExceptionHandler = factory.createBlockingResponseFallbackHandler();

        // when
        BasicResponse basicResponse = blockingResponseExceptionHandler.apply(null);

        // then
        assertNull(basicResponse.getErrorMessage());

    }

    @Test
    public void clientStartMayBeDeferredUntilFirstBatch() {

        // given
        HttpClient httpClient = mock(HttpClient.class);
        HCHttp factory = createTestObjectFactory(httpClient);

        factory.start();

        /* sanity check */
        assertTrue(mockingDetails(httpClient).getInvocations().isEmpty());

        Function<BatchRequest, Boolean> batchListener = factory.createBatchListener(mock(FailoverPolicy.class));

        // when
        batchListener.apply(mock(BatchRequest.class));

        // then
        verify(httpClient, VerificationModeFactory.times(1)).start();

    }

    @Test
    public void setupOpsReturnsTheSameInstance() {

        // given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        // when
        OperationFactory operationFactory1 = factory.setupOperationFactory();
        OperationFactory operationFactory2 = factory.setupOperationFactory();

        // then
        assertSame(operationFactory1, operationFactory2);

    }

    private Response responseMock(HttpClient httpClient, boolean isSucceeded) {
        return responseMock(httpClient, isSucceeded, new AtomicReference<>());
    }

    private Response responseMock(HttpClient httpClient, boolean isSucceeded, AtomicReference<ByteBuf> argCaptor) {
        BatchResult result = mock(BatchResult.class);
        when(httpClient.execute(any(), any())).thenAnswer(invocation -> {
            GenericRequest request = invocation.getArgument(0);
            argCaptor.set(((ByteBuf)request.serialize().getSource()).copy());
            return result;
        });
        when(result.getErrorMessage()).thenReturn("IndexTemplate not added");

        when(result.isSucceeded()).thenReturn(isSucceeded);
        when(result.getResponseCode()).thenReturn(isSucceeded ? 200 : 0);

        return result;
    }

    private HCHttp createTestObjectFactory(HttpClient httpClient, BlockingResponseHandler<BasicResponse> blockingResponseHandler) {
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withValueResolver(ValueResolver.NO_OP);

        return Mockito.spy(new TestHCHttp(builder) {
            @Override
            public HttpClient createClient() {
                return httpClient;
            }

            @Override
            BlockingResponseHandler<BasicResponse> createBlockingResponseHandler() {
                return blockingResponseHandler;
            }
        });
    }

    private HCHttp createTestObjectFactory(HttpClient httpClient) {
        HCHttp.Builder builder = createDefaultHttpObjectFactoryBuilder()
                .withValueResolver(ValueResolver.NO_OP);

        return Mockito.spy(new TestHCHttp(builder) {
            @Override
            public HttpClient createClient() {
                return httpClient;
            }
        });
    }

    private ItemSource<ByteBuf> createDefaultTestBuffereItemSource(String payload) {
        CompositeByteBuf buffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return createTestItemSource(buffer, source -> {});
    }

    @Test
    public void lifecycleStartStartItemSourceFactoryOnlyOnce() {

        // given
        PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        when(itemSourceFactory.isStarted()).thenAnswer(trueOnlyOnce());

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        HttpClient client = mock(HttpClient.class);
        when(objectFactory.createClient()).thenReturn(client);

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        verify(itemSourceFactory).start();

    }

    @Test
    public void lifecycleStopStopsItemSourceFactoryOnlyOnce() {

        // given
        PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        when(itemSourceFactory.isStopped()).thenAnswer(falseOnlyOnce());

        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        HttpClient client = mock(HttpClient.class);
        ClientProvider<HttpClient> clientProvider = () -> client;
        when(objectFactory.getClientProvider(any())).thenReturn(clientProvider);

        objectFactory.start();
        objectFactory.createClient();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(itemSourceFactory).stop();

    }

    // HttpClient is started on first execution outside of LifeCycle scope
    // verify that no interactions took place
    @Test
    public void lifecycleStartDoesntStartClient() {

        // given
        HttpClient client = mock(HttpClient.class);
        HCHttp objectFactory = createTestObjectFactory(client);
        when(objectFactory.isStarted()).thenAnswer(falseOnlyOnce());

        when(objectFactory.createClient()).thenReturn(client);

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        assertEquals(0, mockingDetails(client).getInvocations().size());

    }

    @Test
    public void lifecycleStopStopsClientOnlyOnce() {

        // given
        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder().build());

        HttpClient client = mock(HttpClient.class);
        ClientProvider<HttpClient> clientProvider = () -> client;
        when(objectFactory.getClientProvider(any())).thenReturn(clientProvider);

        objectFactory.start();

        objectFactory.createClient();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(client).stop();

    }

    @Test
    public void lifecycleStopDoesNotStopClientIfClientNotCreated() {

        // given
        HCHttp objectFactory = spy(createDefaultHttpObjectFactoryBuilder().build());

        HttpClient client = mock(HttpClient.class);
        ClientProvider<HttpClient> clientProvider = () -> client;
        when(objectFactory.getClientProvider(any())).thenReturn(clientProvider);

        objectFactory.start();

        // when
        objectFactory.stop();

        // then
        verify(objectFactory, never()).createClient();
        verify(client, never()).stop();

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

    private class TestBackoffPolicy<T> implements BackoffPolicy<BatchRequest> {

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

    private static class TestHCHttp extends HCHttp {
        public TestHCHttp(Builder builder) {
            super(builder);
        }
    }

}
