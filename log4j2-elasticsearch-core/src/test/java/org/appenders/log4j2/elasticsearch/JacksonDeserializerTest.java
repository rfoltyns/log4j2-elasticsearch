package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonDeserializerTest {

    @Test
    public void readsInputStream() throws Exception {

        // given
        final Deserializer<JacksonSerializerTest.Dummy> deserializer = new JacksonDeserializer<>(
                new ObjectMapper().readerFor(JacksonSerializerTest.Dummy.class));

        final String expectedValue = UUID.randomUUID().toString();
        final JacksonSerializerTest.Dummy dummy = new JacksonSerializerTest.Dummy(expectedValue);
        final Serializer<JacksonSerializerTest.Dummy> serializer = new JacksonSerializer<>(new ObjectMapper().writerFor(JacksonSerializerTest.Dummy.class));
        final byte[] bytes = serializer.writeAsBytes(dummy);
        final InputStream inputStream = new ByteArrayInputStream(bytes);

        // when
        final JacksonSerializerTest.Dummy result = deserializer.read(inputStream);

        // then
        assertEquals(expectedValue, result.getExpectedContent());

    }

}
