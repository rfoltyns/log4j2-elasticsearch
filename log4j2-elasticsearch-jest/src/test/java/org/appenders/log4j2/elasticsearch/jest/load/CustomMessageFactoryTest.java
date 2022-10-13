package org.appenders.log4j2.elasticsearch.jest.load;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CustomMessageFactoryTest extends LoadTest {

    @BeforeAll
    public static void beforeClass() {
        // If true or Message is annotated with @AsynchronouslyFormattable
        // LogObject will be serialized in scope of Appender (Disruptor consumer thread)
        // otherwise in Logger
        System.setProperty("log4j.format.msg.async", "true");
    }

    @Test
    public void messageFactoryTest() {

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(true,
                        false,
                        getConfig().getProperty("secure", Boolean.class)));

        ObjectMapper objectMapper = configuredMapper();
        Logger logger = LogManager.getLogger(getConfig().getProperty("loggerName", String.class),
                new SerializedMessageFactory(objectMapper));

        logger.info(new LogObject("Hello, World!"));

    }

    private ObjectMapper configuredMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    private static class LogObject {

        private final Long timeStamp;
        private final String userLog;

        public LogObject(String userLog) {
            this.timeStamp = System.currentTimeMillis();
            this.userLog = userLog;
        }

        public String getUserLog() {
            return userLog;
        }

    }

}
