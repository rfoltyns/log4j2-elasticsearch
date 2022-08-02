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
 * {@link Metric} destination.
 * <p>Implementations of this class can be used as {@link MetricsProcessor} output consumers
 * <p>Can filter with {@link #accepts(Metric.Key)}
 */
public interface MetricOutput {

    /**
     * @param key metric key
     * @return if <i>true</i>, writer SHOULD handle given {@link Metric.Key}. Otherwise, {@link #write(long, Metric.Key, long)} and {@link #flush()} are expected to be noop.
     */
    boolean accepts(final Metric.Key key);

    /**
     * MAY persist or delegate metric value to reliable receiver. Subsequent calls without immediate {@link #flush()} afterwards MUST be accepted
     *
     * @param timestamp timestamp
     * @param key metric key
     * @param value value to be written
     */
    void write(long timestamp, Metric.Key key, long value);

    /**
     * Wrap-up current batch of {@link #write(long, Metric.Key, long)} calls
     */
    void flush();

}
