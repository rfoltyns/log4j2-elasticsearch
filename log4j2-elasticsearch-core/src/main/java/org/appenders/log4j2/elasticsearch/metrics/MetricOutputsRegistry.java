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
 * {@link MetricOutput}s store.
 */
public interface MetricOutputsRegistry {

    /**
     * Latest version of this instance. MUST change if output is added or removed
     *
     * @return latest version
     */
    long version();

    /**
     * @param predicate output filter
     * @return matching outputs
     */
    Set<MetricOutput> get(Predicate<MetricOutput> predicate);

    /**
     * SHOULD register given {@link MetricOutput}. On success, {@link #version()} MUST be updated.
     *
     * @param metricOutput output to be registered
     */
    void register(MetricOutput metricOutput);

    /**
     * SHOULD deregister {@link MetricOutput} by name. See {@link MetricOutput#getName()}. On success, {@link #version()} MUST be updated
     *
     * @param name output name
     */
    void deregister(String name);

    /**
     * SHOULD deregister given {@link MetricOutput}. On success, {@link #version()} MUST be updated
     *
     * @param metricOutput output to be deregistered
     */
    void deregister(MetricOutput metricOutput);

    /**
     * SHOULD remove all known outputs
     */
    void clear();

}
