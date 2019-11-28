package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import static org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter.PLUGIN_NAME;

@Plugin(name = PLUGIN_NAME, category = Node.CATEGORY, elementType = IndexNameFormatter.ELEMENT_TYPE, printObject = true)
public class NoopIndexNameFormatter implements IndexNameFormatter<LogEvent> {

    static final String PLUGIN_NAME = "IndexName";

    private final String indexName;

    protected NoopIndexNameFormatter(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public String format(LogEvent logEvent) {
        return this.indexName;
    }

    @PluginBuilderFactory
    public static NoopIndexNameFormatter.Builder newBuilder() {
        return new NoopIndexNameFormatter.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<NoopIndexNameFormatter> {

        @PluginBuilderAttribute
        @Required(message = "No indexName provided for IndexName")
        private String indexName;

        @Override
        public NoopIndexNameFormatter build() {
            if (indexName == null) {
                throw new ConfigurationException("No indexName provided for IndexName");
            }
            return new NoopIndexNameFormatter(indexName);
        }

        public NoopIndexNameFormatter.Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

    }

}
