package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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
@Plugin(name = VirtualProperty.PLUGIN_NAME, category = Node.CATEGORY, printObject = true)
public class VirtualProperty {

    public static final String PLUGIN_NAME = "VirtualProperty";
    private final String name;
    private String value;
    private final boolean dynamic;
    private final boolean writeRaw;

    /**
     * @param name Name
     * @param value May be static or in a resolvable format defined by <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html">Log4j2 Lookups</a>
     * @param isDynamic In case of resolvable properties, this flag indicates that resolved value may change over time
     */
    public VirtualProperty(final String name, final String value, final boolean isDynamic) {
        this(name, value, isDynamic, false);
    }

    /**
     * @param name Name
     * @param value May be static or in a any format resolvable by configured {@link ValueResolver}
     * @param isDynamic In case of resolvable properties, this flag indicates that resolved value may change over time
     * @param writeRaw Indicates that the value is a valid, structured object (e.g JSON string) and should be written as such.
     */
    public VirtualProperty(final String name, final String value, final boolean isDynamic, boolean writeRaw) {
        this.name = name;
        this.value = value;
        this.dynamic = isDynamic;
        this.writeRaw = writeRaw;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String resolved) {
        this.value = resolved;
    }

    /**
     * @return if false, value SHOULD be resolved during initialization phase and SHOULD replaced using {@link #setValue(String)}, otherwise value SHOULD be resolved (and not replaced) on during serialization
     *
     * @see ValueResolver
     */
    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isWriteRaw() {
        return writeRaw;
    }

    @Override
    public String toString() {
        return String.format("%s=%s", name, value);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<VirtualProperty> {

        @PluginBuilderAttribute
        private String name;

        @PluginBuilderAttribute
        private String value;

        @PluginBuilderAttribute
        private boolean dynamic;

        @PluginBuilderAttribute
        private boolean writeRaw;

        @Override
        public VirtualProperty build() {

            validate();

            return new VirtualProperty(name, value, dynamic, writeRaw);

        }

        public void validate() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME);
            }

            if (value == null) {
                throw new ConfigurationException("No value provided for " + PLUGIN_NAME);
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
