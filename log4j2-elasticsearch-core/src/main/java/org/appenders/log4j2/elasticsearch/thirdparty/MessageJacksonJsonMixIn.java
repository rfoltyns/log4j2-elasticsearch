package org.appenders.log4j2.elasticsearch.thirdparty;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.message.Message;
import org.appenders.log4j2.elasticsearch.json.jackson.JacksonJsonRawMessageSerializer;

@JsonSerialize(
        as = Message.class,
        using = JacksonJsonRawMessageSerializer.class)
public abstract class MessageJacksonJsonMixIn {

}