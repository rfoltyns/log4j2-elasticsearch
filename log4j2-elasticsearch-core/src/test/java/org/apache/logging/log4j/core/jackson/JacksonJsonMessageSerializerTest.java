package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Matchers.eq;
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
