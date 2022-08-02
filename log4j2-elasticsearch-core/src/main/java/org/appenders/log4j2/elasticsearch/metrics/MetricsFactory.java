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

import java.util.List;

public interface MetricsFactory {

    /**
     * @return Set of known {@link MetricConfig}
     */
    List<MetricConfig> getMetricConfigs();

    /**
     * Add or replace metric configs
     *
     * @param metricConfigs metric configs
     * @return metricsFactory instance with given {@code metricConfigs}
     */
    MetricsFactory configure(List<MetricConfig> metricConfigs);


    /**
     * Add or replace metric config
     *
     * @param metricConfig metric config
     * @return metricsFactory instance with given {@code metricConfig}
     */
    MetricsFactory configure(MetricConfig metricConfig);

    /**
     * By default, creates noop. Otherwise, MUST provide {@link Metric} instance configured with {@link #configure(MetricConfig)} or {@link #configure(List)}
     *
     * @param componentName component name
     * @param metricName metric name
     *
     * @return noop
     */
    default Metric createMetric(String componentName, String metricName) {
        return new NoopMetric(componentName, metricName);
    }

    /**
     * By default, creates noop. Otherwise, MUST provide {@link Metric} instance configured with {@link #configure(MetricConfig)} or {@link #configure(List)}
     *
     * @param componentName component name
     * @param metricName metric name
     * @param supplier metric value supplier
     *
     * @return noop, unless {@link MetricConfig} matching given metricName
     */
    default Metric createMetric(String componentName, String metricName, MetricValueSupplier supplier) {
        return new NoopMetric(componentName, metricName);
    }


}
