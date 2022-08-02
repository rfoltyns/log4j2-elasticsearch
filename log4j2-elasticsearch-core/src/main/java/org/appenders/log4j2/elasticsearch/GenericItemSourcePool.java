package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
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

import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricType;
import org.appenders.log4j2.elasticsearch.metrics.Metrics;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

/**
 * <p>Resizable pool of {@link ItemSource} elements.
 * <p>Automatically expands when it runs out of elements. Expansion size depends on {@link ResizePolicy} configuration.
 * <p>Automatically recycles unused elements. Recycle size depends on {@link ResizePolicy} configuration.
 * <p>Pooled elements can be added explicitly with {@link #incrementPoolSize(int)} and/or {@link #incrementPoolSize()} methods.
 * <p>Pooled elements can be purged explicitly with {@link #remove()}.
 * <p>{@link #shutdown()} MUST be called to cleanup underlying resources.
 * <p>NOTE: Consider this class <i>private</i>. Design may change before the code is stabilized.
 */
public class GenericItemSourcePool<T> implements ItemSourcePool<T>, Measured {

    private static final int INITIAL_RESIZE_INTERNAL_STACK_DEPTH = 0;
    public static final String THREAD_NAME_FORMAT = "%s-%s";

    private volatile State state = State.STOPPED;

    private final Queue<ItemSource<T>> objectPool;

    private final String poolName;
    private final PooledObjectOps<T> pooledObjectOps;
    private final PooledItemSourceReleaseCallback releaseCallback = new PooledItemSourceReleaseCallback();

    private final ResizePolicy resizePolicy;
    private final long resizeTimeout;
    private final AtomicBoolean resizing = new AtomicBoolean();
    private final AtomicReference<CountDownLatch> countDownLatch = new AtomicReference<>(new CountDownLatch(1));

    private final Consumer<Boolean> unlatchAndResetResizing = result -> {
        this.countDownLatch.get().countDown();
        resizing.set(false);
    };

    private final Consumer<Boolean> resetResizing = result -> {
        resizing.set(false);
    };

