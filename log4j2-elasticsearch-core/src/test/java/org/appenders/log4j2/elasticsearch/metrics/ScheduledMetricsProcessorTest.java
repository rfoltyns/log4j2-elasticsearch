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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledMetricsProcessorTest {

    long initialDelay = 0L;
    final long interval = 10000L;
    final int envDelay = 100;

    @Test
    public void processWithDelayIfConfigured() {

        // given
        initialDelay = 100;
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final LifeCycle lifeCycle = createLifeCycleTestObject(new BasicMetricsRegistry(), metricOutput, expectedTimestamp, expectedName);

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

        verify(metricOutput, timeout(initialDelay + envDelay)).write(eq(expectedTimestamp), eq(expectedKey), eq(0L));

    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartsOnlyOnce() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");

        final LifeCycle lifeCycle = createLifeCycleTestObject(new BasicMetricsRegistry(), metricOutput, expectedTimestamp, expectedName);

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

        verify(LifeCycle.of(metricOutput)).start();
        verify(metricOutput, timeout(500L)).write(eq(expectedTimestamp), eq(expectedKey), eq(0L));

    }

    @Test
    public void lifecycleStop() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final LifeCycle lifeCycle = createLifeCycleTestObject(new BasicMetricsRegistry(), metricOutput, System.currentTimeMillis(), UUID.randomUUID().toString());

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

        verify(LifeCycle.of(metricOutput)).stop();

    }

    @Test
    public void logsIfNotTerminatedInTime() throws InterruptedException {

        // given
        final Logger logger = mockTestLogger();

        final CountDownLatch metricOutputLatch = new CountDownLatch(1);

        final MetricOutput metricOutput = new MetricOutput() {

            @Override
            public String getName() {
                return UUID.randomUUID().toString();
            }

            @Override
            public boolean accepts(final Metric.Key key) {
                return true;
            }

            @Override
            public void write(final long timestamp, final Metric.Key key, final long value) {
                metricOutputLatch.countDown();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void flush() {

            }
        };

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedName = UUID.randomUUID().toString();

        final BasicMetricsRegistry metricsRegistry = new BasicMetricsRegistry();

        final MetricConfig countConfig = MetricConfigFactory.createCountConfig(true, expectedName, false);
        final Metric metric = new DefaultMetricsFactory(Collections.singletonList(countConfig)).createMetric("test-component", expectedName);
        metricsRegistry.register(metric);
        metric.store(2);

        final ScheduledMetricsProcessor processor = new ScheduledMetricsProcessor( 0, 1000, createTestClock(expectedTimestamp), metricsRegistry, new BasicMetricOutputsRegistry(metricOutput));


        // when
        processor.start();

        final CountDownLatch latch = new CountDownLatch(1);
        final Thread t1 = new Thread(() -> {
            latch.countDown();
            try {
                //noinspection ResultOfMethodCallIgnored
                metricOutputLatch.await(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            processor.stop();
        });

        t1.start();

        //noinspection ResultOfMethodCallIgnored
        latch.await(500L, TimeUnit.MILLISECONDS);

        // then
        verify(logger, timeout(500)).debug("{}: Stopping", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, timeout(500)).warn("{}: Thread did not stop in time. In-flight data may be lost", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, never()).error("{}: Thread interrupted. In-flight data may be lost", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, timeout(500)).debug("{}: Stopped", ScheduledMetricsProcessor.class.getSimpleName());

        InternalLogging.setLogger(null);

    }

    @Test
    public void logsIfInterruptedWhileStopping() throws InterruptedException {

        // given
        final Logger logger = mockTestLogger();

        final MetricOutput metricOutput = new MetricOutput() {

            @Override
            public String getName() {
                return UUID.randomUUID().toString();
            }

            @Override
            public boolean accepts(Metric.Key key) {
                return true;
            }

            @Override
            public void write(long timestamp, Metric.Key key, long value) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }

            @Override
            public void flush() {

            }
        };

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedName = UUID.randomUUID().toString();

        final BasicMetricsRegistry metricsRegistry = new BasicMetricsRegistry();

        final MetricConfig countConfig = MetricConfigFactory.createCountConfig(true, expectedName, false);
        final Metric metric = new DefaultMetricsFactory(Collections.singletonList(countConfig)).createMetric("test-component", expectedName);
        metricsRegistry.register(metric);
        metric.store(2);

        final ScheduledMetricsProcessor processor = new ScheduledMetricsProcessor( 0, 1000, createTestClock(expectedTimestamp), metricsRegistry, new BasicMetricOutputsRegistry(metricOutput));


        // when
        processor.start();

        final CountDownLatch latch = new CountDownLatch(1);
        final Thread t1 = new Thread(() -> {
            latch.countDown();
            processor.stop();
        });

        t1.start();

        //noinspection ResultOfMethodCallIgnored
        latch.await(500L, TimeUnit.MILLISECONDS);

        t1.interrupt();

        // then
        verify(logger, timeout(500)).debug("{}: Stopping", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, never()).warn("{}: Thread did not stop in time. In-flight data may be lost", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, timeout(500)).error("{}: Thread interrupted. In-flight data may be lost", ScheduledMetricsProcessor.class.getSimpleName());
        verify(logger, timeout(500)).debug("{}: Stopped", ScheduledMetricsProcessor.class.getSimpleName());

        InternalLogging.setLogger(null);

    }

    private ScheduledMetricsProcessor createTestProcessor(final long initialDelay,
                                                 final long interval,
                                                 final Clock clock,
                                                 final MetricsRegistry metricsRegistry,
                                                 final MetricOutput... metricOutputs) {
        return new ScheduledMetricsProcessor(initialDelay, interval, clock, metricsRegistry, new BasicMetricOutputsRegistry(metricOutputs));
    }

    private LifeCycle createLifeCycleTestObject(final MetricsRegistry registry, final MetricOutput writer, final long expectedTimestamp, final String expectedName) {

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        registry.register(metric);

        final Clock clock = createTestClock(expectedTimestamp);

        return createTestProcessor(initialDelay, interval, clock, registry, writer);
    }

    private Clock createTestClock(long expectedTimestamp) {

        return new Clock() {

            @Override
            public ZoneId getZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException("Time zone testing with this Clock instance is not available");
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(expectedTimestamp);
            }

        };

    }
}
