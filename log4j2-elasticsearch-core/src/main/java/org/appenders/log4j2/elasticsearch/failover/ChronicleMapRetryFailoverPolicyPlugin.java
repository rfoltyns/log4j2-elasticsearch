package org.appenders.log4j2.elasticsearch.failover;

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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2
 *
 */
@Plugin(name = ChronicleMapRetryFailoverPolicyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = FailoverPolicy.ELEMENT_TYPE, printObject = true)
public class ChronicleMapRetryFailoverPolicyPlugin extends ChronicleMapRetryFailoverPolicy {

    public static final String PLUGIN_NAME = "ChronicleMapRetryFailoverPolicy";

    /**
     * See {@link Builder}
     *
     * @param builder config
     */
    protected ChronicleMapRetryFailoverPolicyPlugin(ChronicleMapRetryFailoverPolicy.Builder builder) {
        super(builder);
    }

    @PluginBuilderFactory
    public static ChronicleMapRetryFailoverPolicyPlugin.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ChronicleMapRetryFailoverPolicyPlugin> {

        @PluginBuilderAttribute("fileName")
        protected String fileName;

        @PluginBuilderAttribute("numberOfEntries")
        protected long numberOfEntries;

        @PluginBuilderAttribute("averageValueSize")
        protected int averageValueSize = ChronicleMapRetryFailoverPolicy.Builder.DEFAULT_AVERAGE_VALUE_SIZE;

        @PluginBuilderAttribute("batchSize")
        protected int batchSize = ChronicleMapRetryFailoverPolicy.Builder.DEFAULT_BATCH_SIZE;

        @PluginBuilderAttribute("retryDelay")
        protected long retryDelay = ChronicleMapRetryFailoverPolicy.Builder.DEFAULT_RETRY_DELAY;

        @PluginElement("keySequenceSelector")
        protected KeySequenceSelector keySequenceSelector;

        @PluginBuilderAttribute("monitored")
        protected boolean monitored;

        @PluginBuilderAttribute("monitorTaskInterval")
        protected long monitorTaskInterval = ChronicleMapRetryFailoverPolicy.Builder.DEFAULT_RETRY_DELAY;

        @Override
        public final ChronicleMapRetryFailoverPolicyPlugin build() {

            ChronicleMapRetryFailoverPolicy.Builder builder = new ChronicleMapRetryFailoverPolicy.Builder()
                    .withAverageValueSize(averageValueSize)
                    .withBatchSize(batchSize)
                    .withFileName(fileName)
                    .withKeySequenceSelector(keySequenceSelector)
                    .withMonitored(monitored)
                    .withMonitorTaskInterval(monitorTaskInterval)
                    .withNumberOfEntries(numberOfEntries)
                    .withRetryDelay(retryDelay)
                    .validate()
                    .lazyInit();

            return new ChronicleMapRetryFailoverPolicyPlugin(builder);

        }

        /**
         * @param fileName ChronicleMap file name. Both absolute and relative paths are allowed
         * @return this
         */
        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * @param numberOfEntries Failed item count limit.
         *                        <p>Underlying storage MAY fail if storing beyond this limit
         *                        <p>Underlying storage MAY fail before this limit is reached
         *                        if averageValueSize of stored entries was exceeded.
         *                        <p>Calculate the {@link #averageValueSize} carefully
         * @return this
         */
        public Builder withNumberOfEntries(long numberOfEntries) {
            this.numberOfEntries = numberOfEntries;
            return this;
        }

        /**
         * @param averageValueSize Average size of failed item. NOTE: metadata requires ~1kB, add payload length on top of it.
         * @return this
         */
        public Builder withAverageValueSize(int averageValueSize) {
            this.averageValueSize = averageValueSize;
            return this;
        }

        /**
         * @param batchSize max number of retried items on each retry attempt
         * @return this
         */
        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * @param retryDelay pause after each retry attempt
         * @return this
         */
        public Builder withRetryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        /**
         * @param monitored if <tt>true</tt>, execution metrics will be printed
         * @return this
         */
        public Builder withMonitored(boolean monitored) {
            this.monitored = monitored;
            return this;
        }

        /**
         * @param monitorTaskInterval interval between metrics print task
         * @return this
         */
        public Builder withMonitorTaskInterval(long monitorTaskInterval) {
            this.monitorTaskInterval = monitorTaskInterval;
            return this;
        }

        /**
         * @param keySequenceSelector key sequence resolver
         * @return this
         */
        public Builder withKeySequenceSelector(KeySequenceSelector keySequenceSelector) {
            this.keySequenceSelector = keySequenceSelector;
            return this;
        }

    }

}
