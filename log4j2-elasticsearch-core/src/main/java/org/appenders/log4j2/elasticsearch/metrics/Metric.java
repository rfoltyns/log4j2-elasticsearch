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
import java.util.Objects;

/**
 * Resettable value store.
 * <p>
 * Identified by {@link Metric.Key}
 * <p>
 * Implementations of this class MUST provide current value via {@link #accept(MetricCollector)} ()}.
 */
public interface Metric {

    Metric.Key getKey();

    /**
     * SHOULD store given {@code long} value
     *
     * @param value value to store
     */
    void store(long value);

    /**
     * SHOULD store given {@code int} value
     *
     * @param value value to store
     */
    void store(int value);

    /**
     * @return current value
     */
    long getValue();

    /**
     * MAY reset to initial state
     *
     * @return value before {@code reset()}
     */
    long reset();

    /**
     * @param metricCollector metric value consumer
     */
    void accept(MetricCollector metricCollector);

    /**
     * {@link Metric} identifier
     *
     * <p>Equality and comparison is determined by {@code componentNamePart} and {@code metricNamePart} only, {@code metricTypePart} is omitted.
     */
    class Key implements Comparable<Key> {

        private static final Comparator<String> NATURAL_ORDER = Comparator.naturalOrder();
        private static final int COMPONENT_NAME_PART_INDEX = 0;
        private static final int METRIC_NAME_PART_INDEX = 1;
        private static final int METRIC_TYPE_PART_INDEX = 2;

        private final String fullKey;

        private final String[] parts = new String[4];

        public Key(final String componentPart, final String metricNamePart, final String metricTypePart) {
            this.parts[COMPONENT_NAME_PART_INDEX] = componentPart;
            this.parts[METRIC_NAME_PART_INDEX] = metricNamePart;
            this.parts[METRIC_TYPE_PART_INDEX] = metricTypePart;
            this.fullKey = componentPart + "." + metricNamePart + "." + metricTypePart;
        }

        String getComponentNamePart() {
            return parts[COMPONENT_NAME_PART_INDEX];
        }

        String getMetricNamePart() {
            return parts[METRIC_NAME_PART_INDEX];
        }

        String getMetricTypePart() {
            return parts[METRIC_TYPE_PART_INDEX];
        }

        /**
         * Natural order. {@link #getComponentNamePart()} first, then {@link #getMetricNamePart()}.
         * <p>IMPORTANT! {@link #getMetricTypePart()} is omitted!
         *
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Key o) {
            final int componentPart = NATURAL_ORDER.compare(getComponentNamePart(), o.getComponentNamePart());
            return componentPart != 0 ? componentPart : NATURAL_ORDER.compare(getMetricNamePart(), o.getMetricNamePart());
        }

        /**
         * @return comma-separated {@link #getComponentNamePart()}, {@link #getMetricNamePart()} and {@link #getMetricTypePart()}
         */
        @Override
        public String toString() {
            return fullKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            final Key key = (Key) o;
            return parts[COMPONENT_NAME_PART_INDEX].equals(key.parts[COMPONENT_NAME_PART_INDEX])
                    && parts[METRIC_NAME_PART_INDEX].equals(key.parts[METRIC_NAME_PART_INDEX]);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parts[COMPONENT_NAME_PART_INDEX], parts[METRIC_NAME_PART_INDEX]);
        }

    }

}
