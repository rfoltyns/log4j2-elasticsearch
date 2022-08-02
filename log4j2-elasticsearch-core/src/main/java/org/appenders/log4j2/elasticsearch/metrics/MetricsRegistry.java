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

import java.util.Set;
import java.util.function.Predicate;

/**
 * {@link Metric}s registry.
 * <p>
 * Each {@link #register(Metric)} and {@link #register(Metric)} call MUST be reflected in {@link #version()} returning a unique identifier of current state.
 */
public interface MetricsRegistry {

    /**
     * Latest version of registry instance. MUST change if registry state changes on {@link #register(Metric)} call.
     *
     * @return latest version
     */
    long version();

    /**
     * SHOULD register given {@link Metric}. On success, registry version MUST be updated.
     *
     * @param metric metric to add
     * @return {@link Registration} handle
     */
    Registration register(Metric metric);

    /**
     * SHOULD deregister given {@link Metric}. On success, registry version MUST be updated.
     *
     * @param metric metric to remove
     */
    void deregister(Metric metric);

    /**
     * Returns metrics matching given predicate
     *
     * @param predicate metrics matcher
     * @return metrics matching given predicate
     */
    Set<Metric> getMetrics(Predicate<Metric> predicate);

    /**
     * MUST deregister all known {@link Metric}s. On success, registry version MUST be updated.
     */
    void clear();

    interface Registration {

        void deregister();

    }
}
