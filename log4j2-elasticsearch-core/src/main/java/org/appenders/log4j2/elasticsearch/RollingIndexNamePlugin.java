package org.appenders.log4j2.elasticsearch;

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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.time.ZoneId;

/**
 * Time-based rolling index name formatter. Caches formatted index name between rolls to minimize overhead.
 * <p>
 * Format: {@code <indexName><separator><datePattern>}
 * <p>
 * Thread-safe
 */
@Plugin(name = RollingIndexNamePlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = IndexNameFormatter.ELEMENT_TYPE, printObject = true)
public class RollingIndexNamePlugin extends RollingMillisFormatter implements IndexNameFormatter<LogEvent> {

    static final String PLUGIN_NAME = "RollingIndexName";

    public static final String DEFAULT_SEPARATOR = "-";

    protected RollingIndexNamePlugin(final MillisFormatter formatter, final RollingTimestamps rollingTimestamps) {
        super(formatter, rollingTimestamps);
    }

    @Override
    public final String format(final LogEvent obj) {
        return format(obj.getTimeMillis());
    }

    @PluginBuilderFactory
    public static RollingIndexNamePlugin.Builder newBuilder() {
        return new RollingIndexNamePlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<RollingIndexNamePlugin> {

        public static final String DEFAULT_TIME_ZONE = ZoneId.systemDefault().getId();

        @PluginBuilderAttribute
        private String indexName;

        @PluginBuilderAttribute
        private String pattern;

        @PluginBuilderAttribute
        private String timeZone = DEFAULT_TIME_ZONE;

        @PluginBuilderAttribute
        private String separator = DEFAULT_SEPARATOR;

        @Override
        public RollingIndexNamePlugin build() {

            if (indexName == null) {
                throw new ConfigurationException("No indexName provided for " + PLUGIN_NAME);
            }
            if (pattern == null) {
                throw new ConfigurationException("No pattern provided for " + PLUGIN_NAME);
            }

            final RollingMillisFormatter.Builder builder = new RollingMillisFormatter.Builder()
                    .withPrefix(indexName)
                    .withSeparator(separator)
                    .withPattern(pattern)
                    .withTimeZone(timeZone);

            return new RollingIndexNamePlugin(builder.createFormatter(), builder.createRollingTimestamps());
        }

        public Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder withPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withTimeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder withSeparator(String separator) {
            this.separator = separator;
            return this;
        }

        long getInitialTimestamp() {
            return System.currentTimeMillis();
        }
    }

}
