package org.appenders.log4j2.elasticsearch.jest;

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

        private HttpClientConfig httpClientConfig;

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
