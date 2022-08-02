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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Supports following, resettable metric types:
 * <ul>
 *     <li>noop</li>
 *     <li>max</li>
 *     <li>count</li>
 * </ul>
 */
public class DefaultMetricsFactory implements MetricsFactory {

    private final Map<String, MetricConfig> metricConfigs = new ConcurrentSkipListMap<>();

    public DefaultMetricsFactory() {
        this(Collections.emptyList());
    }

    /**
     * @param initialConfigs initial metric configs; MUST NOT be null; MAY be empty
     */
    public DefaultMetricsFactory(final List<MetricConfig> initialConfigs) {
        initialConfigs.forEach(this::configure);
    }

    /**
     * @return known {@link MetricConfig}s
     */
    @Override
    public final List<MetricConfig> getMetricConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(metricConfigs.values()));
    }

    /**
     * {@inheritDoc}
     *
     * For each {@code metricConfig}:
     * <ul>
     *     <li>If current component has a metric config with same name and type, it will be replaced with given {@code metricConfig}</li>
     *     <li>If current component has a metric config with same name and different type, it will be replaced with given {@code metricConfig}</li>
     *     <li>If current component does not have a metric config with same name, given {@code metricConfig} will be ignored</li>
     * </ul>
     */
    @Override
    public final MetricsFactory configure(final List<MetricConfig> metricConfigs) {

        for (MetricConfig c : metricConfigs) {
            configure(c);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MetricsFactory configure(final MetricConfig metricConfig) {

        metricConfigs.putIfAbsent(metricConfig.getName(), metricConfig);

        metricConfigs.computeIfPresent(metricConfig.getName(), (key, metricConfig1) -> {
            if (metricConfig1 instanceof SuppliedMetricConfig) {
                return MetricConfigFactory.createSuppliedConfig(metricConfig1.getMetricType(), metricConfig.isEnabled(), metricConfig1.getName());
            }
            return metricConfig;
        });

        return this;
    }

    /**
     * Creates {@link Metric} with configured {@link MetricConfig}.
     * Creates {@link NoopMetric} if no {@link MetricConfig} found.
     * Creates {@link NoopMetric} if {@link MetricConfig#isEnabled()} is {@code false}.
     *
     * @param componentName component name
     * @param metricName metric name
     *
     * @return noop, if enabled, matching {@link MetricConfig} was not found. Otherwise {@link Metric} of configured type
     */
    @Override
    public final Metric createMetric(final String componentName, final String metricName) {

        final MetricConfig metricConfig = metricConfigs.get(metricName);
        if (metricConfig == null) {

            getLogger().debug("{}: Metric {} not found for component {}. Returning no-op.",
                    getClass().getSimpleName(),
                    metricName,
                    componentName);

            return new NoopMetric(componentName, metricName);

        }

        if (!metricConfig.isEnabled()) {

            getLogger().debug("{}: Metric {} disabled for component {}. Returning no-op.",
                    getClass().getSimpleName(),
                    metricName,
                    componentName);
            return new NoopMetric(componentName, metricName);

        }

        switch (metricConfig.getMetricType()) {

            case MAX: {
                return new MaxLongMetric(new Metric.Key(componentName, metricName, "max"), 0L, metricConfig.isReset());
            }
            default:
            case COUNT: {
                return new CountMetric(new Metric.Key(componentName, metricName, "count"), 0L, metricConfig.isReset());
            }

        }

    }

    /**
     * Creates {@link Metric} with configured {@link SuppliedMetricConfig}.
     * Creates {@link NoopMetric} if no {@link MetricConfig} found.
     * Creates {@link NoopMetric} if not {@link MetricConfig#isEnabled()}.
     *
     * @param componentName component name
     * @param metricName metric name
     *
     * @return new {@link Metric}
     */
    @Override
    public final Metric createMetric(final String componentName, final String metricName, final MetricValueSupplier metricValueSupplier) {

        final MetricConfig metricConfig = metricConfigs.get(metricName);
        if (metricConfig == null) {

            getLogger().debug("{}: Metric {} not found for component {}. Returning no-op.",
                    getClass().getSimpleName(),
                    metricName,
                    componentName);

            return new NoopMetric(componentName, metricName);

        }

        if (!(metricConfig instanceof SuppliedMetricConfig)) {

            getLogger().warn("{}: No compatible metric config found for given metric supplier {}:{} . Returning no-op.",
                    getClass().getSimpleName(),
                    componentName,
                    metricName);

            return new NoopMetric(componentName, metricName);

        }

        if (!metricConfig.isEnabled()) {

            getLogger().debug("{}: Metric {} disabled for component {}. Returning no-op.",
                    getClass().getSimpleName(),
                    metricName,
                    componentName);

            return new NoopMetric(componentName, metricName);

        }

        final String metricType = metricConfig.getMetricType().name().toLowerCase();
        final Metric.Key key = new Metric.Key(componentName, metricName, metricType);

        getLogger().debug("{}: Metric {} enabled",
                getClass().getSimpleName(),
                metricName,
                componentName);

        return new SuppliedMetric(key, metricValueSupplier);

    }

}
