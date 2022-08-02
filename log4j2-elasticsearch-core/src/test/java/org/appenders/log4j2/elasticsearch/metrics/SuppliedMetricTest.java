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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SuppliedMetricTest {

    @Test
    public void retainsOriginalKey() {

        // given
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");


        // when
        final Metric metric = new SuppliedMetric(expectedKey, () -> 0L);

        // then
        assertSame(expectedKey, metric.getKey());

    }

    @Test
    public void storesHaveNoEffect() {

        // given
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");
        final long expectedValue = new Random().nextLong();

        final Metric metric = new SuppliedMetric(expectedKey, () -> expectedValue);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(1);
        metric.store(1L);
        metric.accept(metricCollector);

        // then
        verify(metricCollector).collect(eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void resetHasNoSideEffect() {

        // given
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");
        final long expectedValue = new Random().nextLong();

        final Metric metric = new SuppliedMetric(expectedKey, () -> expectedValue);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(1);
        metric.store(1L);

        metric.reset();
        assertEquals(metric.getValue(), expectedValue);

        metric.accept(metricCollector);

        // then
        assertEquals(metric.getValue(), expectedValue);
        verify(metricCollector).collect(eq(expectedKey), eq(expectedValue));

    }

    @Test
    public void resetReturnsZero() {

        // given
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");
        final long expectedValue = new Random().nextLong();

        final Metric metric = new SuppliedMetric(expectedKey, () -> expectedValue);

        // when
        metric.store(1);
        metric.store(1L);
        final long result = metric.reset();

        // then
        assertEquals(0L, result);

    }

    @Test
    public void valueIsAlwaysSupplied() {

        // given
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");
        final long expectedValue = new Random().nextLong();

        final Metric metric = new SuppliedMetric(expectedKey, () -> expectedValue);

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.accept(metricCollector);
        metric.accept(metricCollector);
        metric.accept(metricCollector);

        // then
        verify(metricCollector, times(3)).collect(eq(expectedKey), eq(expectedValue));

    }

}
