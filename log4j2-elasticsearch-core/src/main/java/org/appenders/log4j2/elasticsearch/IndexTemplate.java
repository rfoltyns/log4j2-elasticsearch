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


import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;


@Plugin(name = IndexTemplate.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class IndexTemplate implements OpSource {

    public static final String TYPE_NAME = "IndexTemplate";
    public static final String PLUGIN_NAME = "IndexTemplate";

    private final String name;
    private final String source;

    /**
     * @param name Index template name
     * @param source Index template document
     */
    protected IndexTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @return Index template name
     */
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }


    /**
     * @return Index template document
     */
    @Override
    public String getSource() {
        return this.source;
    }

    @PluginBuilderFactory
    public static IndexTemplate.Builder newBuilder() {
        return new IndexTemplate.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<IndexTemplate> {

        @PluginAttribute("name")
        @Required
        private String name;

        @PluginAttribute("path")
        private String path;

        @PluginValue("sourceString")
        private String source;

        /**
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        @Deprecated
        @PluginConfiguration
        private Configuration configuration;

        /**
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        @Deprecated
        private ValueResolver valueResolver;

        @Override
        public IndexTemplate build() {
            if (name == null) {
                throw new ConfigurationException("No name provided for IndexTemplate");
            }
            if ((path == null && source == null) || (path != null && source != null)) {
                throw new ConfigurationException("Either path or source have to be provided for IndexTemplate");
            }

            return new IndexTemplate(name, getValueResolver().resolve(loadSource()));
        }

        /* visible for testing */
        @Deprecated
        ValueResolver getValueResolver() {

            // allow programmatic override
            if (valueResolver != null) {
                return valueResolver;
            }

            // handle XML config
            if (configuration != null) {
                return new Log4j2Lookup(configuration.getStrSubstitutor());
            }

            // fallback to no-op
            return ValueResolver.NO_OP;
        }

        private String loadSource() {

            if (source != null) {
                return source;
            }

            return ResourceUtil.loadResource(path);

        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        /**
         * @param configuration Log4j2 StrSubstitutor provider
         * @return this
         *
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * @param valueResolver variable resolver
         * @return this
         *
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        @Deprecated
        public Builder withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
            return this;
        }

    }

}
