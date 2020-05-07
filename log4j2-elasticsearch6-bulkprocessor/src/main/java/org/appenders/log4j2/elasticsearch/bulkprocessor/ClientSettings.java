package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.util.Builder;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;
import java.util.List;

@Plugin(
        name = ClientSettings.NAME,
        category = Core.CATEGORY_NAME,
        elementType = ClientSettings.ELEMENT_TYPE,
        printObject = true
)
public class ClientSettings {

    static final String NAME = "ClientSettings";
    static final String ELEMENT_TYPE = "clientSettings";

    private final List<ClientSetting> clientSettings;

    protected ClientSettings(List<ClientSetting> clientSettings) {
        this.clientSettings = clientSettings;
    }

    public void applyTo(Settings.Builder settings) {
        for (ClientSetting clientSetting : clientSettings) {
            settings.put(clientSetting.getName(), clientSetting.getValue());
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new ClientSettings.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ClientSettings> {

        @PluginElement(ClientSetting.ELEMENT_TYPE)
        private ClientSetting[] clientSettings = new ClientSetting[0];

        @Override
        public ClientSettings build() {
            return new ClientSettings(Arrays.asList(clientSettings));
        }

        public Builder withClientSettings(ClientSetting... clientSettings) {
            if (clientSettings != null) {
                this.clientSettings = clientSettings;
            }
            return this;
        }

    }

}
