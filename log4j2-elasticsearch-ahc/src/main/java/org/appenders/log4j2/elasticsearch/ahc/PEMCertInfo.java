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


import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ahc.thirdparty.PemReader;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;
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

public final class PEMCertInfo implements CertInfo<HttpClientFactory.Builder> {

    static final String configExceptionMessage = "Failed to apply SSL/TLS settings";

    private final String keyPath;
    private final String keyPassphrase;
    private final String clientCertPath;
    private final String caPath;

    protected PEMCertInfo(final String keyPath, final String keyPassphrase, final String clientCertPath, final String caPath) {
        this.keyPath = keyPath;
        this.keyPassphrase = keyPassphrase;
        this.clientCertPath = clientCertPath;
        this.caPath = caPath;
    }

    @Override
    public void applyTo(final HttpClientFactory.Builder builder) {

        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }

        try (
                FileInputStream clientCert = new FileInputStream(new File(clientCertPath));
                FileInputStream key = new FileInputStream(new File(keyPath));
                FileInputStream certificateAuthoritiies = new FileInputStream(new File(caPath))
        ) {
            final KeyStore keyStore = PemReader.loadKeyStore(clientCert, key, Optional.ofNullable(keyPassphrase));
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassphrase.toCharArray());

            final KeyStore trustStore = PemReader.loadTrustStore(certificateAuthoritiies);

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // TODO: add support for hostname verification modes
            final JsseSslEngineFactory jsseSslEngineFactory = new JsseSslEngineFactory(sslContext);
            builder.withSslEngineFactory(jsseSslEngineFactory);

        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(configExceptionMessage, e);
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String keyPath;
        private String clientCertPath;
        private String caPath;
        private String keyPassphrase;

        public PEMCertInfo build() {
            if (keyPath == null) {
                throw new IllegalArgumentException("No keyPath provided for " + getClass().getSimpleName());
            }
            if (clientCertPath == null) {
                throw new IllegalArgumentException("No clientCertPath provided for " + getClass().getSimpleName());
            }
            if (caPath == null) {
                throw new IllegalArgumentException("No caPath provided for " + getClass().getSimpleName());
            }
            return new PEMCertInfo(keyPath, keyPassphrase, clientCertPath, caPath);
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
