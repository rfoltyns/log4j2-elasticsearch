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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;

@PluginAliases("XPackAuth")
@Plugin(name = Security.PLUGIN_NAME, category = Node.CATEGORY, elementType = Auth.ELEMENT_TYPE)
public class Security implements Auth<HttpClientFactory.Builder> {

    static final String PLUGIN_NAME = "Security";

    private final Credentials credentials;
    private final CertInfo certInfo;

    protected Security(Credentials<HttpClientFactory.Builder> credentials, CertInfo<HttpClientFactory.Builder> certInfo){
        this.credentials = credentials;
        this.certInfo = certInfo;
    }

    @Override
    public void configure(HttpClientFactory.Builder builder) {

        credentials.applyTo(builder);

        if (certInfo != null) {
            certInfo.applyTo(builder);
        }

    }

    @PluginBuilderFactory
    public static Security.Builder newBuilder() {
        return new Security.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<Security> {

        @PluginElement("credentials")
        @Required(message = "No credentials provided for " + Security.PLUGIN_NAME)
        private Credentials credentials;

        @PluginElement("certInfo")
        private CertInfo certInfo;

        @Override
        public Security build() {

            if (credentials == null) {
                throw new ConfigurationException("No credentials provided for " + Security.PLUGIN_NAME);
            }

            return new Security(credentials, certInfo);

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
