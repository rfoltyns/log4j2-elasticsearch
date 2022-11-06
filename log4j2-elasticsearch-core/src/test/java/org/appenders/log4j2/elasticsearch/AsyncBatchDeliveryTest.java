package org.appenders.log4j2.elasticsearch;

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


import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery.Builder;
import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.failover.FailoverListener;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.ScheduledMetricsProcessor;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.appenders.log4j2.elasticsearch.spi.TestBatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.util.TestClock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncBatchDeliveryTest {

    private static final int TEST_BATCH_SIZE = 100;
    private static final int TEST_DELIVERY_INTERVAL = 100;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";

    public static TestHttpObjectFactory.Builder createTestObjectFactoryBuilder() {
        return TestHttpObjectFactory.newBuilder()
                .withServerUris(TEST_SERVER_URIS);
    }

    public static Builder createTestBatchDeliveryBuilder() {
        return spy(AsyncBatchDelivery.newBuilder()
                .withShutdownDelayMillis(0)
                .withBatchSize(TEST_BATCH_SIZE)
                .withDeliveryInterval(TEST_DELIVERY_INTERVAL)
                .withClientObjectFactory(createTestObjectFactoryBuilder().build()))
                .withFailoverPolicy(new NoopFailoverPolicy());
    }

    @Test
    public void builderReturnsNonNullObject() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        // when
        BatchDelivery<String> delivery = batchDeliveryBuilder.build();

        // then
        assertNotNull(delivery);
    }

    @Test
    public void builderFailsWhenClientObjectFactoryIsNull() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withClientObjectFactory(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, batchDeliveryBuilder::build);

        // then
        assertThat(exception.getMessage(),
                equalTo("No Elasticsearch client factory [HCHttp|JestHttp|ElasticsearchBulkProcessor] provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void builderFailsWhenBatchSizeIsZero() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withBatchSize(0);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, batchDeliveryBuilder::build);

        // then
        assertThat(exception.getMessage(),
                equalTo("No batchSize provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void builderFailsWhenBatchSizeIsLowerThanZero() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withBatchSize(-1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, batchDeliveryBuilder::build);

        // then
        assertThat(exception.getMessage(),
                equalTo("No batchSize provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void builderFailsWhenDeliveryIntervalIsZero() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withDeliveryInterval(0);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, batchDeliveryBuilder::build);

        // then
        assertThat(exception.getMessage(),
                equalTo("No deliveryInterval provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void builderFailsWhenDeliveryIntervalIsLowerThanZero() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withDeliveryInterval(-1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, batchDeliveryBuilder::build);

        // then
        assertThat(exception.getMessage(),
                equalTo("No deliveryInterval provided for " + AsyncBatchDelivery.class.getSimpleName()));

    }

    @Test
    public void builderConfiguresShutdownDelayMillis() {

        // given
        long expectedShutdownDelayMillis = 10 + new Random().nextInt(100);
        FailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());

        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .withShutdownDelayMillis(expectedShutdownDelayMillis);

        AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();
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
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withSetupOpSources(indexTemplate)
                .withClientObjectFactory(clientObjectFactory);

        AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();

        // when
        asyncBatchDelivery.start();

        // then
        verify(operationFactory).create(eq(indexTemplate));
        verify(clientObjectFactory).addOperation(any());

    }

    @Test
    public void builderConfiguresMetricsProcessor() {

        // given
        final MetricsProcessor metricsProcessor = mock(MetricsProcessor.class);

        final Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withMetricProcessor(metricsProcessor);

        // when
        final AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();
        asyncBatchDelivery.start();

        // then
        verify(metricsProcessor).register(eq((Measured)asyncBatchDelivery));

    }

    @Test
    public void constructorWithNoMetricsProcessorCreatesDefault() {

        // when
        final AsyncBatchDelivery asyncBatchDelivery = new AsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy(),
                0L,
                new OpSource[0]
        );

        // then
        assertEquals(MetricsProcessor.class.getName(), asyncBatchDelivery.metricsProcessor.getClass().getName());

    }

    @Test
    public void registersMeasuredInstanceWithMetricProcessor() {

        // given
        final MetricsRegistry registry = mock(MetricsRegistry.class);
        final MetricsProcessor metricsProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry());

        final Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withMetricProcessor(metricsProcessor);

        final AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();

        final Metric metric = new DefaultMetricsFactory(Collections.singletonList(MetricConfigFactory.createCountConfig("test-count")))
                .createMetric("test-component", "test-count");

        // when
        asyncBatchDelivery.register(Measured.of(new Dummy(metric)));

        // then
        verify(registry).register(eq(metric));

    }

    @Test
    public void registersMeasuredComponentsWithMetricsRegistry() {

        // given
        final MetricsRegistry registry = mock(MetricsRegistry.class);
        final MetricsProcessor metricsProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry());

        final TestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());

        final Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(clientObjectFactory)
                .withMetricProcessor(metricsProcessor);

        final AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();

        // when
        asyncBatchDelivery.register(registry);

        // then
        verify(clientObjectFactory).register(eq(registry));

    }

    @Test
    public void deregistersMeasuredComponentsWithMetricsRegistryExcludingMetricsProcessor() {

        // given
        final MetricsRegistry registry = mock(MetricsRegistry.class);
        final MetricsProcessor metricsProcessor = spy(new MetricsProcessor(registry, new BasicMetricOutputsRegistry()));

        final TestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());

        final Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(clientObjectFactory)
                .withMetricProcessor(metricsProcessor);

        final AsyncBatchDelivery asyncBatchDelivery = batchDeliveryBuilder.build();
        asyncBatchDelivery.register(registry);

        // when
        asyncBatchDelivery.deregister();

        // then
        verify(clientObjectFactory).deregister();
        verify(metricsProcessor, never()).deregister();

    }

    @Test
    public void batchDeliveryAddObjectDelegatesToProvidedBatchOperationsObjectApi() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery batchDelivery = batchDeliveryBuilder.build();

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
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery batchDelivery = batchDeliveryBuilder.build();

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

        TestAsyncBatchDelivery delivery = spy(new TestAsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                objectFactory,
                new NoopFailoverPolicy(),
                null) {
            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }
        });

        String testMessage = "test message";

        // when
        delivery.add("testIndexName", testMessage);

        // then
        ArgumentCaptor<BulkEmitterTest.TestBatchItem> captor = ArgumentCaptor.forClass(BulkEmitterTest.TestBatchItem.class);

        verify(emitter, times(1)).add(captor.capture());
        assertEquals(testMessage, captor.getValue().getData(null));
    }

    @Test
    public void lifecycleStartSetsUpIndexTemplateExecutionIfIndexTemplateIsConfigured() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());

        IndexTemplate indexTemplate = mock(IndexTemplate.class);
        when(indexTemplate.getType()).thenReturn(IndexTemplate.TYPE_NAME);

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .withSetupOpSources(indexTemplate)
                .build();

        // when
        batchDelivery.start();

        // then
        verify(objectFactory).addOperation(any());

    }

    @Test
    public void lifecycleStartDoesntSetUpIndexTemplateExecutionIfIndexTemplateIsNotConfigured() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .withSetupOpSources()
                .build();

        // when
        batchDelivery.start();

        // then
        verify(objectFactory, never()).addOperation(any());

    }

    @Test
    public void failoverListenerDelegatesToBatchDelivery() {

        // given
        AsyncBatchDelivery batchDelivery = spy(createTestBatchDeliveryBuilder().build());

        FailoverListener listener = batchDelivery.failoverListener();

        FailedItemInfo failedItemInfo = mock(FailedItemInfo.class);
        String expectedTargetName = UUID.randomUUID().toString();
        when(failedItemInfo.getTargetName()).thenReturn(expectedTargetName);

        FailedItemSource failedItemSource = mock(FailedItemSource.class);
        when(failedItemSource.getInfo()).thenReturn(failedItemInfo);

        // when
        listener.notify(failedItemSource);

        // then
        verify(batchDelivery).add(eq(expectedTargetName), eq(failedItemSource));

    }

    @Test
    public void lifecycleStartSetsUpFailoverListenerIfFailoverPolicyIsNotStarted() {

        TestFailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());
        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .build();

        // when
        batchDelivery.start();

        // then
        verify(failoverPolicy).addListener(any());

    }

    @Test
    public void lifecycleStartDoesNotSetUpFailoverListenerIfFailoverPolicyStarted() {

        TestFailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());
        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .build();

        failoverPolicy.start();

        // when
        batchDelivery.start();

        // then
        verify(failoverPolicy, never()).addListener(any());

    }

    @Test
    public void lifecycleStartStartsBatchEmitter() {

        // given
        BatchEmitter batchEmitter = mock(BatchEmitter.class);

        BatchEmitterServiceProvider batchEmitterFactory = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return batchEmitter;
            }
        };

        TestAsyncBatchDelivery batchDelivery = spy(new TestAsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy(),
                IndexTemplateTest.createTestIndexTemplateBuilder().build()) {
            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }
        });

        // when
        batchDelivery.start();

        // then
        verify(batchEmitter).start();

    }

    @Test
    public void lifecycleStopStopsBatchEmitterOnlyOnce() {

        // given
        BatchEmitter batchEmitter = mock(BatchEmitter.class);
        when(batchEmitter.isStopped()).thenAnswer(falseOnlyOnce());

        BatchEmitterServiceProvider batchEmitterFactory = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return batchEmitter;
            }
        };
        TestAsyncBatchDelivery batchDelivery = spy(new TestAsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy(),
                null) {
            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }
        });

        // when
        batchDelivery.stop();
        batchDelivery.stop();

        // then
        verify(batchEmitter).stop(anyLong(), anyBoolean());

    }

    @Test
    public void lifecycleStartStartsObjectFactoryOnlyOnce() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());
        when(objectFactory.isStarted()).thenAnswer(falseOnlyOnce());

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .build();

        // when
        batchDelivery.start();
        batchDelivery.start();

        // then
        verify(objectFactory).start();

    }

    @Test
    public void lifecycleStopStopsObjectFactoryOnlyOnce() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());
        when(objectFactory.isStopped()).thenAnswer(falseOnlyOnce());

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .build();

        // when
        batchDelivery.stop();
        batchDelivery.stop();

        // then
        verify(objectFactory).stop();

    }

    @Test
    public void lifecycleStartStartsFailoverPolicyOnlyOnce() {

        // given
        FailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());
        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .build();

        // when
        batchDelivery.start();
        batchDelivery.start();

        // then
        verify(LifeCycle.of(failoverPolicy)).start();

    }

    @Test
    public void lifecycleStopStopsFailoverPolicyOnlyOnce() {

        // given
        FailoverPolicy failoverPolicy = spy(new TestFailoverPolicy());
        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withFailoverPolicy(failoverPolicy)
                .withShutdownDelayMillis(0L)
                .build();

        batchDelivery.start();

        // when
        batchDelivery.stop();
        batchDelivery.stop();

        // then
        verify(LifeCycle.of(failoverPolicy)).stop(anyLong(), anyBoolean());

    }

    @Test
    public void lifecycleStartStartsMetricsProcessorOnlyOnce() {

        // given
        final MetricsProcessor metricsProcessor = spy(new ScheduledMetricsProcessor(
                1000,
                1000,
                TestClock.createTestClock(System.currentTimeMillis()),
                new BasicMetricsRegistry(),
                new BasicMetricOutputsRegistry()));

        final BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withMetricProcessor(metricsProcessor)
                .build();

        // when
        batchDelivery.start();
        batchDelivery.start();

        // then
        verify(LifeCycle.of(metricsProcessor)).start();

    }

    @Test
    public void lifecycleStopStopsMetricsProcessorOnlyOnce() {

        // given
        final MetricsProcessor metricsProcessor = spy(new ScheduledMetricsProcessor(
                1000,
                1000,
                TestClock.createTestClock(System.currentTimeMillis()),
                new BasicMetricsRegistry(),
                new BasicMetricOutputsRegistry()));

        final BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withMetricProcessor(metricsProcessor)
                .build();

        batchDelivery.start();

        // when
        batchDelivery.stop();
        batchDelivery.stop();

        // then
        verify(LifeCycle.of(metricsProcessor)).stop();

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
        return createTestBatchDeliveryBuilder()
                .withShutdownDelayMillis(0L)
                .build();
    }

    static class TestAsyncBatchDelivery extends AsyncBatchDelivery {

        public TestAsyncBatchDelivery(int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, IndexTemplate indexTemplate) {
            super(new Builder()
                    .withBatchSize(batchSize)
                    .withDeliveryInterval(deliveryInterval)
                    .withClientObjectFactory(objectFactory)
                    .withFailoverPolicy(failoverPolicy)
                    .withSetupOpSources(indexTemplate));
        }

        @Override
        protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
            return null;
        }

    }

    private static class TestFailoverPolicy implements FailoverPolicy, LifeCycle {

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

    private class Dummy implements Measured {

        private final Metric metric;

        public Dummy(final Metric metric) {
            this.metric = metric;
        }

        @Override
        public void register(final MetricsRegistry registry) {
            registry.register(metric);
        }

    }
}
