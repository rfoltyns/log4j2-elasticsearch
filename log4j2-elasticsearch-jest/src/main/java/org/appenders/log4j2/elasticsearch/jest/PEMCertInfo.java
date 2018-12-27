package org.appenders.log4j2.elasticsearch.jest;

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


import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.thirdparty.PemReader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Optional;

@Plugin(name = PEMCertInfo.PLUGIN_NAME, category = Node.CATEGORY, elementType = CertInfo.ELEMENT_TYPE)
public class PEMCertInfo implements CertInfo<HttpClientConfig.Builder> {

    static final String PLUGIN_NAME = "PEM";

    private final String keyPath;
    private final String keyPassphrase;
    private final String clientCertPath;
    private final String caPath;

    static final String configExceptionMessage = "Failed to apply SSL/TLS settings";

    protected PEMCertInfo(String keyPath, String keyPassphrase, String clientCertPath, String caPath) {
        this.keyPath = keyPath;
        this.keyPassphrase = keyPassphrase;
        this.clientCertPath = clientCertPath;
        this.caPath = caPath;
    }

    @Override
    public void applyTo(HttpClientConfig.Builder builder) {

        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        try (
                FileInputStream clientCert = new FileInputStream(new File(clientCertPath));
                FileInputStream key = new FileInputStream(new File(keyPath));
                FileInputStream certificateAuthoritiies = new FileInputStream(new File(caPath))
        ) {
            KeyStore keyStore = PemReader.loadKeyStore(clientCert, key, Optional.ofNullable(keyPassphrase));
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassphrase.toCharArray());

            KeyStore trustStore = PemReader.loadTrustStore(certificateAuthoritiies);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // TODO: add support for hostname verification modes
            builder.sslSocketFactory(new SSLConnectionSocketFactory(sslContext));
            builder.httpsIOSessionStrategy(new SSLIOSessionStrategy(sslContext, new NoopHostnameVerifier()));

        } catch (IOException | GeneralSecurityException e) {
            throw new ConfigurationException(configExceptionMessage, e);
        }

    }

    @PluginBuilderFactory
    public static PEMCertInfo.Builder newBuilder() {
        return new PEMCertInfo.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PEMCertInfo> {

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
        public PEMCertInfo build() {
            if (keyPath == null) {
                throw new ConfigurationException("No keyPath provided for " + PLUGIN_NAME);
            }
            if (clientCertPath == null) {
                throw new ConfigurationException("No clientCertPath provided for " + PLUGIN_NAME);
            }
            if (caPath == null) {
                throw new ConfigurationException("No caPath provided for " + PLUGIN_NAME);
            }
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
