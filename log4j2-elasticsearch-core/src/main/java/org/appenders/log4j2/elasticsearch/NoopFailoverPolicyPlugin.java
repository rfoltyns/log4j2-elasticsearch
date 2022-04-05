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


import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Default, no-op failure handler. Doesn't perform any action on failed item.
 */
@Plugin(name = "NoopFailoverPolicy", category = Node.CATEGORY, elementType = FailoverPolicy.ELEMENT_TYPE, printObject = true)
public class NoopFailoverPolicyPlugin extends NoopFailoverPolicy {

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<NoopFailoverPolicyPlugin> {

        @Override
        public NoopFailoverPolicyPlugin build() {
            return new NoopFailoverPolicyPlugin();
        }

    }
}
