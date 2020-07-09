package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

/**
 * Wraps {@link AfterburnerModule} configuration to avoid {@link ClassNotFoundException}
 * when {@code org.appenders.log4j2.elasticsearch.JacksonJsonLayout.Builder#withAfterburner(boolean)} is set to false
 * and com.fasterxml.jackson.module:jackson-module-afterburner dependency was not provided
 */
class JacksonAfterburnerModuleConfigurer implements JacksonModule {

    /**
     * @param objectMapper mapper to configure
     * @deprecated As of 1.6, this method will be removed. Use {@link #applyTo(ObjectMapper)} instead
     */
    @Deprecated
    void configure(ObjectMapper objectMapper) {
        applyTo(objectMapper);
    }

    @Override
    public void applyTo(ObjectMapper objectMapper) {
        objectMapper.registerModule(new AfterburnerModule());
    }

}
