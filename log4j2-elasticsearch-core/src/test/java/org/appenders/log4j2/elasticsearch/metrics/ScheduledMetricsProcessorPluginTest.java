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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledMetricsProcessorPluginTest {

    final int envDelay = 100;

    @Test
    public void processWithConfiguredSchedule() {

        // given
        final int initialDelay = 100;
        final long interval = 100;
        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "count");
        final long expectedValue = new Random().nextInt(100) + 1;

        final MetricsRegistry registry = new BasicMetricsRegistry();

        final MetricConfig metricConfig = MetricConfigFactory.createCountConfig(expectedName);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.singletonList(metricConfig));
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        registry.register(metric);
        metric.store(expectedValue);

        final Clock clock = createTestClock(expectedTimestamp);

        final ScheduledMetricsProcessorPlugin plugin = ScheduledMetricsProcessorPlugin.newBuilder()
                .withInitialDelay(initialDelay)
                .withInterval(interval)
                .withClock(clock)
                .withMetricsRegistry(registry)
                .withMetricOutputs(new MetricOutput[] {metricOutput})
                .build();

        assertTrue(plugin.isStopped());

        // when
        plugin.start();

        // then
        assertFalse(plugin.isStopped());
        assertTrue(plugin.isStarted());

        verify(metricOutput, timeout(initialDelay + envDelay)).write(eq(expectedTimestamp), eq(expectedKey), eq(expectedValue));
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(interval + envDelay));
        verify(metricOutput, atLeastOnce()).write(eq(expectedTimestamp), eq(expectedKey), eq(0L));

    }

    @Test
    public void builderThrowsIfClockIsNull() {

        // given
        final ScheduledMetricsProcessorPlugin.Builder builder = createDefaultTestMetricProcessorBuilder()
                .withClock(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), CoreMatchers.containsString("No clock provided for " + ScheduledMetricsProcessorPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfMetricsRegistryIsNull() {

        // given
        final ScheduledMetricsProcessorPlugin.Builder builder = createDefaultTestMetricProcessorBuilder()
                .withMetricsRegistry(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), CoreMatchers.containsString("No metricRegistry provided for " + ScheduledMetricsProcessorPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsIfMetricOutputsAreNull() {

        // given
        final ScheduledMetricsProcessorPlugin.Builder builder = createDefaultTestMetricProcessorBuilder()
                .withMetricOutputs(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), CoreMatchers.containsString("No metricOutputs provided for " + ScheduledMetricsProcessorPlugin.PLUGIN_NAME));

    }

    private ScheduledMetricsProcessorPlugin.Builder createDefaultTestMetricProcessorBuilder() {
        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = mock(MetricOutput.class);
        when(metricOutput.accepts(any())).thenReturn(true);

        final ScheduledMetricsProcessorPlugin.Builder builder = ScheduledMetricsProcessorPlugin.newBuilder()
                .withMetricsRegistry(registry)
                .withMetricOutputs(new MetricOutput[] {metricOutput});
        return builder;
    }

    @Test
    public void builderThrowsIfNameIsNull() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final MetricLogPlugin.Builder builder = MetricLogPlugin.newBuilder()
                .withName(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), CoreMatchers.containsString("No name provided for MetricLog"));

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
