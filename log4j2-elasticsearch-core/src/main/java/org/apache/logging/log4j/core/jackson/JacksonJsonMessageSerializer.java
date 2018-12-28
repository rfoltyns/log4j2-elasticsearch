package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.logging.log4j.message.Message;

import java.io.IOException;

public class JacksonJsonMessageSerializer extends StdScalarSerializer<Message> {

    private static final long serialVersionUID = 1L;

    JacksonJsonMessageSerializer() {
        super(Message.class);
    }

    @Override
    public void serialize(final Message value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        jgen.writeRaw(value.getFormattedMessage());
    }

}