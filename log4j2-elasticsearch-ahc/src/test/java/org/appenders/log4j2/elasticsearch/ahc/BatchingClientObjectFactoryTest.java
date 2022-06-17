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

import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ClientFactory;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchingClientObjectFactoryTest {

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartDoesNotStartClientProvider() {

        // given
        final HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
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
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = spy(createDefaultBatchingObjectFactoryBuilder()
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

        ((ClientFactory<HttpClient>)lifeCycle).createClient();

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
    public void registersBatchingClientMetricsWithMetricRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "serverTookMs", "noop");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "noop");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "noop");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "noop");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "noop");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");
        final Metric.Key expectedKey7 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
                .withName(expectedComponentName)
                .build();

        // when
        objectFactory.register(registry);

        // then
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey4)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey5)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey6)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey7)).size());

    }

    @Test
    public void deregistersBatchingClientMetricsWithMetricRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
                .withName(expectedComponentName)
                .build();

        objectFactory.register(registry);
        assertNotEquals(0, registry.getMetrics(metric -> true).size()); // sanity check

        // when
        objectFactory.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> true).size());

    }

    @Test
    public void enablesAllBatchingClientMetricsWithMetricRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "serverTookMs", "max");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "count");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "count");
        final Metric.Key expectedKey7 = new Metric.Key(expectedComponentName, "failoverTookMs", "max");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfigs(BatchingClientObjectFactory.BatchingClientMetrics.metricConfigs(true))
                .build();

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        // when
        objectFactory.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey6), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey7), eq(0L));

    }

    @Test
    public void registersClientProviderMetricsWithMetricRegistryOnFirstBatch() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final HttpClient httpClient = mock(HttpClient.class);
        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientProvider httpClientProvider = spy(new HttpClientProvider(new HttpClientFactory.Builder()) {
            @Override
            public HttpClient createClient() {
                return httpClient;
            }
        });

        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = spy(createDefaultBatchingObjectFactoryBuilder()
                .withClientProvider(httpClientProvider)
                .withName(expectedComponentName)
                .withMetricConfigs(BatchingClientObjectFactory.BatchingClientMetrics.metricConfigs(true))
                .build());

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        objectFactory.register(registry);
        verify(httpClientProvider, never()).register(eq(registry));

        // when
        objectFactory.start();
        verify(httpClientProvider, never()).register(eq(registry));

        final Function<BatchRequest, Boolean> batchListener = objectFactory.createBatchListener(new NoopFailoverPolicy.Builder().build());
        batchListener.apply(new BatchRequest(new BatchRequest.Builder()));

        // then
        verify(httpClientProvider).register(eq(registry));

    }

    @Test
    public void deregistersClientProviderMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final HttpClient httpClient = mock(HttpClient.class);
        final MetricsRegistry registry = new BasicMetricsRegistry();
        final HttpClientProvider httpClientProvider = spy(new HttpClientProvider(new HttpClientFactory.Builder()) {
            @Override
            public HttpClient createClient() {
                return httpClient;
            }
        });

        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = spy(createDefaultBatchingObjectFactoryBuilder()
                .withClientProvider(httpClientProvider)
                .withName(expectedComponentName)
                .withMetricConfigs(BatchingClientObjectFactory.BatchingClientMetrics.metricConfigs(true))
                .build());

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        objectFactory.register(registry);
        verify(httpClientProvider, never()).register(eq(registry));

        objectFactory.start();
        verify(httpClientProvider, never()).register(eq(registry));

        final Function<BatchRequest, Boolean> batchListener = objectFactory.createBatchListener(new NoopFailoverPolicy.Builder().build());
        batchListener.apply(new BatchRequest(new BatchRequest.Builder()));

        // when
        objectFactory.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> true).size());

    }

    @Test
    public void configuresSubSetOfMetricsWithMetricRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "serverTookMs", "max");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "itemsSent", "max");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "itemsDelivered", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "itemsFailed", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "backoffApplied", "count");
        final Metric.Key expectedKey6 = new Metric.Key(expectedComponentName, "batchesFailed", "noop");
        final Metric.Key expectedKey7 = new Metric.Key(expectedComponentName, "failoverTookMs", "noop");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfig(MetricConfigFactory.createCountConfig("serverTookMs"))
                .withMetricConfig(MetricConfigFactory.createCountConfig("itemsDelivered"))
                .withMetricConfig(MetricConfigFactory.createCountConfig("itemsFailed"))
                .withMetricConfig(MetricConfigFactory.createMaxConfig("itemsSent", false))
                .withMetricConfig(MetricConfigFactory.createCountConfig("backoffApplied"))
                .build();

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        // when
        objectFactory.register(registry);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        verify(metricOutput, never()).write(anyLong(), eq(expectedKey6), eq(0L));
        verify(metricOutput, never()).write(anyLong(), eq(expectedKey7), eq(0L));

    }

    @Test
    public void storesItemsSent() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "itemsSent", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("itemsSent"));
        when(objectFactory.createClient()).thenReturn(mock(HttpClient.class));

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        objectFactory.register(registry);

        final BatchRequest batchRequest = spy(BatchRequestTest.createDefaultTestObjectBuilder().build());

        final int expectedValue = new Random().nextInt(10000);
        when(batchRequest.size()).thenReturn(expectedValue);

        // when
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) expectedValue * 2));
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) expectedValue));

    }

    @Test
    public void storesItemsFailed() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "itemsFailed", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig(true, "itemsFailed", false));

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final BatchRequest batchRequest = mock(BatchRequest.class);

        final int expectedValue = new Random().nextInt(10000);
        when(batchRequest.size()).thenReturn(expectedValue);

        final Function<BatchRequest, Boolean> failureHandler = objectFactory.createFailureHandler(new NoopFailoverPolicy.Builder().build());
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        objectFactory.register(registry);

        // when
        failureHandler.apply(batchRequest);
        metricProcessor.process();
        failureHandler.apply(batchRequest);
        failureHandler.apply(batchRequest);
        metricProcessor.process();

        // then
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) expectedValue * 3));
        verify(metricOutput).write(anyLong(), eq(expectedKey), eq((long) expectedValue));

    }

    @Test
    public void storesBackoffApplied() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key(expectedComponentName, "backoffApplied", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final BackoffPolicy<BatchRequest> backoffPolicy = mock(BackoffPolicy.class);
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = spy(createTestBuilderWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("backoffApplied"))
                .withBackoffPolicy(backoffPolicy)
                .build());
        when(objectFactory.createClient()).thenReturn(mock(HttpClient.class));

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        objectFactory.register(registry);

        final BatchRequest batchRequest = spy(BatchRequestTest.createDefaultTestObjectBuilder()
                .withBuffer(ByteBufItemSourceTest.createTestItemSource()).build());
        doNothing().when(batchRequest).completed();
        when(backoffPolicy.shouldApply(eq(batchRequest))).thenReturn(true);

        final int expectedValue = new Random().nextInt(10000);
        when(batchRequest.size()).thenReturn(expectedValue);

        // when
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
        metricProcessor.process();
        objectFactory.createBatchListener(new NoopFailoverPolicy()).apply(batchRequest);
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
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createTestObjectFactoryWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("batchesFailed"));

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final BatchRequest batchRequest = mock(BatchRequest.class);

        final int expectedValue = new Random().nextInt(10000);
        when(batchRequest.size()).thenReturn(expectedValue);

        final Function<BatchRequest, Boolean> failureHandler = objectFactory.createFailureHandler(new NoopFailoverPolicy.Builder().build());
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        objectFactory.register(registry);

        // when
        failureHandler.apply(batchRequest);
        metricProcessor.process();
        failureHandler.apply(batchRequest);
        failureHandler.apply(batchRequest);
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
        final BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createTestBuilderWithMetric(expectedComponentName, MetricConfigFactory.createCountConfig("failoverTookMs"))
                .withFailedItemOps(new FailedItemOps<IndexRequest>() {
                    @Override
                    public FailedItemSource createItem(final IndexRequest failed) {
                        return new FailedItemSource(failed.getSource(), new FailedItemInfo("test"));
                    }

                    @Override
                    public FailedItemInfo createInfo(final IndexRequest failed) {
                        return null;
                    }
                })
                .build();

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final BatchRequest batchRequest = mock(BatchRequest.class);
        when(batchRequest.getItems()).thenReturn(Collections.singletonList(new IndexRequest.Builder(ByteBufItemSourceTest.createTestItemSource()).index("test-index").type("test-type").build()));

        final Function<BatchRequest, Boolean> failureHandler = objectFactory.createFailureHandler(failedPayload -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new MetricOutput[] { metricOutput });

        objectFactory.register(registry);

        // when
        failureHandler.apply(batchRequest);
        metricProcessor.process();

        // then
        final ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(long.class);
        verify(metricOutput).write(anyLong(), eq(expectedKey), captor.capture());

        System.out.println(captor.getValue());
        assertTrue(captor.getValue().intValue() >= 50);

    }

    private BatchingClientObjectFactory<BatchRequest, IndexRequest> createTestObjectFactoryWithMetric(final String expectedComponentName, final MetricConfig metricConfig) {
        return spy(createTestBuilderWithMetric(expectedComponentName, metricConfig).build());
    }

    private BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest> createTestBuilderWithMetric(
            final String expectedComponentName,
            final MetricConfig metricConfig) {
        return createDefaultBatchingObjectFactoryBuilder()
                .withName(expectedComponentName)
                .withMetricConfig(metricConfig);
    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultBatchingObjectFactoryBuilder().build();
    }

    private BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest> createDefaultBatchingObjectFactoryBuilder() {
        return new BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest>() {
            @Override
            public BatchingClientObjectFactory<BatchRequest, IndexRequest> build() {
                return new BatchingClientObjectFactory<BatchRequest, IndexRequest>(this) {
                    @Override
                    public HttpClient createClient() {
                        return HttpClientTest.createTestHttpClientFactoryBuilder(mock(AsyncHttpClient.class)).build().createInstance();
                    }

                    @Override
                    protected ResponseHandler<BatchResult> createResultHandler(final BatchRequest request, final Function<BatchRequest, Boolean> failureHandler) {
                        return new ResponseHandler<BatchResult>() {
                            @Override
                            public void completed(BatchResult result) {

                            }

                            @Override
                            public void failed(Exception ex) {

                            }

                            @Override
                            public BatchResult deserializeResponse(InputStream inputStream) throws IOException {
                                return null;
                            }
                        };
                    }

                    @Override
                    public BatchOperations<BatchRequest> createBatchOperations() {
                        return null;
                    }

                    @Override
                    public OperationFactory setupOperationFactory() {
                        return null;
                    }
                };
            }

            @Override
            protected FailedItemOps<IndexRequest> createFailedItemOps() {
                return null;
            }
        };
    }

}
