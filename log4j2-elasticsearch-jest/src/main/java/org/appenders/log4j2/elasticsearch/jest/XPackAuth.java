package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.config.HttpClientConfig;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;

@Plugin(name = XPackAuth.PLUGIN_NAME, category = Node.CATEGORY, elementType = Auth.ELEMENT_TYPE)
public class XPackAuth implements Auth<HttpClientConfig.Builder> {

    static final String PLUGIN_NAME = "XPackAuth";

    private final Credentials credentials;
    private final CertInfo certInfo;

    protected XPackAuth(Credentials<HttpClientConfig.Builder> credentials, CertInfo<HttpClientConfig.Builder> certInfo){
        this.credentials = credentials;
        this.certInfo = certInfo;
    }

    @Override
    public void configure(HttpClientConfig.Builder builder) {
        credentials.applyTo(builder);
        certInfo.applyTo(builder);
    }

    @PluginBuilderFactory
    public static XPackAuth.Builder newBuilder() {
        return new XPackAuth.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<XPackAuth> {

        @PluginElement("credentials")
        @Required(message = "No credentials provided for " + XPackAuth.PLUGIN_NAME)
        private Credentials credentials;

        @PluginElement("certInfo")
        @Required(message = "No certInfo provided for " + XPackAuth.PLUGIN_NAME)
        private CertInfo certInfo;

        @Override
        public XPackAuth build() {
            if (credentials == null) {
                throw new ConfigurationException("No credentials provided for " + XPackAuth.PLUGIN_NAME);
            }
            if (certInfo == null) {
                throw new ConfigurationException("No certInfo provided for " + XPackAuth.PLUGIN_NAME);
            }
            return new XPackAuth(credentials, certInfo);
        }

        public Builder withCredentials(Credentials credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder withCertInfo(CertInfo certInfo) {
            this.certInfo = certInfo;
            return this;
        }

    }

}
