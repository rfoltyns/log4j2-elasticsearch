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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "IndexName", category = Node.CATEGORY, elementType = IndexNameFormatter.ELEMENT_TYPE, printObject = true)
public class NoopIndexNameFormatter implements IndexNameFormatter {

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
