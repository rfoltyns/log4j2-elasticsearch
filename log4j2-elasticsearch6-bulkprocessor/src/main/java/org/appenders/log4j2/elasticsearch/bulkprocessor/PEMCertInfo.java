package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.elasticsearch.common.settings.Settings;

@Plugin(name = "PEM", category = Node.CATEGORY, elementType = CertInfo.ELEMENT_TYPE)
public class PEMCertInfo implements CertInfo<Settings.Builder> {

    static final String XPACK_SECURITY_TRANSPORT_SSL_ENABLED = "xpack.security.transport.ssl.enabled";
    static final String XPACK_SSL_KEY = "xpack.ssl.key";
    static final String XPACK_SSL_KEY_PASSPHRASE = "xpack.ssl.key_passphrase";
    static final String XPACK_SSL_CERTIFICATE = "xpack.ssl.certificate";
    static final String XPACK_SSL_CERTIFICATE_AUTHORITIES = "xpack.ssl.certificate_authorities";

    private final String keyPath;
    private final String keyPassphrase;
    private final String clientCertPath;
    private final String caPath;

    protected PEMCertInfo(String keyPath, String keyPassphrase, String clientCertPath, String caPath) {
        this.keyPath = keyPath;
        this.keyPassphrase = keyPassphrase;
        this.clientCertPath = clientCertPath;
        this.caPath = caPath;
    }

    @Override
    public void applyTo(Settings.Builder clientConfigBuilder) {

        clientConfigBuilder.put(XPACK_SECURITY_TRANSPORT_SSL_ENABLED, "true");

        if (keyPath != null) {
            clientConfigBuilder.put(XPACK_SSL_KEY, keyPath);
        }

        if (keyPassphrase != null) {
            clientConfigBuilder.put(XPACK_SSL_KEY_PASSPHRASE, keyPassphrase);
        }

        if (clientCertPath != null) {
            clientConfigBuilder.put(XPACK_SSL_CERTIFICATE, clientCertPath);
        }

        if (caPath != null) {
            clientConfigBuilder.put(XPACK_SSL_CERTIFICATE_AUTHORITIES, caPath);
        }

    }

    @PluginBuilderFactory
    public static PEMCertInfo.Builder newBuilder() {
        return new PEMCertInfo.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PEMCertInfo> {

        @PluginBuilderAttribute
        private String keyPath;

        @PluginBuilderAttribute
        private String clientCertPath;

        @PluginBuilderAttribute
        private String caPath;

        @PluginBuilderAttribute
        @PluginAliases({"keyPassword"})
        private String keyPassphrase;

        @Override
        public PEMCertInfo build() {
            return new PEMCertInfo(keyPath, keyPassphrase, clientCertPath, caPath);
        }

        public Builder withKeyPath(String keyPath) {
            this.keyPath = keyPath;
            return this;
        }

        public Builder withClientCertPath(String clientCertPath) {
            this.clientCertPath = clientCertPath;
            return this;
        }

        public Builder withCaPath(String caPath) {
            this.caPath = caPath;
            return this;
        }

        public Builder withKeyPassphrase(String keyPassphrase) {
            this.keyPassphrase = keyPassphrase;
            return this;
        }
    }

}
