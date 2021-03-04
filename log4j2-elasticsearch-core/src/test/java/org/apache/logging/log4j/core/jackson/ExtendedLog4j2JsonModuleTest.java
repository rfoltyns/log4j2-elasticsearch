package org.apache.logging.log4j.core.jackson;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import org.apache.logging.log4j.core.LogEvent;
import org.appenders.log4j2.elasticsearch.JacksonHandlerInstantiator;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.VirtualPropertyFilter;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtendedLog4j2JsonModuleTest {

    @Test
    public void appliesMixinsToGivenObjectMapper() throws JsonProcessingException {

        // given
        final ExtendedLog4j2JsonModule module = new ExtendedLog4j2JsonModule();
        final ObjectMapper objectMapper = new ObjectMapper();

        SerializationConfig customConfig = objectMapper.getSerializationConfig()
                .with(new JacksonHandlerInstantiator(
                        new VirtualProperty[0],
                        ValueResolver.NO_OP,
                        new VirtualPropertyFilter[0]
                ));

        objectMapper.setConfig(customConfig);

        final long expectedMillis = System.currentTimeMillis();
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(expectedMillis);

        // when
        module.applyTo(objectMapper);
        final String result = objectMapper.writeValueAsString(logEvent);

        // then
        MatcherAssert.assertThat(result, CoreMatchers.containsString("" + expectedMillis));

    }

}