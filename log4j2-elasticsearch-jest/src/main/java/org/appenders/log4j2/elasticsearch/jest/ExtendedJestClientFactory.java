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

import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import java.util.Map;

public class ExtendedJestClientFactory extends JestClientFactory {

    protected final WrappedHttpClientConfig wrappedHttpClientConfig;

    public ExtendedJestClientFactory(WrappedHttpClientConfig wrappedHttpClientConfig) {

        this.wrappedHttpClientConfig = wrappedHttpClientConfig;

        // FIXME: replace JestClientFactory at some point if possible..
        super.setHttpClientConfig(wrappedHttpClientConfig.getHttpClientConfig());

    }

    @Override
    protected NHttpClientConnectionManager getAsyncConnectionManager() {

        PoolingNHttpClientConnectionManager connectionManager = createUnconfiguredPoolingNHttpClientConnectionManager();

        HttpClientConfig httpClientConfig = this.wrappedHttpClientConfig.getHttpClientConfig();

        final Integer maxTotal = httpClientConfig.getMaxTotalConnection();
        if (maxTotal != null) {
            connectionManager.setMaxTotal(maxTotal);
        }
        final Integer defaultMaxPerRoute = httpClientConfig.getDefaultMaxTotalConnectionPerRoute();
        if (defaultMaxPerRoute != null) {
            connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        }
        final Map<HttpRoute, Integer> maxPerRoute = httpClientConfig.getMaxTotalConnectionPerRoute();
        for (Map.Entry<HttpRoute, Integer> entry : maxPerRoute.entrySet()) {
            connectionManager.setMaxPerRoute(entry.getKey(), entry.getValue());
        }

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
        HttpClientConfig httpClientConfig = wrappedHttpClientConfig.getHttpClientConfig();
        return RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", httpClientConfig.getHttpIOSessionStrategy())
                    .register("https", httpClientConfig.getHttpsIOSessionStrategy())
                    .build();
    }

    /* visible for testing */
    IOReactorConfig createIoReactorConfig() {
        HttpClientConfig httpClientConfig = wrappedHttpClientConfig.getHttpClientConfig();
        return IOReactorConfig.custom()
                    .setConnectTimeout(httpClientConfig.getConnTimeout())
                    .setSoTimeout(httpClientConfig.getReadTimeout())
                    .setIoThreadCount(wrappedHttpClientConfig.getIoThreadCount())
                    .build();
    }

    /* visible for testing */
    ConnectingIOReactor createIOReactor() throws IOReactorException {
        return new DefaultConnectingIOReactor(createIoReactorConfig());
    }

}
