package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.elasticsearch.common.settings.Settings;

@Plugin(name = JKSCertInfo.PLUGIN_NAME, category = Node.CATEGORY, elementType = CertInfo.ELEMENT_TYPE)
public class JKSCertInfo implements CertInfo<Settings.Builder> {

    static final String PLUGIN_NAME = "JKS";

    static final String SHIELD_TRANSPORT_SSL_ENABLED = "shield.transport.ssl";
    static final String SHIELD_SSL_KEYSTORE_PATH = "shield.ssl.keystore.path";
    static final String SHIELD_SSL_KEYSTORE_PASSWORD = "shield.ssl.keystore.password";
    static final String SHIELD_SSL_TRUSTSTORE_PATH = "shield.ssl.truststore.path";
    static final String SHIELD_SSL_TRUSTSTORE_PASSWORD = "shield.ssl.truststore.password";

    private final String keystorePath;
    private final String truststorePath;
    private final String keystorePassword;
    private final String truststorePassword;

    protected JKSCertInfo(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
    }

    @Override
    public void applyTo(Settings.Builder clientConfigBuilder) {

        clientConfigBuilder.put(SHIELD_TRANSPORT_SSL_ENABLED, "true");

        if (keystorePath != null) {
            clientConfigBuilder.put(SHIELD_SSL_KEYSTORE_PATH, keystorePath);
        }
        if (truststorePath != null) {
            clientConfigBuilder.put(SHIELD_SSL_TRUSTSTORE_PATH, truststorePath);
        }

        clientConfigBuilder.put(SHIELD_SSL_KEYSTORE_PASSWORD, keystorePassword);
        clientConfigBuilder.put(SHIELD_SSL_TRUSTSTORE_PASSWORD, truststorePassword);

    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JKSCertInfo> {

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
        public JKSCertInfo build() {
            if (keystorePassword == null) {
                throw new ConfigurationException("No keystorePassword provided for " + PLUGIN_NAME);
            }
            if (truststorePassword == null) {
                throw new ConfigurationException("No truststorePassword provided for " + PLUGIN_NAME);
            }
            return new JKSCertInfo(keystorePath, keystorePassword, truststorePath, truststorePassword);
        }

        public Builder withKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public Builder withKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Builder withTruststorePath(String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public Builder withTruststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

    }

}
