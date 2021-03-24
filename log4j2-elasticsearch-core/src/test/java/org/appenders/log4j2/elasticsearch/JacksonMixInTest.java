package org.appenders.log4j2.elasticsearch;

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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.appenders.log4j2.elasticsearch.thirdparty.LogEventJacksonJsonMixIn;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JacksonMixInTest {

    public static JacksonMixIn.Builder createDefaultTestBuilder() {
        JacksonMixIn.Builder builder = JacksonMixIn.newBuilder();
        builder.withMixInClass(LogEventJacksonJsonMixIn.class.getName());
        builder.withTargetClass(Log4jLogEvent.class.getName());
        return builder;
    }

    @Test
    public void buiderSucceedsOnValidConfig() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();

        // when
        JacksonMixIn jacksonMixIn = builder.build();

        // then
        assertNotNull(jacksonMixIn);
        assertEquals(Log4jLogEvent.class, jacksonMixIn.getTargetClass());
        assertEquals(LogEventJacksonJsonMixIn.class, jacksonMixIn.getMixInClass());
    }

    @Test
    public void builderThrowsOnNullTargetClass() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        builder.withTargetClass(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No targetClass provided for " + JacksonMixIn.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnNullMixInClass() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        builder.withMixInClass(null);


        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No mixInClass provided for " + JacksonMixIn.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnMixInClassNotFound() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        String mixInClass = "org.appenders.test.NonExistingClass";
        builder.withMixInClass(mixInClass);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Cannot load mixInClass"));
        assertThat(exception.getMessage(), containsString(mixInClass));

    }

    @Test
    public void builderThrowsOnTargetClassNotFound() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        String targetClass = "org.appenders.test.NonExistingClass";
        builder.withTargetClass(targetClass);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Cannot load targetClass"));
        assertThat(exception.getMessage(), containsString(targetClass));

    }

}
