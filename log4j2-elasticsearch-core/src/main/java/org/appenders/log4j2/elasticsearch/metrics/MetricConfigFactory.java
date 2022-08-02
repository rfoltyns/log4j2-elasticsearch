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

public class MetricConfigFactory {

    private MetricConfigFactory() {
        // static
    }

    public static MetricConfig createMaxConfig(final String name, final boolean reset) {
        return createMaxConfig(true, name, reset);
    }

    public static MetricConfig createMaxConfig(final boolean enabled, final String name, final boolean reset) {
        return new MetricConfig(MetricType.MAX, name, enabled, reset);
    }

    public static MetricConfig createCountConfig(final String name) {
        return createCountConfig(true, name);
    }

    public static MetricConfig createCountConfig(final boolean enabled, final String name) {
        return createCountConfig(enabled, name, true);
    }

    public static MetricConfig createCountConfig(final boolean enabled, final String name, boolean reset) {
        return new MetricConfig(MetricType.COUNT, name, enabled, reset);
    }

    public static MetricConfig createSuppliedConfig(final MetricType metricType, final boolean enabled, final String name) {
        return new SuppliedMetricConfig(metricType, name, enabled);
    }

}
