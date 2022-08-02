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

import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Consumer;

/**
 * Resettable, {@code java.util.concurrent.atomic.AtomicLong}-based metric
 * <p><i>Thread-safe</i>
 */
public final class MaxLongMetric implements Metric {

    private final Metric.Key key;
    private final LongAccumulator value;
    private final Consumer<MetricCollector> consumer;

    /**
     * 0 by default.
     *
     * @param key metric key
     * @param reset if <i>true</i>, reset to {@code initialValue} on each {@link #accept(MetricCollector)} call
     */
    public MaxLongMetric(final Metric.Key key, final boolean reset) {
        this(key, 0L, reset);
    }

    /**
     * @param key metric key
     * @param initialValue initial value
     * @param reset if <i>true</i>, reset to {@code initialValue} on each {@link #accept(MetricCollector)} call
     */
    public MaxLongMetric(final Metric.Key key, final long initialValue, final boolean reset) {
        this.key = key;
        this.value = new LongAccumulator(Long::max, initialValue);
        this.consumer = getConsumer(reset);
    }

    private Consumer<MetricCollector> getConsumer(final boolean reset) {
        return reset ? new ResettingConsumer(this) : new DefaultConsumer();
    }

    @Override
    public Metric.Key getKey() {
        return key;
    }

    /**
     * Resets this instance to {@code initialValue} regardless of {@code reset} setting.
     *
     * @return value before reset
     */
    @Override
    public long reset() {
        return value.getThenReset();
    }

    /**
     * Stores given value, if it's higher than current.
     *
     * <p>Shares store with {@link #store(int)}
     *
     * @param value value to store
     */
    @Override
    public void store(final long value) {
        storeInternal(value);
    }

    /**
     * Stores given value, if it's higher than current.
     *
     * <p>Shares store with {@link #store(long)}

     * @param value value to store
     */
    @Override
    public void store(final int value) {
        storeInternal(value);
    }

    private void storeInternal(long l) {
        this.value.accumulate(l);
    }

    @Override
    public long getValue() {
        return value.longValue();
    }

    /**
     * Pass current value to given {@link MetricCollector}, then reset if needed.
     *
     * @param metricCollector metric value consumer
     */
    @Override
    public void accept(final MetricCollector metricCollector) {
        consumer.accept(metricCollector);
    }

    private class DefaultConsumer implements Consumer<MetricCollector> {


        @Override
        public void accept(final MetricCollector metricCollector) {
            metricCollector.collect(getKey(), getValue());
        }

    }

    private static class ResettingConsumer implements Consumer<MetricCollector> {

        private final Metric metric;

        public ResettingConsumer(final Metric metric) {
            this.metric = metric;
        }

        @Override
        public void accept(final MetricCollector metricCollector) {
            metricCollector.collect(metric.getKey(), metric.reset());
        }

    }

}
