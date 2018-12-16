package org.appenders.log4j2.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

/**
 * Wraps {@link AfterburnerModule} configuration to avoid {@link ClassNotFoundException}
 * when {@code org.appenders.log4j2.elasticsearch.JacksonJsonLayout.Builder#withAfterburner(boolean)} is set to false
 * and com.fasterxml.jackson.module:jackson-module-afterburner dependency was not provided
 */
final class JacksonAfterburnerModuleConfigurer {

    void configure(ObjectMapper objectMapper) {
        objectMapper.registerModule(new AfterburnerModule());
    }

}