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
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

public class MetricsFactoryTest {

    @Test
    public void createsNoopByDefault() {

        // given
        final MetricsFactory factory = new MetricsFactory() {

            @Override
            public List<MetricConfig> getMetricConfigs() {
                return null;
            }

            @Override
            public MetricsFactory configure(List<MetricConfig> metricConfigs) {
                return this;
            }

            @Override
            public MetricsFactory configure(MetricConfig metricConfig) {
                return this;
            }

        };

        // when
        final Metric result = factory.createMetric("test-component", "not-found");

        // then
        assertTrue(result instanceof NoopMetric);

    }

    @Test
    public void ignoresMetricSupplierByDefault() {

        // given
        final MetricValueSupplier metricValueSupplier = Mockito.mock(MetricValueSupplier.class);

        final MetricsFactory factory = new MetricsFactory() {

            @Override
            public List<MetricConfig> getMetricConfigs() {
                return null;
            }

            @Override
            public MetricsFactory configure(List<MetricConfig> metricConfigs) {
                return this;
            }

            @Override
            public MetricsFactory configure(MetricConfig metricConfig) {
                return this;
            }

        };

        // when
        final Metric result = factory.createMetric("test-component", "not-found", metricValueSupplier);

        // then
        verifyNoInteractions(metricValueSupplier);
        assertTrue(result instanceof NoopMetric);

    }

}
