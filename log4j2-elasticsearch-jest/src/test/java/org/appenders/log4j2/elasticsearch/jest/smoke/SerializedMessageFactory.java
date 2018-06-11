package org.appenders.log4j2.elasticsearch.jest.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

public class SerializedMessageFactory extends FormattedMessageFactory {

    final ObjectMapper objectMapper;

    public SerializedMessageFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message newMessage(final Object message) {
        return new SerializedMessage(message, objectMapper);
    }

}