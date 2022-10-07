package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscovery;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory for Apache HC Client specific objects
 */
public class HttpClientFactory {

    protected final Collection<String> serverList;
    protected final int connTimeout;
    protected final int readTimeout;
    protected final int maxTotalConnections;
    protected final int ioThreadCount;
    protected final CredentialsProvider defaultCredentialsProvider;
    protected final LayeredConnectionSocketFactory sslSocketFactory;
    protected final ConnectionSocketFactory plainSocketFactory;
    protected final SchemeIOSessionStrategy httpIOSessionStrategy;
    protected final SchemeIOSessionStrategy httpsIOSessionStrategy;
    protected final boolean pooledResponseBuffersEnabled;
    protected final int pooledResponseBuffersSizeInBytes;
    protected final ServiceDiscovery serviceDiscovery;
    protected final MetricsFactory metricsFactory;
    private final String name;

    HttpClientFactory(HttpClientFactory.Builder httpClientFactoryBuilder) {
        this.serverList = httpClientFactoryBuilder.serverList;
        this.connTimeout = httpClientFactoryBuilder.connTimeout;
        this.readTimeout = httpClientFactoryBuilder.readTimeout;
        this.maxTotalConnections = httpClientFactoryBuilder.maxTotalConnections;
        this.ioThreadCount = httpClientFactoryBuilder.ioThreadCount;
        this.defaultCredentialsProvider = httpClientFactoryBuilder.defaultCredentialsProvider;
        this.plainSocketFactory = httpClientFactoryBuilder.plainSocketFactory;
        this.sslSocketFactory = httpClientFactoryBuilder.sslSocketFactory;
        this.httpIOSessionStrategy = httpClientFactoryBuilder.httpIOSessionStrategy;
        this.httpsIOSessionStrategy = httpClientFactoryBuilder.httpsIOSessionStrategy;
        this.pooledResponseBuffersEnabled = httpClientFactoryBuilder.pooledResponseBuffersEnabled;
        this.pooledResponseBuffersSizeInBytes = httpClientFactoryBuilder.pooledResponseBuffersSizeInBytes;
        this.serviceDiscovery = httpClientFactoryBuilder.serviceDiscovery;
        this.metricsFactory = httpClientFactoryBuilder.metricsFactory;
        this.name = httpClientFactoryBuilder.name;
    }

    public HttpClient createInstance() {

        final NHttpClientConnectionManager asyncConnectionManager = getAsyncConnectionManager();
        CloseableHttpAsyncClient asyncHttpClient = createAsyncHttpClient(asyncConnectionManager);

        HttpAsyncResponseConsumerFactory httpAsyncResponseConsumerFactory =
                createHttpAsyncResponseConsumerFactory();

        final ServerPool serverPool = new ServerPool(new ArrayList<>(serverList));
        if (serviceDiscovery != null) {
            serviceDiscovery.addListener(serverPool);
        }

        return createConfiguredClient(
                asyncHttpClient,
                serverPool,
                httpAsyncResponseConsumerFactory
        );

    }

    protected HttpAsyncResponseConsumerFactory createHttpAsyncResponseConsumerFactory() {
        if (pooledResponseBuffersEnabled) {
            final String componentName = name == null ? HttpClient.class.getSimpleName() : name;
            return new PoolingAsyncResponseConsumerFactory(createPool(componentName), componentName, metricsFactory);
        }
        return HttpAsyncMethods::createConsumer;
    }

    protected HttpClient createConfiguredClient(
            CloseableHttpAsyncClient asyncHttpClient,
            ServerPool serverPool,
            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory
    ) {
        return createConfiguredClient(
                asyncHttpClient,
                serverPool,
                createRequestFactory(),
                asyncResponseConsumerFactory
        );
    }

    protected HttpClient createConfiguredClient(
            CloseableHttpAsyncClient asyncHttpClient,
            ServerPool serverPool,
            RequestFactory requestFactory,
            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory
    ) {
        return new HttpClient(
                asyncHttpClient,
                serverPool,
                requestFactory,
                asyncResponseConsumerFactory
        );
    }

    protected RequestFactory createRequestFactory() {
        return new HCRequestFactory();
    }

    protected CloseableHttpAsyncClient createAsyncHttpClient(NHttpClientConnectionManager connectionManager) {
        return HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .setDefaultCredentialsProvider(defaultCredentialsProvider)
                .build();
    }

