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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
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
        return new Builder();
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
