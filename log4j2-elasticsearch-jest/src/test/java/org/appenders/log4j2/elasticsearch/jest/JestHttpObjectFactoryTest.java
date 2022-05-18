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


import io.searchbox.action.AbstractAction;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory.Builder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.createTestIndexTemplateBuilder;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

public class JestHttpObjectFactoryTest {

    private static final int TEST_CONNECTION_TIMEOUT = 1111;
    private static final int TEST_READ_TIMEOUT = 2222;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final int TEST_MAX_TOTAL_CONNECTIONS = 11;
    private static final int TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE = 22;
    private static final boolean TEST_DISCOVERY_ENABLED = true;
    private static final int TEST_IO_THREAD_COUNT = 4;

    public static Builder createTestObjectFactoryBuilder() {
        Builder builder = JestHttpObjectFactory.newBuilder();
        builder.withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void builderThrowsIfServerUrisStringIsNull() {

        // given
        Builder builder = createTestObjectFactoryBuilder()
                .withServerUris(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No serverUris provided"));

    }

    @Test
    public void builderThrowsIfBackoffPolicyIsNotProvided() {

        // given
        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + BackoffPolicy.NAME + " provided"));

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withServerUris("http://localhost:9200;http://localhost:9201;http://localhost:9202");
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList, config.getServerList());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        JestClient client1 = factory.createClient();
        JestClient client2 = factory.createClient();

