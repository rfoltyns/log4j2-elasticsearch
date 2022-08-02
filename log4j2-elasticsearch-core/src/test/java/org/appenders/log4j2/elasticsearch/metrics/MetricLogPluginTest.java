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
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.core.logging.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class MetricLogPluginTest {

    @Test
    public void writesLongs() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final long expectedTimestamp = System.currentTimeMillis();
        final String expectedLogName = UUID.randomUUID().toString();
        final long expectedMetricValue = new Random().nextLong();

        final Logger logger = InternalLoggingTest.mockTestLogger();
        final MetricLog metricLog = MetricLogPlugin.newBuilder()
                .withName(expectedLogName)
                .build();

        // when
        metricLog.write(expectedTimestamp, key, expectedMetricValue);

        // then
        verify(logger).info("{} {}: {}={}",
                expectedTimestamp,
                expectedLogName,
                key,
                expectedMetricValue);

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

    @Test
    public void doesNotAcceptMetricsExcludedByName() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final MetricLog metricLog = MetricLogPlugin.newBuilder()
                .withName("test-metric-log")
                .withExcludes("test-metric")
                .build();

        // when
        final boolean result = metricLog.accepts(key);

        // then
        assertFalse(result);

    }

    @Test
    public void acceptsMetricsIncludedByName() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final MetricLog metricLog = MetricLogPlugin.newBuilder()
                .withName("test-metric-log")
                .withIncludes("test-metric")
                .build();

        // when
        final boolean result = metricLog.accepts(key);

        // then
        assertTrue(result);

    }

    @Test
    public void acceptsAllMetricsIfBothIncludesAndExcludesAreNull() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final MetricLog metricLog = MetricLogPlugin.newBuilder()
                .withName("test-metric-log")
                .withIncludes(null)
                .withExcludes(null)
                .build();

        // when
        final boolean result = metricLog.accepts(key);

        // then
        assertTrue(result);

    }

    @Test
    public void flushHasNoEffect() {

        // given
        final Metric.Key key = new Metric.Key("test-component", "test-metric", "test");

        final long notExpectedTimestamp = System.currentTimeMillis();
        final String notExpectedLogName = UUID.randomUUID().toString();
        final long notExpectedMetricValue = new Random().nextLong();

        final Logger logger = InternalLoggingTest.mockTestLogger();
        final MetricLog metricLog = new MetricLog(notExpectedLogName, logger);

        // when
        metricLog.write(notExpectedTimestamp, key, notExpectedMetricValue);
        assertEquals(1, mockingDetails(logger).getInvocations().size());
        reset(logger); // reset mock

        metricLog.flush();

        // then
        assertEquals(0, mockingDetails(logger).getInvocations().size());

    }

}
