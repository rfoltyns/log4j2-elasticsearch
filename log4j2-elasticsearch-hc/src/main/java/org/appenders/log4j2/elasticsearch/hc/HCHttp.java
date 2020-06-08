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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.hc.failover.HCFailedItemOps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.appenders.log4j2.elasticsearch.hc.HCHttp.PLUGIN_NAME;

/**
 * {@link PooledItemSourceFactory}-based {@link ClientObjectFactory}. {@link PooledItemSourceFactory} MUST be configured.
 * Produces {@link HttpClient} and related objects.
 */
@Plugin(name = PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class HCHttp implements ClientObjectFactory<HttpClient, BatchRequest> {

    public static final String PLUGIN_NAME = "HCHttp";

    private static Logger LOG = InternalLogging.getLogger();

    private volatile State state = State.STOPPED;

    private final Collection<String> serverUris;
    protected final int connTimeout;
    protected final int readTimeout;
    protected final int maxTotalConnections;
    protected final int ioThreadCount;
    protected final Auth<HttpClientFactory.Builder> auth;
    protected final PooledItemSourceFactory itemSourceFactory;
    protected final ObjectReader objectReader;
    protected final String mappingType;
    protected final boolean pooledResponseBuffers;
    protected final int pooledResponseBuffersSizeInBytes;
    protected final FailedItemOps<IndexRequest> failedItemOps;
    protected final BackoffPolicy<BatchRequest> backoffPolicy;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    private HttpClient client;

    public HCHttp(Builder builder) {
        this.serverUris = Arrays.asList(builder.serverUris.split(";"));
        this.connTimeout = builder.connTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxTotalConnections = builder.maxTotalConnections;
        this.ioThreadCount = builder.ioThreadCount;
        this.auth = builder.auth;
        this.itemSourceFactory = builder.pooledItemSourceFactory;
        this.mappingType = builder.mappingType;
        this.pooledResponseBuffers = builder.pooledResponseBuffers;
        this.pooledResponseBuffersSizeInBytes = builder.pooledResponseBuffersSizeInBytes;
        this.failedItemOps = builder.failedItemOps;
        this.objectReader = configuredReader();
        this.backoffPolicy = builder.backoffPolicy;
    }

    @Override
    public Function<BatchRequest, Boolean> createFailureHandler(FailoverPolicy failover) {
        return batchRequest -> {

            long start = System.currentTimeMillis();
            int batchSize = batchRequest.getIndexRequests().size();

            LOG.warn("BatchRequest of {} indexRequests failed. Redirecting to {}", batchSize, failover.getClass().getName());

            batchRequest.getIndexRequests().forEach(indexRequest -> {
                // TODO: FailoverPolicyChain
                try {
                    failover.deliver(failedItemOps.createItem(indexRequest));
                } catch (Exception e) {
                    // let's handle here as exception thrown at this stage will cause the client to shutdown
                    LOG.error(e.getMessage(), e);
                }
            });

            LOG.trace("BatchRequest of {} indexRequests redirected in {} ms", batchSize, System.currentTimeMillis() - start);

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

                LOG.debug("Cluster service time: {}", result.getTook());

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

                LOG.warn(ex.getMessage(), ex);

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

    // visible for testing
    ClientProvider<HttpClient> getClientProvider(HttpClientFactory.Builder httpClientFactoryBuilder) {
        return new HttpClientProvider(httpClientFactoryBuilder);
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(serverUris);
    }

    @Override
    public HttpClient createClient() {
        if (client == null) {

            HttpClientFactory.Builder builder = new HttpClientFactory.Builder()
                    .withServerList(serverUris)
                    .withConnTimeout(connTimeout)
                    .withReadTimeout(readTimeout)
                    .withMaxTotalConnections(maxTotalConnections)
                    .withIoThreadCount(ioThreadCount)
                    .withPooledResponseBuffers(pooledResponseBuffers)
                    .withPooledResponseBuffersSizeInBytes(pooledResponseBuffersSizeInBytes);

            if (this.auth != null) {
                auth.configure(builder);
            }

            client = getClientProvider(builder).createClient();
        }
        return client;
    }

    @Override
    public Function<BatchRequest, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<BatchRequest, Boolean>() {

            private Function<BatchRequest, Boolean> failureHandler = createFailureHandler(failoverPolicy);

            @Override
            public Boolean apply(BatchRequest request) {

                while (!operations.isEmpty()) {
                    try {
                        operations.remove().execute();
                    } catch (Exception e) {
                        // TODO: redirect to failover (?) retry with exp. backoff (?) multiple options here
                        InternalLogging.getLogger().error("before-batch failed: {}", e.getMessage());
                    }
                }

                if (backoffPolicy.shouldApply(request)) {
                    LOG.warn("Backoff applied. Request rejected.");
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

        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(4096)
                .writeBytes(indexTemplate.getSource().getBytes());

        IndexTemplateRequest request = new IndexTemplateRequest.Builder()
                .withTemplateName(indexTemplate.getName())
                .withSource(byteBuf)
                .build();

        try {
            Function<Exception, BasicResponse> errorResponseTemplate =
                    (ex) -> new BasicResponse().withErrorMessage("IndexTemplate not added: " + ex.getMessage());

            BlockingResponseHandler<BasicResponse> responseHandler =
                    new BlockingResponseHandler<>(objectReader, errorResponseTemplate);

            Response result = createClient().execute(request, responseHandler);
            if (!result.isSucceeded()) {
                LOG.error(result.getErrorMessage());
            }
        } finally {
            byteBuf.release();
        }

    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    @PluginBuilderFactory
    public static HCHttp.Builder newBuilder() {
        return new HCHttp.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<HCHttp> {

        public static final int DEFAULT_RESPONSE_BUFFER_SIZE = 1024 * 1024;

        private static final BackoffPolicy<BatchRequest> DEFAULT_BACKOFF_POLICY = new NoopBackoffPolicy<>();

        @PluginBuilderAttribute
        @Required(message = "No serverUris provided for " + PLUGIN_NAME)
        protected String serverUris;

        @PluginBuilderAttribute
        protected int connTimeout = 1000;

        @PluginBuilderAttribute
        protected int readTimeout = 0;

        @PluginBuilderAttribute
        protected int maxTotalConnections = 8;

        @PluginBuilderAttribute
        protected int ioThreadCount = Runtime.getRuntime().availableProcessors();

        @PluginBuilderAttribute
        protected boolean pooledResponseBuffers = true;

        @PluginBuilderAttribute
        protected int pooledResponseBuffersSizeInBytes = DEFAULT_RESPONSE_BUFFER_SIZE;

        @PluginElement("auth")
        protected Auth auth;

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        protected PooledItemSourceFactory pooledItemSourceFactory;

        @PluginBuilderAttribute
        protected String mappingType = "_doc";

        @PluginElement(BackoffPolicy.NAME)
        protected BackoffPolicy<BatchRequest> backoffPolicy = DEFAULT_BACKOFF_POLICY;

        protected FailedItemOps<IndexRequest> failedItemOps = createFailedItemOps();

        @Override
        public HCHttp build() {

            validate();

            return new HCHttp(this);
        }

        protected void validate() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for " + PLUGIN_NAME);
            }
            if (pooledItemSourceFactory == null) {
                throw new ConfigurationException("No PooledItemSourceFactory provided for " + PLUGIN_NAME);
            }
            if (backoffPolicy == null) {
                throw new ConfigurationException("No BackoffPolicy provided for " + PLUGIN_NAME);
            }
        }

        protected FailedItemOps<IndexRequest> createFailedItemOps() {
            return new HCFailedItemOps();
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public Builder withMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
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

        public Builder withIoThreadCount(int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
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

        public Builder withAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public Builder withMappingType(String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        public Builder withPooledResponseBuffers(boolean pooledResponseBuffersEnabled) {
            this.pooledResponseBuffers = pooledResponseBuffersEnabled;
            return this;
        }

        public Builder withPooledResponseBuffersSizeInBytes(int estimatedResponseSizeInBytes) {
            this.pooledResponseBuffersSizeInBytes = estimatedResponseSizeInBytes;
            return this;
        }

    }

    /**
     * Consider this class <i>private</i>.
     */
    class HttpClientProvider implements ClientProvider<HttpClient> {

        private final HttpClientFactory.Builder httpClientFactoryBuilder;

        public HttpClientProvider(HttpClientFactory.Builder httpClientFactoryBuilder) {
            this.httpClientFactoryBuilder = httpClientFactoryBuilder;
        }

        @Override
        public HttpClient createClient() {
            return httpClientFactoryBuilder.build().createInstance();
        }

    }

    @Override
    public void start() {

        addOperation(() -> createClient().start());

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

        LOG.debug("Stopping {}", getClass().getSimpleName());

        if (client != null) {
            client.stop();
        }
        itemSourceFactory.stop();

        state = State.STOPPED;

        LOG.debug("{} stopped", getClass().getSimpleName());

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
