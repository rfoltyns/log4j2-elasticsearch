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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class JKSCertInfo implements CertInfo<HttpClientFactory.Builder> {

    static final String configExceptionMessage = "Failed to apply SSL/TLS settings";

    private final String keystorePath;
    private final String truststorePath;
    private final String keystorePassword;
    private final String truststorePassword;

    public JKSCertInfo(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
    }

    @Override
    public void applyTo(HttpClientFactory.Builder httpClientFactoryBuilder) {

        try (
                FileInputStream keystoreFile = new FileInputStream(new File(keystorePath));
                FileInputStream truststoreFile = new FileInputStream(new File(truststorePath))
        ) {
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(keystoreFile, keystorePassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

            KeyStore trustStore = KeyStore.getInstance("jks");
            trustStore.load(truststoreFile, truststorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // TODO: add support for hostname verification modes
            httpClientFactoryBuilder.withSslSocketFactory(new SSLConnectionSocketFactory(sslContext));
            httpClientFactoryBuilder.withHttpsIOSessionStrategy(new SSLIOSessionStrategy(sslContext, new NoopHostnameVerifier()));

        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(configExceptionMessage, e);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JKSCertInfo> {

        public static final String EMPTY_PASSWORD = "";

        private String keystorePath;
        private String keystorePassword = EMPTY_PASSWORD;
        private String truststorePath;
        private String truststorePassword = EMPTY_PASSWORD;

        @Override
        public JKSCertInfo build() {
            if (keystorePath == null) {
                throw new IllegalArgumentException("No keystorePath provided for " + getClass().getSimpleName());
            }
            if (keystorePassword == null) {
                throw new IllegalArgumentException("No keystorePassword provided for " + getClass().getSimpleName());
            }
            if (truststorePath == null) {
                throw new IllegalArgumentException("No truststorePath provided for " + getClass().getSimpleName());
            }
            if (truststorePassword == null) {
                throw new IllegalArgumentException("No truststorePassword provided for " + getClass().getSimpleName());
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

