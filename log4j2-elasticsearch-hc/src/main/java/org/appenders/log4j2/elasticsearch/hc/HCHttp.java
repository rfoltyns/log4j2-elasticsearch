package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.OperationFactoryDispatcher;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.hc.failover.HCFailedItemOps;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@link PooledItemSourceFactory}-based {@link ClientObjectFactory}. {@link PooledItemSourceFactory} MUST be configured.
 * Produces {@link HttpClient} and related objects.
 */
public class HCHttp implements ClientObjectFactory<HttpClient, BatchRequest> {

    private volatile State state = State.STOPPED;

    protected final HttpClientProvider clientProvider;
    protected final PooledItemSourceFactory itemSourceFactory;
    protected final ObjectReader objectReader;
    protected final String mappingType;
    protected final FailedItemOps<IndexRequest> failedItemOps;
    protected final BackoffPolicy<BatchRequest> backoffPolicy;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();
    private final ValueResolver valueResolver;
    private final PooledItemSourceFactory operationFactoryItemSourceFactory;
    private final OperationFactoryDispatcher setupOps;

    public HCHttp(Builder builder) {
        this.clientProvider = builder.clientProvider;
        this.itemSourceFactory = builder.pooledItemSourceFactory;
        this.mappingType = builder.mappingType;
        this.failedItemOps = builder.failedItemOps;
        this.objectReader = configuredReader();
        this.backoffPolicy = builder.backoffPolicy;

        // FIXME: setupOps should be injected here..
        this.valueResolver = builder.valueResolver;
        this.operationFactoryItemSourceFactory = createSetupOpsItemSourceFactory();
        this.setupOps = createSetupOps();
    }

    @Override
    public Function<BatchRequest, Boolean> createFailureHandler(FailoverPolicy failover) {
        return batchRequest -> {

            long start = System.currentTimeMillis();
            int batchSize = batchRequest.getIndexRequests().size();

            getLogger().warn("BatchRequest of {} indexRequests failed. Redirecting to {}", batchSize, failover.getClass().getName());

            batchRequest.getIndexRequests().forEach(indexRequest -> {
                // TODO: FailoverPolicyChain
                try {
                    failover.deliver(failedItemOps.createItem(indexRequest));
                } catch (Exception e) {
                    // let's handle here as exception thrown at this stage will cause the client to shutdown
                    getLogger().error(e.getMessage(), e);
                }
            });

            getLogger().trace("BatchRequest of {} indexRequests redirected in {} ms", batchSize, System.currentTimeMillis() - start);

            return true;
        };
    }

    @Override
    public BatchOperations<BatchRequest> createBatchOperations() {
        return new HCBatchOperations(itemSourceFactory, mappingType);
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectReader} to deserialize {@link BatchResult}
     */
    protected ObjectReader configuredReader() {
        return new ObjectMapper()
                .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .addMixIn(BatchResult.class, BatchResultMixIn.class)
                .addMixIn(Error.class, ErrorMixIn.class)
                .addMixIn(BatchItemResult.class, BatchItemResultMixIn.class)
                .readerFor(BatchResult.class);
    }

    protected ResponseHandler<BatchResult> createResultHandler(BatchRequest request, Function<BatchRequest, Boolean> failureHandler) {
        return new ResponseHandler<BatchResult>() {

            @Override
            public void completed(BatchResult result) {

                getLogger().debug("Cluster service time: {}", result.getTook());

                backoffPolicy.deregister(request);

                if (!result.isSucceeded()) {
                    // TODO: filter only failed indexRequests when retry is ready.
                    // failing whole request for now
                    failureHandler.apply(request);
                }
                request.completed();

            }

            @Override
            public void failed(Exception ex) {

                getLogger().warn(ex.getMessage(), ex);

                backoffPolicy.deregister(request);

                failureHandler.apply(request);
                request.completed();

            }

            @Override
            public BatchResult deserializeResponse(InputStream responseBody) throws IOException {
                return objectReader.readValue(responseBody);
            }

        };
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(clientProvider.getHttpClientFactoryBuilder().serverList);
    }

    @Override
    public HttpClient createClient() {
        return clientProvider.createClient();
    }

