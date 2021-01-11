package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

public abstract class BatchingClientObjectFactory<BATCH_TYPE extends Batch<ITEM_TYPE>, ITEM_TYPE extends Item<?>>
        implements ClientObjectFactory<HttpClient, BATCH_TYPE> {

    private volatile State state = State.STOPPED;

    protected final HttpClientProvider clientProvider;
    protected final FailedItemOps<ITEM_TYPE> failedItemOps;
    protected final BackoffPolicy<BATCH_TYPE> backoffPolicy;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    public BatchingClientObjectFactory(BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> builder) {
        this.clientProvider = builder.clientProvider;
        this.failedItemOps = builder.failedItemOps;
        this.backoffPolicy = builder.backoffPolicy;
    }

    @Override
    public Function<BATCH_TYPE, Boolean> createFailureHandler(FailoverPolicy failover) {
        return batchRequest -> {

            long start = System.currentTimeMillis();
            int batchSize = batchRequest.getItems().size();

            getLogger().warn("Batch of {} items failed. Redirecting to {}", batchSize, failover.getClass().getName());

            batchRequest.getItems().forEach(batchItem -> {
                // TODO: FailoverPolicyChain
                try {
                    failover.deliver(failedItemOps.createItem(batchItem));
                } catch (Exception e) {
                    // let's handle here as exception thrown at this stage will cause the client to shutdown
                    getLogger().error(e.getMessage(), e);
                }
            });

            getLogger().trace("Batch of {} items redirected in {} ms", batchSize, System.currentTimeMillis() - start);

            return true;
        };
    }

    protected abstract ResponseHandler<BatchResult> createResultHandler(BATCH_TYPE request, Function<BATCH_TYPE, Boolean> failureHandler);

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(clientProvider.getHttpClientFactoryBuilder().serverList);
    }

    @Override
    public HttpClient createClient() {
        return clientProvider.createClient();
    }

    @Override
    public Function<BATCH_TYPE, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<BATCH_TYPE, Boolean>() {

            private final Function<BATCH_TYPE, Boolean> failureHandler = createFailureHandler(failoverPolicy);

            @Override
            public Boolean apply(BATCH_TYPE request) {

                // FIXME: Wrap in a queue of some sort.. BatchPhaseQueue?
                //        The goal is to have: beforeBatchQueue().executeAll() or queue.beforeBatch().execute() or similar
                //        This should pave the way for before/on/afterBatch style handling
                while (!operations.isEmpty()) {
                    try {
                        operations.remove().execute();
                    } catch (Exception e) {
                        // TODO: redirect to failover (?) retry with exp. backoff (?) multiple options here
                        getLogger().error("before-batch failed: " + e.getMessage(), e);
                    }
                }

                if (backoffPolicy.shouldApply(request)) {
                    getLogger().warn("Backoff applied. Request rejected.");
                    failureHandler.apply(request);
                    request.completed();
                    return false;
                } else {
                    backoffPolicy.register(request);
                }

                ResponseHandler<BatchResult> responseHandler = createResultHandler(request, failureHandler);
                // FIXME: Batch interface shouldn't extend Request!
                createClient().executeAsync(request, responseHandler);

                return true;
            }

        };
    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    public static abstract class Builder<BATCH_TYPE extends Batch<ITEM_TYPE>, ITEM_TYPE extends Item<?>> {

        protected HttpClientProvider clientProvider = new HttpClientProvider(new HttpClientFactory.Builder());
        protected BackoffPolicy<BATCH_TYPE> backoffPolicy = new NoopBackoffPolicy<>();
        protected FailedItemOps<ITEM_TYPE> failedItemOps;

        public abstract BatchingClientObjectFactory<BATCH_TYPE, ITEM_TYPE> build();

        protected BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> validate() {

            if (clientProvider == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(ClientProvider.class.getSimpleName()));
            }

            if (backoffPolicy == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(BackoffPolicy.class.getSimpleName()));
            }

            if (failedItemOps == null) {
                failedItemOps = createFailedItemOps();
            }

            return this;

        }

        protected abstract FailedItemOps<ITEM_TYPE> createFailedItemOps();

        private String nullValidationExceptionMessage(final String className) {
            return String.format("No %s provided for %s", className, HCHttp.class.getSimpleName());
        }

        public Builder<BATCH_TYPE, ITEM_TYPE> withClientProvider(HttpClientProvider clientProvider) {
            this.clientProvider = clientProvider;
            return this;
        }

        public Builder<BATCH_TYPE, ITEM_TYPE> withBackoffPolicy(BackoffPolicy<BATCH_TYPE> backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder<BATCH_TYPE, ITEM_TYPE> withFailedItemOps(FailedItemOps<ITEM_TYPE> failedItemOps) {
            this.failedItemOps = failedItemOps;
            return this;
        }

        /**
         * @param serverUris semicolon-separated list of target addresses
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public Builder<BATCH_TYPE, ITEM_TYPE> withServerUris(String serverUris) {
            this.clientProvider.getHttpClientFactoryBuilder().withServerList(SplitUtil.split(serverUris, ";"));
            return this;
        }

        /**
         * @param maxTotalConnections maximum number of available HTTP connections
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withMaxTotalConnections(int maxTotalConnections) {
            this.clientProvider.getHttpClientFactoryBuilder().withMaxTotalConnections(maxTotalConnections);
            return this;
        }

        /**
         * @param connTimeout connection timeout
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withConnTimeout(int connTimeout) {
            this.clientProvider.getHttpClientFactoryBuilder().withConnTimeout(connTimeout);
            return this;
        }

        /**
         * @param readTimeout read timeout
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withReadTimeout(int readTimeout) {
            this.clientProvider.getHttpClientFactoryBuilder().withReadTimeout(readTimeout);
            return this;
        }

        /**
         * @param ioThreadCount number of 'IO Dispatcher' threads
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withIoThreadCount(int ioThreadCount) {
            this.clientProvider.getHttpClientFactoryBuilder().withIoThreadCount(ioThreadCount);
            return this;
        }

        /**
         * @param auth Credentials and SSL/TLS config
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withAuth(Auth<HttpClientFactory.Builder> auth) {
            // Special treatment here
            if (auth != null) {
                auth.configure(this.clientProvider.getHttpClientFactoryBuilder());
            }
            return this;
        }

        /**
         * @param pooledResponseBuffersEnabled if <i>true</i>, pooled response buffers will be used while processing response, false otherwise
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
            this.clientProvider.getHttpClientFactoryBuilder().withPooledResponseBuffers(pooledResponseBuffersEnabled);
            return this;
        }

        /**
         * @param estimatedResponseSizeInBytes initial size of response buffer if response buffers enabled (see {@link #withPooledResponseBuffers(boolean)}), ignored otherwise
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> withPooledResponseBuffersSizeInBytes(int estimatedResponseSizeInBytes) {
            this.clientProvider.getHttpClientFactoryBuilder().withPooledResponseBuffersSizeInBytes(estimatedResponseSizeInBytes);
            return this;
        }

    }

    @Override
    public final void start() {

        if (isStarted()) {
            return;
        }

        addOperation(() -> LifeCycle.of(clientProvider).start());

        startExtensions();

        state = State.STARTED;

    }

    @Override
    public final void stop() {

        if (isStopped()) {
            return;
        }

        getLogger().debug("Stopping {}", getClass().getSimpleName());

        stopExtensions();

        LifeCycle.of(clientProvider).stop();

        state = State.STOPPED;

        getLogger().debug("{} stopped", getClass().getSimpleName());

    }

    @Override
    public final boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public final boolean isStopped() {
        return state == State.STOPPED;
    }

}
