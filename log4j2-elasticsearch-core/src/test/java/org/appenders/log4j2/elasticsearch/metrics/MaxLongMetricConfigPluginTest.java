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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MaxLongMetricConfigPluginTest {

    private static final String DEFAULT_TEST_NAME = "testMetricName";

    static MaxMetricConfigPlugin.Builder createDefaultTestBuilder() {
        return MaxMetricConfigPlugin.newBuilder();
    }

    static MaxMetricConfigPlugin.Builder createDefaultTestBuilder(final String defaultTestName) {
        return createDefaultTestBuilder()
                .withName(defaultTestName);
    }

    @Test
    public void defaultBuilderThrows() {

        // given
        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No name provided for Max metric"));

    }

    @Test
    public void builderThrowsWhenNameIsNull() {

        // given
        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder()
                .withName(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No name provided for Max metric"));

    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder()
                .withName(DEFAULT_TEST_NAME);

        // when
        final MaxMetricConfigPlugin result = builder.build();

        // then
        assertNotNull(result);

    }

    @Test
    public void builderSetsAllFields() {

        // given
        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder()
                .withName(DEFAULT_TEST_NAME)
                .enabled(true)
                .withResetOnCollect(true);

        // when
        final MaxMetricConfigPlugin result = builder.build();

        // then
        assertNotNull(result);
        assertTrue(result.isReset());
        assertEquals(DEFAULT_TEST_NAME, result.getName());

    }

    @Test
    public void enabledByDefault() {

        // given
        long expectedOnCollect = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(expectedOnCollect);
        metric.accept(metricCollector);

        // then
        verify(metricCollector).collect(eq(expectedKey), eq(expectedOnCollect));

    }

    @Test
    public void collectsIfEnabled() {

        // given
        long expectedOnCollect = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);
        builder.enabled(true);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(expectedOnCollect);
        metric.accept(metricCollector);

        // then
        verify(metricCollector).collect(eq(expectedKey), eq(expectedOnCollect));

    }

    @Test
    public void doesNotCollectIfDisabled() {

        // given
        long expectedOnCollect = 0L;
        long notExpectedOnCollect = 10L;

        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);
        builder.enabled(false);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(notExpectedOnCollect);
        metric.accept(metricCollector);
        metric.accept(metricCollector);

        // then
        verify(metricCollector, never()).collect(eq(expectedKey), eq(notExpectedOnCollect));
        verify(metricCollector, never()).collect(eq(expectedKey), eq(expectedOnCollect));

    }

    @Test
    public void resetsByDefault() {

        // given
        long expectedOnCollect = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(expectedOnCollect);
        metric.accept(metricCollector);
        metric.accept(metricCollector);

        // then
        verify(metricCollector).collect(eq(expectedKey), eq(0L));
        verify(metricCollector).collect(eq(expectedKey), eq(expectedOnCollect));

    }

    @Test
    public void doesNotResetIfResetDisabled() {

        // given
        long expectedOnCollect = 10L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);
        builder.withResetOnCollect(false);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(expectedOnCollect);
        metric.accept(metricCollector);
        metric.accept(metricCollector);

        // then
        verify(metricCollector, times(2)).collect(eq(expectedKey), eq(expectedOnCollect));

    }

    @Test
    public void resetsIfResetEnabled() {

        // given
        long expectedOnCollect = 10L;
        long expectedAfterCollect = 0L;
        final String expectedName = UUID.randomUUID().toString();
        final Metric.Key expectedKey = new Metric.Key("test-component", expectedName, "max");

        final MaxMetricConfigPlugin.Builder builder = createDefaultTestBuilder(expectedName);
        builder.withResetOnCollect(true);

        final MetricsFactory metricsFactory = new DefaultMetricsFactory(Collections.emptyList());
        metricsFactory.configure(builder.build());
        final Metric metric = metricsFactory.createMetric("test-component", expectedName);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(expectedOnCollect);
        metric.accept(metricCollector);
        metric.accept(metricCollector);

        // then
        verify(metricCollector).collect(eq(expectedKey), eq(expectedOnCollect));
        verify(metricCollector).collect(eq(expectedKey), eq(expectedAfterCollect));

    }

}
