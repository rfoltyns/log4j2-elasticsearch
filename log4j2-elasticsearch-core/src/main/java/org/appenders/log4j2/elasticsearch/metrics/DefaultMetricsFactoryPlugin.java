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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;

import java.util.Arrays;
import java.util.List;

/**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@Plugin(name = DefaultMetricsFactoryPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = DefaultMetricsFactoryPlugin.ELEMENT_TYPE, printObject = true)
public final class DefaultMetricsFactoryPlugin extends DefaultMetricsFactory {

    static final String PLUGIN_NAME = "Metrics";
    static final String ELEMENT_TYPE = "metricsFactory";

    public DefaultMetricsFactoryPlugin(final List<MetricConfig> initialConfigs) {
        super(initialConfigs);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<DefaultMetricsFactoryPlugin> {

        @PluginElement("metricConfig")
        protected MetricConfig[] metricConfigs = new MetricConfig[0];

        @Override
        public DefaultMetricsFactoryPlugin build() {
            return new DefaultMetricsFactoryPlugin(Arrays.asList(metricConfigs));
        }

        /**
         * @param metricConfigs Metric configurations. Replaces all previously configured metric configs.
         * @return this
         */
        public Builder withMetricConfigs(final MetricConfig[] metricConfigs) {
            this.metricConfigs = metricConfigs;
            return this;
        }

    }

}