    protected RequestConfig getDefaultRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(connTimeout)
                .setSocketTimeout(readTimeout)
                .build();
    }

    protected NHttpClientConnectionManager getAsyncConnectionManager() {
        PoolingNHttpClientConnectionManager connectionManager = createUnconfiguredPoolingNHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        return connectionManager;
    }

    /* visible for testing */
    PoolingNHttpClientConnectionManager createUnconfiguredPoolingNHttpClientConnectionManager() {

        try {
            return new PoolingNHttpClientConnectionManager(createIOReactor(), createSchemeIOSessionStrategyRegistry());
        } catch (IOReactorException e) {
            throw new IllegalStateException(e);
        }

    }

    /* visible for testing */
    Registry<SchemeIOSessionStrategy> createSchemeIOSessionStrategyRegistry() {
        return RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", httpIOSessionStrategy)
                .register("https", httpsIOSessionStrategy)
                .build();
    }

    /* visible for testing */
    IOReactorConfig createIOReactorConfig() {
        return IOReactorConfig.custom()
                .setConnectTimeout(connTimeout)
                .setSoTimeout(readTimeout)
                .setIoThreadCount(ioThreadCount)
                .build();
    }

    /* visible for testing */
    ConnectingIOReactor createIOReactor() throws IOReactorException {
        return new DefaultConnectingIOReactor(createIOReactorConfig());
    }


    private GenericItemSourcePool<SimpleInputBuffer> createPool(final String poolName) {
        return new GenericItemSourcePool<>(
                poolName,
                new SimpleInputBufferPooledObjectOps(
                        HeapByteBufferAllocator.INSTANCE,
                        pooledResponseBuffersSizeInBytes
                ),
                new UnlimitedResizePolicy.Builder().withResizeFactor(0.5).build(),
                1000,
                maxTotalConnections,
                metricsFactory
        );
    }

    public static class Builder {

        protected Collection<String> serverList = new ArrayList<>();
        protected int connTimeout = 1000;
        protected int readTimeout = 1000;
        protected int maxTotalConnections = 1;
        protected int ioThreadCount = maxTotalConnections;
        protected CredentialsProvider defaultCredentialsProvider;
        protected LayeredConnectionSocketFactory sslSocketFactory;
        protected ConnectionSocketFactory plainSocketFactory;
        protected SchemeIOSessionStrategy httpIOSessionStrategy;
        protected SchemeIOSessionStrategy httpsIOSessionStrategy;
        protected boolean pooledResponseBuffersEnabled;
        protected int pooledResponseBuffersSizeInBytes;
        protected Auth<Builder> auth;
        protected ServiceDiscovery serviceDiscovery;
        final MetricsFactory metricsFactory = new DefaultMetricsFactory()
                .configure(PoolingAsyncResponseConsumer.metricConfigs(false))
                .configure(GenericItemSourcePool.metricConfigs(false));
        String name;

        public HttpClientFactory build() {
            return new HttpClientFactory(lazyInit());
        }

        /**
         * Initializes Apache HC factories.
         * MUST be called by extending classes before {@link #build()} call if all factories not set explicitly.
         *
         * @return this
         */
        protected Builder lazyInit() {

            if (this.auth != null) {
                this.auth.configure(this);
            }
            if (this.sslSocketFactory == null) {
                this.sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
            }
            if (this.plainSocketFactory == null) {
                this.plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
            }
            if (this.httpIOSessionStrategy == null) {
                this.httpIOSessionStrategy = NoopIOSessionStrategy.INSTANCE;
            }
            if (this.httpsIOSessionStrategy == null) {
                this.httpsIOSessionStrategy = SSLIOSessionStrategy.getSystemDefaultStrategy();
            }

            return this;

        }

        public Builder withServerList(Collection<String> serverList) {
            this.serverList = serverList;
            return this;
        }

        public Builder withConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
            return this;
        }

        public Builder withReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder withIoThreadCount(int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        public Builder withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
            this.pooledResponseBuffersEnabled = pooledResponseBuffersEnabled;
            return this;
        }

        public Builder withPooledResponseBuffersSizeInBytes(int pooledResponseBuffersSizeInBytes) {
            this.pooledResponseBuffersSizeInBytes = pooledResponseBuffersSizeInBytes;
            return this;
        }

        public Builder withAuth(Auth<Builder> auth) {
            this.auth = auth;
            return this;
        }

        public Builder withDefaultCredentialsProvider(CredentialsProvider credentialsProvider) {
            this.defaultCredentialsProvider = credentialsProvider;
            return this;
        }

        public Builder withSslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder withPlainSocketFactory(ConnectionSocketFactory plainSocketFactory) {
            this.plainSocketFactory = plainSocketFactory;
            return this;
        }

        public Builder withHttpIOSessionStrategy(SchemeIOSessionStrategy httpIOSessionStrategy) {
            this.httpIOSessionStrategy = httpIOSessionStrategy;
            return this;
        }

        public Builder withHttpsIOSessionStrategy(SchemeIOSessionStrategy httpsIOSessionStrategy) {
            this.httpsIOSessionStrategy = httpsIOSessionStrategy;
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

        public Builder withName(String name) {
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
                    ", pooledResponseBuffersEnabled=" + pooledResponseBuffersEnabled +
                    ", pooledResponseBuffersSizeInBytes=" + pooledResponseBuffersSizeInBytes +
                    ", auth=" + (auth != null) +
                    ", serviceDiscovery=" + (serviceDiscovery != null) +
                    ", metrics=" + metricsFactory.getMetricConfigs().size() +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

}
