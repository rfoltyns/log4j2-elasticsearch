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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @deprecated As of 2.0, this class will be removed. Use {@link org.appenders.log4j2.elasticsearch.thirdparty.MessageJacksonJsonMixIn} instead
 */
@Deprecated
@JsonSerialize(using = JacksonJsonMessageSerializer.class)
public abstract class MessageJacksonJsonMixIn {

}
