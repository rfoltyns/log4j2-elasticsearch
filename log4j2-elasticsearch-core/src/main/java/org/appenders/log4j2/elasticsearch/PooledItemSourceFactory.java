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

import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Uses underlying {@link ItemSourcePool} to get {@link ItemSource} instances.
 */
public class PooledItemSourceFactory<T, R> implements ItemSourceFactory<T, R> {

    private volatile State state = State.STOPPED;

    final ItemSourcePool<R> bufferedItemSourcePool;

    protected PooledItemSourceFactory(ItemSourcePool<R> itemSourcePool) {
        this.bufferedItemSourcePool = itemSourcePool;
    }

    /**
     * @return true
     */
    @Override
    public final boolean isBuffered() {
        return true;
    }

    /**
     * Serializes given object to {@link ByteBufItemSource} using given {@link ObjectWriter}
     *
     * @param source item to serialize
     * @param objectWriter writer to be used to serialize given item
     * @throws IllegalStateException if underlying pool cannot provide {@link ByteBufItemSource}
     * @throws IllegalArgumentException if serialization failed
     * @return {@link ByteBufItemSource} with serialized event
     * @deprecated As of 1.7, this method will be removed. Use {@link #create(Object, Serializer)} instead.
     */
    @Override
    public ItemSource create(Object source, ObjectWriter objectWriter) {

        final ItemSource<R> pooled = createEmptySource();

        try {
            OutputStream byteBufOutputStream = new ReusableByteBufOutputStream((ByteBuf) pooled.getSource());
            objectWriter.writeValue(byteBufOutputStream, source);
            return pooled;
        } catch (IOException e) {
            pooled.release();
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ItemSource<R> create(T source, Serializer<T> serializer) {

        final ItemSource<R> pooled = createEmptySource();

        try {
            final OutputStream byteBufOutputStream = new ReusableByteBufOutputStream((ByteBuf) pooled.getSource());
            serializer.write(byteBufOutputStream, source);
            return pooled;
        } catch (Exception e) {
            pooled.release();
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @return {@link ByteBufItemSource} with no content
     */
    @Override
    public ItemSource<R> createEmptySource() {
        try {
            return bufferedItemSourcePool.getPooled();
        } catch (PoolResourceException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Builder<T, R> {

        public static final long DEFAULT_RESIZE_TIMEOUT = 1000L;
        public static final long DEFAULT_MONITOR_TASK_INTERVAL = 30000L;
        private static final int DEFAULT_ITEM_SIZE_IN_BYTES = 0;
        private static final int DEFAULT_MAX_ITEM_SIZE_IN_BYTES = Integer.MAX_VALUE;

        protected String poolName;
        protected ResizePolicy resizePolicy;
        protected int initialPoolSize;
        protected boolean monitored;
        protected long monitorTaskInterval = DEFAULT_MONITOR_TASK_INTERVAL;
        protected long resizeTimeout = DEFAULT_RESIZE_TIMEOUT;
        protected PooledObjectOps<R> pooledObjectOps;

        /**
         * @deprecated As of 1.7, this field will be removed. Use {@link #pooledObjectOps} instead.
         */
        @Deprecated
        protected int itemSizeInBytes = DEFAULT_ITEM_SIZE_IN_BYTES;

        /**
         * @deprecated As of 1.7, this field will be removed. Use {@link #pooledObjectOps} instead.
         */
        @Deprecated
        protected int maxItemSizeInBytes = DEFAULT_MAX_ITEM_SIZE_IN_BYTES;

        public PooledItemSourceFactory<T, R> build() {

            if (initialPoolSize <= 0) {
                throw new IllegalArgumentException("initialPoolSize must be higher than 0 for " + PooledItemSourceFactory.class.getSimpleName());
            }
            if (pooledObjectOps == null) {

                getLogger().warn("No PooledObjectOps provided for " + PooledItemSourceFactory.class.getSimpleName() + ". Falling back to deprecated size limit config based on itemSizeInBytes and maxItemSizeInBytes and Netty allocator");

                if (itemSizeInBytes <= 0) {
                    throw new IllegalArgumentException("itemSizeInBytes must be higher than 0 for " + PooledItemSourceFactory.class.getSimpleName());
                }
                if (maxItemSizeInBytes <= 0) {
                    throw new IllegalArgumentException("maxItemSizeInBytes must be higher than 0 for " + PooledItemSourceFactory.class.getSimpleName());
                }
                if (maxItemSizeInBytes < itemSizeInBytes) {
                    throw new IllegalArgumentException("maxItemSizeInBytes must be higher than or equal to itemSizeInBytes for " + PooledItemSourceFactory.class.getSimpleName());
                }

                pooledObjectOps = configuredPooledObjectOps(UnpooledByteBufAllocator.DEFAULT);

            }

            if (poolName == null) {
                poolName = ItemSourcePool.class.getSimpleName();
            }

            if (resizePolicy == null) {

                ResizePolicy resizePolicy = createResizePolicy();

                getLogger().info("No configured {} found for pool {}. Defaulting to {}",
                        ResizePolicy.ELEMENT_TYPE,
                        poolName,
                        resizePolicy.getClass().getSimpleName());

                this.resizePolicy = resizePolicy;
            }

            return new PooledItemSourceFactory<>(configuredItemSourcePool());

        }

        /**
         * Creates default {@link ResizePolicy} if one was not configured
         * @return {@link UnlimitedResizePolicy}
         */
        ResizePolicy createResizePolicy() {
            return UnlimitedResizePolicy.newBuilder().build();
        }

        /* extension point */
        ItemSourcePool<R> configuredItemSourcePool() {
            return new GenericItemSourcePool<>(
                    poolName,
                    pooledObjectOps,
                    resizePolicy,
                    resizeTimeout,
                    monitored,
                    monitorTaskInterval,
                    initialPoolSize
            );
        }

        /**
         * @param byteBufAllocator Netty ByteBuf allocator
         * @return this
         * @deprecated As of 1.7, this method will be removed. Construct {@link PooledObjectOps} outside of this builder and use {@link #withPooledObjectOps(PooledObjectOps)} instead.
         */
        @Deprecated
        PooledObjectOps<R> configuredPooledObjectOps(UnpooledByteBufAllocator byteBufAllocator) {
            return (PooledObjectOps<R>) new ByteBufPooledObjectOps(
                    byteBufAllocator,
                    new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, maxItemSizeInBytes));
        }

        /**
         * @param pooledObjectOps pooled items ops
         * @return this
         */
        public Builder<T, R> withPooledObjectOps(PooledObjectOps<R> pooledObjectOps) {

            if (itemSizeInBytes != DEFAULT_ITEM_SIZE_IN_BYTES || maxItemSizeInBytes != DEFAULT_MAX_ITEM_SIZE_IN_BYTES) {
                throw new IllegalArgumentException("Cannot use both [max]itemSizeInBytes and pooledObjectOps. Set size limits pooledObjectOps.sizeLimitPolicy instead");
            }

            this.pooledObjectOps = pooledObjectOps;
            return this;

        }

        /**
         * @param itemSizeInBytes initial pooled item size
         * @return this
         */
        public Builder<T, R> withItemSizeInBytes(int itemSizeInBytes) {

            if (pooledObjectOps != null) {
                throw new IllegalArgumentException("Cannot use both itemSizeInBytes and pooledObjectOps. Set size limits with pooledObjectOps.sizeLimitPolicy instead");
            }

            this.itemSizeInBytes = itemSizeInBytes;
            return this;

        }

        /**
         * @param maxItemSizeInBytes maximum pooled item size (target size if oversized in runtime)
         * @return this
         */
        public Builder<T, R> withMaxItemSizeInBytes(int maxItemSizeInBytes) {

            if (pooledObjectOps != null) {
                throw new IllegalArgumentException("Cannot use both maxItemSizeInBytes and pooledObjectOps. Set size limits with pooledObjectOps.sizeLimitPolicy instead");
            }

            this.maxItemSizeInBytes = maxItemSizeInBytes;
            return this;

        }

        /**
         * @param initialPoolSize pool size before resizing
         * @return this
         */
        public Builder<T, R> withInitialPoolSize(int initialPoolSize) {
            this.initialPoolSize = initialPoolSize;
            return this;
        }

        /**
         * Default: random UUID
         *
         * @param poolName name of underlying pool
         * @return this
         */
        public Builder<T, R> withPoolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        /**
         * Default: {@link #createResizePolicy()}
         *
         * @param resizePolicy policy to be applied when underlying pool run out of elements
         * @return this
         */
        public Builder<T, R> withResizePolicy(ResizePolicy resizePolicy) {
            this.resizePolicy = resizePolicy;
            return this;
        }

        /**
         * @param monitored if true, a log entry with pool metrics will be produced every {@link Builder#monitorTaskInterval},
         *                  no metrics logging otherwise
         * @return this
         */
        public Builder<T, R> withMonitored(boolean monitored) {
            this.monitored = monitored;
            return this;
        }

        /**
         * @param monitorTaskInterval milliseconds between two metrics snapshots
         * @return this
         */
        public Builder<T, R> withMonitorTaskInterval(long monitorTaskInterval) {
            this.monitorTaskInterval = monitorTaskInterval;
            return this;
        }

        /**
         * @param resizeTimeout milliseconds to wait until {@link ResizePolicy} is applied
         * @return this
         */
        public Builder<T, R> withResizeTimeout(long resizeTimeout) {
            this.resizeTimeout = resizeTimeout;
            return this;
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {
        if (!bufferedItemSourcePool.isStarted()) {
            bufferedItemSourcePool.start();
        }
        state = State.STARTED;
    }

    @Override
    public void stop() {
        if (!bufferedItemSourcePool.isStopped()) {
            bufferedItemSourcePool.stop();
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
