package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.JestClient;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.discovery.NodeChecker;
import io.searchbox.client.config.idle.HttpReapableConnectionManager;
import io.searchbox.client.config.idle.IdleConnectionReaper;
import io.searchbox.client.http.JestHttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;

/**
 * Creates configured {@link BufferedJestHttpClient}
 */
public class BufferedJestClientFactory extends ExtendedJestClientFactory {

    private static final Logger LOG = InternalLogging.getLogger();

    /**
     * This constructor is deprecated, it will be removed in 1.5,
     * use {@link #BufferedJestClientFactory(WrappedHttpClientConfig)} instead
     *
     * @param httpClientConfig {@link io.searchbox.client.config.HttpClientConfig}
     * @deprecated As of 1.5, this constructor will be removed
     */
    @Deprecated
    public BufferedJestClientFactory(io.searchbox.client.config.HttpClientConfig httpClientConfig) {
        super(new WrappedHttpClientConfig.Builder(httpClientConfig).build());
    }

    public BufferedJestClientFactory(WrappedHttpClientConfig wrappedHttpClientConfig) {
        super(wrappedHttpClientConfig);
    }

    @Override
    public final JestClient getObject() {

        // no other way than copying almost whole super.getObject() ..
        BufferedJestHttpClient client = createDefaultClient();

        HttpClientConfig httpClientConfig = wrappedHttpClientConfig.getHttpClientConfig();
        client.setServers(httpClientConfig.getServerList());

        final HttpClientConnectionManager connectionManager = getConnectionManager();
        client.setHttpClient(createHttpClient(connectionManager));

        final NHttpClientConnectionManager asyncConnectionManager = getAsyncConnectionManager();
        client.setAsyncClient(createAsyncHttpClient(asyncConnectionManager));

        // schedule idle connection reaping if configured
        if (httpClientConfig.getMaxConnectionIdleTime() > 0) {
            createConnectionReaper(client, connectionManager, asyncConnectionManager);
        } else {
            LOG.info("Idle connection reaping disabled");
        }

        // set discovery (should be set after setting the httpClient on jestClient)
        if (httpClientConfig.isDiscoveryEnabled()) {
            createNodeChecker(client, httpClientConfig);
        } else {
            LOG.info("Node Discovery disabled");
        }

        client.getAsyncClient().start();
        return client;

    }

    protected BufferedJestHttpClient createDefaultClient() {
        return new BufferedJestHttpClient();
    }

    /* visible for testing */
    IdleConnectionReaper createConnectionReaper(JestHttpClient client, HttpClientConnectionManager connectionManager, NHttpClientConnectionManager asyncConnectionManager) {
        LOG.info("Idle connection reaping enabled...");

        IdleConnectionReaper reaper = new IdleConnectionReaper(wrappedHttpClientConfig.getHttpClientConfig(),
                new HttpReapableConnectionManager(connectionManager, asyncConnectionManager));
        client.setIdleConnectionReaper(reaper);
        reaper.startAsync();
        reaper.awaitRunning();
        return reaper;
    }

    protected NodeChecker createNodeChecker(JestHttpClient client, io.searchbox.client.config.HttpClientConfig httpClientConfig) {
        LOG.info("Node Discovery enabled...");
        NodeChecker nodeChecker = new NodeChecker(client, httpClientConfig);
        client.setNodeChecker(nodeChecker);
        nodeChecker.startAsync();
        nodeChecker.awaitRunning();
        return nodeChecker;
    }

    protected CloseableHttpClient createHttpClient(HttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
                        .setConnectionManager(connectionManager)
                        .setDefaultRequestConfig(getRequestConfig())
                        .setProxyAuthenticationStrategy(wrappedHttpClientConfig.getHttpClientConfig().getProxyAuthenticationStrategy())
                        .setRoutePlanner(getRoutePlanner())
                        .setDefaultCredentialsProvider(wrappedHttpClientConfig.getHttpClientConfig().getCredentialsProvider())
        .build();
    }

    protected CloseableHttpAsyncClient createAsyncHttpClient(NHttpClientConnectionManager connectionManager) {
        return HttpAsyncClients.custom()
                        .setConnectionManager(connectionManager)
                        .setDefaultRequestConfig(getRequestConfig())
                        .setProxyAuthenticationStrategy(wrappedHttpClientConfig.getHttpClientConfig().getProxyAuthenticationStrategy())
                        .setRoutePlanner(getRoutePlanner())
                        .setDefaultCredentialsProvider(wrappedHttpClientConfig.getHttpClientConfig().getCredentialsProvider())
        .build();
    }

}
