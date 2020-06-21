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
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

/**
 * By default, {@code com.fasterxml.jackson.databind.ObjectWriter} instantiates {@code new com.fasterxml.jackson.databind.ser.DefaultSerializerProvider}
 * on each <i>write*</i> call.
 * This class introduces lazily initialized, on-serializer-cache-size-change {@code com.fasterxml.jackson.databind.ser.DefaultSerializerProvider}
 * per {@link ExtendedObjectWriter} object
 * in order to reduce memory allocation when serializing using reused writer.
 */
public class ExtendedObjectWriter extends ObjectWriter {

    private volatile int lastCacheSize = 0;

    private volatile DefaultSerializerProvider serializerProvider;

    public ExtendedObjectWriter(ObjectMapper objectMapper, SerializationConfig serializationConfig) {
        super(objectMapper, serializationConfig);
    }

    public ExtendedObjectWriter(ObjectMapper objectMapper, SerializationConfig serializationConfig, FormatSchema formatSchema) {
        super(objectMapper, serializationConfig, formatSchema);
    }

    public ExtendedObjectWriter(ObjectMapper objectMapper, SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        super(objectMapper, config, rootType, pp);
    }

    @Override
    protected DefaultSerializerProvider _serializerProvider() {
        if (this.serializerProvider == null) {
            serializerProvider = super._serializerProvider();
        }
        if (_serializerProvider.cachedSerializersCount() > lastCacheSize) {
            lastCacheSize = _serializerProvider.cachedSerializersCount();
            serializerProvider = super._serializerProvider();
        }
        return serializerProvider;
    }

}
