package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.hc.discovery.HCServiceDiscovery;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class HttpClientProvider implements ClientProvider<HttpClient>, LifeCycle {

    private volatile State state = State.STOPPED;

    private final HttpClientFactory.Builder httpClientFactoryBuilder;

    private HttpClient httpClient;

    public HttpClientProvider(HttpClientFactory.Builder httpClientFactoryBuilder) {
        this.httpClientFactoryBuilder = httpClientFactoryBuilder;
    }

    @Override
    public HttpClient createClient() {

        if (httpClient == null) {
            httpClient = httpClientFactoryBuilder.build().createInstance();
        }

        return httpClient;

    }

    public HttpClientFactory.Builder getHttpClientFactoryBuilder() {
        return httpClientFactoryBuilder;
    }

    @Override
    public void start() {

        if (!LifeCycle.of(getHttpClientFactoryBuilder().serviceDiscovery).isStarted()) {
            LifeCycle.of(getHttpClientFactoryBuilder().serviceDiscovery).start();
        }

        HttpClient httpClient = createClient();
        if (httpClient.isStarted()) {
            getLogger().debug("{}: HTTP client already started", HCServiceDiscovery.class.getSimpleName());
        } else {
            getLogger().debug("{}: Starting HTTP client", HCServiceDiscovery.class.getSimpleName());
            httpClient.start();
        }

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        LifeCycle.of(getHttpClientFactoryBuilder().serviceDiscovery).stop();
        LifeCycle.of(httpClient).stop();
        httpClient = null;

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

}
