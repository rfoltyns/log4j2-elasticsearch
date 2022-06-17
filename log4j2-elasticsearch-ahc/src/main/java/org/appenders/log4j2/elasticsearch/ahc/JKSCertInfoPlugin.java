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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.CertInfo;

@Plugin(name = JKSCertInfoPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "certInfo")
public class JKSCertInfoPlugin implements CertInfo<HttpClientFactory.Builder> {

    static final String PLUGIN_NAME = "JKS";

    private final JKSCertInfo delegate;

    protected JKSCertInfoPlugin(final String keystorePath, final String keystorePassword, final String truststorePath, final String truststorePassword) {
        this.delegate = new JKSCertInfo(keystorePath, keystorePassword, truststorePath, truststorePassword);
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

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JKSCertInfoPlugin> {

        public static final String EMPTY_PASSWORD = "";

        @PluginBuilderAttribute
        private String keystorePath;

        @PluginBuilderAttribute
        private String keystorePassword = EMPTY_PASSWORD;

        @PluginBuilderAttribute
        private String truststorePath;

        @PluginBuilderAttribute
        private String truststorePassword = EMPTY_PASSWORD;

        @Override
        public JKSCertInfoPlugin build() {
            if (keystorePath == null) {
                throw new ConfigurationException("No keystorePath provided for " + PLUGIN_NAME);
            }
            if (keystorePassword == null) {
                throw new ConfigurationException("No keystorePassword provided for " + PLUGIN_NAME);
            }
            if (truststorePath == null) {
                throw new ConfigurationException("No truststorePath provided for " + PLUGIN_NAME);
            }
            if (truststorePassword == null) {
                throw new ConfigurationException("No truststorePassword provided for " + PLUGIN_NAME);
            }
            return new JKSCertInfoPlugin(keystorePath, keystorePassword, truststorePath, truststorePassword);
        }

        public Builder withKeystorePath(final String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public Builder withKeystorePassword(final String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Builder withTruststorePath(final String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public Builder withTruststorePassword(final String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

    }
}

