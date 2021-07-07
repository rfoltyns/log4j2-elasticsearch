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

/**
 * Allows to define a property which will be appended to JSON output.
 * Value may be static (resolved) or in format resolvable by configured {@link ValueResolver}.
 */
public class VirtualProperty {

    private final String name;
    private String value;
    private final boolean dynamic;

    /**
     * @param name Name
     * @param value May be static or in a any format resolvable by configured {@link ValueResolver}
     * @param isDynamic In case of resolvable properties, this flag indicates that resolved value may change over time
     */
    public VirtualProperty(final String name, final String value, final boolean isDynamic) {
        this.name = name;
        this.value = value;
        this.dynamic = isDynamic;
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

    @Override
    public String toString() {
        return String.format("%s=%s", name, value);
    }

    public static class Builder {

        private String name;
        private String value;
        private boolean dynamic;

        public VirtualProperty build() {

            validate();

            return new VirtualProperty(name, value, dynamic);

        }

        public void validate() {

            if (name == null) {
                throw new IllegalArgumentException("No name provided for " + VirtualProperty.class.getSimpleName());
            }

            if (value == null) {
                throw new IllegalArgumentException("No value provided for " + VirtualProperty.class.getSimpleName());
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

    }

}