    private final int maxRetries = Integer.parseInt(
            System.getProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".resize.retries", "5"));

    private final int initialPoolSize;
    private final AtomicInteger totalPoolSize = new AtomicInteger();

    private final PoolMetrics metrics;

    private final boolean monitored;
    private final long monitorTaskInterval;

    private final AtomicInteger thIndex = new AtomicInteger();

    ScheduledExecutorService executor;

    /**
     * @param poolName metrics componentName part
     * @param pooledObjectOps pooled objects manager
     * @param resizePolicy resizing strategy when no more pooled elements available
     * @param resizeTimeout single resize attempt timeout on {@link #getPooled()} call. 5 retries by default. Use {@code appenders.GenericItemSourcePool.resize.retries} to adjust
     * @param monitored if <i>true</i>, legacy metric printer will be enabled
     * @param monitorTaskInterval time to wait after last legacy metric printer completion
     * @param initialPoolSize target size after {@link #start()}
     *
     * @deprecated This constructor is deprecated and will be removed in future releases. Use {@link #GenericItemSourcePool(String, PooledObjectOps, ResizePolicy, long, int, MetricsFactory)} instead.
     */
    @Deprecated
    public GenericItemSourcePool(final String poolName,
                                 final PooledObjectOps<T> pooledObjectOps,
                                 final ResizePolicy resizePolicy,
                                 final long resizeTimeout,
                                 final boolean monitored,
                                 final long monitorTaskInterval,
                                 final int initialPoolSize) {
        this(poolName,
                pooledObjectOps,
                resizePolicy,
                resizeTimeout,
                monitored,
                monitorTaskInterval,
                initialPoolSize,
                getQueueFactoryInstance(GenericItemSourcePool.class.getSimpleName()).tryCreateMpmcQueue(initialPoolSize),
                new DefaultMetricsFactory(metricConfigs(monitored)));
    }

    /**
     * @param poolName metrics componentName part
     * @param pooledObjectOps pooled objects manager
     * @param resizePolicy resizing strategy when no more pooled elements available
     * @param resizeTimeout single resize attempt timeout on {@link #getPooled()} call. 5 retries by default. Use {@code appenders.GenericItemSourcePool.resize.retries} to adjust
     * @param initialPoolSize target size after {@link #start()}
     * @param metricsFactory metric factory for this pool instance. Any changes to given metric factory made after this call will not affect constructed pool instance.
     */
    public GenericItemSourcePool(final String poolName,
                                 final PooledObjectOps<T> pooledObjectOps,
                                 final ResizePolicy resizePolicy,
                                 final long resizeTimeout,
                                 final int initialPoolSize,
                                 final MetricsFactory metricsFactory) {
        this(poolName,
                pooledObjectOps,
                resizePolicy,
                resizeTimeout,
                false,
                Long.MAX_VALUE,
                initialPoolSize,
                getQueueFactoryInstance(GenericItemSourcePool.class.getSimpleName()).tryCreateMpmcQueue(initialPoolSize),
                metricsFactory);
    }

    GenericItemSourcePool(final String poolName,
                          final PooledObjectOps<T> pooledObjectOps,
                          final ResizePolicy resizePolicy,
                          final long resizeTimeout,
                          final boolean monitored,
                          final long monitorTaskInterval,
                          final int initialPoolSize,
                          final Queue<ItemSource<T>> objectPool,
                          final MetricsFactory metricsFactory) {
        this.poolName = poolName;
        this.pooledObjectOps = pooledObjectOps;
        this.resizePolicy = resizePolicy;
        this.resizeTimeout = resizeTimeout;
        this.initialPoolSize = initialPoolSize;
        this.monitored = monitored;
        this.monitorTaskInterval = monitorTaskInterval;
        this.objectPool = objectPool;
        this.metrics = new PoolMetrics(poolName, metricsFactory);
    }

    public static List<MetricConfig> metricConfigs(final boolean enabled) {
        return Arrays.asList(
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "initial"),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "total"),
                MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, enabled, "available"),
                MetricConfigFactory.createCountConfig(enabled, "noSuchElementCaught"),
                MetricConfigFactory.createCountConfig(enabled, "resizeAttempts")
        );
    }


    private void startRecyclerTask() {
        executor.scheduleAtFixedRate(new Recycler(this, resizePolicy), 1000, 10000, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a task that prints pool statistics
     *
     * @param monitorTaskInterval interval between two snapshots
     * @param additionalMetricsSupplier metrics added on top of defaults
     * @deprecated As of 1.7, this method will be removed. Use {@link MetricsFactory instead}
     */
    @Deprecated
    void startMonitorTask(final long monitorTaskInterval, Supplier<String> additionalMetricsSupplier) {
        final MetricPrinter command = new MetricPrinter(getName() + "-MetricPrinter", metrics, additionalMetricsSupplier);
        executor.scheduleAtFixedRate(command,
                Long.parseLong(System.getProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay", "1000")),
                monitorTaskInterval,
                TimeUnit.MILLISECONDS
        );

    }

    ScheduledExecutorService createExecutor() {
        return createExecutor(poolName);
    }

    ScheduledExecutorService createExecutor(String threadName) {
        return Executors.newScheduledThreadPool(2,
                r -> new Thread(r, String.format(THREAD_NAME_FORMAT, threadName, thIndex.incrementAndGet())));
    }

    /**
     * Creates pooled {@link ItemSource} instances
     *
     * @param delta number of elements to be pooled
     */
    @Override
    public final void incrementPoolSize(int delta) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < delta; i++) {
            incrementPoolSize();
        }

        getLogger().info("Pool [{}] {} pooled elements added. Total pooled elements: {}. Took: {}ms",
                getName(),
                delta,
                getTotalSize(),
                System.currentTimeMillis() - start);

    }

    /**
     * Creates ONE pooled {@link ItemSource}
     */
    @Override
    public final void incrementPoolSize() {
        objectPool.offer(pooledObjectOps.createItemSource(releaseCallback));
        totalPoolSize.getAndIncrement();
    }

    /**
     * <p>Elements returned by this method MUST be returned to the pool with {@link ItemSource#release()}.</p>
     * <p>If pool has no more elements, {@link ResizePolicy} will try to create more pooled elements. Will throw on failure.</p>
     *
     * @throws PoolResourceException if {@link ResizePolicy} was not sufficient or didn't create any new elements
     * @throws IllegalStateException  if thread was interrupted while awaiting resizing
     * @return pooled {@link ItemSource} if not empty or resized, throws otherwise
     */
    @Override
    public final ItemSource<T> getPooled() throws PoolResourceException {
        return removeInternal(INITIAL_RESIZE_INTERNAL_STACK_DEPTH);
    }

    /**
     * <p>Elements returned by this method MUST be returned to the pool with {@link ItemSource#release()}.</p>
     * <p>This call ignore <code>"appenders.GenericItemSourcePool.resize.retries"</code> setting.</p>
     * <p>If pool has no more elements, {@link ResizePolicy} will try to create more pooled elements ONCE. Will return null on failure.</p>
     *
     * @return pooled {@link ItemSource} if not empty or resized, {@code null} otherwise
     */
    @Override
    public final ItemSource<T> getPooledOrNull() {

        try {

            if (objectPool.isEmpty() && !resizeNow()) {
                return null;
            }

            return objectPool.remove();

        } catch (NoSuchElementException e) {

            metrics.noSuchElement();

            // don't push it on races. just bail.
            return null;

        }

    }

    private boolean resizeNow() {

        if (!resizePolicy.canResize(this)) {
            return false;
        }

        // let's allow only one thread to get in
        if (resizing.compareAndSet(false, true)) {
            return resize(resetResizing);
        }

        return false;

    }

    @Override
    public final boolean remove() {
        try {
            ItemSource<T> pooled = removeInternal(maxRetries);
            pooledObjectOps.purge(pooled);
            totalPoolSize.decrementAndGet();
        } catch (PoolResourceException e) {
            return false;
        }
        return true;
    }

    private ItemSource<T> removeInternal(int depth) throws PoolResourceException {

        try {

            if (objectPool.isEmpty()){
                awaitResize(depth);
            }

            return objectPool.remove();

        } catch (NoSuchElementException e) {
            // TODO: add backoff policy
            metrics.noSuchElement();
        }

        // let's go recursive to handle case when resize is smaller than number of threads arriving at the latch
        return removeInternal(++depth);
    }

    private boolean awaitResize(int depth) throws PoolResourceException {

        // NOTE: let's prevent stack overflow when pool resizing is not sufficient and thread gets stuck doing recursive calls, either:
        // * application is in bad shape already or
        // * initialPoolSize is insufficient and/or ResizePolicy is not configured properly
        if (depth > maxRetries) {
            // this will not get anywhere.. throwing to resurface
            throw new PoolResourceException(
                    String.format("ResizePolicy is ineffective. Pool %s has to be reconfigured to handle current load.",
                            poolName));
        }

        // let's allow only one thread to get in
        if (resizing.compareAndSet(false, true)) {
            this.countDownLatch.set(new CountDownLatch(1));
            return resize(unlatchAndResetResizing);
        }

        try {
            countDownLatch.get().await(resizeTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
            throw new IllegalStateException("Thread interrupted while waiting for resizing to complete");
        }

        return false;

    }

    private boolean resize(Consumer<Boolean> callback) {

        boolean resized = false;

        try {
            metrics.resizeAttempt();
            resized = resizePolicy.increase(this);

        } finally {
            callback.accept(resized);
        }

        return resized;

    }

    @Override
    public final String getName() {
        return poolName;
    }

    @Override
    public final int getInitialSize() {
        return initialPoolSize;
    }

    /**
     * @return Total number of elements managed by this pool
     */
    @Override
    public final int getTotalSize() {
        return totalPoolSize.get();
    }

    /**
     * @return Number of pooled elements currently available
     */
    @Override
    public final int getAvailableSize() {
        return objectPool.size();
    }

    @Override
    // TODO: add shutdown(timeout)
    public void shutdown() {

        getLogger().debug("{} shutting down. Releasing buffers..", poolName);

        while (!objectPool.isEmpty()) {
            pooledObjectOps.purge(objectPool.remove());
            totalPoolSize.decrementAndGet();
        }

        getLogger().debug("{} stopping internal threads..", poolName);
        executor.shutdown();

        metrics.deregister();

        getLogger().debug("{} shutdown complete", poolName);

    }

    @Override
    public void register(final MetricsRegistry registry) {

        metrics.register(registry);
        Measured.of(pooledObjectOps).register(registry);

    }

    @Override
    public void deregister() {

        metrics.deregister();
        Measured.of(pooledObjectOps).deregister();

    }

    class PooledItemSourceReleaseCallback implements ReleaseCallback<T> {

        @Override
        public void completed(ItemSource<T> itemSource) {
            pooledObjectOps.reset(itemSource);
            if (!isStarted()) {
                pooledObjectOps.purge(itemSource);
                totalPoolSize.decrementAndGet();
                return;
            }
            objectPool.offer(itemSource);
        }

    }

    static class Recycler extends Thread {

        private final ItemSourcePool pool;
        private final ResizePolicy resizePolicy;

        Recycler(ItemSourcePool pool, ResizePolicy resizePolicy) {
            super(pool.getName() + "-Recycler");
            this.pool = pool;
            this.resizePolicy = resizePolicy;
        }

        @Override
        public void run() {
            resizePolicy.decrease(pool);
        }
    }

    class MetricPrinter extends Thread {

        private final Supplier<String> additionalMetricsSupplier;
        private final Consumer<String> printer;

        MetricPrinter(final String threadName, final PoolMetrics metrics, final Supplier<String> additionalMetricsSupplier) {
            super(threadName);
            this.additionalMetricsSupplier = additionalMetricsSupplier;
            this.printer = additionalMetrics -> {

                final int capacity = additionalMetrics != null ? additionalMetrics.length() + 384 : 128; // because we love less garbage

                final StringBuilder sb = new StringBuilder(capacity)
                        .append('{')
                        .append(" poolName: ").append(getName())
                        .append(", initialPoolSize: ").append(metrics.initialSize.getValue())
                        .append(", totalPoolSize: ").append(metrics.totalSize.getValue())
                        .append(", availablePoolSize: ").append(metrics.availableSize.getValue());

                if (additionalMetrics != null) {
                    sb.append(", additionalMetrics: ").append(additionalMetrics);
                }

                sb.append('}');

                getLogger().info(sb.toString());
            };
        }

        @Override
        public void run() {
            printer.accept(additionalMetricsSupplier.get());
        }

    }

    class PoolMetrics implements Metrics {

        private final List<MetricsRegistry.Registration> registrations = new ArrayList<>();
        private final Metric noSuchElementCaught;
        private final Metric initialSize;
        private final Metric totalSize;
        private final Metric availableSize;
        private final Metric resizeAttempts;

        public PoolMetrics(final String name, final MetricsFactory factory) {
            this.initialSize = factory.createMetric(name, "initial", GenericItemSourcePool.this::getInitialSize);
            this.totalSize = factory.createMetric(name, "total", GenericItemSourcePool.this::getTotalSize);
            this.availableSize = factory.createMetric(name, "available", GenericItemSourcePool.this::getAvailableSize);
            this.noSuchElementCaught = factory.createMetric(name, "noSuchElementCaught");
            this.resizeAttempts = factory.createMetric(name, "resizeAttempts");
        }

        @Override
        public void register(final MetricsRegistry registry) {

            registrations.add(registry.register(initialSize));
            registrations.add(registry.register(totalSize));
            registrations.add(registry.register(availableSize));
            registrations.add(registry.register(noSuchElementCaught));
            registrations.add(registry.register(resizeAttempts));

        }

        @Override
        public void deregister() {
            registrations.forEach(MetricsRegistry.Registration::deregister);
            registrations.clear();
        }

        public final void noSuchElement() {
            noSuchElementCaught.store(1L);
        }

        public final void resizeAttempt() {
            resizeAttempts.store(1L);
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {

        if (!isStarted()) {
            incrementPoolSize(initialPoolSize);
            this.executor = createExecutor(poolName);
            startRecyclerTask();
            if (monitored) {
                startMonitorTask(monitorTaskInterval, pooledObjectOps.createMetricsSupplier());
            }

            state = State.STARTED;

        }

    }

    @Override
    public void stop() {

        if (!isStopped()) {
            // let's change state immediately to allow to release properly
            state = State.STOPPED;
            shutdown();
        }

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
