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
import org.apache.logging.log4j.core.jackson.LogEventJacksonJsonMixIn;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JacksonMixInTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No targetClass provided for " + JacksonMixIn.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnNullMixInClass() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        builder.withMixInClass(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No mixInClass provided for " + JacksonMixIn.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnMixInClassNotFound() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        String mixInClass = "org.appenders.test.NonExistingClass";
        builder.withMixInClass(mixInClass);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("Cannot load mixInClass");
        expectedException.expectMessage(mixInClass);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnTargetClassNotFound() {

        // given
        JacksonMixIn.Builder builder = createDefaultTestBuilder();
        String targetClass = "org.appenders.test.NonExistingClass";
        builder.withTargetClass(targetClass);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("Cannot load targetClass");
        expectedException.expectMessage(targetClass);

        // when
        builder.build();

    }

}
