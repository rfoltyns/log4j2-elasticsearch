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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Resettable, {@code java.util.concurrent.atomic.AtomicLong}-based metric
 * <p>Both {@link #store(long)} and {@link #store(int)} add to the same, shared atomic variable.
 * <p><i>Thread-safe</i>
 */
public class CountMetric implements Metric {

    private final Metric.Key key;
    private final long initialValue;
    private final AtomicLong value;
    private final Consumer<MetricCollector> consumer;

    /**
     * 0 by default.
     *
     * @param key metric key
     */
    public CountMetric(final Metric.Key key) {
        this(key, 0L, false);
    }

    /**
     * @param key metric key
     * @param initialValue initial value
     * @param reset if <i>true</i>, set to {@code initialValue} on each {@link #accept(MetricCollector)} call
     */
    public CountMetric(final Metric.Key key, final long initialValue, final boolean reset) {
        this.key = key;
        this.initialValue = initialValue;
        this.value = new AtomicLong(initialValue);
        this.consumer = createMetricCollectorConsumer(reset);
    }

    private Consumer<MetricCollector> createMetricCollectorConsumer(final boolean reset) {
        if (reset) {
            return new ResettingConsumer(this);
        }
        return new DefaultConsumer();
    }

    /**
     * @return metric key
     */
    @Override
    public Metric.Key getKey() {
        return key;
    }

    /**
     * Resets metric to {@code initialValue}
     * @return previous value
     */
    @Override
    public long reset() {
        return value.getAndSet(initialValue);
    }

    /**
     * Stores given {@code long} value
     *
     * @param value value to store
     */
    @Override
    public void store(final long value) {
        this.value.addAndGet(value);
    }

    /**
     * Stores given {@code long} value
     *
     * @param value value to store
     */
    @Override
    public void store(final int value) {
        this.value.addAndGet(value);
    }

    /**
     * @return current value
     */
    @Override
    public long getValue() {
        return value.longValue();
    }

    /**
     * Passes current value to given {@code metricCollector}. Resets to {@code initialValue} if constructed with {@code reset=true}
     *
     * @param metricCollector metric value consumer
     */
    @Override
    public void accept(final MetricCollector metricCollector) {
        consumer.accept(metricCollector);
    }

    private static class ResettingConsumer implements Consumer<MetricCollector> {

        private final CountMetric metric;

        public ResettingConsumer(final CountMetric metric) {
            this.metric = metric;
        }

        @Override
        public void accept(final MetricCollector metricCollector) {
            metricCollector.collect(metric.getKey(), metric.reset());
        }

    }

    private class DefaultConsumer implements Consumer<MetricCollector> {

        @Override
        public void accept(final MetricCollector metricCollector) {
            metricCollector.collect(getKey(), getValue());
        }

    }

}
