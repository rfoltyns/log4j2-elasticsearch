package org.appenders.log4j2.elasticsearch.hc;

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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Credentials;

@Plugin(name = BasicCredentialsPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "credentials")
public final class BasicCredentialsPlugin implements Credentials<HttpClientFactory.Builder> {

    static final String PLUGIN_NAME = "BasicCredentials";

    private final BasicCredentials delegate;

    protected BasicCredentialsPlugin(String username, String password) {
        this.delegate = new BasicCredentials(username, password);
    }

    @Override
    public void applyTo(HttpClientFactory.Builder settings) {
        delegate.applyTo(settings);
    }

    @PluginBuilderFactory
    public static BasicCredentialsPlugin.Builder newBuilder() {
        return new BasicCredentialsPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BasicCredentialsPlugin> {

        @PluginBuilderAttribute
        @Required(message = "No username provided for " + BasicCredentialsPlugin.PLUGIN_NAME)
        private String username;

        @PluginBuilderAttribute
        @Required(message = "No password provided for " + BasicCredentialsPlugin.PLUGIN_NAME)
        private String password;

        @Override
        public BasicCredentialsPlugin build() {
            if (username == null) {
                throw new ConfigurationException("No username provided for " + BasicCredentialsPlugin.PLUGIN_NAME);
            }
            if (password == null) {
                throw new ConfigurationException("No password provided for " + BasicCredentialsPlugin.PLUGIN_NAME);
            }
            return new BasicCredentialsPlugin(username, password);
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

    }

}
