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


import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;

public class Security implements Auth<HttpClientFactory.Builder> {

    private final Credentials<HttpClientFactory.Builder> credentials;
    private final CertInfo<HttpClientFactory.Builder> certInfo;

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

    public static class Builder {

        private Credentials<HttpClientFactory.Builder> credentials;
        private CertInfo<HttpClientFactory.Builder> certInfo;

        public Security build() {

            if (credentials == null) {
                throw new IllegalArgumentException("No credentials provided for " + Security.class.getSimpleName());
            }

            return new Security(credentials, certInfo);

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
