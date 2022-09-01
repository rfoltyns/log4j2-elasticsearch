package org.appenders.log4j2.elasticsearch;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@PluginAliases("PooledItemSourceFactory")
@Plugin(name = ByteBufItemSourceFactoryPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ItemSourceFactory.ELEMENT_TYPE, printObject = true)
public class ByteBufItemSourceFactoryPlugin extends PooledItemSourceFactory<Object, ByteBuf> {

    public static final String PLUGIN_NAME = "ByteBufItemSourceFactory";

    protected ByteBufItemSourceFactoryPlugin(ItemSourcePool<ByteBuf> itemSourcePool) {
        super(itemSourcePool);
    }

    @PluginBuilderFactory
    public static ByteBufItemSourceFactoryPlugin.Builder newBuilder() {
        return new ByteBufItemSourceFactoryPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ByteBufItemSourceFactoryPlugin> {

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
        public ByteBufItemSourceFactoryPlugin build() {

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

            return new ByteBufItemSourceFactoryPlugin(configuredItemSourcePool());

        }

        /**
         * Creates default {@link ResizePolicy} if one was not configured
         * @return {@link UnlimitedResizePolicy}
         */
        ResizePolicy createResizePolicy() {
            return UnlimitedResizePolicyPlugin.newBuilder().build();
        }

        /* extension point */
        ItemSourcePool<ByteBuf> configuredItemSourcePool() {

            UnpooledByteBufAllocator byteBufAllocator = new UnpooledByteBufAllocator(false, false, false);
            ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                    byteBufAllocator,
                    new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, maxItemSizeInBytes));

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
         * @param itemSizeInBytes initial pooled item size
         * @return this
         */
        public Builder withItemSizeInBytes(int itemSizeInBytes) {
            this.itemSizeInBytes = itemSizeInBytes;
            return this;
        }

        /**
         * @param maxItemSizeInBytes maximum pooled item size (target size if oversized in runtime)
         * @return this
         */
        public Builder withMaxItemSizeInBytes(int maxItemSizeInBytes) {
            this.maxItemSizeInBytes = maxItemSizeInBytes;
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
}
