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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.StringItemSourceFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogEventJacksonEcsJsonMixInTest {

    @Test
    public void appliesEcsFields() throws JsonProcessingException {

        // given
        LogEvent logEvent = mock(LogEvent.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(logEvent.getMessage()).thenReturn(new StringFormattedMessage(expectedMessage));

        String expectedExceptionMessage = UUID.randomUUID().toString();
        ThrowableProxy throwableProxy = new ThrowableProxy(new Throwable(expectedExceptionMessage));
        when(logEvent.getThrownProxy()).thenReturn(throwableProxy);

        String expectedThreadName = UUID.randomUUID().toString();
        when(logEvent.getThreadName()).thenReturn(expectedThreadName);

        Level expectedLevel = Level.TRACE;
        when(logEvent.getLevel()).thenReturn(expectedLevel);

        long expectedMillis = 5L;
        when(logEvent.getTimeMillis()).thenReturn(expectedMillis);

        String expectedLoggerName = UUID.randomUUID().toString();
        when(logEvent.getLoggerName()).thenReturn(expectedLoggerName);

        JacksonJsonLayout layout = JacksonJsonLayout.newBuilder()
                .setConfiguration(LoggerContext.getContext(false).getConfiguration())
                .withItemSourceFactory(StringItemSourceFactory.newBuilder().build())
                .withMixins(new JacksonMixIn.Builder()
                        .withMixInClass(LogEventJacksonEcsJsonMixIn.class.getName())
                        .withTargetClass(LogEvent.class.getName())
                        .build())
                .build();

        // when
        ItemSource itemSource = layout.serialize(logEvent);

        // then
        ObjectMapper objectMapper = new ObjectMapper().addMixIn(LogEvent.class, LogEventJacksonEcsJsonMixIn.class);
        TestLogEvent result = objectMapper.readValue((String)itemSource.getSource(), TestLogEvent.class);

        assertEquals(expectedMessage, result.message);
        assertEquals(expectedMillis, result.timeMillis);
        assertTrue(result.stackTrace.contains(expectedExceptionMessage));
        assertEquals(expectedLevel.name(), result.level);
        assertEquals(expectedLoggerName, result.loggerName);
        assertEquals(expectedThreadName, result.threadName);

    }

    public static class TestLogEvent {

        public final long timeMillis;
        public final String message;
        public final String level;
        public final String loggerName;
        public final String threadName;
        public final String stackTrace;

        @JsonCreator
        public TestLogEvent(
                @JsonProperty("message") String message,
                @JsonProperty("@timestamp") long timeMillis,
                @JsonProperty("error.stack_trace") String stackTrace,
                @JsonProperty("log.level") String level,
                @JsonProperty("log.logger") String loggerName,
                @JsonProperty("process.thread.name") String threadName) {
            this.message = message;
            this.timeMillis = timeMillis;
            this.stackTrace = stackTrace;
            this.level = level;
            this.loggerName = loggerName;
            this.threadName = threadName;
        }

    }

}