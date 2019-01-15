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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * <p>Resizable pool of {@link BufferedItemSource} elements.
 * <p>Automatically expands when it runs out of elements. Expansion size depends on {@link ResizePolicy} configuration.
 * <p>Automatically recycles unused elements. Recycle size depends on {@link ResizePolicy} configuration.
 * <p>Pooled elements can be added explicitly with {@link #incrementPoolSize(int)} and/or {@link #incrementPoolSize()} methods.
 * <p>Pooled elements can be recycled explicitly with {@link #remove()}.
 * <p>{@link #shutdown()} MUST be called to cleanup underlying resources.
 * <p>NOTE: Consider this class <i>private</i>. Design may change before the code is stabilized.
 */
class BufferedItemSourcePool implements ItemSourcePool<ByteBuf> {

    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final int INITIAL_RESIZE_INTERNAL_STACK_DEPTH = 0;
    // TODO: make configurable via system property
    private static final int MAX_RESIZE_INTERNAL_STACK_DEPTH = 50;

    ScheduledExecutorService executor;

    private final String poolName;
    private final UnpooledByteBufAllocator byteBufAllocator;
    private final ConcurrentLinkedQueue<ItemSource<ByteBuf>> objectPool = new ConcurrentLinkedQueue<>();
    private final int estimatedSourceSize;

    private final ResizePolicy resizePolicy;
    private final long resizeTimeout;
    private final AtomicBoolean resizing = new AtomicBoolean();
    private final AtomicReference<CountDownLatch> countDownLatch = new AtomicReference<>(new CountDownLatch(1));

    private final int initialPoolSize;
    private final AtomicInteger totalPoolSize;

    BufferedItemSourcePool(String poolName, UnpooledByteBufAllocator byteBufAllocator, ResizePolicy resizePolicy, long resizeTimeout, boolean monitored, long monitorTaskInterval, int initialPoolSize, int itemSizeInBytes) {
        this.poolName = poolName;
        this.byteBufAllocator = byteBufAllocator;
        this.resizePolicy = resizePolicy;
        this.resizeTimeout = resizeTimeout;
        this.initialPoolSize = initialPoolSize;
        this.totalPoolSize = new AtomicInteger();
        this.estimatedSourceSize = itemSizeInBytes;
        this.executor = createExecutor();

        incrementPoolSize(initialPoolSize);

        startRecyclerTask();

        if (monitored) {
            startMonitorTask(monitorTaskInterval);
        }

    }

    private void startRecyclerTask() {
        executor.scheduleAtFixedRate(new Recycler(this, resizePolicy), 1000, 10000, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a task that prints pool statistics
     *
     * @param monitorTaskInterval interval between two snapshots
     */
    void startMonitorTask(long monitorTaskInterval) {
        executor.scheduleAtFixedRate(new MetricPrinter(getName(), byteBufAllocator.metric(), this.new PoolMetrics()),
                1000L,
                monitorTaskInterval,
                TimeUnit.MILLISECONDS
        );

    }

    ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Creates pooled {@link BufferedItemSource} instances
     *
     * @param delta number of elements to be pooled
     */
    @Override
    public final void incrementPoolSize(int delta) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < delta; i++) {
            incrementPoolSize();
        }

        LOGGER.info("Pool [{}] {} pooled elements added. Total pooled elements: {}. Took: {}ms",
                getName(),
                delta,
                getTotalSize(),
                (System.currentTimeMillis() - start));

    }

    /**
     * Creates ONE pooled {@link BufferedItemSource}
     */
    @Override
    public final void incrementPoolSize() {

        CompositeByteBuf buffer = new CompositeByteBuf(byteBufAllocator, false, 2).capacity(estimatedSourceSize);

        objectPool.add(new BufferedItemSource(buffer, bufferedItemSource -> {
            bufferedItemSource.getSource().clear();
            objectPool.add(bufferedItemSource);
        }));

        totalPoolSize.getAndIncrement();
    }

    /**
     * Elements returned by this method MUST be returned to the pool by calling {@link ItemSource#release()}.
     * If pool has no more elements, {@link ResizePolicy} will try to create more pooled elements.
     *
     * @throws PoolResourceException if {@link ResizePolicy} was not sufficient or didn't create any new elements or thread calling this method was interrupted
     * @return pooled {@link BufferedItemSource}
     */
    @Override
    public ItemSource<ByteBuf> getPooled() throws PoolResourceException {
        return removeInternal(INITIAL_RESIZE_INTERNAL_STACK_DEPTH);
    }

    @Override
    public boolean remove() {
        try {
            removeInternal(MAX_RESIZE_INTERNAL_STACK_DEPTH).getSource().release();
            totalPoolSize.getAndDecrement();
        } catch (PoolResourceException e) {
            return false;
        }
        return true;
    }

    private ItemSource<ByteBuf> removeInternal(int depth) throws PoolResourceException {

        try {

            if (objectPool.isEmpty()) {
                tryResize(depth);
            }

            return objectPool.remove();

        } catch (NoSuchElementException e) {
            tryResize(depth);
        }

        // let's go recursive to handle case when resize is smaller than number of threads arriving at the latch
        return removeInternal(++depth);
    }

    private boolean tryResize(int depth) throws PoolResourceException {

        // NOTE: let's prevent stack overflow when pool resizing is not sufficient and thread gets stuck doing recursive calls, either:
        // * application is in bad shape already or
        // * initialPoolSize is insufficient and/or ResizePolicy is not configured properly
        if (depth > MAX_RESIZE_INTERNAL_STACK_DEPTH) {
            // this will not get anywhere.. throwing to resurface
            throw new PoolResourceException(
                    String.format("ResizePolicy is ineffective. Pool %s has to be reconfigured to handle current load.",
                            poolName));
        }

        // let's allow only one thread to get in
        if (resizing.compareAndSet(false, true)) {
            this.countDownLatch.set(new CountDownLatch(1));
            return resize(result -> {
                this.countDownLatch.get().countDown();
                resizing.set(false);
            });
        }

        try {
            countDownLatch.get().await(resizeTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
            throw new IllegalStateException("Thread interrupted while waiting for resizing to complete");
        }

        return false;

    }

    private boolean resize(Consumer<Boolean> callback) throws PoolResourceException {

        boolean resized = false;

        try {
            LOGGER.info("Pool [{}] attempting to resize using policy [{}]",
                    getName(), resizePolicy.getClass().getName());

            resized = resizePolicy.increase(this);
            if (!resized) {
                // TODO: remove when limited resize policy is ready
                // throw to resurface issues
                throw new PoolResourceException(String.format("Unable to resize. Creation of %s was unsuccessful",
                        ItemSource.class.getSimpleName()));
            }
        } finally {
            callback.accept(resized);
        }

        return resized;
    }

    @Override
    public String getName() {
        return poolName;
    }

    @Override
    public int getInitialSize() {
        return initialPoolSize;
    }

    /**
     * @return Total number of elements managed by this pool
     */
    @Override
    public int getTotalSize() {
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
    public void shutdown() {
        objectPool.forEach(pooled -> pooled.getSource().release());
        objectPool.clear();
        executor.shutdown();
    }

    static class Recycler extends Thread {

        private final BufferedItemSourcePool pool;
        private final ResizePolicy resizePolicy;

        Recycler(BufferedItemSourcePool pool, ResizePolicy resizePolicy) {
            super(pool.getName() + "-Recycler");
            this.pool = pool;
            this.resizePolicy = resizePolicy;
        }

        @Override
        public void run() {
            resizePolicy.decrease(pool);
        }
    }

    static class MetricPrinter extends Thread {

        private final Consumer<ByteBufAllocatorMetric> printer;
        private final ByteBufAllocatorMetric allocatorMetric;

        MetricPrinter(String threadName, ByteBufAllocatorMetric allocatorMetric, PoolMetrics poolMetrics) {
            super(threadName);
            this.allocatorMetric = allocatorMetric;
            this.printer = metric -> LOGGER.info(poolMetrics.formattedMetrics(allocatorMetric));
        }

        @Override
        public void run() {
            printer.accept(allocatorMetric);
        }
    }

    class PoolMetrics {

        @Override
        public String toString() {
            return formattedMetrics(null);
        }

        public String formattedMetrics(ByteBufAllocatorMetric allocatorMetric) {

            int capacity = allocatorMetric != null ? 384: 96; // roughly with or without allocator metrics

            StringBuilder sb = new StringBuilder(capacity)
                    .append('{')
                    .append(" poolName: ").append(getName())
                    .append(", initialPoolSize: ").append(getInitialSize())
                    .append(", totalPoolSize: ").append(getTotalSize())
                    .append(", availablePoolSize: ").append(getAvailableSize());

            if (allocatorMetric != null) {
                sb.append(", allocatorMetric: ").append(allocatorMetric);
            }
            return sb.append('}').toString();
        }

    }

}
