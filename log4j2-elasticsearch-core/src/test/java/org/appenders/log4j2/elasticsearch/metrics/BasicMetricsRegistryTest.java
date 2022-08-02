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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicMetricsRegistryTest {

    public static final String TEST_COMPONENT_NAME = "basic-metrics-registry-test";
    public static final String TEST_METRIC_NAME = "test-metric-name";

    @Test
    public void registersGivenMetric() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.register(metric);

        // then
        assertEquals(1, registry.getMetrics(registered -> true).size());

    }

    @Test
    public void deregistersGivenMetric() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.register(metric);
        registry.deregister(metric);

        // then
        assertEquals(0, registry.getMetrics(registered -> true).size());

    }

    @Test
    public void doesNotDeregisterUnknownMetric() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric1 = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);
        final Metric metric2 = new NoopMetric(TEST_COMPONENT_NAME + 2, TEST_METRIC_NAME);

        // when
        registry.register(metric1);
        registry.deregister(metric2);

        // then
        assertEquals(1, registry.getMetrics(registered -> true).size());

    }

    @Test
    public void versionIsIncrementedOnRegistration() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.register(metric);

        // then
        assertEquals(1, registry.version());

    }

    @Test
    public void versionIsIncrementedOnSuccessfulDeregistration() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.register(metric);
        registry.deregister(metric);

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void versionIsNotIncrementedOnUnsuccessfulDeregistration() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.deregister(metric);

        // then
        assertEquals(0, registry.version());

    }

    @Test
    public void versionIsIncrementedPerRegistration() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);

        // when
        registry.register(metric);
        registry.register(metric);

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void versionIsIncrementedOnClearIfMetricsAreRegistered() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);
        registry.register(metric);
        assertEquals(1, registry.version());

        // when
        registry.clear();

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void versionIsNotIncrementedOnClearIfRegistryIsEmpty() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);
        registry.register(metric);
        assertEquals(1, registry.version());

        // when
        registry.clear();
        assertEquals(2, registry.version());

        registry.clear();

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void replacesPreviousMetricOnRegistration() {

        // given
        final BasicMetricsRegistry registry = new BasicMetricsRegistry();

        final Metric metric1 = new NoopMetric(TEST_COMPONENT_NAME, TEST_METRIC_NAME);
        final Metric metric2 = new MaxLongMetric(new Metric.Key(TEST_COMPONENT_NAME, TEST_METRIC_NAME, "max"), 0L, false);

        // when
        registry.register(metric1);
        registry.register(metric2);
        final Set<Metric> metrics = registry.getMetrics(registered -> true);

        // then
        assertEquals(2, registry.version());
        assertEquals(1, metrics.size());
        assertEquals(metric2, metrics.toArray(new Metric[0])[0]);

    }

}
