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
import edu.emory.mathcs.backport.java.util.Arrays;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.searchbox.action.AbstractAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutputTest;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createDefaultTestByteBuf;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.trueOnlyOnce;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BufferedJestHttpObjectFactoryTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    private static final int TEST_CONNECTION_TIMEOUT = 1111;
    private static final int TEST_READ_TIMEOUT = 2222;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final int TEST_MAX_TOTAL_CONNECTIONS = 11;
    private static final int TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE = 22;
    private static final boolean TEST_DISCOVERY_ENABLED = true;
    private static final int TEST_IO_THREAD_COUNT = 4;
    private static final String TEST_MAPPING_TYPE = UUID.randomUUID().toString();
    private static final JacksonMixIn[] TEST_MIXINS = new JacksonMixIn[4];

    public static BufferedJestHttpObjectFactory.Builder createTestObjectFactoryBuilder() {

        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        return (BufferedJestHttpObjectFactory.Builder) BufferedJestHttpObjectFactory.newBuilder()
                .withItemSourceFactory(bufferedSourceFactory)
                .withServerUris(TEST_SERVER_URIS);
    }

    @Test
    public void builderThrowsIfSourceFactoryIsNotProvided() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withItemSourceFactory(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No " + PooledItemSourceFactory.class.getSimpleName() + " configured"));

    }

    @Test
    public void builderFailsIfServerUrisStringIsNull() {

        // given
        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withServerUris(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No serverUris provided for JestHttpObjectFactory"));

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withServerUris("http://localhost:9200;http://localhost:9201;http://localhost:9202");
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList, config.getServerList());

    }


    @Test
    public void createsBufferedBulkOperationsByDefault() {

        // given
        BufferedJestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        BatchOperations<Bulk> batchOperation = factory.createBatchOperations();

        // then
        assertEquals(BufferedBulkOperations.class, batchOperation.getClass());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        BufferedJestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        JestClient client1 = factory.createClient();
        JestClient client2 = factory.createClient();

        // then
        assertEquals(client1, client2);

    }

    @Test
    public void paramsArePassedToCreatedObject() throws IllegalArgumentException, IllegalAccessException {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();

        builder.withMixIns(TEST_MIXINS)
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnection(TEST_MAX_TOTAL_CONNECTIONS)
                .withDefaultMaxTotalConnectionPerRoute(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE)
                .withDiscoveryEnabled(TEST_DISCOVERY_ENABLED)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withMappingType(TEST_MAPPING_TYPE)
                .withDataStreamsEnabled(true);

        // when
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // then
        assertEquals(TEST_CONNECTION_TIMEOUT,
                org.powermock.api.mockito.PowerMockito.field(config.getClass(), "connTimeout").get(config));
        assertEquals(TEST_READ_TIMEOUT,
                PowerMockito.field(config.getClass(), "readTimeout").get(config));
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS,
                PowerMockito.field(config.getClass(), "maxTotalConnections").get(config));
        assertEquals(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE,
                PowerMockito.field(config.getClass(), "defaultMaxTotalConnectionsPerRoute").get(config));
        assertEquals(TEST_DISCOVERY_ENABLED,
                PowerMockito.field(config.getClass(), "discoveryEnabled").get(config));
        assertEquals(TEST_IO_THREAD_COUNT,
                PowerMockito.field(config.getClass(), "ioThreadCount").get(config));
        assertEquals(TEST_MAPPING_TYPE,
                PowerMockito.field(config.getClass(), "mappingType").get(config));
        assertEquals(true,
                PowerMockito.field(config.getClass(), "dataStreamsEnabled").get(config));
        assertEquals(TEST_MIXINS,
                PowerMockito.field(config.getClass(), "mixIns").get(config));

    }

    @Test
    public void authIsAppliedIfConfigured() {

        // given
        Auth auth = mock(Auth.class);

        BufferedJestHttpObjectFactory factory = (BufferedJestHttpObjectFactory) createTestObjectFactoryBuilder()
                .withAuth(auth)
                .build();

        // when
        factory.createClient();

        //then
        verify(auth).configure(any());
    }

    @Test
    public void dataStreamsCapableBatchOperationsIsCreatedIfDataStreamsConfigured() {

        // given
        final BufferedJestHttpObjectFactory factory = (BufferedJestHttpObjectFactory) createTestObjectFactoryBuilder()
                .withDataStreamsEnabled(true)
                .build();

        final String expectedIndexName = UUID.randomUUID().toString();
        final String payload1 = "test1";
        final ItemSource<ByteBuf> source1 = createDefaultTestBufferedItemSource(payload1);

        // then
        final BatchOperations<Bulk> batchOperations = factory.createBatchOperations();
        final BufferedIndex index = (BufferedIndex) batchOperations.createBatchItem(expectedIndexName, source1);
        final BatchBuilder<Bulk> batchBuilder = batchOperations.createBatchBuilder();
        batchBuilder.add(index);
        final BufferedBulk bulk = (BufferedBulk) batchBuilder.build();

        // then
        assertEquals(expectedIndexName + "/_bulk", bulk.getURI());

    }

    @Test
    public void defaultBatchOperationsIsCreatedIfDataStreamsNotConfigured() {

        // given
        final BufferedJestHttpObjectFactory factory = createTestObjectFactoryBuilder()
                .build();

        final String expectedIndexName = UUID.randomUUID().toString();
        final String payload1 = "test1";
        final ItemSource<ByteBuf> source1 = createDefaultTestBufferedItemSource(payload1);

        // then
        final BatchOperations<Bulk> batchOperations = factory.createBatchOperations();
        final BufferedIndex index = (BufferedIndex) batchOperations.createBatchItem(expectedIndexName, source1);
        final BatchBuilder<Bulk> batchBuilder = batchOperations.createBatchBuilder();
        batchBuilder.add(index);
        final BufferedBulk bulk = (BufferedBulk) batchBuilder.build();

        // then
        assertEquals("/_bulk", bulk.getURI());

    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        ItemSource<ByteBuf> source1 = createDefaultTestBufferedItemSource(payload1);
        ItemSource<ByteBuf> source2 = createDefaultTestBufferedItemSource(payload2);
        Bulk bulk = createTestBatch(source1, source2);

        // when
        config.createFailureHandler(failoverPolicy).apply(bulk);

        // then
        ArgumentCaptor<FailedItemSource> captor = ArgumentCaptor.forClass(FailedItemSource.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        FailedItemSource<ByteBuf> item1 = captor.getAllValues().get(0);
        assertTrue(item1.getSource().toString(Charset.defaultCharset()).equals(payload1));

        FailedItemSource<ByteBuf> item2 = captor.getAllValues().get(1);
        assertTrue(item2.getSource().toString(Charset.defaultCharset()).equals(payload2));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBufferedItemSource("test2");
        Bulk bulk = createTestBatch(payload1, payload2);

        // when
        listener.apply(bulk);

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(mockedJestClient, times(1)).executeAsync(captor.capture(), Mockito.any());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBufferedItemSource("test2");
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
        verify((BufferedBulk)bulk, times(1)).completed();

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBufferedItemSource("test2");
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
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBufferedItemSource("test2");

        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        // when
        resultHandler.failed(new IOException());

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify((BufferedBulk)bulk, times(1)).completed();

        assertEquals(bulk, captor.getValue());

    }

     @Test
    public void failureHandlerDoesNotRethrowExceptions() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory objectFactory = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");

        Function<Bulk, Boolean> failoverHandler = objectFactory.createFailureHandler(failedPayload -> {
            throw new ClassCastException("test exception");
        });

        Bulk bulk = createTestBatch(payload1);
        JestResultHandler<JestResult> responseHandler = objectFactory.createResultHandler(bulk, failoverHandler);

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        responseHandler.completed(result);

    }

    @Test
    public void responseHandlerDeregistersRequestFromBackoffPolicyAfterException() {

        // given
        BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withBackoffPolicy(backoffPolicy);

        JestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
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
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withBackoffPolicy(backoffPolicy);

        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
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

    private ItemSource<ByteBuf> createDefaultTestBufferedItemSource(String payload) {
        CompositeByteBuf buffer = createDefaultTestByteBuf();
        buffer.writeBytes(payload.getBytes());
        return createTestItemSource(buffer, source -> {});
    }

    @Test
    public void lifecycleStartStartItemSourceFactoryOnlyOnce() {

        // given
        PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        when(itemSourceFactory.isStarted()).thenAnswer(trueOnlyOnce());

        BufferedJestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        JestClient client = mock(JestClient.class);
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

        BufferedJestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        JestClient client = mock(JestClient.class);
        ClientProvider<JestClient> clientProvider = () -> client;
        when(objectFactory.getClientProvider(any())).thenReturn(clientProvider);

        objectFactory.start();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(itemSourceFactory).stop();

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

    // =======
    // METRICS
    // =======

    @Test
    public void registersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "noop");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "noop");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "noop");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "noop");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withName(expectedComponentName);

        final JestHttpObjectFactory config = spy(builder.build());

        // when
        config.register(registry);

        // then
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey4)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey5)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey6)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());

    }

    @Test
    public void deregistersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "noop");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "noop");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "noop");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "noop");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withName(expectedComponentName);

        final JestHttpObjectFactory config = spy(builder.build());
        config.register(registry);

        // when
        config.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey4)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey5)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey6)).size());
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());

    }

    @Test
    public void registersMeasuredComponentsWithMetricsRegistry() {

        // given
        final MetricsRegistry registry = mock(MetricsRegistry.class);

        final PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        when(itemSourceFactory.isStopped()).thenAnswer(falseOnlyOnce());

        final BufferedJestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        // when
        clientObjectFactory.register(registry);

        // then
        verify(itemSourceFactory).register(eq(registry));

    }

    @Test
    public void deregistersMeasuredComponentsWithMetricsRegistry() {

        // given
        final PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        when(itemSourceFactory.isStopped()).thenAnswer(falseOnlyOnce());

        final BufferedJestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder()
                .withItemSourceFactory(itemSourceFactory).build());

        // when
        clientObjectFactory.deregister();

        // then
        verify(itemSourceFactory).deregister();

    }

    @Test
    public void enablesAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "failoverTookMs", "max");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "count");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(JestHttpObjectFactory.metricConfigs(true))
                .build();

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        // when
        config.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey6), eq(0L));

    }

    @Test
    public void configuresSubSetOfMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "max");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "count");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();

        final JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withName(expectedComponentName);

        //noinspection unchecked
        builder.withMetricConfigs(Arrays.asList(new MetricConfig[] {
                MetricConfigFactory.createCountConfig("itemsDelivered"),
                MetricConfigFactory.createCountConfig("itemsFailed"),
                MetricConfigFactory.createMaxConfig("itemsSent", false),
                MetricConfigFactory.createCountConfig("backoffApplied")
        }));

        final JestHttpObjectFactory config = spy(builder.build());

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        // when
        config.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput, never()).write(anyLong(), eq(expectedKey1), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        verify(metricOutput, never()).write(anyLong(), eq(expectedKey6), eq(0L));

    }

    @Test
    public void storesItemsSent() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "itemsSent", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("itemsSent"));
        when(config.createClient()).thenReturn(mock(JestClient.class));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        // when
        config.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq(1L));

    }

    @Test
    public void storesItemsDelivered() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "itemsDelivered", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("itemsDelivered"));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        final JestResultHandler<JestResult> resultHandler = config.createResultHandler(batchRequest, config.createFailureHandler(new NoopFailoverPolicy.Builder().build()));
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        resultHandler.completed(result);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq(1L));

    }

    @Test
    public void storesItemsFailed() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "itemsFailed", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("itemsFailed"));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        final JestResultHandler<JestResult> resultHandler = config.createResultHandler(batchRequest, config.createFailureHandler(new NoopFailoverPolicy.Builder().build()));
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        resultHandler.completed(result);
        resultHandler.failed(new Exception("test-exception"));
        metricProcessor.process();
        resultHandler.failed(new Exception("test-exception"));
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 1));
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 1));

    }

    @Test
    public void storesBackoffApplied() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "backoffApplied", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        @SuppressWarnings("unchecked")
        final BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = mock(BackoffPolicy.class);
        final JestHttpObjectFactory config = spy(createTestObjectFactoryBuilderWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("backoffApplied"))
                .withBackoffPolicy(backoffPolicy)
                .build());
        when(config.createClient()).thenReturn(mock(JestClient.class));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        when(backoffPolicy.shouldApply(eq(batchRequest))).thenReturn(true);

        // when
        config.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        config.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();
        config.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 2));
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 1));

    }

    @Test
    public void storesBatchesFailed() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "batchesFailed", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("batchesFailed"));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        final JestResultHandler<JestResult> resultHandler = config.createResultHandler(batchRequest, config.createFailureHandler(new NoopFailoverPolicy.Builder().build()));
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        resultHandler.completed(result);
        resultHandler.failed(new Exception("test-exception"));
        metricProcessor.process();
        resultHandler.failed(new Exception("test-exception"));
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 2));
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) 1));

    }

    @Test
    public void storesFailoverTookMs() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "failoverTookMs", "max");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final JestHttpObjectFactory config = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("failoverTookMs"));

        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final ItemSource<ByteBuf> payload1 = createDefaultTestBufferedItemSource("test1");
        final Bulk batchRequest = createTestBatch(payload1);

        final JestResultHandler<JestResult> resultHandler = config.createResultHandler(batchRequest, config.createFailureHandler(failedPayload -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        config.register(registry);

        final JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        resultHandler.completed(result);
        metricProcessor.process();

        // then
        final ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(long.class);
        verify(metricOutput).write(anyLong(), eq(expectedKey), captor.capture());

        assertTrue(captor.getValue() >= 50L);

    }

    private JestHttpObjectFactory createTestObjectFactoryWithMetric(final String expectedComponentName, final MetricConfig metricConfig) {
        final JestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfig(metricConfig);

        return spy(builder.build());
    }

    private JestHttpObjectFactory.Builder createTestObjectFactoryBuilderWithMetric(final String expectedComponentName, final MetricConfig metricConfig) {
        return createTestObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfig(metricConfig);
    }

    private LifeCycle createLifeCycleTestObject() {
        return createTestObjectFactoryBuilder().build();
    }

    private Bulk createTestBatch(ItemSource<ByteBuf>... payloads) {
        BufferedBulk.Builder builder = new BufferedBulk.Builder();
        builder.withBuffer(createTestItemSource(createDefaultTestByteBuf(), source -> {}));
        builder.withObjectWriter(mock(ObjectWriter.class));
        builder.withObjectReader(mock(ObjectReader.class));

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.addAction(new BufferedIndex.Builder(payload).build());
        }
        return spy(builder.build());
    }

}
