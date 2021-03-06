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
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Uses underlying {@link ItemSourcePool} to get {@link ItemSource} instances.
 */
@Plugin(name = PooledItemSourceFactory.PLUGIN_NAME, category = Node.CATEGORY, elementType = ItemSourceFactory.ELEMENT_TYPE, printObject = true)
public class PooledItemSourceFactory<T, R> implements ItemSourceFactory<T, R> {

    public static final String PLUGIN_NAME = "PooledItemSourceFactory";

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

    @PluginBuilderFactory
    public static PooledItemSourceFactory.Builder<Object, ByteBuf> newBuilder() {
        return new PooledItemSourceFactory.Builder<>();
    }

    public static class Builder<T, R> implements org.apache.logging.log4j.core.util.Builder<PooledItemSourceFactory<T, R>> {

        public static final long DEFAULT_RESIZE_TIMEOUT = 1000L;
        public static final long DEFAULT_MONITOR_TASK_INTERVAL = 30000L;

        @PluginBuilderAttribute
        protected String poolName;

        @PluginElement(ResizePolicy.ELEMENT_TYPE)
        protected ResizePolicy resizePolicy;

        @PluginBuilderAttribute
        protected int itemSizeInBytes;

        @PluginBuilderAttribute
        protected int initialPoolSize;

        @PluginBuilderAttribute
        protected boolean monitored;

        @PluginBuilderAttribute
        protected long monitorTaskInterval = DEFAULT_MONITOR_TASK_INTERVAL;

        @PluginBuilderAttribute
        protected long resizeTimeout = DEFAULT_RESIZE_TIMEOUT;

        @PluginBuilderAttribute
        protected int maxItemSizeInBytes = Integer.MAX_VALUE;

        @Override
        public PooledItemSourceFactory<T, R> build() {

            if (initialPoolSize <= 0) {
                throw new ConfigurationException("initialPoolSize must be higher than 0 for " + PLUGIN_NAME);
            }
            if (itemSizeInBytes <= 0) {
                throw new ConfigurationException("itemSizeInBytes must be higher than 0 for " + PLUGIN_NAME);
            }
            if (maxItemSizeInBytes <= 0) {
                throw new ConfigurationException("maxItemSizeInBytes must be higher than 0 for " + PLUGIN_NAME);
            }
            if (maxItemSizeInBytes < itemSizeInBytes) {
                throw new ConfigurationException("maxItemSizeInBytes must be higher than or equal to itemSizeInBytes for " + PLUGIN_NAME);
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
                    configuredPooledObjectOps(new UnpooledByteBufAllocator(false, false, false)),
                    resizePolicy,
                    resizeTimeout,
                    monitored,
                    monitorTaskInterval,
                    initialPoolSize
            );
        }

        PooledObjectOps<R> configuredPooledObjectOps(UnpooledByteBufAllocator byteBufAllocator) {
            return (PooledObjectOps<R>) new ByteBufPooledObjectOps(
                    byteBufAllocator,
                    new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, maxItemSizeInBytes));
        }

        /**
         * @param itemSizeInBytes initial pooled item size
         * @return this
         */
        public Builder<T, R> withItemSizeInBytes(int itemSizeInBytes) {
            this.itemSizeInBytes = itemSizeInBytes;
            return this;
        }

        /**
         * @param maxItemSizeInBytes maximum pooled item size (target size if oversized in runtime)
         * @return this
         */
        public Builder<T, R> withMaxItemSizeInBytes(int maxItemSizeInBytes) {
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
