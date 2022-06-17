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


import org.appenders.log4j2.elasticsearch.Credentials;
import org.asynchttpclient.Realm;

public final class BasicCredentials implements Credentials<HttpClientFactory.Builder> {

    static final String PLUGIN_NAME = "BasicCredentials";

    private final String username;
    private final String password;

    public BasicCredentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void applyTo(final HttpClientFactory.Builder builder) {

        final Realm realm = new Realm.Builder(username, password)
                .setUsePreemptiveAuth(true)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();

        builder.withRealm(realm);

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String username;
        private String password;

        public BasicCredentials build() {
            if (username == null) {
                throw new IllegalArgumentException("No username provided for " + BasicCredentials.PLUGIN_NAME);
            }
            if (password == null) {
                throw new IllegalArgumentException("No password provided for " + BasicCredentials.PLUGIN_NAME);
            }
            return new BasicCredentials(username, password);
        }

        public Builder withUsername(final String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(final String password) {
            this.password = password;
            return this;
        }

    }

}
