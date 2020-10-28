package org.appenders.log4j2.elasticsearch.ecs;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.jackson.LogEventJacksonJsonMixIn;

/**
 * ECS-friendly output.
 *
 * NOTE: {@code @timestamp} is of type {@code long}.
 */
public abstract class LogEventJacksonEcsJsonMixIn extends LogEventJacksonJsonMixIn {

    private static final long serialVersionUID = 1L;

    @JsonProperty("@timestamp")
    @Override
    public abstract long getTimeMillis();

    @JsonProperty("error.stack_trace")
    @JsonSerialize(using = ThrowableProxyStackTraceAsStringEcsSerializer.class)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JsonProperty("log.level")
    @Override
    public abstract Level getLevel();

    @JsonProperty("log.logger")
    @Override
    public abstract String getLoggerName();

    @JsonProperty("process.thread.name")
    @Override
    public abstract String getThreadName();

    @JsonIgnore
    @Override
    public abstract Marker getMarker();

}

