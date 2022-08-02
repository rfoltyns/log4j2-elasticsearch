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
 * All methods of this class have no side effects.
 * <p>{@link #getKey()} returns {@link Metric.Key} with metric type {@code "noop"}
 */
public class NoopMetric implements Metric {

    private static final String NOOP_METRIC_TYPE = "noop";
    private static final long ZERO = 0L;

    private final Key key;

    public NoopMetric(final String componentName, final String metricName) {
        this.key = new Key(componentName, metricName, NOOP_METRIC_TYPE);
    }

    @Override
    public Metric.Key getKey() {
        return key;
    }

    @Override
    public long reset() {
        return ZERO;
    }

    @Override
    public void store(long value) {

    }

    @Override
    public void store(int value) {

    }

    @Override
    public long getValue() {
        return ZERO;
    }

    @Override
    public void accept(MetricCollector metricCollector) {
        noop();
    }

    public static void noop() {

    }

}
