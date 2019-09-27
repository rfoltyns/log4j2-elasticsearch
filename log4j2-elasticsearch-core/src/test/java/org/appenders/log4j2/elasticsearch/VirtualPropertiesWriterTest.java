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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.VirtualAnnotatedMember;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VirtualPropertiesWriterTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void defaultConstructorIsNotSupported() {

        // given
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Invalid use of " + VirtualPropertiesWriter.class.getSimpleName());

        // when
        new VirtualPropertiesWriter();
    }

    @Test
    public void startsSuccessfully() {

        // when
        VirtualPropertiesWriter result = new VirtualPropertiesWriter(
                new VirtualProperty[0],
                mock(ValueResolver.class)
        );

        // then
        assertNotNull(result);

    }

    @Test
    public void overridenValueIsNotSupported() {

        // given
        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[0],
                mock(ValueResolver.class)
        );

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Should not be used with this implementation");

        // when
        writer.value(null, null, null);

    }

    @Test
    public void overridenFixAccessIsNoop() {

        // given
        ObjectMapper objectMapper = new ObjectMapper();
        SerializationConfig config = objectMapper.getSerializationConfig();

        VirtualPropertiesWriter writer = spy(new VirtualPropertiesWriter(
                new VirtualProperty[0],
                mock(ValueResolver.class)
        ));

        // when
        writer.fixAccess(config);

        // then
        verify(writer, never()).getMember();

    }

    @Test
    public void withConfigReturnsConfiguredWriter() {

        // given
        ObjectMapper objectMapper = new ObjectMapper();
        SerializationConfig config = objectMapper.getSerializationConfig();

        VirtualPropertiesWriter writer = spy(new VirtualPropertiesWriter(
                new VirtualProperty[0],
                mock(ValueResolver.class)
        ));

        JavaType javaType = config.constructType(LogEvent.class);
        AnnotatedClass annotatedClass = AnnotatedClassResolver.resolve(
                config,
                javaType,
                null
        );

        SimpleBeanPropertyDefinition simpleBeanPropertyDefinition = SimpleBeanPropertyDefinition.construct(
                config,
                new VirtualAnnotatedMember(
                        annotatedClass,
                        LogEvent.class,
                        "virtualProperties",
                        javaType
                )
        );

        VirtualPropertiesWriter result = writer.withConfig(
                config,
                annotatedClass,
                simpleBeanPropertyDefinition,
                config.constructType(VirtualProperty.class)
        );

        // then
        assertArrayEquals(writer.virtualProperties, result.virtualProperties);
        assertEquals(writer.valueResolver, result.valueResolver);

    }

    @Test
    public void serializeAsFieldResolvesVirtualPropertyValue() throws Exception {

        // given
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = spy(VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withValue(expectedValue)
                .withDynamic(false)
                .build()
        );

        ValueResolver valueResolver = mock(ValueResolver.class);

        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[] { virtualProperty },
                valueResolver
        );

        // when
        writer.serializeAsField(new Object(), mock(JsonGenerator.class), mock(SerializerProvider.class));

        // then
        verify(valueResolver).resolve(eq(virtualProperty));

    }

    @Test
    public void serializeAsFieldWritesGivenProperties() throws Exception {

        // given
        String expectedName = UUID.randomUUID().toString();
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withName(expectedName)
                .withValue(expectedValue)
                .build();

        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve((VirtualProperty) any())).thenReturn(expectedValue);

        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[] { virtualProperty },
                valueResolver
        );

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        // when
        writer.serializeAsField(new Object(), jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(jsonGenerator).writeFieldName(eq(expectedName));
        verify(jsonGenerator).writeString(eq(expectedValue));

    }

    @Test
    public void serializeAsFieldDoesNotWritePropertiesIfNoPropertiesProvided() throws Exception {

        // given
        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[0],
                mock(ValueResolver.class)
        );

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        // when
        writer.serializeAsField(new Object(), jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(jsonGenerator, never()).writeFieldName(anyString());
        verify(jsonGenerator, never()).writeString(anyString());

    }

}
