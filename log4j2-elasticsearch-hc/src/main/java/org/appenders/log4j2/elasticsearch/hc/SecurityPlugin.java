package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;

@PluginAliases("XPackAuth")
@Plugin(name = SecurityPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "auth")
public class SecurityPlugin extends Security {

    static final String PLUGIN_NAME = "Security";

    protected SecurityPlugin(Credentials<HttpClientFactory.Builder> credentials, CertInfo<HttpClientFactory.Builder> certInfo){
        super(credentials, certInfo);
    }

    @PluginBuilderFactory
    public static SecurityPlugin.Builder newBuilder() {
        return new SecurityPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SecurityPlugin> {

        @PluginElement("credentials")
        @Required(message = "No credentials provided for " + SecurityPlugin.PLUGIN_NAME)
        private Credentials<HttpClientFactory.Builder> credentials;

        @PluginElement("certInfo")
        private CertInfo<HttpClientFactory.Builder> certInfo;

        @Override
        public SecurityPlugin build() {

            if (credentials == null) {
                throw new ConfigurationException("No credentials provided for " + SecurityPlugin.PLUGIN_NAME);
            }

            return new SecurityPlugin(credentials, certInfo);

        }

        public Builder withCredentials(Credentials<HttpClientFactory.Builder> credentials) {
            this.credentials = credentials;
            return this;
        }

        public Builder withCertInfo(CertInfo<HttpClientFactory.Builder> certInfo) {
            this.certInfo = certInfo;
            return this;
        }

    }

}
