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

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@link Metric} registry.
 * <p>Each {@link #register(Metric)} call results in {@link #version()} incremented by 1.
 * <p>Each {@link #register(Metric)} results in metric being successfully registered there was no {@code }metric.
 */
public class BasicMetricsRegistry implements MetricsRegistry {

    private static final Comparator<Metric> METRIC_KEY_COMPARATOR = Comparator.comparing(Metric::getKey);
    private static final String SIMPLE_NAME = BasicMetricsRegistry.class.getSimpleName();

    private final AtomicInteger version = new AtomicInteger();

    private final Set<Metric> metrics = new ConcurrentSkipListSet<>(Comparator.comparing(Metric::getKey));

    /**
     * Latest version of registry instance. Increments by 1 on each {@link #register(Metric)} call.
     *
     * @return latest version
     */
    @Override
    public long version() {
        return version.get();
    }

    /**
     * Registers given {@link Metric}. Registry version in incremented by 1 on each call.
     * <p>Metrics are stored and returned in natural order. See {@link Metric.Key#compareTo(Metric.Key)}.
     * <p>If no registered metric with same name and component found, given metric is registered successfully.
     * <p>If registered metric with same {@code metricName} and component found, registered metric is removed and given metric is registered successfully.
     *
     * @param metric metric to become registered
     * @return {@link org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry.Registration} post-registration callbacks
     */
    @Override
    public final Registration register(final Metric metric) {

        metrics.remove(metric);
        metrics.add(metric);
        version.incrementAndGet();

        getLogger().debug("{}: Added {}", SIMPLE_NAME, metric.getKey());

        return () -> this.deregister(metric);

    }

    /**
     * Deregisters given {@link Metric}.
     * <p>If registered metric with same {@code metricName} and {@code componentName} found, registered metric is removed and version is incremented by 1.
     * <p>If no registered metric with same name and component found, there are no side effects.
     *
     * @param metric metric to become unregistered
     */
    @Override
    public void deregister(final Metric metric) {

        if (metrics.remove(metric)) {
            version.incrementAndGet();
            getLogger().debug("{}: Removed {} ", SIMPLE_NAME, metric.getKey());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Metric> getMetrics(final Predicate<Metric> predicate) {
        return metrics.stream()
                .filter(predicate)
                .collect(Collectors.toCollection(() -> new ConcurrentSkipListSet<>(METRIC_KEY_COMPARATOR)));
    }

    @Override
    public void clear() {

        if (metrics.size() > 0) {
            getLogger().debug("{}: Removing {} metrics. Components: {}",
                    SIMPLE_NAME,
                    metrics.size(),
                    metrics.stream()
                            .map(metric -> metric.getKey().getComponentNamePart())
                            .distinct()
                            .collect(Collectors.toList()));
            metrics.clear();
            version.incrementAndGet();
        }

    }

}
