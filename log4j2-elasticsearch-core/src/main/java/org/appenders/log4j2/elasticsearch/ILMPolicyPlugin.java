package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2
 */
@Plugin(name = ILMPolicyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class ILMPolicyPlugin extends ILMPolicy {

    public static final String PLUGIN_NAME = "ILMPolicy";

    /**
     * {@inheritDoc}
     */
    protected ILMPolicyPlugin(String policyName, String rolloverAlias, boolean createBootstrapIndex, String source) {
        super(policyName, rolloverAlias, createBootstrapIndex, source);
    }

    @PluginBuilderFactory
    public static ILMPolicyPlugin.Builder newBuilder() {
        return new ILMPolicyPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ILMPolicyPlugin> {

        @PluginAttribute("name")
        @Required
        private String name;

        @PluginAttribute("rolloverAlias")
        @Required
        private String rolloverAlias;

        @PluginAttribute("createBootstrapIndex")
        private boolean createBootstrapIndex = true;

        @PluginAttribute("path")
        private String path;

        @PluginValue("sourceString")
        private String source;

        public ILMPolicyPlugin build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME);
            }

            if (rolloverAlias == null) {
                throw new ConfigurationException("No rolloverAlias provided for " + PLUGIN_NAME);
            }

            if ((path == null && source == null) || (path != null && source != null)) {
                throw new ConfigurationException("Either path or source have to be provided for " + ILMPolicyPlugin.class.getSimpleName());
            }

            return new ILMPolicyPlugin(name, rolloverAlias, createBootstrapIndex, loadSource());
        }

        private String loadSource() {

            if (source != null) {
                return source;
            }

            return ResourceUtil.loadResource(path);
        }

        /**
         * @param name ILM policy name
         * @return this
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param path ILM policy document resource path.
         *             MUST be resolvable by {@link ResourceUtil#loadResource(String)}.
         *             Resource MAY contain placeholders resolvable by {@link ValueResolver}.
         * @return this
         */
        public Builder withPath(String path) {
            this.path = path;
            return this;
        }


        /**
         * @param rolloverAlias Index rollover alias
         * @return this
         */
        public Builder withRolloverAlias(String rolloverAlias) {
            this.rolloverAlias = rolloverAlias;
            return this;
        }

        public Builder withCreateBootstrapIndex(boolean createBootstrapIndex) {
            this.createBootstrapIndex = createBootstrapIndex;
            return this;
        }

        /**
         * @param source ILM policy document. MAY contain placeholders resolvable by {@link ValueResolver}
         * @return this
         */
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

    }

}
