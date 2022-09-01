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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * {@inheritDoc}
 *
 * <p>Log4j2 Core Plugin. See <a href="https://logging.apache.org/log4j/2.x/manual/plugins.html">Log4j2 Plugins docs</a>
 */
@Plugin(name = LimitedResizePolicyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ResizePolicy.ELEMENT_TYPE, printObject = true)
public class LimitedResizePolicyPlugin extends LimitedResizePolicy {

    public static final String PLUGIN_NAME = "LimitedResizePolicy";

    protected LimitedResizePolicyPlugin(double resizeFactor, int maxSize) {
        super(resizeFactor, maxSize);
    }

    @PluginBuilderFactory
    public static LimitedResizePolicyPlugin.Builder newBuilder() {
        return new LimitedResizePolicyPlugin.Builder();
    }

    public static class Builder extends LimitedResizePolicy.Builder implements org.apache.logging.log4j.core.util.Builder<LimitedResizePolicy> {

        /**
         * Default resize factor
         */
        public static final double DEFAULT_RESIZE_FACTOR = 0.50;

        @PluginBuilderAttribute
        protected double resizeFactor = DEFAULT_RESIZE_FACTOR;

        @PluginBuilderAttribute
        @Required(message = "No maxSize provided for " + PLUGIN_NAME)
        protected int maxSize;

        @Override
        public LimitedResizePolicyPlugin build() {

            if (resizeFactor <= 0) {
                throw new ConfigurationException("resizeFactor must be higher than 0");
            }

            if (resizeFactor > 1) {
                throw new ConfigurationException("resizeFactor must be lower or equal 1");
            }

            if (maxSize <= 0) {
                throw new ConfigurationException("maxSize must be higher or equal 1");
            }
            return new LimitedResizePolicyPlugin(resizeFactor, maxSize);
        }

        /**
         * @param resizeFactor fraction of {@link ItemSourcePool#getInitialSize()} by which given pool will be increased, e.g.:
         *                     GIVEN given initial pool size is 100 and resizeFactor is 0.5
         *                     WHEN pool is resized 3 times
         *                     THEN total pooled items is 250
         * @return this
         */
        public LimitedResizePolicyPlugin.Builder withResizeFactor(double resizeFactor) {
            this.resizeFactor = resizeFactor;
            return this;
        }

        /**
         * @param maxSize max no. of elements after pool resize
         * @return this
         */
        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
    }

}
