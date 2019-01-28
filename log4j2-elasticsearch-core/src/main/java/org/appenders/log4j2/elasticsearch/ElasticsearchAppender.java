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


import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;


/**
 * Plugin class responsible for delivery of incoming {@link LogEvent}(s) to {@link BatchDelivery} implementation.
 * <p>
 * Formatted message may be produced by
 * <ul>
 * <li> (default) {@code org.apache.logging.log4j.core.layout.JsonLayout.toSerializable(LogEvent)}
 * <li> provided {@code org.apache.logging.log4j.core.layout.AbstractLayout.toSerializable(LogEvent)}
 * <li> or {@code org.apache.logging.log4j.message.Message.getFormattedMessage()} (see {@link ElasticsearchAppender.Builder#withMessageOnly(boolean)}
 * messageOnly})
 * </ul>
 */
@Plugin(name = ElasticsearchAppender.PLUGIN_NAME, category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ElasticsearchAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "Elasticsearch";

    private final IndexNameFormatter indexNameFormatter;
    private final ItemAppender itemAppender;

    protected ElasticsearchAppender(String name, Filter filter, AbstractLayout layout,
            boolean ignoreExceptions, BatchDelivery batchDelivery, boolean messageOnly, IndexNameFormatter indexNameFormatter) {
        super(name, filter, layout, ignoreExceptions);
        this.indexNameFormatter = indexNameFormatter;
        this.itemAppender = createItemAppenderFactory().createInstance(messageOnly, layout, batchDelivery);
    }

    /* extension point */
    protected ItemAppenderFactory createItemAppenderFactory() {
        return new ItemAppenderFactory();
    }

    public void append(LogEvent event) {
        String formattedIndexName = indexNameFormatter.format(event);
        itemAppender.append(formattedIndexName, event);
    }

    @Override
    public void start() {
        itemAppender.start();
        super.start();
    }

    @Override
    public void stop() {
        itemAppender.stop();
        super.stop();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ElasticsearchAppender>  {

        /**
         * Default: {@link NoopIndexNameFormatter}
         */
        public static final IndexNameFormatter DEFAULT_INDEX_NAME_FORMATTER = NoopIndexNameFormatter.newBuilder().withIndexName("log4j2").build();

        @PluginBuilderAttribute
        @Required(message = "No name provided for Elasticsearch appender")
        private String name;

        @PluginElement("filter")
        private Filter filter;

        @PluginElement("layout")
        private AbstractLayout layout;

        @PluginBuilderAttribute
        private boolean ignoreExceptions;

        @PluginElement("batchDelivery")
        @Required(message = "No BatchDelivery method provided for ElasticSearch appender")
        private BatchDelivery batchDelivery;

        @PluginBuilderAttribute
        private boolean messageOnly;

        @PluginElement("indexNameFormatter")
        private IndexNameFormatter indexNameFormatter = DEFAULT_INDEX_NAME_FORMATTER;

        @Override
        public ElasticsearchAppender build() {
            if (name == null) {
                throw new ConfigurationException("No name provided for Elasticsearch appender");
            }
            if (batchDelivery == null) {
                throw new ConfigurationException("No batchDelivery [AsyncBatchDelivery] provided for Elasticsearch appender");
            }

            if (layout == null) {
                layout = JacksonJsonLayout.newBuilder().build();
            }

            return new ElasticsearchAppender(name, filter, layout, ignoreExceptions, batchDelivery, messageOnly, indexNameFormatter);
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Default: {@code org.apache.logging.log4j.core.layout.JsonLayout}
         *
         * @param layout layout to be used
         * @return Builder this
         */
        public Builder withLayout(AbstractLayout layout) {
            this.layout = layout;
            return this;
        }

        /**
         * See {@code org.apache.logging.log4j.core.appender.AbstractAppender.ignoreExceptions}
         * <p>
         * Default: false
         *
         * @param ignoreExceptions whether to suppress exceptions or not
         * @return Builder this
         */
        public Builder withIgnoreExceptions(boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public Builder withBatchDelivery(BatchDelivery batchDelivery) {
            this.batchDelivery = batchDelivery;
            return this;
        }

        /**
         * Default: false
         *
         * @param messageOnly If true, formatted message will be produced by {@link org.apache.logging.log4j.message.Message#getFormattedMessage}.
         *                    Otherwise, configured {@link AbstractStringLayout} will be used
         * @return Builder this
         */
        public Builder withMessageOnly(boolean messageOnly) {
            this.messageOnly = messageOnly;
            return this;
        }

        public Builder withIndexNameFormatter(IndexNameFormatter indexNameFormatter) {
            this.indexNameFormatter = indexNameFormatter;
            return this;
        }
    }
}
