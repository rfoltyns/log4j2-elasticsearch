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

import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscovery;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

import java.util.concurrent.atomic.AtomicReference;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class HttpClientProvider implements ClientProvider<HttpClient>, LifeCycle, Measured {

    private volatile State state = State.STOPPED;

    private final HttpClientFactory.Builder httpClientFactoryBuilder;

    private final AtomicReference<HttpClient> httpClient = new AtomicReference<>(null);

    public HttpClientProvider(final HttpClientFactory.Builder httpClientFactoryBuilder) {
        this.httpClientFactoryBuilder = httpClientFactoryBuilder;
    }

    @Override
    public HttpClient createClient() {

        if (httpClient.get() == null) {
            // FIXME: ensure created once
            httpClient.set(httpClientFactoryBuilder.build().createInstance());
        }

        return httpClient.get();

    }

    public HttpClientFactory.Builder getHttpClientFactoryBuilder() {
        return httpClientFactoryBuilder;
    }

    @Override
    public void start() {

        if (!LifeCycle.of(getHttpClientFactoryBuilder().serviceDiscovery).isStarted()) {
            LifeCycle.of(getHttpClientFactoryBuilder().serviceDiscovery).start();
        }

        final HttpClient httpClient = createClient();
        if (httpClient.isStarted()) {
            getLogger().debug("{}: HTTP client already started", HttpClient.class.getSimpleName());
        } else {
            getLogger().debug("{}: Starting HTTP client", HttpClient.class.getSimpleName());
            httpClient.start();
        }

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        final ServiceDiscovery serviceDiscovery = getHttpClientFactoryBuilder().serviceDiscovery;
        Measured.of(serviceDiscovery).deregister(); // prevent leaks
        LifeCycle.of(serviceDiscovery).stop();

        Measured.of(httpClient.get()).deregister(); // prevent leaks
        LifeCycle.of(httpClient.get()).stop();

        httpClient.set(null);

        state = State.STOPPED;

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
    public void register(MetricsRegistry registry) {
        if (httpClient.get() == null) {
            getLogger().warn("{}: Metrics not ready. HttpClient not created yet", HttpClientProvider.class.getSimpleName());
            return;
        }
        httpClient.get().register(registry);
    }

    @Override
    public void deregister() {
        Measured.of(httpClient.get()).deregister();
    }

    @Override
    public String toString() {
        return "HttpClientProvider{" +
                "config=" + httpClientFactoryBuilder +
                '}';
    }

}
