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
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.hc.failover.HCFailedItemOps;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates {@link HttpClient} and batch handlers.
 */
public class HCHttp extends BatchingClientObjectFactory<BatchRequest, IndexRequest> {

    protected final BatchOperations<BatchRequest> batchOperations;
    protected final OperationFactory operationFactory;

    private final ObjectReader objectReader;

    public HCHttp(Builder builder) {
        super(builder);
        this.batchOperations = builder.batchOperations;
        this.operationFactory = builder.operationFactory;
        this.objectReader = configuredReader();
    }

    @Override
    public BatchOperations<BatchRequest> createBatchOperations() {
        return batchOperations;
    }

    @Override
    public OperationFactory setupOperationFactory() {
        return operationFactory;
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectReader} to deserialize {@link BatchResult}
     * @deprecated This method will be removed in future releases (not earlier than 1.6)
     */
    @Deprecated
    protected ObjectReader configuredReader() {
        // TODO: Inject..?
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
                return objectReader.readValue(responseBody, BatchResult.class);
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

    public static class Builder extends BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest> {

        protected BatchOperations<BatchRequest> batchOperations;
        protected OperationFactory operationFactory;

        /**
         * @deprecated As of 1.6, this field will be removed. Use {@link #batchOperations} instead.
         */
        @Deprecated
        protected String mappingType;

        /**
         * @deprecated As of 1.6, this field will be removed. Use {@link #batchOperations} instead.
         */
        @Deprecated
        protected PooledItemSourceFactory pooledItemSourceFactory;

        @Override
        public HCHttp build() {
            return new HCHttp(validate());
        }

        protected Builder validate() {
            super.validate();

            if (operationFactory == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(OperationFactory.class.getSimpleName()));
            }

            handleDeprecations();

            if (batchOperations == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(BatchOperations.class.getSimpleName()));
            }

            return this;

        }

        private void handleDeprecations() {
            if (batchOperations != null && (mappingType != null || pooledItemSourceFactory != null)) {
                getLogger().warn("{}: DEPRECATION! {} and {} fields are deprecated and will be ignored. Using provided {}",
                        HCHttp.class.getSimpleName(), "mappingType", "pooledItemSourceFactory", "batchOperations");
            } else if (mappingType != null && pooledItemSourceFactory != null) {
                getLogger().warn("{}: DEPRECATION! {} and {} fields are deprecated. Use {} instead",
                        HCHttp.class.getSimpleName(), "mappingType", "itemSourceFactory", "batchOperations");
                batchOperations = new HCBatchOperations(pooledItemSourceFactory, mappingType);
            }
        }

        private String nullValidationExceptionMessage(final String className) {
            return String.format("No %s provided for %s", className, HCHttp.class.getSimpleName());
        }

        protected FailedItemOps<IndexRequest> createFailedItemOps() {
            return new HCFailedItemOps();
        }

        @Override
        public final Builder withClientProvider(HttpClientProvider clientProvider) {
            return (Builder) super.withClientProvider(clientProvider);
        }

        @Override
        public final Builder withBackoffPolicy(BackoffPolicy<BatchRequest> backoffPolicy) {
            return (Builder) super.withBackoffPolicy(backoffPolicy);
        }

        @Override
        public final Builder withFailedItemOps(FailedItemOps<IndexRequest> failedItemOps) {
            return (Builder) super.withFailedItemOps(failedItemOps);
        }

        public Builder withBatchOperations(BatchOperations<BatchRequest> batchOperations) {
            this.batchOperations = batchOperations;
            return this;
        }

        public Builder withOperationFactory(OperationFactory operationFactory) {
            this.operationFactory = operationFactory;
            return this;
        }

        /**
         * @param mappingType mapping type
         * @deprecated As of 1.6, this method will be removed. Use {@link #batchOperations} instead.
         * @return this
         */
        @Deprecated
        public Builder withMappingType(String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        /**
         * @param pooledItemSourceFactory {@link org.appenders.log4j2.elasticsearch.ItemSource} pool
         * @deprecated As of 1.6, this method will be removed. Use {@link #batchOperations} instead.
         * @return this
         */
        @Deprecated
        public Builder withItemSourceFactory(PooledItemSourceFactory pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }

    }

    @Override
    public void startExtensions() {
        LifeCycle.of(batchOperations).start();
        LifeCycle.of(operationFactory).start();
    }

    @Override
    public void stopExtensions() {
        LifeCycle.of(batchOperations).stop();
        LifeCycle.of(operationFactory).stop();
    }

}
