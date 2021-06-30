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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonHandlerInstantiatorTest {

    @Test
    public void startsWithNoErrors() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        ValueResolver valueResolver = Mockito.mock(ValueResolver.class);

        // when
        JacksonHandlerInstantiator result = createTestHandlerInstantiator(customProperties, valueResolver);

        // then
        assertNotNull(result);
    }

    @Test
    public void virtualPropertyWriterInstanceReturnsSingleton() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        MapperConfig config = new ObjectMapper().getSerializationConfig();

        // when
        VirtualBeanPropertyWriter result1 = handlerInstantiator.virtualPropertyWriterInstance(
                config,
                VirtualPropertiesWriter.class
        );
        VirtualBeanPropertyWriter result2 = handlerInstantiator.virtualPropertyWriterInstance(
                config,
                VirtualPropertiesWriter.class
        );

        // then
        assertTrue(result1 == result2);

    }

    @Test
    public void virtualPropertyWriterInstanceReturnsNullForUnrelatedTypes() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        ValueResolver valueResolver = Mockito.mock(ValueResolver.class);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        MapperConfig config = new ObjectMapper().getSerializationConfig();

        // when
        VirtualPropertiesWriter instance = handlerInstantiator.virtualPropertyWriterInstance(config, VirtualBeanPropertyWriter.class);

        // then
        assertNull(instance);

    }

    @Test
    public void deserializerInstanceReturnsNull() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        // when
        JsonDeserializer<?> result = handlerInstantiator.deserializerInstance(null, null, null);

        // then
        assertNull(result);

    }

    @Test
    public void keyDeserializerInstanceReturnsNull() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        // when
        KeyDeserializer result = handlerInstantiator.keyDeserializerInstance(null, null, null);

        // then
        assertNull(result);

    }

    @Test
    public void serializerInstanceReturnsNull() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        // when
        JsonSerializer<?> result = handlerInstantiator.serializerInstance(null, null, null);

        // then
        assertNull(result);

    }


    @Test
    public void typeResolverBuilderInstanceReturnsNull() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        // when
        TypeResolverBuilder<?> result = handlerInstantiator.typeResolverBuilderInstance(null, null, null);

        // then
        assertNull(result);

    }

    @Test
    public void typeIdResolverInstanceReturnsNull() {

        // given
        VirtualProperty[] customProperties = new VirtualProperty[0];
        Log4j2Lookup valueResolver = new Log4j2Lookup(null);
        JacksonHandlerInstantiator handlerInstantiator = createTestHandlerInstantiator(customProperties, valueResolver);

        // when
        TypeIdResolver result = handlerInstantiator.typeIdResolverInstance(null, null, null);

        // then
        assertNull(result);

    }

    private JacksonHandlerInstantiator createTestHandlerInstantiator(VirtualProperty[] customProperties, ValueResolver valueResolver) {
        return new JacksonHandlerInstantiator(
                customProperties,
                valueResolver
        );
    }

}
