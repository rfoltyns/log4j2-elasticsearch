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

import org.appenders.log4j2.elasticsearch.LifeCycle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@link MetricOutput} registry.
 * <p>Output storage and {@link #get(Predicate)} result order are determined by {@link MetricOutput#COMPARATOR}
 * <p>Each {@link #register(MetricOutput)} call results in {@link #version()} incremented by 1.
 * <p>Each {@link #register(MetricOutput)} results in metric output being successfully registered.
 */
public class BasicMetricOutputsRegistry implements MetricOutputsRegistry, LifeCycle {

    static final Predicate<MetricOutput> ALL = metricOutput -> true;
    private volatile State state = State.STOPPED;
    private final AtomicInteger version = new AtomicInteger(1);
    private final Collection<MetricOutput> outputs = new ConcurrentSkipListSet<>(MetricOutput.COMPARATOR);

    public BasicMetricOutputsRegistry() {
        this(Collections.emptyList());
    }

    /**
     * @param initialMetricOutputs initial outputs
     */
    public BasicMetricOutputsRegistry(final MetricOutput... initialMetricOutputs) {
        this(Arrays.asList(initialMetricOutputs));
    }

    /**
     * @param initialMetricOutputs initial outputs
     */
    public BasicMetricOutputsRegistry(final List<MetricOutput> initialMetricOutputs) {
        if (initialMetricOutputs.size() > 0) {
            outputs.addAll(initialMetricOutputs);
        }
    }

    @Override
    public long version() {
        return version.get();
    }

    @Override
    public Set<MetricOutput> get(final Predicate<MetricOutput> predicate) {
        return outputs.stream()
                .filter(predicate)
                .collect(Collectors.toCollection(() -> new ConcurrentSkipListSet<>(MetricOutput.COMPARATOR)));

    }

    /**
     * Registers given {@link MetricOutput}. Registry version in incremented by 1 on each call.
     * <p>Metric outputs are stored and returned in natural order. See {@link MetricOutput#COMPARATOR}.
     * <p>If no matching metric output found, given metric output is registered successfully.
     * <p>If matching metric output found, given metric output replaces it.
     *
     * @param metricOutput metric output to become registered
     */
    @Override
    public void register(final MetricOutput metricOutput) {
        outputs.remove(metricOutput);
        outputs.add(metricOutput);
        version.incrementAndGet();
    }

    @Override
    public void deregister(final String name) {
        if (outputs.removeIf(metricOutput -> metricOutput.getName().equals(name))) {
            version.incrementAndGet();
        }
    }

    @Override
    public void deregister(final MetricOutput metricOutput) {
        if (outputs.remove(metricOutput)) {
            version.incrementAndGet();
        }
    }

    @Override
    public void clear() {
        outputs.clear();
        version.incrementAndGet();
    }


    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        final String name = BasicMetricOutputsRegistry.class.getSimpleName();
        getLogger().debug("{}: Starting", name);

        for (MetricOutput output : outputs) {
            LifeCycle.of(output).start();
        }

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        final String name = BasicMetricOutputsRegistry.class.getSimpleName();
        getLogger().debug("{}: Stopping", name);

        for (MetricOutput output : outputs) {
            LifeCycle.of(output).stop();
        }


        state = State.STOPPED;

        getLogger().debug("{}: Stopped", name);

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
