package org.appenders.log4j2.elasticsearch.ahc.backoff;

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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.ahc.BatchRequest;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;

@Plugin(name = Log4j2BatchLimitBackoffPolicy.PLUGIN_NAME, category = Node.CATEGORY, elementType = BackoffPolicy.NAME, printObject = true)
public class Log4j2BatchLimitBackoffPolicy extends BatchLimitBackoffPolicy<BatchRequest> {

    static final String PLUGIN_NAME = "BatchLimitBackoffPolicy";

    public Log4j2BatchLimitBackoffPolicy(final int maxBatchesInFlight) {
        super(maxBatchesInFlight);
    }

    @PluginBuilderFactory
    public static Log4j2BatchLimitBackoffPolicy.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BatchLimitBackoffPolicy> {

        static final int DEFAULT_MAX_BATCHES_IN_FLIGHT = 8;

        @PluginBuilderAttribute("maxBatchesInFlight")
        protected int maxBatchesInFlight = DEFAULT_MAX_BATCHES_IN_FLIGHT;

        @Override
        public final BatchLimitBackoffPolicy build() {

            if (maxBatchesInFlight <= 0) {
                throw new ConfigurationException("maxBatchesInFlight must be higher than 0 for " +
                        BatchLimitBackoffPolicy.class.getSimpleName());
            }

            return new Log4j2BatchLimitBackoffPolicy(maxBatchesInFlight);

        }

        /**
         * Sets the limit of allowed concurrent batches
         *
         * @param maxBatchesInFlight max number of concurrent batches
         * @return this
         */
        public Builder withMaxBatchesInFlight(final int maxBatchesInFlight) {
            this.maxBatchesInFlight = maxBatchesInFlight;
            return this;
        }

    }

}
