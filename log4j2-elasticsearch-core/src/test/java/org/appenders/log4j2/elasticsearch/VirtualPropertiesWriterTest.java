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
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.VirtualPropertyTest.createDefaultVirtualPropertyBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VirtualPropertiesWriterTest {

    @Test
    public void defaultConstructorIsNotSupported() {

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, VirtualPropertiesWriter::new);

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Invalid use of " + VirtualPropertiesWriter.class.getSimpleName() + ". Use virtualProperties based constructors"));

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

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> writer.value(null, null, null));

        // then
        assertThat(exception.getMessage(), containsString("Should not be used with this implementation. Use serializeAsField() to write value directly."));

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
                mock(ValueResolver.class),
                new VirtualPropertyFilter[0]
        ));

        JavaType javaType = config.constructType(LogEvent.class);
        AnnotatedClass annotatedClass = createTestAnnotatedClass(config, javaType);

        SimpleBeanPropertyDefinition simpleBeanPropertyDefinition =
                getTestBeanPropertyDefinition(config, javaType, annotatedClass);

        VirtualPropertiesWriter result = writer.withConfig(
                config,
                annotatedClass,
                simpleBeanPropertyDefinition,
                config.constructType(VirtualProperty.class)
        );

        // then
        assertArrayEquals(writer.virtualProperties, result.virtualProperties);
        assertEquals(writer.valueResolver, result.valueResolver);
        assertArrayEquals(writer.filters, result.filters);

    }

    @Test
    public void serializeAsFieldResolvesVirtualPropertyValue() throws Exception {

        // given
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = spy(createNonDynamicVirtualProperty(null, expectedValue));

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

        VirtualProperty excludedVirtualProperty = createNonDynamicVirtualProperty(expectedName, expectedValue);
        VirtualProperty virtualProperty = createNonDynamicVirtualProperty(expectedName, expectedValue);

        ValueResolver valueResolver = createTestValueResolver(virtualProperty, expectedValue);

        VirtualPropertyFilter virtualPropertyFilter =
                createNonExcludingTestVirtualPropertyFilter(expectedName, expectedValue);

        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[] { excludedVirtualProperty, virtualProperty },
                valueResolver,
                new VirtualPropertyFilter[] { virtualPropertyFilter }
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
        verify(jsonGenerator, never()).writeString(eq((String)null));

    }

    @Test
    public void serializeAsFieldDoesNotWritePropertiesIfPropertiesExcludedByFilters() throws Exception {

        // given
        VirtualProperty virtualProperty = mock(VirtualProperty.class);

        ValueResolver valueResolver = mock(ValueResolver.class);

        VirtualPropertyFilter virtualPropertyFilter = mock(VirtualPropertyFilter.class);
        when(virtualPropertyFilter.isIncluded(any(), any())).thenReturn(false);

        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[] { virtualProperty },
                valueResolver,
                new VirtualPropertyFilter[] { virtualPropertyFilter }
        );

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        // when
        writer.serializeAsField(new Object(), jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(jsonGenerator, never()).writeFieldName(anyString());
        verify(jsonGenerator, never()).writeString(anyString());
        verify(jsonGenerator, never()).writeString(eq((String)null));

    }

    @Test
    public void serializeAsFieldWritesPropertiesIfPropertiesNotExcludedByFilters() throws Exception {

        // given
        String expectedName = UUID.randomUUID().toString();
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = spy(createNonDynamicVirtualProperty(expectedName, expectedValue));

        ValueResolver valueResolver = createTestValueResolver(virtualProperty, expectedValue);

        VirtualPropertyFilter virtualPropertyFilter =
                createNonExcludingTestVirtualPropertyFilter(expectedName, expectedValue);

        VirtualPropertiesWriter writer = new VirtualPropertiesWriter(
                new VirtualProperty[] { virtualProperty },
                valueResolver,
                new VirtualPropertyFilter[] { virtualPropertyFilter }
        );

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        // when
        writer.serializeAsField(new Object(), jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(jsonGenerator).writeFieldName(eq(expectedName));
        verify(jsonGenerator).writeString(eq(expectedValue));

    }

    private ValueResolver createTestValueResolver(VirtualProperty virtualProperty, String expectedValue) {
        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(virtualProperty)).thenReturn(expectedValue);
        return valueResolver;
    }

    private VirtualProperty createNonDynamicVirtualProperty(String expectedName, String expectedValue) {

        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder();

        if (expectedName != null) {
            builder.withName(expectedName);
        }

        if (expectedValue != null) {
            builder.withValue(expectedValue);
        }

        return builder.withDynamic(false).build();
    }

    private VirtualPropertyFilter createNonExcludingTestVirtualPropertyFilter(String expectedName, String expectedValue) {
        VirtualPropertyFilter virtualPropertyFilter = mock(VirtualPropertyFilter.class);
        when(virtualPropertyFilter.isIncluded(expectedName, expectedValue)).thenReturn(true);
        return virtualPropertyFilter;
    }

    private SimpleBeanPropertyDefinition getTestBeanPropertyDefinition(SerializationConfig config, JavaType javaType, AnnotatedClass annotatedClass) {
        return SimpleBeanPropertyDefinition.construct(
                config,
                new VirtualAnnotatedMember(
                        annotatedClass,
                        LogEvent.class,
                        "virtualProperties",
                        javaType
                )
        );
    }

    private AnnotatedClass createTestAnnotatedClass(SerializationConfig config, JavaType javaType) {
        return AnnotatedClassResolver.resolve(
                config,
                javaType,
                null
        );
    }

}
