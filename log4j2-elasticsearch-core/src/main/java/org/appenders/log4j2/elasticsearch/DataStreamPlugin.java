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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;


@Plugin(name = DataStreamPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class DataStreamPlugin extends DataStream {

    public static final String PLUGIN_NAME = "DataStream";

    /**
     * @param name Data stream name
     */
    protected DataStreamPlugin(final String name) {
        super(name);
    }

    @PluginBuilderFactory
    public static DataStreamPlugin.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<DataStreamPlugin> {

        @PluginBuilderAttribute
        private String name;

        @Override
        public DataStreamPlugin build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME);
            }

            return new DataStreamPlugin(name);
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

    }

}
