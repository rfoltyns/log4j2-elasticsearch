package org.appenders.log4j2.elasticsearch;

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
 * {@link ResizePolicy} resizing without upper limits to given {@link ItemSourcePool} size.
 */
@Plugin(name = UnlimitedResizePolicyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = ResizePolicy.ELEMENT_TYPE, printObject = true)
public final class UnlimitedResizePolicyPlugin extends UnlimitedResizePolicy {

    public static final String PLUGIN_NAME = "UnlimitedResizePolicy";

    protected UnlimitedResizePolicyPlugin(final double resizeFactor) {
        super(resizeFactor);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<UnlimitedResizePolicyPlugin> {

        @PluginBuilderAttribute
        private double resizeFactor = UnlimitedResizePolicy.Builder.DEFAULT_RESIZE_FACTOR;

        @Override
        public UnlimitedResizePolicyPlugin build() {

            if (resizeFactor <= 0) {
                throw new ConfigurationException("resizeFactor must be higher than 0");
            }

            if (resizeFactor > 1) {
                throw new ConfigurationException("resizeFactor must be lower or equal 1");
            }

            return new UnlimitedResizePolicyPlugin(resizeFactor);
        }

        /**
         * @param resizeFactor fraction of {@link ItemSourcePool#getInitialSize()} by which given pool will be increased, e.g.:
         *                     GIVEN given initial pool size is 100 and resizeFactor is 0.5
         *                     WHEN pool is resized 3 times
         *                     THEN total pooled items is 250
         * @return this
         */
        public Builder withResizeFactor(double resizeFactor) {
            this.resizeFactor = resizeFactor;
            return this;
        }

    }

}
