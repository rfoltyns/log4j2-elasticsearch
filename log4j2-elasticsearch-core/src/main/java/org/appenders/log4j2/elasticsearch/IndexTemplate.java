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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

@Plugin(name = IndexTemplate.PLUGIN_NAME, category = Node.CATEGORY, elementType = IndexTemplate.ELEMENT_TYPE, printObject = true)
public class IndexTemplate {

    public static final String PLUGIN_NAME = "IndexTemplate";
    public static final String ELEMENT_TYPE = "indexTemplate";

    private String name;
    private String source;

    protected IndexTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return this.source;
    }

    @PluginBuilderFactory
    public static IndexTemplate.Builder newBuilder() {
        return new IndexTemplate.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<IndexTemplate> {

        public static final String CLASSPATH_PREFIX = "classpath:";
        @PluginAttribute("name")
        @Required
        private String name;

        @PluginAttribute("path")
        private String path;

        @PluginValue("sourceString")
        private String source;

        /**
         * @param valueResolver variable resolver
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        @Deprecated
        @PluginConfiguration
        private Configuration configuration;

        /**
         * @param valueResolver variable resolver
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

            final String source = getValueResolver().resolve(loadSource());

            return new IndexTemplate(name, source);
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
            return new ValueResolver() {

                @Override
                public String resolve(String unresolved) {
                    return unresolved;
                }

                @Override
                public String resolve(VirtualProperty property) {
                    return property.getValue();
                }

            };
        }

        private String loadSource() {

            if (source != null) {
                return source;
            }

            if (path.contains(CLASSPATH_PREFIX)) {
                return loadClasspathResource();
            }

            return loadFileSystemResource();

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
         *
         * @param configuration Log4j2 StrSubstitutor provider
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         * @return this
         */
        Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * @param valueResolver variable resolver
         * @deprecated Added temporarily, solely to support variables in programmatic config.
         * Will be removed when SetupOps API is added.
         */
        @Deprecated
        public Builder withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
            return this;
        }

        private String loadClasspathResource() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(
                                path.replace(CLASSPATH_PREFIX, "")),
                        "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
                return sb.toString();
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        }

        private String loadFileSystemResource() {
            try {
                return new String(Files.readAllBytes(Paths.get(path)));
            } catch (IOException e){
                throw new ConfigurationException(e.getMessage(), e);
            }
        }

    }

}
