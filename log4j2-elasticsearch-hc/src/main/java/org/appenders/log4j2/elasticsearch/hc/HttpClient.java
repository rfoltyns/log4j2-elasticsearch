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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.appenders.log4j2.elasticsearch.LifeCycle;

import java.io.IOException;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Apache HC based client with optional response buffer pooling
 */
class HttpClient implements LifeCycle {

    private volatile State state = State.STOPPED;

    private final CloseableHttpAsyncClient asyncClient;
    private final ServerPool serverPool;
    private final RequestFactory httpRequestFactory;
    private final HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory;

    /**
     * @param asyncClient actual Apache HTTP client
     * @param serverPool pool of servers to use
     * @param requestFactory {@link Request} adapter
     * @param asyncResponseConsumerFactory async response consumer provider
     */
    HttpClient(
            CloseableHttpAsyncClient asyncClient,
            ServerPool serverPool,
            RequestFactory requestFactory,
            HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory
    ) {
        this.asyncClient = asyncClient;
        this.serverPool = serverPool;
        this.httpRequestFactory = requestFactory;
        this.asyncResponseConsumerFactory = asyncResponseConsumerFactory;
    }

    public <T extends Response> T execute(
            Request clientRequest,
            BlockingResponseHandler<T> responseHandler
    ) {
        executeAsync(clientRequest, responseHandler);
        return responseHandler.getResult();
    }

    public <T extends Response> void executeAsync(
            final Request request,
            final ResponseHandler<T> responseHandler,
            final HttpClientContext httpClientContext
    ) {

        HttpUriRequest clientRequest;
        try {
            clientRequest = createClientRequest(request);
        } catch (IOException e) {
            responseHandler.failed(e);
            return;
        }

        FutureCallback<HttpResponse> responseCallback = createCallback(responseHandler);
        getAsyncClient().execute(
                HttpAsyncMethods.create(clientRequest),
                asyncResponseConsumerFactory.create(),
                httpClientContext,
                responseCallback);
    }

    public <T extends Response> void executeAsync(
            final Request request,
            final ResponseHandler<T> responseHandler
    ) {
        executeAsync(request, responseHandler, createContextInstance());
    }

    HttpUriRequest createClientRequest(final Request request) throws IOException {

        String url = new StringBuilder(128)
                .append(serverPool.getNext())
                .append('/')
                .append(request.getURI())
                .toString();

        return (HttpUriRequest) httpRequestFactory.create(url, request);
    }

    <T extends Response> FutureCallback<HttpResponse> createCallback(ResponseHandler<T> responseHandler) {
        return new HCResultCallback(responseHandler);
    }

    HttpClientContext createContextInstance() {
        return HttpClientContext.create();
    }

    public CloseableHttpAsyncClient getAsyncClient() {
        return asyncClient;
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }

        if (asyncResponseConsumerFactory instanceof LifeCycle) {
            ((LifeCycle)asyncResponseConsumerFactory).start();
        }

        asyncClient.start();

        state = State.STARTED;
    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        if (asyncClient.isRunning()) {
            try {
                asyncClient.close();
            } catch (IOException e) {
                getLogger().warn("Async client might not have been stopped properly. Cause: " + e.getMessage());
            }
        }

        if (asyncResponseConsumerFactory instanceof LifeCycle) {
            ((LifeCycle)asyncResponseConsumerFactory).stop();
        }

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
