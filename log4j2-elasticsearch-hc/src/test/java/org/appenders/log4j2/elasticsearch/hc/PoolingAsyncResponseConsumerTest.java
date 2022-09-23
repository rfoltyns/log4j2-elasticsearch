package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourcePool;
import org.appenders.log4j2.elasticsearch.PoolResourceException;
import org.appenders.log4j2.elasticsearch.ResizePolicy;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.util.TestClock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.hc.SimpleInputBufferObjectOpsTest.createDefaultTestGenericItemSourcePool;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PoolingAsyncResponseConsumerTest {

    @Test
    public void onResponseReceivedReturnsTheSameResponse() {

        // given
        HttpResponse response = mock(HttpResponse.class);
        PoolingAsyncResponseConsumer consumer = createDefaultTestObject();

        // when
        consumer.onResponseReceived(response);
        HttpResponse actual = consumer.buildResult(null);

        // then
        assertEquals(response, actual);

    }

    @Test
    public void onEntityEnclosedSetsResponseInputStream() throws IOException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                        GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                        false
                );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        // when
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        // then
        HttpResponse response = consumer.buildResult(null);
        verify(response, times(1)).setEntity(any());

    }

    @Test
    public void onEntityEnclosedPoolsTheBufferOnce() throws IOException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = spy(createDefaultTestGenericItemSourcePool(
                GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                false
        ));

        PoolingAsyncResponseConsumer consumer = spy(createDefaultTestObject(itemSourcePool));
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        // when
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        // then
        verify(consumer, times(1)).getPooled();

    }

    @Test
    public void onEntityEnclosedStoresResponseBytesMetric() throws IOException {

        // given
        final GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                false
        );

        final DefaultMetricsFactory metricsFactory = new DefaultMetricsFactory(PoolingAsyncResponseConsumer.metricConfigs(true));
        final String expectedName = UUID.randomUUID().toString();
        final PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics metrics = new PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics(expectedName, metricsFactory);

        final PoolingAsyncResponseConsumer consumer = new PoolingAsyncResponseConsumer(metrics, itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        final HttpEntity httpEntity = mock(HttpEntity.class);
        final Random random = new Random();
        final long expectedContentLength = random.nextInt();
        when(httpEntity.getContentLength()).thenReturn(expectedContentLength);

        final MetricOutput metricOutput1 = mock(MetricOutput.class);
        when(metricOutput1.accepts(any())).thenReturn(true);

        final BasicMetricsRegistry metricsRegistry = new BasicMetricsRegistry();

        long expectedTimestamp = random.nextLong();
        final Clock clock = TestClock.createTestClock(expectedTimestamp);
        final MetricsProcessor processor = new MetricsProcessor(clock, metricsRegistry, new MetricOutput[] { metricOutput1 });

        // when
        metrics.register(metricsRegistry);
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));
        processor.process();

        // then
        final Metric.Key expectedKey = new Metric.Key(expectedName, "responseBytes", "count");
        verify(metricOutput1).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedContentLength));

    }

    @Test
    public void onEntityEnclosedSucceedsWhenCreatedWithItemPoolConstructor() throws IOException, PoolResourceException {

        // given
        final ItemSourcePool<SimpleInputBuffer> itemSourcePool = mock(ItemSourcePool.class);
        when(itemSourcePool.getPooled()).thenReturn(SimpleInputBufferObjectOpsTest.createDefaultTestObject().createItemSource(item -> {}));

        final PoolingAsyncResponseConsumer consumer = new PoolingAsyncResponseConsumer(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        final HttpEntity httpEntity = mock(HttpEntity.class);
        final Random random = new Random();
        final long expectedContentLength = random.nextLong();
        when(httpEntity.getContentLength()).thenReturn(expectedContentLength);

        // when
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

    }

    @Test
    public void onContentReceivedPassedDecoderToBuffer() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = mock(ItemSourcePool.class);

        ItemSource<SimpleInputBuffer> itemSource = mock(ItemSource.class);
        when(itemSourcePool.getPooled()).thenReturn(itemSource);

        SimpleInputBuffer buffer = mock(SimpleInputBuffer.class);
        when(itemSource.getSource()).thenReturn(buffer);

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        ContentDecoder contentDecoder = mock(ContentDecoder.class);

        // when
        consumer.onContentReceived(contentDecoder, mock(IOControl.class));

        // then
        verify(buffer, times(1)).consumeContent(eq(contentDecoder));

    }

    @Test
    public void onContentReceivedThrowsIOExceptionOnEmptyPool() {

        // given
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                0,
                false,
                new ResizePolicy() {
                    @Override
                    public boolean increase(ItemSourcePool itemSourcePool) {
                        return false;
                    }

                    @Override
                    public boolean decrease(ItemSourcePool itemSourcePool) {
                        return false;
                    }
                }
        );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        // when
        final IOException exception = assertThrows(IOException.class, () -> consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json")));

        // then
        assertThat(exception.getMessage(), containsString("ResizePolicy is ineffective. Pool testPool has to be reconfigured to handle current load."));

    }

    @Test
    public void releaseResourcesNullifiesTheResponse() throws IOException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                false
        );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        HttpResponse before = consumer.buildResult(null);
        assertNotNull(before);

        // when
        consumer.releaseResources();

        // then
        HttpResponse response = consumer.buildResult(null);
        assertNull(response);

    }

    private PoolingAsyncResponseConsumer createDefaultTestObject(ItemSourcePool<SimpleInputBuffer> itemSourcePool) {
        final PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics metricsFactory = new PoolingAsyncResponseConsumer.AsyncResponseConsumerMetrics("test-component", new DefaultMetricsFactory(Collections.emptyList()));
        return new PoolingAsyncResponseConsumer(metricsFactory, itemSourcePool);
    }

    private PoolingAsyncResponseConsumer createDefaultTestObject() {
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = mock(ItemSourcePool.class);
        return createDefaultTestObject(itemSourcePool);
    }

}
