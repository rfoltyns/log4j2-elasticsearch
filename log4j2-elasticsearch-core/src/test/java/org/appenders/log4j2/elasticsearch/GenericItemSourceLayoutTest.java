package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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
import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutputTest;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.TestKeyAccessor;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericItemSourceLayoutTest {

    public static GenericItemSourceLayout.Builder<Object, String> createDefaultTestLayoutBuilder() {
        final JacksonSerializer<Object> serializer = new JacksonSerializer<>(new ObjectMapper().writer());
        final ItemSourceFactory<Object, String> itemSourceFactory = new StringItemSourceFactory<>();
        return new GenericItemSourceLayout.Builder<Object, String>()
                .withItemSourceFactory(itemSourceFactory)
                .withSerializer(serializer);
    }

    public static GenericItemSourceLayout.Builder<Object, ByteBuf> createDefaultTestByteByfBasedLayoutBuilder() {
        final JacksonSerializer<Object> serializer = new JacksonSerializer<>(new ObjectMapper().writer());
        final ItemSourceFactory<Object, ByteBuf> itemSourceFactory = ByteBufItemSourceFactoryPluginTest.createDefaultTestSourceFactoryConfig().build();
        return new GenericItemSourceLayout.Builder<Object, ByteBuf>()
                .withItemSourceFactory(itemSourceFactory)
                .withSerializer(serializer);
    }

    @Test
    public void builderBuilderSuccessfully() {

        // when
        final GenericItemSourceLayout<Object, String> layout = createDefaultTestLayoutBuilder().build();

        // then
        assertNotNull(layout);

    }

    @Test
    public void builderThrowsOnNullSerializer() {

        // given
        final GenericItemSourceLayout.Builder<Object, String> builder = createDefaultTestLayoutBuilder()
                .withSerializer(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No Serializer provided for " + GenericItemSourceLayout.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnNullItemSourceFactory() {

        // given
        final GenericItemSourceLayout.Builder<Object, String> builder = createDefaultTestLayoutBuilder()
                .withItemSourceFactory(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No ItemSourceFactory provided for " + GenericItemSourceLayout.class.getSimpleName()));

    }

    @Test
    public void lifecycleStopStopsItemSourceFactoryOnlyOnce() {

        // given
        ItemSourceFactory<Object, String> itemSourceFactory = mock(ItemSourceFactory.class);
        when(itemSourceFactory.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        GenericItemSourceLayout<Object, String> layout = createDefaultTestLayoutBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        // when
        layout.stop();
        layout.stop();

        // then
        verify(itemSourceFactory).stop();
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
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "available", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "initial", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "total", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "noSuchElementCaught", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "resizeAttempts", "count");

        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedComponentName)
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(false))
                .build();

        final GenericItemSourceLayout<Object, ByteBuf> itemSourceLayout = createDefaultTestByteByfBasedLayoutBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        // when
        itemSourceLayout.register(registry);

        // then
        assertEquals(5, registry.getMetrics(metric -> TestKeyAccessor.getMetricType(metric.getKey()).equals("noop")).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey1)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey2)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey3)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey4)).size());
        assertEquals(1, registry.getMetrics(metric -> metric.getKey().equals(expectedKey5)).size());

    }

    @Test
    public void deregistersAllMetricsWithMetricsRegistry() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();

        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedComponentName)
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(false))
                .build();

        final GenericItemSourceLayout<Object, ByteBuf> itemSourceLayout = createDefaultTestByteByfBasedLayoutBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        itemSourceLayout.register(registry);
        assertEquals(5, registry.getMetrics(metric -> metric.getKey().toString().contains(expectedComponentName)).size());

        // when
        itemSourceLayout.deregister();

        // then
        assertEquals(0, registry.getMetrics(metric -> metric.getKey().toString().contains(expectedComponentName)).size());

    }

    @Test
    public void registersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = spy(PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedComponentName)
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(false))
                .build());

        final GenericItemSourceLayout<Object, ByteBuf> itemSourceLayout = createDefaultTestByteByfBasedLayoutBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        final MetricsRegistry registry = mock(MetricsRegistry.class);

        // when
        itemSourceLayout.start();
        itemSourceLayout.register(registry);

        // then
        verify(itemSourceFactory).register(eq(registry));

    }

    @Test
    public void deregistersComponentsMetrics() {

        // given
        final String expectedComponentName = UUID.randomUUID().toString();
        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = spy(PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedComponentName)
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(false))
                .build());

        final GenericItemSourceLayout<Object, ByteBuf> itemSourceLayout = createDefaultTestByteByfBasedLayoutBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        // when
        itemSourceLayout.deregister();

        // then
        verify(itemSourceFactory).deregister();

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestLayoutBuilder().build();
    }

}
