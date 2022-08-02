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
import java.util.function.Predicate;

/**
 * Matches {@link Metric.Key} parts against {@code includes} first, then {@code excludes}.
 */
public class IncludeExclude implements MetricFilter {

    private final Predicate<Metric.Key> includes;
    private final Predicate<Metric.Key> excludes;

    /**
     * <p>{@code includes} and {@code excludes} accept comma-separated list of values that {@link Metric.Key} parts should contain.
     * <p>If {@code includes} list is empty, all {@link Metric.Key}s are accepted unless matched against {@code excludes}.
     * <p>If {@code excludes} list is empty, all {@link Metric.Key}s are matched against {@code includes}.
     * <p>Wildcards support is limited to "all or nothing" in {@code includes} and {@code excludes} respectively.
     * <p>If {@code includes} list contains wildcard {@code "*"}, all {@link Metric.Key}s are accepted unless matched against {@code excludes}.
     * <p>If {@code excludes} list contains wildcard {@code "*"}, {@code includes} are overridden, all {@link Metric.Key}s are rejected.
     *
     * @param includes list of comma-separated patterns
     * @param excludes list of comma-separated patterns
     */
    public IncludeExclude(final List<String> includes, final List<String> excludes) {
        this.includes = getIncludes(includes);
        this.excludes = getExcludes(excludes);
    }

    /**
     * @param key {@link Metric.Key} to check
     * @return <i>true</i>, if {@link Metric.Key} accepted, <i>false</i> otherwise
     */
    @Override
    public boolean accepts(final Metric.Key key) {

        if (!includes.test(key)) {
            return false;
        }

        //noinspection RedundantIfStatement
        if (excludes.test(key)) {
            return false;
        }

        return true;

    }

    private Predicate<Metric.Key> getIncludes(final List<String> includes) {

        if (includes.contains("*") || includes.isEmpty()) {
            return key -> true;
        }

        return key -> {

            final String metricName = key.getMetricNamePart().toUpperCase();
            final String componentName = key.getComponentNamePart().toUpperCase();
            for (String include : includes) {
                final String value = include.toUpperCase();
                if (metricName.contains(value)) {
                    return true;
                }
                if (componentName.contains(value)) {
                    return true;
                }
            }

            return false;
        };

    }

    private Predicate<Metric.Key> getExcludes(final List<String> excludes) {

        if (excludes.contains("*")) {
            return key -> true;
        }

        if (excludes.isEmpty()) {
            return key -> false;
        }

        return key -> excludes.stream()
                .anyMatch(exclude -> {
                    final String metricName = key.getMetricNamePart().toUpperCase();
                    final String componentName = key.getComponentNamePart().toUpperCase();
                    final String value = exclude.toUpperCase();
                    return metricName.contains(value) || componentName.contains(value);
                });

    }

}
