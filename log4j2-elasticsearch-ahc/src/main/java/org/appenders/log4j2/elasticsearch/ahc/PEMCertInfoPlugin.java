package org.appenders.log4j2.elasticsearch.ahc;

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
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.CertInfo;

@Plugin(name = PEMCertInfoPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "certInfo")
public class PEMCertInfoPlugin implements CertInfo<HttpClientFactory.Builder> {

    static final String PLUGIN_NAME = "PEM";

    private final PEMCertInfo delegate;

    protected PEMCertInfoPlugin(final String keyPath, final String keyPassphrase, final String clientCertPath, final String caPath) {
        this.delegate = new PEMCertInfo(keyPath, keyPassphrase, clientCertPath, caPath);
    }

    @Override
    public void applyTo(final HttpClientFactory.Builder builder) {
        try {
            delegate.applyTo(builder);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PEMCertInfoPlugin> {

        @PluginBuilderAttribute
        @Required(message = "No keyPath provided for " + PLUGIN_NAME)
        private String keyPath;

        @PluginBuilderAttribute
        @Required(message = "No clientCertPath provided for " + PLUGIN_NAME)
        private String clientCertPath;

        @PluginBuilderAttribute
        @Required(message = "No caPath provided for " + PLUGIN_NAME)
        private String caPath;

        @PluginBuilderAttribute
        @PluginAliases({"keyPassword"})
        private String keyPassphrase;

        @Override
        public PEMCertInfoPlugin build() {
            if (keyPath == null) {
                throw new ConfigurationException("No keyPath provided for " + PLUGIN_NAME);
            }
            if (clientCertPath == null) {
                throw new ConfigurationException("No clientCertPath provided for " + PLUGIN_NAME);
            }
            if (caPath == null) {
                throw new ConfigurationException("No caPath provided for " + PLUGIN_NAME);
            }
            return new PEMCertInfoPlugin(keyPath, keyPassphrase, clientCertPath, caPath);
        }

        public Builder withKeyPath(final String keyPath) {
            this.keyPath = keyPath;
            return this;
        }

        public Builder withClientCertPath(final String clientCertPath) {
            this.clientCertPath = clientCertPath;
            return this;
        }

        public Builder withCaPath(final String caPath) {
            this.caPath = caPath;
            return this;
        }

        public Builder withKeyPassphrase(final String keyPassphrase) {
            this.keyPassphrase = keyPassphrase;
            return this;
        }
    }

}
