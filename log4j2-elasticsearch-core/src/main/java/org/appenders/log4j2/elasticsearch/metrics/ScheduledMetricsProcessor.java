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

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@inheritDoc}
 */
public class ScheduledMetricsProcessor extends MetricsProcessor implements LifeCycle {

    private static final long SHUTDOWN_TIMEOUT_MILLIS = Long.parseLong(System.getProperty("appenders.metrics.shutdownTimeoutMillis", "100"));

    private volatile State state = State.STOPPED;

    private final ScheduledExecutorService executor;
    private final long initialDelay;
    private final long interval;

    // visible for testing
    ScheduledMetricsProcessor(final ScheduledExecutorService executor,
                              final long initialDelay,
                              final long interval,
                              final Clock clock,
                              final MetricsRegistry metricsRegistry,
                              final MetricOutput[] metricOutputs) {
        super(clock, metricsRegistry, metricOutputs);
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.executor = executor;
    }

    /**
     * @param initialDelay millis before first run
     * @param interval time to wait after metric collection is completed
     * @param clock metric timestamp source
     * @param metricsRegistry registered metrics store
     * @param metricOutputs metric values listeners
     */
    public ScheduledMetricsProcessor(final long initialDelay,
                              final long interval,
                              final Clock clock,
                              final MetricsRegistry metricsRegistry,
                              final MetricOutput[] metricOutputs) {
        this(Executors.newSingleThreadScheduledExecutor(ScheduledMetricsProcessor::newThread),
                initialDelay,
                interval,
                clock,
                metricsRegistry,
                metricOutputs);
    }

    private static Thread newThread(final Runnable r) {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(ScheduledMetricsProcessor.class.getSimpleName());
        return t;
    }

    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        executor.scheduleWithFixedDelay(this::process,
                initialDelay,
                interval,
                TimeUnit.MILLISECONDS);

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        final String name = ScheduledMetricsProcessor.class.getSimpleName();
        getLogger().info("{}: Stopping", name);

        try {

            executor.shutdown();

            final boolean terminated = executor.awaitTermination(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!terminated) {
                getLogger().warn("{}: Thread did not stop in time. In-flight data may be lost", name);
                executor.shutdownNow();
            }

        } catch (InterruptedException e) {
            getLogger().error("{}: Thread interrupted. In-flight data may be lost", name);
        }

        state = State.STOPPED;

        getLogger().info("{}: Stopped", name);

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
