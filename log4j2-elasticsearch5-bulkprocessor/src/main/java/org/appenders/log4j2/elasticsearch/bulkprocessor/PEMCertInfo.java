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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.elasticsearch.common.settings.Settings;

@Plugin(name = "PEM", category = Node.CATEGORY, elementType = CertInfo.ELEMENT_TYPE)
public class PEMCertInfo implements CertInfo<Settings.Builder> {

    static final String XPACK_SECURITY_TRANSPORT_SSL_ENABLED = "xpack.security.transport.ssl.enabled";
    static final String XPACK_SSL_KEY = "xpack.ssl.key";
    static final String XPACK_SSL_CERTIFICATE = "xpack.ssl.certificate";
    static final String XPACK_SSL_CERTIFICATE_AUTHORITIES = "xpack.ssl.certificate_authorities";

    private final String keyPath;
    private final String clientCertPath;
    private final String caPath;

    protected PEMCertInfo(String keyPath, String clientCertPath, String caPath) {
        this.keyPath = keyPath;
        this.clientCertPath = clientCertPath;
        this.caPath = caPath;
    }

    @Override
    public void applyTo(Settings.Builder clientConfigBuilder) {

        clientConfigBuilder.put(XPACK_SECURITY_TRANSPORT_SSL_ENABLED, "true");

        if (keyPath != null) {
            clientConfigBuilder.put(XPACK_SSL_KEY, keyPath);
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

        @Override
        public PEMCertInfo build() {
            return new PEMCertInfo(keyPath, clientCertPath, caPath);
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

    }

}
