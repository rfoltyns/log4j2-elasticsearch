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
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Logging metric output.
 */
@Plugin(name = MetricLogPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = MetricLogPlugin.ELEMENT_TYPE, printObject = true)
public final class MetricLogPlugin extends MetricLog {

    static final String PLUGIN_NAME = "MetricLog";
    static final String ELEMENT_TYPE = "metricLog";

    /**
     * @param name output name
     * @param logger {@link Logger} instance
     * @param filter metric key filter
     */
    public MetricLogPlugin(final String name, final Logger logger, final MetricFilter filter) {
        super(name, logger, filter);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<MetricLogPlugin> {

        @PluginBuilderAttribute
        private String name;

        @PluginBuilderAttribute
        private String includes;

        @PluginBuilderAttribute
        private String excludes;

        private final Set<String> includesSet = new TreeSet<>();
        private final Set<String> excludesSet = new TreeSet<>();

        @Override
        public MetricLogPlugin build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME);
            }

            includesSet.addAll(SplitUtil.split(includes));
            excludesSet.addAll(SplitUtil.split(excludes));

            return new MetricLogPlugin(
                    name,
                    InternalLogging.getLogger(),
                    new IncludeExclude(
                            new ArrayList<>(includesSet),
                            new ArrayList<>(excludesSet)
                    )
            );
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withIncludes(final String includes) {
            this.includes = includes;
            return this;
        }

        public Builder withExcludes(final String excludes) {
            this.excludes = excludes;
            return this;
        }

    }

}
