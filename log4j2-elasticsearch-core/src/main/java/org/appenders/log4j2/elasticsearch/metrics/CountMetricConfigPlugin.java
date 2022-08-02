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

/**
 * {@inheritDoc}
 *
 /**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@Plugin(name = CountMetricConfigPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = CountMetricConfigPlugin.ELEMENT_TYPE, printObject = true)
public class CountMetricConfigPlugin extends MetricConfig {

    static final String PLUGIN_NAME = "Count";
    static final String ELEMENT_TYPE = "metricConfig";

    public CountMetricConfigPlugin(final String name, final boolean enabled, final boolean reset) {
        super(MetricType.COUNT, name, enabled, reset);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<CountMetricConfigPlugin> {

        @PluginBuilderAttribute
        private String name;

        @PluginBuilderAttribute
        private Boolean reset = Boolean.TRUE;

        @PluginBuilderAttribute
        private Boolean enabled = Boolean.TRUE;

        @Override
        public CountMetricConfigPlugin build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME + " metric");
            }

            return new CountMetricConfigPlugin(name, enabled, reset);

        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withReset(boolean reset) {
            this.reset = reset;
            return this;
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }

}
