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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultMetricsFactoryPluginTest {

    @Test
    public void defaultBuilderBuildsSuccessfully() {

        // given
        final DefaultMetricsFactoryPlugin.Builder builder = DefaultMetricsFactoryPlugin.newBuilder();

        // when
        final DefaultMetricsFactoryPlugin factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void defaultBuilderHasNoConfigs() {

        // given
        final DefaultMetricsFactoryPlugin.Builder builder = DefaultMetricsFactoryPlugin.newBuilder();

        // when
        final DefaultMetricsFactoryPlugin factory = builder.build();

        // then
        assertEquals(0, factory.getMetricConfigs().size());

    }

    @Test
    public void builderBuildsWithGivenMetricConfigs() {

        // given
        final String expectedCountName = UUID.randomUUID().toString();
        final String expectedResettableMaxName = UUID.randomUUID().toString();
        final String expectedMaxName = UUID.randomUUID().toString();

        final DefaultMetricsFactoryPlugin.Builder builder = DefaultMetricsFactoryPlugin.newBuilder()
                .withMetricConfigs(new MetricConfig[] {
                        MetricConfigFactory.createCountConfig(expectedCountName),
                        MetricConfigFactory.createMaxConfig(expectedResettableMaxName, false),
                        MetricConfigFactory.createMaxConfig(expectedMaxName, true)
                });

        // when
        final DefaultMetricsFactoryPlugin factory = builder.build();

        // then
        assertEquals(3, factory.getMetricConfigs().size());
        assertEquals(expectedCountName, factory.createMetric("test-component", expectedCountName).getKey().getMetricNamePart());
        assertEquals(expectedResettableMaxName, factory.createMetric("test-component", expectedResettableMaxName).getKey().getMetricNamePart());
        assertEquals(expectedMaxName, factory.createMetric("test-component", expectedMaxName).getKey().getMetricNamePart());

    }

}
