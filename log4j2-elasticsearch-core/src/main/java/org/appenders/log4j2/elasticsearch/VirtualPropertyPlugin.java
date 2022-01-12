package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

/**
 * Allows to define a property which will be appended to JSON output.
 * Similar to Log4j2 KeyValuePair, value resolution is done with {@code org.apache.logging.log4j.core.lookup.StrSubstitutor}.
 * Value may be static (resolved) or in a resolvable format defined by <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html">Log4j2 Lookups</a>
 */
@Plugin(name = VirtualPropertyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = VirtualPropertyPlugin.ELEMENT_NAME, printObject = true)
public class VirtualPropertyPlugin extends VirtualProperty {

    public static final String PLUGIN_NAME = "VirtualProperty";
    public static final String ELEMENT_NAME = "virtualProperty";

    /**
     * @param name Name
     * @param value May be static or in a resolvable format defined by <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html">Log4j2 Lookups</a>
     * @param isDynamic In case of resolvable properties, this flag indicates that resolved value may change over time
     * @param writeRaw Indicates that the value is a valid, structured object (e.g JSON string) and should be written as such.
     */
    public VirtualPropertyPlugin(final String name, final String value, final boolean isDynamic, final boolean writeRaw) {
        super(name, value, isDynamic, writeRaw);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<VirtualPropertyPlugin> {

        @PluginBuilderAttribute
        private String name;

        @PluginBuilderAttribute
        private String value;

        @PluginBuilderAttribute
        private boolean dynamic;

        @PluginBuilderAttribute
        private boolean writeRaw;

        @Override
        public VirtualPropertyPlugin build() {

            final VirtualProperty.Builder builder = new VirtualProperty.Builder()
                    .withName(name)
                    .withValue(value)
                    .withDynamic(dynamic)
                    .withWriteRaw(writeRaw);

            try {
                final VirtualProperty virtualProperty = builder.build();
                return new VirtualPropertyPlugin(virtualProperty.getName(),
                        virtualProperty.getValue(),
                        virtualProperty.isDynamic(),
                        virtualProperty.isWriteRaw());
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }

        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public Builder withDynamic(boolean isDynamic) {
            this.dynamic = isDynamic;
            return this;
        }

        public Builder withWriteRaw(boolean writeRaw) {
            this.writeRaw = writeRaw;
            return this;
        }
    }

}
