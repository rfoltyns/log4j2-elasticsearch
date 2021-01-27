package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import io.searchbox.client.config.HttpClientConfig;

/**
 * Consider this class <i>private</i> - the whole HTTP client config will be rewritten at some point
 */
class WrappedHttpClientConfig {

    private final HttpClientConfig httpClientConfig;

    private final int ioThreadCount;

    protected WrappedHttpClientConfig(Builder builder) {
        this.httpClientConfig = builder.httpClientConfig;
        this.ioThreadCount = builder.ioThreadCount;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public int getIoThreadCount() {
        return ioThreadCount;
    }

    public static class Builder {

        // same as org.apache.httpcomponents:httpcore-nio:IOReactorConfig.AVAIL_PROCS
        protected int ioThreadCount = Runtime.getRuntime().availableProcessors();

        private final HttpClientConfig httpClientConfig;

        public Builder(HttpClientConfig httpClientConfig) {
            this.httpClientConfig = httpClientConfig;
        }

        public WrappedHttpClientConfig build() {
            return new WrappedHttpClientConfig(this);
        }

        public Builder ioThreadCount(int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

    }

}
