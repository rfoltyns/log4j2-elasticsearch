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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import static org.appenders.core.logging.InternalLogging.getLogger;

@Plugin(name = "JacksonModuleExample", category = Node.CATEGORY, elementType = "JacksonModule", printObject = true)
public class ExampleJacksonModule extends SimpleModule implements JacksonModule {

    @Override
    public void applyTo(ObjectMapper objectMapper) {
        // Uncomment to register your module
        // objectMapper.registerModule(this);
        // ...
        // And log whatever useful info you need. Or not..?
        getLogger().info("{} applied", getClass().getSimpleName());
    }

    @PluginBuilderFactory
    public static ExampleJacksonModule.Builder newBuilder() {
        return new ExampleJacksonModule.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ExampleJacksonModule> {

        @Override
        public ExampleJacksonModule build() {
            return new ExampleJacksonModule();
        }

    }

}
