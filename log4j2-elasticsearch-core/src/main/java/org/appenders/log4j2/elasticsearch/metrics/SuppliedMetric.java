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

/**
 * Read-only, {@link MetricValueSupplier}-based metric.
 *
 * <p>All store operations are noop.
 * <p>{@link org.appenders.log4j2.elasticsearch.metrics.Metric.Key} metric type SHOULD describe supplier behaviour as close as possible.
 */
public class SuppliedMetric implements Metric {

    private final Key key;
    private final MetricValueSupplier metricValueSupplier;

    /**
     * @param key metric key
     * @param metricValueSupplier value supplier
     */
    public SuppliedMetric(final Key key, final MetricValueSupplier metricValueSupplier) {
        this.key = key;
        this.metricValueSupplier = metricValueSupplier;
    }

    @Override
    public Key getKey() {
        return key;
    }

    @Override
    public void store(long value) {
        // noop
    }

    @Override
    public void store(int value) {
        // noop
    }

    @Override
    public long getValue() {
        return metricValueSupplier.getLong();
    }

    @Override
    public long reset() {
        return 0;
    }

    @Override
    public void accept(final MetricCollector metricCollector) {
        metricCollector.collect(key, metricValueSupplier.getLong());
    }

}
