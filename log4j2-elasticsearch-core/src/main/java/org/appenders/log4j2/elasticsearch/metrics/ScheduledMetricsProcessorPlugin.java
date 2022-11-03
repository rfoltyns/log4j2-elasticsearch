package org.appenders.log4j2.elasticsearch.metrics;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.log4j2.elasticsearch.LifeCycle;

import java.time.Clock;

/**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@Plugin(name = ScheduledMetricsProcessorPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ScheduledMetricsProcessorPlugin.ELEMENT_TYPE, printObject = true)
public class ScheduledMetricsProcessorPlugin extends ScheduledMetricsProcessor implements LifeCycle {
    static final String PLUGIN_NAME = "MetricsProcessor";
    static final String ELEMENT_TYPE = "metricsProcessor";

    /**
     * @param initialDelay millis before first run
     * @param interval time to wait after metric collection is completed
     * @param clock clock
     * @param metricsRegistry registered metrics store
     * @param metricOutputs metric collection listeners
     */
    public ScheduledMetricsProcessorPlugin(final long initialDelay,
                                           final long interval,
                                           final Clock clock,
                                           final MetricsRegistry metricsRegistry,
                                           final MetricOutputsRegistry metricOutputs) {
        super(initialDelay,
                interval,
                clock,
                metricsRegistry,
                metricOutputs);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ScheduledMetricsProcessorPlugin> {

        static final long DEFAULT_INITIAL_DELAY = 1000L;
        static final long DEFAULT_INTERVAL = 10000L;

        @PluginBuilderAttribute
        private long initialDelay = DEFAULT_INITIAL_DELAY;

        @PluginBuilderAttribute
        private long interval = DEFAULT_INTERVAL;

        private Clock clock = Clock.systemDefaultZone();

        private MetricsRegistry metricRegistry = new BasicMetricsRegistry();

        @PluginElement(value = "metricOutput")
        private MetricOutput[] metricOutputs;

        @Override
        public ScheduledMetricsProcessorPlugin build() {

            if (clock == null) {
                throw new ConfigurationException("No clock provided for " + MetricsProcessor.class.getSimpleName());
            }

            if (metricRegistry == null) {
                throw new ConfigurationException("No metricRegistry provided for " + MetricsProcessor.class.getSimpleName());
            }

            if (metricOutputs == null) {
                throw new ConfigurationException("No metricOutputs provided for " + MetricsProcessor.class.getSimpleName());
            }

            return new ScheduledMetricsProcessorPlugin(initialDelay,
                    interval,
                    clock,
                    metricRegistry,
                    new BasicMetricOutputsRegistry(metricOutputs));

        }

        /**
         * @param initialDelay millis before first run
         * @return this
         */
        public Builder withInitialDelay(final long initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * @param interval time to wait after metric collection is completed
         * @return this
         */
        public Builder withInterval(final long interval) {
            this.interval = interval;
            return this;
        }

        /**
         * @param clock metric timestamp source
         * @return this
         */
        public Builder withClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * @param metricRegistry registered metrics store
         * @return this
         */
        public Builder withMetricsRegistry(final MetricsRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        /**
         * @param metricOutputs metric outputs. See {@link MetricsProcessor}
         * @return this
         */
        public Builder withMetricOutputs(final MetricOutput[] metricOutputs) {
            this.metricOutputs = metricOutputs;
            return this;
        }

    }

}
