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
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;

import java.util.ArrayList;
import java.util.Collection;

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

    public HttpClientFactory(HttpClientFactory.Builder httpClientFactoryBuilder) {
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
    }

    public HttpClient createInstance() {

        final NHttpClientConnectionManager asyncConnectionManager = getAsyncConnectionManager();
        CloseableHttpAsyncClient asyncHttpClient = createAsyncHttpClient(asyncConnectionManager);

        HttpAsyncResponseConsumerFactory httpAsyncResponseConsumerFactory =
                createHttpAsyncResponseConsumerFactory();

        ServerPool serverPool = new ServerPool(new ArrayList<>(serverList));

        return createConfiguredClient(
                asyncHttpClient,
                serverPool,
                httpAsyncResponseConsumerFactory
        );

    }

    protected HttpAsyncResponseConsumerFactory createHttpAsyncResponseConsumerFactory() {
        if (pooledResponseBuffersEnabled) {
            return new PoolingAsyncResponseConsumerFactory(createPool());
        }
        return () -> HttpAsyncMethods.createConsumer();
    }

    private GenericItemSourcePool<SimpleInputBuffer> createPool() {
        GenericItemSourcePool<SimpleInputBuffer> bufferPool = new GenericItemSourcePool<>(
                "hc-responseBufferPool",
                new SimpleInputBufferPooledObjectOps(
                        HeapByteBufferAllocator.INSTANCE,
                        pooledResponseBuffersSizeInBytes
                ),
                new UnlimitedResizePolicy.Builder().withResizeFactor(0.5).build(),
                1000L,
                false,
                30000,
                maxTotalConnections
        );
        return bufferPool;
    }

    protected HttpClient createConfiguredClient(
            CloseableHttpAsyncClient asyncHttpClient,
            ServerPool serverPool,
            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory
    ) {
        return new HttpClient(
                asyncHttpClient,
                serverPool,
                new HCRequestFactory(),
                asyncResponseConsumerFactory
        );
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

    public static class Builder {

        protected Collection<String> serverList;
        protected int connTimeout;
        protected int readTimeout;
        protected int maxTotalConnections;
        protected int ioThreadCount = Runtime.getRuntime().availableProcessors();
        protected CredentialsProvider defaultCredentialsProvider;
        protected LayeredConnectionSocketFactory sslSocketFactory;
        protected ConnectionSocketFactory plainSocketFactory;
        protected SchemeIOSessionStrategy httpIOSessionStrategy;
        protected SchemeIOSessionStrategy httpsIOSessionStrategy;
        protected boolean pooledResponseBuffersEnabled;
        protected int pooledResponseBuffersSizeInBytes;

        public HttpClientFactory build() {

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

            return new HttpClientFactory(this);
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

        public Builder withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
            this.pooledResponseBuffersEnabled = pooledResponseBuffersEnabled;
            return this;
        }

        public Builder withPooledResponseBuffersSizeInBytes(int pooledResponseBuffersSizeInBytes) {
            this.pooledResponseBuffersSizeInBytes = pooledResponseBuffersSizeInBytes;
            return this;
        }

    }

}
