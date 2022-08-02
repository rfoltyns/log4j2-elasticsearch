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

import org.appenders.core.logging.Logger;

import java.util.Collections;

/**
 * Logging metric output.
 */
public class MetricLog implements MetricOutput {

    private final String name;
    private final Logger logger;
    private final MetricFilter filter;

    /**
     * @param name output name
     * @param logger {@link Logger} instance
     */
    public MetricLog(final String name, final Logger logger) {
        this(name, logger, new IncludeExclude(Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * @param name output name
     * @param logger {@link Logger} instance
     * @param filter metric key filter
     */
    public MetricLog(final String name, final Logger logger, final MetricFilter filter) {
        this.name = name;
        this.logger = logger;
        this.filter = filter;
    }

    /**
     * @param timestamp timestamp
     * @param key metric key
     * @param value value to be written
     */
    @Override
    public final void write(long timestamp, Metric.Key key, long value) {
        logger.info("{} {}: {}={}", timestamp, this.name, key, value);
    }

    /**
     * Noop
     */
    @Override
    public final void flush() {
        // noop
    }

    /**
     * @param key metric key
     *
     * @return <i>true</i>, if fiven {@link Metric.Key} is accepted by this log instance, <i>false</i> otherwise.
     */
    @Override
    public final boolean accepts(final Metric.Key key) {
        return filter.accepts(key);
    }

}