        // then
        assertEquals(client1, client2);

    }

    @Test
    public void httpParamsArePassedToCreatedObject() throws IllegalArgumentException, IllegalAccessException {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withConnTimeout(TEST_CONNECTION_TIMEOUT);
        builder.withReadTimeout(TEST_READ_TIMEOUT);
        builder.withMaxTotalConnection(TEST_MAX_TOTAL_CONNECTIONS);
        builder.withDefaultMaxTotalConnectionPerRoute(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE);
        builder.withDiscoveryEnabled(TEST_DISCOVERY_ENABLED);
        builder.withIoThreadCount(TEST_IO_THREAD_COUNT);

        // when
        JestHttpObjectFactory httpObjectFactory = spy(builder.build());
        httpObjectFactory.createClient();

        // then
        assertEquals(TEST_CONNECTION_TIMEOUT,
                org.powermock.api.mockito.PowerMockito.field(httpObjectFactory.getClass(), "connTimeout").get(httpObjectFactory));
        assertEquals(TEST_READ_TIMEOUT,
                PowerMockito.field(httpObjectFactory.getClass(), "readTimeout").get(httpObjectFactory));
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS,
                PowerMockito.field(httpObjectFactory.getClass(), "maxTotalConnections").get(httpObjectFactory));
        assertEquals(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE,
                PowerMockito.field(httpObjectFactory.getClass(), "defaultMaxTotalConnectionsPerRoute").get(httpObjectFactory));
        assertEquals(TEST_DISCOVERY_ENABLED,
                PowerMockito.field(httpObjectFactory.getClass(), "discoveryEnabled").get(httpObjectFactory));
        assertEquals(TEST_IO_THREAD_COUNT,
                PowerMockito.field(httpObjectFactory.getClass(), "ioThreadCount").get(httpObjectFactory));

    }

    @Test
    public void authIsAppliedIfConfigured() {

        // given
        Auth auth = mock(Auth.class);

        JestHttpObjectFactory factory = createTestObjectFactoryBuilder()
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
        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withValueResolver(null);

        // when
        JestHttpObjectFactory factory = builder.build();

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

        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withValueResolver(null)
                .withConfiguration(configuration);

        JestHttpObjectFactory factory = builder.build();

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

        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withConfiguration(mock(Configuration.class))
                .withValueResolver(valueResolver);

        JestHttpObjectFactory factory = builder.build();

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
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = new Bulk.Builder()
                .addAction(spy(new Index.Builder(payload1)).build())
                .addAction(spy(new Index.Builder(payload2)).build())
                .build();

        // when
        config.createFailureHandler(failoverPolicy).apply(bulk);

        // then
        ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        assertTrue(captor.getAllValues().get(0).getSource().equals(payload1));
        assertTrue(captor.getAllValues().get(1).getSource().equals(payload2));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        // when
        listener.apply(bulk);

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(mockedJestClient, times(1)).executeAsync(captor.capture(), Mockito.any());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void batchListenerExecutesOperationsIfOperationsAvailable() throws Exception {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        Operation operation = spy(new Operation() {
            @Override
            public void execute() {
            }
        });

        config.addOperation(operation);

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        // when
        listener.apply(bulk);

        // then
        verify(operation).execute();

    }

    @Test
    public void batchListenerOperationExceptionIsNotPropagated() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        AtomicInteger callCount = new AtomicInteger();
        Operation operation = spy(new Operation() {
            @Override
            public void execute() throws Exception {
                callCount.incrementAndGet();
                throw new Exception("test exception");
            }
        });

        config.addOperation(operation);

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        // when
        listener.apply(bulk);

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        resultHandler.completed(result);

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        resultHandler.completed(result);

        // then
        verify(failoverHandler, never()).apply(Mockito.any(Bulk.class));
    }

    @Test
    public void failoverIsExecutedAfterFailedRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        // when
        resultHandler.failed(new IOException());

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverHandlerIsExecutedImmediatelyIfBackoffPolicyShouldApply() {

        // given
        BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        when(backoffPolicy.shouldApply(any())).thenReturn(true);

        Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(backoffPolicy);

        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        Bulk bulk = createTestBatch(payload1);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        // when
        listener.apply(bulk);

        // then
        ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, times(1)).deliver(captor.capture());

        assertEquals(payload1, captor.getValue().getSource());

    }

    @Test
    public void failoverHandlerIsNotExecutedImmediatelyIfBackoffPolicyShouldNotApply() {

        // given
        BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        when(backoffPolicy.shouldApply(any())).thenReturn(false);

        Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(backoffPolicy);
        final JestHttpObjectFactory factory = new JestHttpObjectFactory(builder) {
            @Override
            public JestClient createClient() {
                return new JestClient() {

                    @Override
                    public void close() throws IOException {

                    }

                    @Override
                    public <T extends JestResult> T execute(Action<T> clientRequest) throws IOException {
                        return null;
                    }

                    @Override
                    public <T extends JestResult> void executeAsync(Action<T> clientRequest, JestResultHandler<? super T> jestResultHandler) {

                    }

                    @Override
                    public void shutdownClient() {

                    }

                    @Override
                    public void setServers(Set<String> servers) {

                    }
                };
            }
        };

        String payload1 = "test1";
        Bulk bulk = createTestBatch(payload1);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);
        Function<Bulk, Boolean> listener = factory.createBatchListener(failoverPolicy);

        // when
        listener.apply(bulk);

        // then
        ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, never()).deliver(captor.capture());

    }

    @Test
    public void responseHandlerDeregistersRequestFromBackoffPolicyAfterException() {

        // given
        BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(backoffPolicy);

        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        Bulk bulk = createTestBatch(payload1);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);

        JestResultHandler<JestResult> responseHandler = config.createResultHandler(bulk, config.createFailureHandler(failoverPolicy));

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

        // then
        verify(backoffPolicy, times(1)).deregister(eq(bulk));

    }

    @Test
    public void responseHandlerDeregistersRequestFromBackoffPolicyAfterSuccess() {

        // given
        BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(backoffPolicy);

        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        Bulk bulk = createTestBatch(payload1);

        FailoverPolicy failoverPolicy = mock(FailoverPolicy.class);

        JestResultHandler<JestResult> responseHandler = config.createResultHandler(bulk, config.createFailureHandler(failoverPolicy));

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        responseHandler.completed(result);

        // then
        verify(backoffPolicy, times(1)).deregister(eq(bulk));

    }

    @Test
    public void setupOpsReturnsTheSameInstance() {

        // given
        JestHttpObjectFactory factory = Mockito.spy(createTestObjectFactoryBuilder().build());

        // when
        OperationFactory operationFactory1 = factory.setupOperationFactory();
        OperationFactory operationFactory2 = factory.setupOperationFactory();

        // then
        assertSame(operationFactory1, operationFactory2);

    }

    // JestClient is started on first execution outside of LifeCycle scope
    // verify that no interactions took place
    @Test
    public void lifecycleStartDoesntStartClient() {

        // given
        JestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());
        when(objectFactory.isStarted()).thenAnswer(falseOnlyOnce());

        JestClient client = mock(JestClient.class);
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
        JestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());

        JestClient client = mock(JestClient.class);
        ClientProvider<JestClient> clientProvider = () -> client;
        when(objectFactory.getClientProvider(any())).thenReturn(clientProvider);

        objectFactory.start();

        objectFactory.createClient();

        int expectedInteractions = mockingDetails(client).getInvocations().size();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(client).shutdownClient();
        assertEquals(expectedInteractions + 1, mockingDetails(client).getInvocations().size());

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
        return createTestObjectFactoryBuilder().build();
    }

    private Bulk createTestBatch(String... payloads) {
        io.searchbox.core.Bulk.Builder builder = spy(new Bulk.Builder());
        for (String payload : payloads) {
            builder.addAction(spy(new Index.Builder(payload)).build());
        }
        return builder.build();
    }

}
