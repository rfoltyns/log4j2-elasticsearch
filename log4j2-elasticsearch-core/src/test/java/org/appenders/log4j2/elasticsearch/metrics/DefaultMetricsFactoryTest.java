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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultMetricsFactoryTest {

    @Test
    public void createsMetricsWithInitialMetricConfigs() {

        // given
        final String expectedCountName = UUID.randomUUID().toString();
        final String expectedMaxName = UUID.randomUUID().toString();
        final String expectedSuppliedName = UUID.randomUUID().toString();
        final long expectedSuppliedValue = new Random().nextLong();

        final List<MetricConfig> expectedMetricConfigs = Arrays.asList(MetricConfigFactory.createCountConfig(expectedCountName),
                MetricConfigFactory.createMaxConfig(expectedMaxName, true),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, true, expectedSuppliedName));

        // when
        final MetricsFactory factory = new DefaultMetricsFactory(expectedMetricConfigs);

        // then
        assertEquals(3, factory.getMetricConfigs().size());
        assertEquals(expectedCountName, factory.createMetric("test-component", expectedCountName).getKey().getMetricNamePart());
        assertEquals(expectedMaxName, factory.createMetric("test-component", expectedMaxName).getKey().getMetricNamePart());

        final Metric suppliedMetric = factory.createMetric("test-component", expectedSuppliedName, () -> expectedSuppliedValue);
        assertEquals(expectedSuppliedName, suppliedMetric.getKey().getMetricNamePart());
        assertEquals(expectedSuppliedValue, suppliedMetric.getValue());

    }

    @Test
    public void createsNoopByIfMetricNotRegistered() {

        // given
        final MetricsFactory factory = new DefaultMetricsFactory();

        // when
        final Metric result1 = factory.createMetric("test-component", "not-found");
        final Metric result2 = factory.createMetric("test-component", "not-found", () -> 1L);

        // then
        assertTrue(result1 instanceof NoopMetric);
        assertTrue(result2 instanceof NoopMetric);

    }

    @Test
    public void createsNoopIfMetricNotEnabled() {

        // given
        final String expectedMaxName = UUID.randomUUID().toString();
        final String expectedSuppliedName = UUID.randomUUID().toString();
        final long expectedSuppliedValue = new Random().nextLong();

        final List<MetricConfig> expectedMetricConfigs = Arrays.asList(
                MetricConfigFactory.createMaxConfig(false, expectedMaxName, true),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, false, expectedSuppliedName));

        final MetricsFactory factory = new DefaultMetricsFactory(expectedMetricConfigs);

        // when
        final Metric result1 = factory.createMetric("test-component", expectedMaxName);
        final Metric result2 = factory.createMetric("test-component", expectedSuppliedName, () -> expectedSuppliedValue);

        // then
        assertTrue(result1 instanceof NoopMetric);
        assertTrue(result2 instanceof NoopMetric);

    }

    @Test
    public void createsNoopIfSuppliedMetricNotCompatibleWithConfig() {

        // given
        final String expectedMaxName = UUID.randomUUID().toString();
        final long expectedSuppliedValue = new Random().nextLong();

        final List<MetricConfig> expectedMetricConfigs = Arrays.asList(
                MetricConfigFactory.createMaxConfig(true, expectedMaxName, true));

        final MetricsFactory factory = new DefaultMetricsFactory(expectedMetricConfigs);

        // when
        final Metric result1 = factory.createMetric("test-component", expectedMaxName, () -> expectedSuppliedValue);

        // then
        assertTrue(result1 instanceof NoopMetric);

    }

    @Test
    public void configuresWithEmptyListHasNoEffect() {

        final String metric1 = UUID.randomUUID().toString();
        final String metric2 = UUID.randomUUID().toString();

        final List<MetricConfig> expectedMetricConfigs = Arrays.asList(
                MetricConfigFactory.createCountConfig(metric1),
                MetricConfigFactory.createCountConfig(metric2));

        final MetricsFactory factory = new DefaultMetricsFactory(expectedMetricConfigs);

        // when
        factory.configure(Collections.emptyList());

        // then
        assertEquals(2, factory.getMetricConfigs().size());
        assertTrue(factory.createMetric("test-component", metric1) instanceof CountMetric);
        assertTrue(factory.createMetric("test-component", metric2) instanceof CountMetric);
        assertEquals(metric1, factory.createMetric("test-component", metric1).getKey().getMetricNamePart());
        assertEquals(metric2, factory.createMetric("test-component", metric2).getKey().getMetricNamePart());

    }

    @Test
    public void configureReplacesMetricConfigWithSameName() {

        // given
        final String metric1 = UUID.randomUUID().toString();
        final String metric2 = UUID.randomUUID().toString();
        final String metric3 = UUID.randomUUID().toString();

        final List<MetricConfig> expectedMetricConfigs = Arrays.asList(
                MetricConfigFactory.createCountConfig(metric1),
                MetricConfigFactory.createCountConfig(metric2),
                MetricConfigFactory.createCountConfig(metric3));

        final MetricsFactory factory = new DefaultMetricsFactory(expectedMetricConfigs);

        // when
        factory.configure(MetricConfigFactory.createMaxConfig(metric1, false));
        factory.configure(Arrays.asList(
                MetricConfigFactory.createMaxConfig(metric2, false),
                MetricConfigFactory.createMaxConfig(metric3, false)));

        // then
        assertEquals(3, factory.getMetricConfigs().size());
        assertTrue(factory.createMetric("test-component1", metric1) instanceof MaxLongMetric);
        assertTrue(factory.createMetric("test-component2", metric2) instanceof MaxLongMetric);
        assertTrue(factory.createMetric("test-component3", metric3) instanceof MaxLongMetric);
        assertEquals(metric2, factory.createMetric("test-component2", metric2).getKey().getMetricNamePart());
        assertEquals(metric3, factory.createMetric("test-component3", metric3).getKey().getMetricNamePart());

    }

}
