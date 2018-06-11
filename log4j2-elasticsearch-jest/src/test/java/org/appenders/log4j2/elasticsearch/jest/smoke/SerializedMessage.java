package org.appenders.log4j2.elasticsearch.jest.smoke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.message.ObjectMessage;

public class SerializedMessage extends ObjectMessage {

    private final ObjectMapper objectMapper;
    private String serializedMessage;

    public SerializedMessage(Object userLog, ObjectMapper objectMapper) {
        super(userLog);
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFormattedMessage() {

        if (serializedMessage == null) {
            try {
                serializedMessage = objectMapper.writeValueAsString(getParameter());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return serializedMessage;
    }

}
