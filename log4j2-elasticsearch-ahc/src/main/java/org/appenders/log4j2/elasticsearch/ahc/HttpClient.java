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

import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricType;
import org.appenders.log4j2.elasticsearch.metrics.Metrics;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * AsyncHttpClient based client
 */
public class HttpClient implements LifeCycle, Measured {

    private volatile State state = State.STOPPED;

    private final AsyncHttpClient asyncClient;
    private final ServerPool serverPool;
    private final RequestFactory httpRequestFactory;
    private final HttpClientMetrics metrics;

    /**
     * @param asyncClient actual AsyncHttpClient client
     * @param serverPool pool of servers to use
     * @param requestFactory {@link Request} adapter
     */
    public HttpClient(
            final AsyncHttpClient asyncClient,
            final ServerPool serverPool,
            final RequestFactory requestFactory
    ) {
        this(HttpClient.class.getSimpleName(), new DefaultMetricsFactory(Collections.emptyList()), asyncClient, serverPool, requestFactory);
    }

    /**
     * @param name HTTP client instance name. Used as Metric component name
     * @param metricsFactory Metrics factory
     * @param asyncClient actual AsyncHttpClient client
     * @param serverPool pool of servers to use
     * @param requestFactory {@link Request} adapter
     */
    public HttpClient(
            final String name,
            final MetricsFactory metricsFactory,
            final AsyncHttpClient asyncClient,
            final ServerPool serverPool,
            final RequestFactory requestFactory
    ) {
        this.asyncClient = asyncClient;
        this.serverPool = serverPool;
        this.httpRequestFactory = requestFactory;
        this.metrics = new HttpClientMetrics(name == null ? HttpClient.class.getSimpleName() : name, metricsFactory);
    }

    public static List<MetricConfig> metricConfigs(final boolean enabled) {
        return Arrays.asList(
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "connectionsActive"),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "connectionsIdle"),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "connectionsTotal")
        );
    }

    public <T extends Response> T execute(
            final Request clientRequest,
            final BlockingResponseHandler<T> responseHandler
    ) {
        executeAsync(clientRequest, responseHandler);
        return responseHandler.getResult();
    }

    public <T extends Response> void executeAsync(
            final Request request,
            final ResponseHandler<T> responseHandler
    ) {

        final RequestBuilder clientRequest;
        try {
            clientRequest = createClientRequest(request);
        } catch (Exception e) {
            responseHandler.failed(e);
            return;
        }

        getAsyncClient().executeRequest(clientRequest, createCallback(responseHandler));

    }

    RequestBuilder createClientRequest(final Request request) throws Exception {
        final String url = serverPool.getNext() + '/' + request.getURI();
        return (RequestBuilder) httpRequestFactory.create(url, request);
    }

    <T extends Response> AsyncHandler<T> createCallback(final ResponseHandler<T> responseHandler) {
        return new AHCResultCallback<>(responseHandler);
    }

    public AsyncHttpClient getAsyncClient() {
        return asyncClient;
    }

    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        state = State.STARTED;

        getLogger().debug("{}: Started", HttpClient.class.getSimpleName());

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        getLogger().debug("{}: Stopping client", HttpClient.class.getSimpleName());

        if (!asyncClient.isClosed()) {
            try {
                asyncClient.close();
            } catch (IOException e) {
                getLogger().warn("Async client might not have been stopped properly. Cause: " + e.getMessage());
            }
        }

        state = State.STOPPED;

        getLogger().debug("{}: Stopping client", HttpClient.class.getSimpleName());

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

    @Override
    public void register(final MetricsRegistry registry) {
        metrics.register(registry);
    }

    @Override
    public void deregister() {
        metrics.deregister();
    }

    public class HttpClientMetrics implements Metrics {

        private final List<MetricsRegistry.Registration> registrations = new ArrayList<>();
        private final Metric connectionsTotal;
        private final Metric connectionsActive;
        private final Metric connectionsIdle;

        private HttpClientMetrics(final String name, final MetricsFactory metricsFactory) {
            this.connectionsActive = metricsFactory.createMetric(name, "connectionsActive", () -> asyncClient.getClientStats().getTotalActiveConnectionCount());
            this.connectionsIdle = metricsFactory.createMetric(name, "connectionsIdle", () -> asyncClient.getClientStats().getTotalIdleConnectionCount());
            this.connectionsTotal = metricsFactory.createMetric(name, "connectionsTotal", () -> asyncClient.getClientStats().getTotalConnectionCount());
        }

        @Override
        public void register(MetricsRegistry registry) {
            registrations.add(registry.register(connectionsActive));
            registrations.add(registry.register(connectionsIdle));
            registrations.add(registry.register(connectionsTotal));
        }

        @Override
        public void deregister() {
            registrations.forEach(MetricsRegistry.Registration::deregister);
            registrations.clear();
        }

    }

}
