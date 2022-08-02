package org.appenders.log4j2.elasticsearch.metrics;

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

import org.appenders.log4j2.elasticsearch.util.TestClock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MetricsProcessorTest {

    private Metric createTestMetric(final MetricsFactory metricsFactory, final String expectedName) {
        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        metricsFactory.configure(metricConfig);
        return metricsFactory.createMetric("test-component", expectedName);
    }

    @Test
    public void collectsAndWritesPreRegisteredMetrics() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        metricsRegistry.register(metric);

        final Clock clock = TestClock.createTestClock(expectedTimestamp);

        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput);

        // when
        metric.store(expectedValue);
        processor.process();

        // then
        verify(metricOutput).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void writesWithDefaultClock() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        metricsRegistry.register(metric);

        final MetricsProcessor processor = new MetricsProcessor(metricsRegistry, new MetricOutput[] { metricOutput });

        // when
        metric.store(expectedValue);
        processor.process();

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(long.class);
        verify(metricOutput).write(captor.capture(), eq(expectedKey), eq(expectedValue));

        assertThat(captor.getValue(), greaterThanOrEqualTo(expectedTimestamp));

    }

    @Test
    public void registersMeasuredInstances() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        final MeasuredDummy measuredDummy = new MeasuredDummy(metric);

        final Clock clock = TestClock.createTestClock(expectedTimestamp);

        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput);

        // when
        metric.store(expectedValue);
        processor.register(measuredDummy);
        processor.process();

        // then
        verify(metricOutput).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void checksForNewMetrics() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName1 = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key("test-component", expectedName1, "count");

        final MetricConfig metricConfig1 = MetricConfigFactory.createCountConfig(expectedName1);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig1));
        final Metric metric1 = metricsFactory.createMetric("test-component", expectedName1);

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        metricsRegistry.register(metric1);

        final Clock clock = TestClock.createTestClock(expectedTimestamp);

        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput);

        metric1.store(expectedValue);
        processor.process();

        verify(metricOutput).write(eq(expectedTimestamp), eq(expectedKey1), eq(expectedValue));

        // when

        final String expectedName2 = UUID.randomUUID().toString();
        final Metric metric2 = createTestMetric(metricsFactory, expectedName2);
        final Metric.Key expectedKey2 = new Metric.Key("test-component", expectedName2, "count");

        metric2.store(expectedValue - 1);
        metric1.store(1);

        metricsRegistry.register(metric2);
        processor.process();

        // then
        verify(metricOutput).write(eq(expectedTimestamp), eq(expectedKey1), eq(1L));
        verify(metricOutput).write(eq(expectedTimestamp), eq(expectedKey2), eq(expectedValue - 1));

    }

    @Test
    public void doesNotWriteIfNotAcceptedByMetricWriter() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricOutput metricOutput1 = mock(MetricOutput.class);
        when(metricOutput1.accepts(any())).thenReturn(false);

        final MetricOutput metricOutput2 = mock(MetricOutput.class);
        when(metricOutput2.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        metricsRegistry.register(metric);

        final Clock clock = TestClock.createTestClock(expectedTimestamp);

        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput1, metricOutput2);

        // when
        metric.store(expectedValue);
        processor.process();

        // then
        verify(metricOutput1, never()).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));
        verify(metricOutput2).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void doesNotWriteNoop() {

        // given
        long expectedTimestamp = System.currentTimeMillis();
        final long expectedValue = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "noop");

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        metricsRegistry.register(metric);

        // sanity check
        assertThat(metricsRegistry.getMetrics(metric1 -> true), Matchers.hasItem(metric));

        final Clock clock = TestClock.createTestClock(expectedTimestamp);

        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput);

        // when
        metric.store(expectedValue);
        processor.process();

        // then
        verify(metricOutput, never()).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void doesNotHaveAnyMetricsToRegister() {

        // given
        final Clock clock = TestClock.createTestClock(0L);
        final MetricsRegistry metricsRegistry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = mock(MetricOutput.class);
        final MetricsProcessor processor = createTestProcessor(clock, metricsRegistry, metricOutput);

        final MetricsRegistry otherRegistry = mock(MetricsRegistry.class);

        // when
        processor.register(otherRegistry);

        // then
        verifyNoInteractions(otherRegistry);

    }

    public static MetricsProcessor createTestProcessor(final Clock clock, final MetricsRegistry metricsRegistry, final MetricOutput... metricOutputs) {
        return new MetricsProcessor(clock, metricsRegistry, metricOutputs);
    }

    private static class MeasuredDummy implements Measured {

        private final Metric[] metrics;

        private MeasuredDummy(final Metric... metrics) {
            this.metrics = metrics;
        }

        @Override
        public void register(MetricsRegistry registry) {

            for (Metric metric : metrics) {
                registry.register(metric);
            }

        }

    }
}
