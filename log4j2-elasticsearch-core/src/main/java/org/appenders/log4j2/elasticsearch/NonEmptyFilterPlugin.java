package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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


@Plugin(
        name = NonEmptyFilterPlugin.PLUGIN_NAME,
        category = Node.CATEGORY,
        elementType = NonEmptyFilterPlugin.ELEMENT_TYPE,
        printObject = true
)
public class NonEmptyFilterPlugin extends NonEmptyFilter {

    public static final String PLUGIN_NAME = "NonEmptyFilter";
    public static final String ELEMENT_TYPE = "virtualPropertyFilter";

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new NonEmptyFilterPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<NonEmptyFilterPlugin> {

        @Override
        public NonEmptyFilterPlugin build() {
            return new NonEmptyFilterPlugin();
        }

    }
}
