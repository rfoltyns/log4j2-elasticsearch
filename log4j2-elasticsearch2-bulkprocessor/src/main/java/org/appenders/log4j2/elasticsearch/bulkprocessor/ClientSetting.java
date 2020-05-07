package org.appenders.log4j2.elasticsearch.bulkprocessor;

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
    public static final Builder newBuilder() {
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
