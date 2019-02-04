package org.appenders.log4j2.elasticsearch.jest;

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