    @Override
    public Function<BatchRequest, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<BatchRequest, Boolean>() {

            private final Function<BatchRequest, Boolean> failureHandler = createFailureHandler(failoverPolicy);

            @Override
            public Boolean apply(BatchRequest request) {

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
                createClient().executeAsync(request, responseHandler);

                return true;
            }

        };
    }

    @Override
    public void execute(IndexTemplate indexTemplate) {
        try {
            setupOperationFactory().create(indexTemplate).execute();
        } catch (Exception e) {
            getLogger().error("IndexTemplate not added", e);
        }
    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    @Override
    public OperationFactory setupOperationFactory() {
        return setupOps;
    }

    /* visible for testing */
    ValueResolver valueResolver() {
        return valueResolver;
    }

    /* visible for testing */
    BlockingResponseHandler<BasicResponse> createBlockingResponseHandler() {
        return new BlockingResponseHandler<>(
                this.objectReader,
                createBlockingResponseFallbackHandler()
        );
    }

    /* visible for testing */
    Function<Exception, BasicResponse> createBlockingResponseFallbackHandler() {
        return (ex) -> {
            BasicResponse basicResponse = new BasicResponse();
            if (ex != null) {
                basicResponse.withErrorMessage(ex.getMessage());
            }
            return basicResponse;
        };
    }

    private HCOperationFactoryDispatcher createSetupOps() {
        return new HCOperationFactoryDispatcher(
                this::execute,
                valueResolver,
                operationFactoryItemSourceFactory);
    }

    private Result execute(SetupStep<Request, Response> setupStep) {

        Response response = createClient().execute(
                setupStep.createRequest(),
                createBlockingResponseHandler()
        );

        return setupStep.onResponse(response);

    }

    private PooledItemSourceFactory createSetupOpsItemSourceFactory() {
        return PooledItemSourceFactory.newBuilder()
                .withItemSizeInBytes(4096)
                .withInitialPoolSize(4)
                .withResizePolicy(UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build())
                .build();
    }

    public static class Builder {

        public static final BackoffPolicy<BatchRequest> DEFAULT_BACKOFF_POLICY = new NoopBackoffPolicy<>();
        public static final String DEFAULT_MAPPING_TYPE = "_doc";

        protected HttpClientProvider clientProvider = new HttpClientProvider(new HttpClientFactory.Builder());
        protected PooledItemSourceFactory pooledItemSourceFactory;
        protected BackoffPolicy<BatchRequest> backoffPolicy = DEFAULT_BACKOFF_POLICY;
        protected String mappingType = DEFAULT_MAPPING_TYPE;
        protected FailedItemOps<IndexRequest> failedItemOps = createFailedItemOps();
        protected ValueResolver valueResolver = ValueResolver.NO_OP;

        public HCHttp build() {
            return new HCHttp(validate());
        }

        protected Builder validate() {

            if (valueResolver == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(ValueResolver.class.getSimpleName()));
            }
            if (clientProvider == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(ClientProvider.class.getSimpleName()));
            }
            if (pooledItemSourceFactory == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(PooledItemSourceFactory.class.getSimpleName()));
            }
            if (backoffPolicy == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(BackoffPolicy.class.getSimpleName()));
            }

            return this;

        }

        private String nullValidationExceptionMessage(final String className) {
            return String.format("No %s provided for %s", className, HCHttp.class.getSimpleName());
        }

        protected FailedItemOps<IndexRequest> createFailedItemOps() {
            return new HCFailedItemOps();
        }

        public Builder withClientProvider(HttpClientProvider clientProvider) {
            this.clientProvider = clientProvider;
            return this;
        }

        public Builder withItemSourceFactory(PooledItemSourceFactory pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }

        public Builder withBackoffPolicy(BackoffPolicy<BatchRequest> backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder withMappingType(String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        public Builder withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
            return this;
        }

        /**
         * @param serverUris semicolon-separated list of target addresses
         * @return this
         *
         * @deprecated As of 1.6, this method will be removed. Use {@link #withClientProvider(HttpClientProvider)} instead.
         */
        @Deprecated
        public Builder withServerUris(String serverUris) {
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
        public Builder withMaxTotalConnections(int maxTotalConnections) {
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
        public Builder withConnTimeout(int connTimeout) {
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
        public Builder withReadTimeout(int readTimeout) {
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
        public Builder withIoThreadCount(int ioThreadCount) {
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
        public Builder withAuth(Auth<HttpClientFactory.Builder> auth) {
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
        public Builder withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
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
        public Builder withPooledResponseBuffersSizeInBytes(int estimatedResponseSizeInBytes) {
            this.clientProvider.getHttpClientFactoryBuilder().withPooledResponseBuffersSizeInBytes(estimatedResponseSizeInBytes);
            return this;
        }

    }

    @Override
    public void start() {

        addOperation(() -> LifeCycle.of(clientProvider).start());

        if (!operationFactoryItemSourceFactory.isStarted()) {
            operationFactoryItemSourceFactory.start();
        }

        if (!itemSourceFactory.isStarted()) {
            itemSourceFactory.start();
        }

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        getLogger().debug("Stopping {}", getClass().getSimpleName());

        LifeCycle.of(clientProvider).stop();

        itemSourceFactory.stop();

        operationFactoryItemSourceFactory.stop();

        state = State.STOPPED;

        getLogger().debug("{} stopped", getClass().getSimpleName());

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
