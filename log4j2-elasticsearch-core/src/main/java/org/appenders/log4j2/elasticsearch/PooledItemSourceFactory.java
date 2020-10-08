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
public class PooledItemSourceFactory implements ItemSourceFactory {

    public static final String PLUGIN_NAME = "PooledItemSourceFactory";

    private volatile State state = State.STOPPED;

    final ItemSourcePool bufferedItemSourcePool;

    protected PooledItemSourceFactory(ItemSourcePool bufferedItemSourcePool) {
        this.bufferedItemSourcePool = bufferedItemSourcePool;
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
     */
    @Override
    public ItemSource create(Object source, ObjectWriter objectWriter) {
        ItemSource<ByteBuf> pooled;
        try {
            pooled = bufferedItemSourcePool.getPooled();
        } catch (PoolResourceException e) {
            // FIXME: stop throwing and redirect to failover policy here
            throw new IllegalStateException(e);
        }

        try {
            OutputStream byteBufOutputStream = new ReusableByteBufOutputStream(pooled.getSource());
            objectWriter.writeValue(byteBufOutputStream, source);
            return pooled;
        } catch (IOException e) {
            pooled.release();
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @return {@link ByteBufItemSource} with no content
     */
    @Override
    public ItemSource createEmptySource() {
        try {
            return bufferedItemSourcePool.getPooled();
        } catch (PoolResourceException e) {
            throw new IllegalStateException(e);
        }
    }

    @PluginBuilderFactory
    public static PooledItemSourceFactory.Builder newBuilder() {
        return new PooledItemSourceFactory.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PooledItemSourceFactory> {

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

        @Override
        public PooledItemSourceFactory build() {

            if (initialPoolSize <= 0) {
                throw new ConfigurationException("initialPoolSize must be higher than 0 for " + PLUGIN_NAME);
            }
            if (itemSizeInBytes <= 0) {
                throw new ConfigurationException("itemSizeInBytes must be higher than 0 for " + PLUGIN_NAME);
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

            return new PooledItemSourceFactory(configuredItemSourcePool());

        }

        /**
         * Creates default {@link ResizePolicy} if one was not configured
         * @return {@link UnlimitedResizePolicy}
         */
        ResizePolicy createResizePolicy() {
            return UnlimitedResizePolicy.newBuilder().build();
        }

        /* extension point */
        ItemSourcePool configuredItemSourcePool() {

            UnpooledByteBufAllocator byteBufAllocator = new UnpooledByteBufAllocator(false, false, false);
            ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                    byteBufAllocator,
                    itemSizeInBytes);

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
         * @param itemSizeInBytes estimated pooled item size
         * @return this
         */
        public Builder withItemSizeInBytes(int itemSizeInBytes) {
            this.itemSizeInBytes = itemSizeInBytes;
            return this;
        }

        /**
         * @param initialPoolSize pool size before resizing
         * @return this
         */
        public Builder withInitialPoolSize(int initialPoolSize) {
            this.initialPoolSize = initialPoolSize;
            return this;
        }

        /**
         * Default: random UUID
         *
         * @param poolName name of underlying pool
         * @return this
         */
        public Builder withPoolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        /**
         * Default: {@link #createResizePolicy()}
         *
         * @param resizePolicy policy to be applied when underlying pool run out of elements
         * @return this
         */
        public Builder withResizePolicy(ResizePolicy resizePolicy) {
            this.resizePolicy = resizePolicy;
            return this;
        }

        /**
         * @param monitored if true, a log entry with pool metrics will be produced every {@link Builder#monitorTaskInterval},
         *                  no metrics logging otherwise
         * @return this
         */
        public Builder withMonitored(boolean monitored) {
            this.monitored = monitored;
            return this;
        }

        /**
         * @param monitorTaskInterval milliseconds between two metrics snapshots
         * @return this
         */
        public Builder withMonitorTaskInterval(long monitorTaskInterval) {
            this.monitorTaskInterval = monitorTaskInterval;
            return this;
        }

        /**
         * @param resizeTimeout milliseconds to wait until {@link ResizePolicy} is applied
         * @return this
         */
        public Builder withResizeTimeout(long resizeTimeout) {
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
