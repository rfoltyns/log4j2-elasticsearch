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

import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metrics;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

public abstract class BatchingClientObjectFactory<BATCH_TYPE extends Batch<ITEM_TYPE>, ITEM_TYPE extends Item<?>>
        implements ClientObjectFactory<HttpClient, BATCH_TYPE>, Measured {

    private volatile State state = State.STOPPED;

    protected final HttpClientProvider clientProvider;
    protected final FailedItemOps<ITEM_TYPE> failedItemOps;
    protected final BackoffPolicy<BATCH_TYPE> backoffPolicy;

    protected final BatchingClientMetrics metrics;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();


    public BatchingClientObjectFactory(BatchingClientObjectFactory.Builder<BATCH_TYPE, ITEM_TYPE> builder) {
        this.clientProvider = builder.clientProvider;
        this.failedItemOps = builder.failedItemOps;
        this.backoffPolicy = builder.backoffPolicy;
        // TODO: consider builder.metrics for better extensions support in future releases
        this.metrics = new BatchingClientMetrics(builder.name, builder.metricsFactory);
    }

    @Override
    public Function<BATCH_TYPE, Boolean> createFailureHandler(FailoverPolicy failover) {
        return batchRequest -> {

            long start = System.currentTimeMillis();
            metrics.batchFailed();

            final Collection<ITEM_TYPE> items = batchRequest.getItems();

            int batchSize = batchRequest.size();
            metrics.itemsFailed(batchSize);
            getLogger().warn("Batch of {} items failed. Redirecting to {}", batchSize, failover.getClass().getName());

            items.forEach(batchItem -> {
                // TODO: FailoverPolicyChain
                try {
                    failover.deliver(failedItemOps.createItem(batchItem));
                } catch (Exception e) {
                    // let's handle here as exception thrown at this stage will cause the client to shutdown
                    getLogger().error(e.getMessage(), e);
                }
            });

            metrics.failoverTookMs(System.currentTimeMillis() - start);

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

                    getLogger().warn("Backoff applied. Batch of {} items rejected", request.size());
                    metrics.backoffApplied(1);

                    failureHandler.apply(request);
                    request.completed();

                    return false;

                } else {
                    backoffPolicy.register(request);
                }

                ResponseHandler<BatchResult> responseHandler = createResultHandler(request, failureHandler);
                // FIXME: Batch interface shouldn't extend Request!
                createClient().executeAsync(request, responseHandler);

                metrics.itemsSent(request.size());

                return true;
            }

        };
    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    public static abstract class Builder<BATCH_TYPE extends Batch<ITEM_TYPE>, ITEM_TYPE extends Item<?>> {

        private static final AtomicInteger counter = new AtomicInteger();

        private String name = BatchingClientMetrics.class.getSimpleName() + "-" + counter.getAndIncrement();

        protected HttpClientProvider clientProvider = new HttpClientProvider(new HttpClientFactory.Builder());
        protected BackoffPolicy<BATCH_TYPE> backoffPolicy = new NoopBackoffPolicy<>();
        protected FailedItemOps<ITEM_TYPE> failedItemOps;
        protected final MetricsFactory metricsFactory = new DefaultMetricsFactory(BatchingClientMetrics.createConfigs(false));

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

        public Builder<BATCH_TYPE, ITEM_TYPE> withName(String name) {
            this.name = name;
            return this;
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

        public Builder<BATCH_TYPE, ITEM_TYPE> withMetricConfig(final MetricConfig metricConfig) {
            this.metricsFactory.configure(metricConfig);
            return this;
        }

        public Builder<BATCH_TYPE, ITEM_TYPE> withMetricConfigs(final List<MetricConfig> metricConfigs) {
            this.metricsFactory.configure(metricConfigs);
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

    @Override
    public void register(final MetricsRegistry registry) {
        metrics.register(registry);
        addOperation(() -> Measured.of(clientProvider).register(registry));
        addOperation(() -> Measured.of(clientProvider.getHttpClientFactoryBuilder().serviceDiscovery).register(registry));
    }

    @Override
    public void deregister() {
        metrics.deregister();
        Measured.of(clientProvider).deregister();
        Measured.of(clientProvider.getHttpClientFactoryBuilder().serviceDiscovery).deregister();
    }

    public static class BatchingClientMetrics implements Metrics {

        private final List<MetricsRegistry.Registration> registrations = new ArrayList<>();;
        private final Metric serverTookMs;
        private final Metric itemsSent;
        private final Metric itemsDelivered;
        private final Metric itemsFailed;
        private final Metric backoffApplied;
        private final Metric batchesFailed;
        private final Metric failoverTookMs;

        public BatchingClientMetrics(final String name, final MetricsFactory factory) {
            this.serverTookMs = factory.createMetric(name, "serverTookMs");
            this.itemsSent = factory.createMetric(name, "itemsSent");
            this.itemsDelivered = factory.createMetric(name, "itemsDelivered");
            this.itemsFailed = factory.createMetric(name, "itemsFailed");
            this.backoffApplied = factory.createMetric(name, "backoffApplied");
            this.batchesFailed = factory.createMetric(name, "batchesFailed");
            this.failoverTookMs = factory.createMetric(name, "failoverTookMs");
        }

        public static List<MetricConfig> createConfigs(final boolean enabled) {
            return Collections.unmodifiableList(Arrays.asList(
                    MetricConfigFactory.createMaxConfig(enabled, "serverTookMs", true),
                    MetricConfigFactory.createCountConfig(enabled, "itemsSent"),
                    MetricConfigFactory.createCountConfig(enabled, "itemsDelivered"),
                    MetricConfigFactory.createCountConfig(enabled, "itemsFailed"),
                    MetricConfigFactory.createCountConfig(enabled, "backoffApplied"),
                    MetricConfigFactory.createCountConfig(enabled, "batchesFailed"),
                    MetricConfigFactory.createMaxConfig(enabled, "failoverTookMs", true))
            );
        }

        @Override
        public void register(MetricsRegistry registry) {
            registrations.add(registry.register(serverTookMs));
            registrations.add(registry.register(itemsSent));
            registrations.add(registry.register(itemsDelivered));
            registrations.add(registry.register(itemsFailed));
            registrations.add(registry.register(backoffApplied));
            registrations.add(registry.register(batchesFailed));
            registrations.add(registry.register(failoverTookMs));
        }

        @Override
        public void deregister() {
            registrations.forEach(MetricsRegistry.Registration::deregister);
            registrations.clear();
        }


        public void serverTookMs(int tookMs) {
            this.serverTookMs.store(tookMs);
        }

        public void itemsSent(int itemsSent) {
            this.itemsSent.store(itemsSent);
        }

        public void itemsDelivered(int count) {
            this.itemsDelivered.store(count);
        }

        public void itemsFailed(int count) {
            this.itemsFailed.store(count);
        }

        public void backoffApplied(int count) {
            this.backoffApplied.store(count);
        }

        public void batchFailed() {
            this.batchesFailed.store(1);
        }

        public void failoverTookMs(long tookMs) {
            this.failoverTookMs.store(tookMs);
        }

    }

}
