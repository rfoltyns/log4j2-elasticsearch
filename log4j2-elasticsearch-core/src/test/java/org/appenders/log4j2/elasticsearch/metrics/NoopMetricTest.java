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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class NoopMetricTest {

    public static final NoopMetric DEFAULT_TEST_NOOP_METRIC = new NoopMetric("test-component", "test-metric");

    @Test
    public void metricTypeNameIsNoop() {

        // given
        final Metric metric = new NoopMetric("test-component", "test-metric");
        final Metric.Key expectedKey = new Metric.Key("test-component", "test-metric", "noop");

        // when
        final Metric.Key result = metric.getKey();

        // then
        assertEquals(expectedKey.getMetricTypePart(), result.getMetricTypePart());

    }

    @Test
    public void storesHaveNoEffect() {

        // given
        final Metric metric = DEFAULT_TEST_NOOP_METRIC;

        final MetricCollector metricCollector = mock(MetricCollector.class);

        // when
        metric.store(1);
        metric.store(1L);
        metric.accept(metricCollector);

        // then
        verifyNoInteractions(metricCollector);

    }

    @Test
    public void resetReturnsZero() {

        // given
        final Metric metric = DEFAULT_TEST_NOOP_METRIC;

        // when
        metric.store(1);
        metric.store(1L);
        final long result = metric.reset();

        // then
        assertEquals(0L, result);

    }

    @Test
    public void valueIsAlwaysZero() {

        // given
        final Metric metric = DEFAULT_TEST_NOOP_METRIC;

        // when
        metric.store(1);
        metric.store(1L);
        final long result = metric.getValue();

        // then
        assertEquals(0L, result);

    }

}
