package org.appenders.log4j2.elasticsearch;

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

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;

public class ExtendedObjectMapper extends ObjectMapper {

    public ExtendedObjectMapper(JsonFactory jsonFactory) {
        super(jsonFactory);
    }

    public ExtendedObjectMapper(ExtendedObjectMapper extendedObjectMapper) {
        super(extendedObjectMapper);
    }

    @Override
    protected ObjectWriter _newWriter(SerializationConfig config) {
        return new ExtendedObjectWriter(this, config);
    }

    protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
        return new ExtendedObjectWriter(this, config, schema);
    }

    @Override
    public ObjectWriter _newWriter(SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        return new ExtendedObjectWriter(this, config, rootType, pp);
    }

    @Override
    public ObjectMapper copy() {
        _checkInvalidCopy(ExtendedObjectMapper.class);
        return new ExtendedObjectMapper(this);
    }

}
