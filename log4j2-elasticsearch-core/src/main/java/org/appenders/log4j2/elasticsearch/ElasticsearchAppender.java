package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;

/**
 * Plugin class responsible for delivery of incoming {@link LogEvent}(s) to {@link BatchDelivery} implementation.
 * <p>
 * Formatted message may be produced by
 * <ul>
 * <li> (default) {@code org.apache.logging.log4j.core.layout.JsonLayout.toSerializable(LogEvent)}
 * <li> provided {@code org.apache.logging.log4j.core.layout.AbstractStringLayout.toSerializable(LogEvent)}
 * <li> or {@code org.apache.logging.log4j.message.Message.getFormattedMessage()} (see {@link ElasticsearchAppender.Builder#withMessageOnly(boolean)}
 * messageOnly})
 * </ul>
 */
@Plugin(name = ElasticsearchAppender.PLUGIN_NAME, category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ElasticsearchAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "Elasticsearch";

    private BatchDelivery<String> batchDelivery;
    private boolean messageOnly;
    private IndexNameFormatter indexNameFormatter;

    protected ElasticsearchAppender(String name, Filter filter, AbstractStringLayout layout,
            boolean ignoreExceptions, BatchDelivery batchDelivery, boolean messageOnly, IndexNameFormatter indexNameFormatter) {
        super(name, filter, layout, ignoreExceptions);
        this.messageOnly = messageOnly;
        this.batchDelivery = batchDelivery;
        this.indexNameFormatter = indexNameFormatter;
    }

    public void append(LogEvent event) {
        String formattedIndexName = indexNameFormatter.format(event);
        batchDelivery.add(formattedIndexName, messageOnly ? event.getMessage().getFormattedMessage() : (String) getLayout().toSerializable(event));
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
        private AbstractStringLayout layout;

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
                layout = JsonLayout.newBuilder().setCompact(true).build();
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
         */
        public Builder withLayout(AbstractStringLayout layout) {
            this.layout = layout;
            return this;
        }

        /**
         * See {@code org.apache.logging.log4j.core.appender.AbstractAppender.ignoreExceptions}
         * <p>
         * Default: false
         *
         * @param ignoreExceptions whether to suppress exceptions or not
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
         *                    Otherwise, configured {@link AbstractStringLayout} will be used.
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
