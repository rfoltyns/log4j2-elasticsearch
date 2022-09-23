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


import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.hc.failover.HCFailedItemOps;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates {@link HttpClient} and batch handlers.
 */
public class HCHttp extends BatchingClientObjectFactory<BatchRequest, IndexRequest> {

    protected final BatchOperations<BatchRequest> batchOperations;
    protected final OperationFactory operationFactory;

    public HCHttp(Builder builder) {
        super(builder);
        this.batchOperations = builder.batchOperations;
        this.operationFactory = builder.operationFactory;
    }

    public static List<MetricConfig> metricConfigs(final boolean enabled) {
        return BatchingClientMetrics.createConfigs(enabled);
    }

    @Override
    public BatchOperations<BatchRequest> createBatchOperations() {
        return batchOperations;
    }

    @Override
    public OperationFactory setupOperationFactory() {
        return operationFactory;
    }

    protected ResponseHandler<BatchResult> createResultHandler(BatchRequest request, Function<BatchRequest, Boolean> failureHandler) {
        return new HCResponseHandler(request, failureHandler);
    }

    public static class Builder extends BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest> {

        protected BatchOperations<BatchRequest> batchOperations;
        protected OperationFactory operationFactory;

        @Override
        public HCHttp build() {
            return new HCHttp(validate());
        }

        protected Builder validate() {
            super.validate();

            if (operationFactory == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(OperationFactory.class.getSimpleName()));
            }

            if (batchOperations == null) {
                throw new IllegalArgumentException(nullValidationExceptionMessage(BatchOperations.class.getSimpleName()));
            }

            return this;

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

    @Override
    public void register(MetricsRegistry registry) {
        super.register(registry);
        Measured.of(batchOperations).register(registry);
    }

    @Override
    public void deregister() {
        super.deregister();
        Measured.of(batchOperations).deregister();
    }

    private class HCResponseHandler implements ResponseHandler<BatchResult> {

        private final BatchRequest request;
        private final Function<BatchRequest, Boolean> failureHandler;

        public HCResponseHandler(final BatchRequest request, final Function<BatchRequest, Boolean> failureHandler) {

            this.request = request;
            this.failureHandler = failureHandler;
        }

        @Override
        public void completed(BatchResult result) {

            metrics.serverTookMs(result.getTook());

            backoffPolicy.deregister(request);

            if (!result.isSucceeded()) {
                // TODO: filter only failed indexRequests when retry is ready.
                // failing whole request for now
                failureHandler.apply(request);
            } else {
                metrics.itemsDelivered(request.size());
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
        public BatchResult deserializeResponse(final InputStream responseBody) throws IOException {
            return request.deserialize(responseBody);
        }

    }

}
