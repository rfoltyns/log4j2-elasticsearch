package org.appenders.log4j2.elasticsearch.hc;

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


import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.hc.thirdparty.PemReader;
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

    protected PEMCertInfo(String keyPath, String keyPassphrase, String clientCertPath, String caPath) {
        this.keyPath = keyPath;
        this.keyPassphrase = keyPassphrase;
        this.clientCertPath = clientCertPath;
        this.caPath = caPath;
    }

    @Override
    public void applyTo(HttpClientFactory.Builder builder) {

        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
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
            builder.withSslSocketFactory(new SSLConnectionSocketFactory(sslContext));
            builder.withHttpsIOSessionStrategy(new SSLIOSessionStrategy(sslContext, new NoopHostnameVerifier()));

        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(configExceptionMessage, e);
        }

    }

    public static PEMCertInfo.Builder newBuilder() {
        return new PEMCertInfo.Builder();
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
