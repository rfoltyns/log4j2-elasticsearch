package org.appenders.log4j2.elasticsearch;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.appenders.log4j2.elasticsearch.spi.TestBatchEmitterFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncBatchDeliveryPluginTest {
    private static final int TEST_BATCH_SIZE = 100;

    private static final int TEST_DELIVERY_INTERVAL = 100;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";

    public static TestHttpObjectFactory.Builder createTestObjectFactoryBuilder() {
        TestHttpObjectFactory.Builder builder = TestHttpObjectFactory.newBuilder();
        builder.withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    public static AsyncBatchDeliveryPlugin.Builder createTestBatchDeliveryBuilder() {
        return spy(AsyncBatchDeliveryPlugin.newBuilder()
                .withBatchSize(TEST_BATCH_SIZE)
                .withDeliveryInterval(TEST_DELIVERY_INTERVAL)
                .withClientObjectFactory(createTestObjectFactoryBuilder().build()))
                .withFailoverPolicy(new NoopFailoverPolicy());
    }

    /* To make testing easier and break when changed */
    private BatchDelivery<String> invokePluginFactory(AsyncBatchDelivery.Builder builder) {
        return AsyncBatchDeliveryPlugin.createAsyncBatchDelivery(
                builder.clientObjectFactory,
                builder.deliveryInterval,
                builder.batchSize,
                builder.failoverPolicy,
                builder.shutdownDelayMillis,
                builder.setupOpSources);
    }

    @Test
    public void pluginFactoryReturnsNonNullObject() {

        // given
        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        // when
        BatchDelivery<String> delivery = invokePluginFactory(batchDeliveryBuilder);

        // then
        assertNotNull(delivery);
    }

    @Test
    public void pluginFactoryFailsWhenClientObjectFactoryIsNull() {

        // given
        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withClientObjectFactory(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> invokePluginFactory(batchDeliveryBuilder));

        // then
        assertThat(exception.getMessage(),
                equalTo("No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void pluginFactoryFallsBackToDefaults() {

        // given
        Function<BulkEmitterTest.TestBatch, Boolean> listener = mock(Function.class);

        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());
        when(objectFactory.createBatchListener(any())).thenReturn(listener);

        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .withBatchSize(0)
                .withDeliveryInterval(0)
                .withShutdownDelayMillis(-1)
                .withFailoverPolicy(null)
                .withSetupOpSources();

        // when
        AsyncBatchDelivery batchDelivery = (AsyncBatchDelivery) invokePluginFactory(batchDeliveryBuilder);

        int expectedBatches = 10;
        for (int i = 0; i < AsyncBatchDelivery.Builder.DEFAULT_BATCH_SIZE * expectedBatches; i++) {
            batchDelivery.add(NoopIndexNameFormatterTest.TEST_INDEX_NAME, "test");
        }

        // then
        assertEquals(AsyncBatchDelivery.Builder.DEFAULT_FAILOVER_POLICY, batchDelivery.failoverPolicy);
        assertEquals(Arrays.asList(AsyncBatchDelivery.Builder.DEFAULT_OP_SOURCES), batchDelivery.setupOpSources);
        assertEquals(AsyncBatchDelivery.Builder.DEFAULT_SHUTDOWN_DELAY, batchDelivery.shutdownDelayMillis);

        verify(listener, times(expectedBatches)).apply(any());

    }

    @Test
    public void builderConfiguresShutdownDelayMillis() {

        // given
        long expectedShutdownDelayMillis = 10 + new Random().nextInt(100);
        FailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());

        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .withShutdownDelayMillis(expectedShutdownDelayMillis);

        BatchDelivery<String> asyncBatchDelivery = invokePluginFactory(batchDeliveryBuilder);
        asyncBatchDelivery.start();

        // when
        asyncBatchDelivery.stop();

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(LifeCycle.of(failoverPolicy)).stop(captor.capture(), anyBoolean());
        assertEquals((Long) expectedShutdownDelayMillis, captor.getValue());

    }

    @Test
    public void builderConfiguresSetupOpSources() {

        // given
        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());

        OperationFactory operationFactory = mock(OperationFactory.class);
        when(clientObjectFactory.setupOperationFactory()).thenReturn(operationFactory);

        IndexTemplate indexTemplate = mock(IndexTemplate.class);
        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withSetupOpSources(indexTemplate)
                .withClientObjectFactory(clientObjectFactory);

        BatchDelivery<String> asyncBatchDelivery = invokePluginFactory(batchDeliveryBuilder);

        // when
        asyncBatchDelivery.start();

        // then
        verify(operationFactory).create(eq(indexTemplate));
        verify(clientObjectFactory).addOperation(any());

    }

    @Test
    public void batchDeliveryAddObjectDelegatesToProvidedBatchOperationsObjectApi() {

        // given
        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery<String> batchDelivery = invokePluginFactory(batchDeliveryBuilder);

        String indexName = UUID.randomUUID().toString();
        String logObject = UUID.randomUUID().toString();

        // when
        batchDelivery.add(indexName, logObject);

        // then
        verify(batchOperations).createBatchItem(eq(indexName), eq(logObject));

    }

    @Test
    public void batchDeliveryAddItemSourceDelegatesToProvidedBatchOperationsItemSourceApi() {

        // given
        AsyncBatchDeliveryPlugin.Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery batchDelivery = invokePluginFactory(batchDeliveryBuilder);

        String indexName = UUID.randomUUID().toString();
        ItemSource itemSource = mock(ItemSource.class);

        // when
        batchDelivery.add(indexName, itemSource);

        // then
        verify(batchOperations).createBatchItem(eq(indexName), eq(itemSource));

    }

    @Test
    public void deliveryAddsBatchItemToBatchEmitter() {

        // given
        TestHttpObjectFactory objectFactory = createTestObjectFactoryBuilder().build();

        TestBatchEmitterFactory batchEmitterFactory = spy(new TestBatchEmitterFactory());

        BatchEmitter emitter = batchEmitterFactory.createInstance(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, objectFactory, new NoopFailoverPolicy());

        TestAsyncBatchDelivery delivery = spy(new TestAsyncBatchDelivery(createTestBatchDeliveryBuilder()
                .withBatchSize(1)
                .withDeliveryInterval(TEST_DELIVERY_INTERVAL)
                .withClientObjectFactory(objectFactory)
                .withFailoverPolicy(new NoopFailoverPolicy())
                .withSetupOpSources()) {

            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }

        });

        String testMessage = "test message";

        // when
        delivery.add("testIndexName", testMessage);

        // then
        verify(batchEmitterFactory).createInstance(eq(1), eq(TEST_DELIVERY_INTERVAL), eq(objectFactory), any());

        ArgumentCaptor<BulkEmitterTest.TestBatchItem> captor = ArgumentCaptor.forClass(BulkEmitterTest.TestBatchItem.class);
        verify(emitter, times(1)).add(captor.capture());
        assertEquals(testMessage, captor.getValue().getData(null));


    }

    public static class TestAsyncBatchDelivery extends AsyncBatchDeliveryPlugin {


        private BatchEmitterServiceProvider mockedProvider;

        public TestAsyncBatchDelivery(Builder builder) {
            super(builder);
        }

        @Override
        protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
            return mockedProvider;
        }

    }
    private class TestFailoverPolicy implements FailoverPolicy, LifeCycle {


        private State state = State.STOPPED;

        @Override
        public void deliver(Object failedPayload) {

        }

        @Override
        public void start() {
            state = State.STARTED;
        }

        @Override
        public void stop() {
            state = State.STOPPED;
        }

        @Override
        public LifeCycle stop(long timeout, boolean runInBackground) {
            state = State.STOPPED;
            return this;
        }

        @Override
        public boolean isStarted() {
            return state == State.STARTED;
        }

        @Override
        public boolean isStopped() {
            return state == State.STOPPED;
        }


    }

}
