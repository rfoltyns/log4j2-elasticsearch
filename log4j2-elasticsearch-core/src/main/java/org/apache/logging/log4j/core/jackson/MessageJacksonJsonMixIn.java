package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = JacksonJsonMessageSerializer.class)
public abstract class MessageJacksonJsonMixIn {

}
