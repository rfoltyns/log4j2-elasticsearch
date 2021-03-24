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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacksonJsonMessageSerializerTest {

    @Test
    public void serializerUsesFormatedMesage() throws IOException {

        // given
        JsonSerializer<Message> serializer = new JacksonJsonMessageSerializer();

        Message message = mock(Message.class);

        // when
        serializer.serialize(message, mock(JsonGenerator.class), mock(SerializerProvider.class));

        // then
        verify(message).getFormattedMessage();

    }

    @Test
    public void serializerWritesRawValue() throws IOException {

        // given
        JsonSerializer<Message> serializer = new JacksonJsonMessageSerializer();

        Message message = mock(Message.class);

        String expectedValue = UUID.randomUUID().toString();
        when(message.getFormattedMessage()).thenReturn(expectedValue);

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        // when
        serializer.serialize(message, jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(jsonGenerator).writeRaw(eq(expectedValue));

    }

}
