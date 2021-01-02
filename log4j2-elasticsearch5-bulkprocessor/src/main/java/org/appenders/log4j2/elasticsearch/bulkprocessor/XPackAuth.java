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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.elasticsearch.common.settings.Settings;

@Plugin(name = XPackAuth.PLUGIN_NAME, category = Node.CATEGORY, elementType = "auth")
public class XPackAuth implements Auth<Settings.Builder> {

    static final String PLUGIN_NAME = "XPackAuth";

    private final Credentials credentials;
    private final CertInfo certInfo;

    protected XPackAuth(Credentials<Settings.Builder> credentials, CertInfo<Settings.Builder> certInfo){
        this.credentials = credentials;
        this.certInfo = certInfo;
    }

    @Override
    public void configure(Settings.Builder settings) {
        credentials.applyTo(settings);
        certInfo.applyTo(settings);
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
