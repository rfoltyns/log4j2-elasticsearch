package org.apache.logging.log4j.core.jackson;

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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 *
 * MODIFICATIONS:
 * rfoltyns:
 * - timeInMillis not ignored anymore (patch for log4j-core:2.11+)
 * - nanoTime, parameterCount, formattedMessage loggerFqcn, source, threadId, threadPriority, endOfBatch, instant ignored
 * - XML-related annotations removed
 * - setters removed
 * - JsonDeserialize annotations removed
 * - JsonFilter removed
 *
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.util.Map;

@JsonPropertyOrder({ "timeMillis", "loggerName", "level", "marker", "message", "thrown", "threadName"})
public abstract class TestLogEventMixIn implements LogEvent {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    @Override
    public abstract Map<String, String> getContextMap();

    @JsonIgnore
    @Override
    public abstract ReadOnlyStringMap getContextData();

    @JsonIgnore
    @Override
    public abstract ContextStack getContextStack();

    @JsonProperty
    @Override
    public abstract Level getLevel();

    @JsonIgnore
    @Override
    public abstract String getLoggerFqcn();

    @JsonProperty
    @Override
    public abstract String getLoggerName();

    @JsonProperty(JsonConstants.ELT_MARKER)
    @Override
    public abstract Marker getMarker();

    @JsonProperty(JsonConstants.ELT_MESSAGE)
    @JsonSerialize(using = MessageSerializer.class)
    @Override
    public abstract Message getMessage();

    @JsonIgnore
    @Override
    public abstract StackTraceElement getSource();

    @JsonProperty
    @Override
    public abstract long getThreadId();

    @JsonProperty("thread")
    @Override
    public abstract String getThreadName();

    @JsonIgnore
    @Override
    public abstract int getThreadPriority();

    @JsonIgnore
    @Override
    public abstract Throwable getThrown();

    @JsonProperty(JsonConstants.ELT_THROWN)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JsonProperty
    @Override
    public abstract long getTimeMillis();

    @JsonIgnore
    @Override
    public abstract boolean isEndOfBatch();

    @JsonIgnore
    @Override
    public abstract boolean isIncludeLocation();

    @JsonIgnore
    @Override
    public abstract long getNanoTime();

    @JsonIgnore
    @Override
    public abstract Instant getInstant();

}

