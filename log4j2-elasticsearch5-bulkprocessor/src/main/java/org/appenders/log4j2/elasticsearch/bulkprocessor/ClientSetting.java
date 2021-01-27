package org.appenders.log4j2.elasticsearch.bulkprocessor;

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

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(
        name = ClientSetting.NAME,
        category = Core.CATEGORY_NAME,
        elementType = ClientSetting.ELEMENT_TYPE,
        printObject = true
)
public class ClientSetting {

    static final String NAME = "ClientSetting";
    static final String ELEMENT_TYPE = "clientSetting";

    private final String name;
    private final String value;

    protected ClientSetting(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ClientSetting> {

        @PluginAttribute("name")
        @Required(message = "No name provided for " + NAME)
        private String name;

        @PluginAttribute("value")
        @Required(message = "No value provided for " + NAME)
        private String value;

        @Override
        public ClientSetting build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + NAME);
            }

            if (value == null) {
                throw new ConfigurationException("No value provided for " + NAME);
            }

            return new ClientSetting(name, value);

        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withValue(String value) {
            this.value = value;
            return this;
        }

    }
}
