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

import io.netty.buffer.PooledByteBufAllocator;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscovery;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.SslEngineFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory for AsyncHttpClient specific objects
 */
public class HttpClientFactory {

    protected final Collection<String> serverList;
    protected final int connTimeout;
    protected final int readTimeout;
    protected final int maxTotalConnections;
    protected final int ioThreadCount;
    protected final boolean gzipCompression;
    protected final Realm realm;
    protected final SslEngineFactory sslEngineFactory;
    protected final ServiceDiscovery serviceDiscovery;
    protected final String name;
    protected final MetricsFactory metricsFactory;

    HttpClientFactory(final Builder httpClientFactoryBuilder) {
        this.serverList = httpClientFactoryBuilder.serverList;
        this.connTimeout = httpClientFactoryBuilder.connTimeout;
        this.readTimeout = httpClientFactoryBuilder.readTimeout;
        this.maxTotalConnections = httpClientFactoryBuilder.maxTotalConnections;
        this.ioThreadCount = httpClientFactoryBuilder.ioThreadCount;
        this.gzipCompression = httpClientFactoryBuilder.gzipCompression;
        this.realm = httpClientFactoryBuilder.realm;
        this.sslEngineFactory = httpClientFactoryBuilder.sslEngineFactory;
        this.serviceDiscovery = httpClientFactoryBuilder.serviceDiscovery;
        this.metricsFactory = httpClientFactoryBuilder.metricsFactory;
        this.name = httpClientFactoryBuilder.name;
    }

    public HttpClient createInstance() {

        final AsyncHttpClient asyncHttpClient = createAsyncHttpClient();

        final ServerPool serverPool = new ServerPool(new ArrayList<>(serverList));
        if (serviceDiscovery != null) {
            serviceDiscovery.addListener(serverPool);
        }

        return createConfiguredClient(
                asyncHttpClient,
                serverPool
        );

    }

    protected HttpClient createConfiguredClient(
            final AsyncHttpClient asyncHttpClient,
            final ServerPool serverPool
    ) {
        return createConfiguredClient(
                asyncHttpClient,
                serverPool,
                createRequestFactory()
        );
    }

    protected HttpClient createConfiguredClient(
            final AsyncHttpClient asyncHttpClient,
            final ServerPool serverPool,
            final RequestFactory requestFactory
    ) {
        return new HttpClient(
                name,
                metricsFactory,
                asyncHttpClient,
                serverPool,
                requestFactory
        );
    }

    protected RequestFactory createRequestFactory() {
        return new AHCRequestFactory();
    }

    protected AsyncHttpClient createAsyncHttpClient() {
        final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setIoThreadsCount(ioThreadCount)
                .setConnectTimeout(connTimeout)
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                .setMaxConnections(maxTotalConnections)
                .setReadTimeout(readTimeout)
                .setCompressionEnforced(gzipCompression)
                .setRealm(realm)
                .setSslEngineFactory(sslEngineFactory);

        return new DefaultAsyncHttpClient(builder.build());
    }

    public static class Builder {

        protected Collection<String> serverList = new ArrayList<>();
        protected int connTimeout = 1000;
        protected int readTimeout = 1000;
        protected int maxTotalConnections = 1;
        protected int ioThreadCount = maxTotalConnections;
        protected Auth<Builder> auth;
        protected ServiceDiscovery serviceDiscovery;
        protected final MetricsFactory metricsFactory = new DefaultMetricsFactory(HttpClient.metricConfigs(false));
        protected String name;
        protected boolean gzipCompression;
        protected Realm realm;
        protected SslEngineFactory sslEngineFactory;

        public HttpClientFactory build() {
            return new HttpClientFactory(lazyInit());
        }

        /**
         * Initializes AsyncHttpClient factories.
         * MUST be called by extending classes before {@link #build()} call if all factories not set explicitly.
         *
         * @return this
         */
        protected Builder lazyInit() {

            if (this.auth != null) {
                this.auth.configure(this);
            }

            return this;

        }

        public Builder withServerList(final Collection<String> serverList) {
            this.serverList = serverList;
            return this;
        }

        public Builder withConnTimeout(final int connTimeout) {
            this.connTimeout = connTimeout;
            return this;
        }

        public Builder withReadTimeout(final int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withMaxTotalConnections(final int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder withIoThreadCount(final int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        public Builder withAuth(final Auth<Builder> auth) {
            this.auth = auth;
            return this;
        }

        public Builder withGzipCompression(final boolean gzipCompression) {
            this.gzipCompression = gzipCompression;
            return this;
        }

        public Builder withRealm(final Realm realm) {
            this.realm = realm;
            return this;
        }

        public Builder withSslEngineFactory(final SslEngineFactory sslSocketFactory) {
            this.sslEngineFactory = sslSocketFactory;
            return this;
        }

        public Builder withServiceDiscovery(final ServiceDiscovery serviceDiscovery) {
            this.serviceDiscovery = serviceDiscovery;
            return this;
        }

        public Builder withMetricConfigs(final List<MetricConfig> metricConfigs) {
            metricsFactory.configure(metricConfigs);
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "serverList=" + serverList +
                    ", connTimeout=" + connTimeout +
                    ", readTimeout=" + readTimeout +
                    ", maxTotalConnections=" + maxTotalConnections +
                    ", ioThreadCount=" + ioThreadCount +
                    ", auth=" + (auth != null) +
                    ", serviceDiscovery=" + (serviceDiscovery != null) +
                    ", metrics=" + metricsFactory.getMetricConfigs().size() +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

}
