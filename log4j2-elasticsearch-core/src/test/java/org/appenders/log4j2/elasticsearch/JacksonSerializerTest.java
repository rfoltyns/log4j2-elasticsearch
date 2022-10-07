package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.appenders.st.jackson.SingleThreadJsonFactory;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacksonSerializerTest {

    public static JacksonSerializer.Builder<Object> createDefaultTestBuilder() {
        return new JacksonSerializer.Builder<>();
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final JacksonSerializer.Builder<Object> builder = createDefaultTestBuilder();

        // when
        final Serializer<Object> layout = builder.build();

        // then
        assertNotNull(layout);

    }

    @Test
    public void builderBuildsMapperWithAfterburnerIfConfigured() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder())
                .withAfterburner(true);

        ObjectMapper objectMapper = spy(new ObjectMapper());
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        // when
        builder.build();

        ArgumentCaptor<Module> captor = ArgumentCaptor.forClass(Module.class);
        verify(objectMapper, atLeastOnce()).registerModule(captor.capture());

        assertThat(captor.getAllValues(), CoreMatchers.hasItem(CoreMatchers.instanceOf(AfterburnerModule.class)));

    }

    @Test
    public void builderBuildsMapperWithMixInsIfConfigured() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());
        final JacksonMixIn mixIn = JacksonMixInTest.createDefaultTestBuilder().build();
        builder.withMixins(mixIn);

        ObjectMapper objectMapper = spy(ObjectMapper.class);

        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        // when
        builder.build();

        // then
        ArgumentCaptor<Class> targetCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Class> mixInCaptor = ArgumentCaptor.forClass(Class.class);
        verify(objectMapper).addMixIn(targetCaptor.capture(), mixInCaptor.capture());

        assertEquals(1, mixInCaptor.getAllValues().size());

    }

    @Test
    public void builderBuildsMapperWithCustomHandlerInstantiator() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        ObjectMapper objectMapper = spy(ObjectMapper.class);

        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        // when
        builder.build();

        // then
        ArgumentCaptor<SerializationConfig> captor = ArgumentCaptor.forClass(SerializationConfig.class);
        verify(objectMapper).setConfig(captor.capture());

        HandlerInstantiator handlerInstantiator = captor.getValue().getHandlerInstantiator();
        assertTrue(handlerInstantiator instanceof JacksonHandlerInstantiator);

    }

    @Test
    public void builderUsesConfiguredValueResolver() {

        // given
        final ValueResolver valueResolver = mock(ValueResolver.class);
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder())
                .withValueResolver(valueResolver);

        // when
        builder.build();

        verify(builder).createValueResolver();

        final ValueResolver result = builder.createValueResolver();

        // then
        assertSame(valueResolver, result);

    }

    @Test
    public void builderResolvesNonDynamicVirtualProperties() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        ValueResolver valueResolver = mock(ValueResolver.class);
        when(builder.createValueResolver()).thenReturn(valueResolver);

        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = new VirtualProperty.Builder()
                .withDynamic(false)
                .withName(UUID.randomUUID().toString())
                .withValue(expectedValue)
                .build();

        builder.withVirtualProperties(virtualProperty);

        // when
        builder.build();

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueResolver).resolve(captor.capture());

        assertEquals(expectedValue, captor.getValue());
    }

    @Test
    public void builderDoesNotUseFiltersWhileResolvingNonDynamicVirtualProperties() {

        // given
        VirtualPropertyFilter filter = mock(VirtualPropertyFilter.class);

        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder()
                .withVirtualPropertyFilters(new VirtualPropertyFilter[] { filter }));

        ValueResolver valueResolver = mock(ValueResolver.class);
        when(builder.createValueResolver()).thenReturn(valueResolver);

        VirtualProperty virtualProperty = new VirtualProperty.Builder()
                .withDynamic(false)
                .withName(UUID.randomUUID().toString())
                .withValue(UUID.randomUUID().toString())
                .build();

        builder.withVirtualProperties(virtualProperty);

        // when
        builder.build();

        // then
        verify(filter, never()).isIncluded(any(), any());
    }

    @Test
    public void builderDoesNotResolveDynamicVirtualProperties() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        ValueResolver valueResolver = mock(ValueResolver.class);
        when(builder.createValueResolver()).thenReturn(valueResolver);

        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = new VirtualProperty.Builder()
                .withDynamic(true)
                .withName(UUID.randomUUID().toString())
                .withValue(expectedValue)
                .build();

        builder.withVirtualProperties(virtualProperty);

        // when
        builder.build();

        // then
        verify(valueResolver, never()).resolve(anyString());

    }

    @Test
    public void defaultJacksonModulesListIsEmpty() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        final ObjectMapper objectMapper = spy(new ObjectMapper());
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        // when
        builder.build();

        // then
        verify(objectMapper, times(0)).registerModule(any());

    }

    @Test
    public void jacksonModulesConfigReplacesPrevious() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        final ObjectMapper objectMapper = spy(new ObjectMapper());
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        final JacksonModule jacksonModule1 = spy(new TestJacksonModule());
        final JacksonModule jacksonModule2 = spy(new TestJacksonModule());
        builder.withJacksonModules(jacksonModule1);
        builder.withJacksonModules(jacksonModule2);

        // when
        builder.build();

        // then
        verify(jacksonModule1, never()).applyTo(objectMapper);
        verify(jacksonModule2).applyTo(objectMapper);
        verify(objectMapper, times(1)).registerModule(any());

    }

    @Test
    public void builderConfiguresAdditionalJacksonModulesIfConfigured() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        ObjectMapper objectMapper = new ObjectMapper();
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        JacksonModule jacksonModule1 = mock(JacksonModule.class);
        JacksonModule jacksonModule2 = spy(new TestJacksonModule());
        builder.withJacksonModules(jacksonModule1, jacksonModule2);

        // when
        builder.build();

        // then
        verify(jacksonModule1).applyTo(eq(objectMapper));
        verify(jacksonModule2).applyTo(eq(objectMapper));

    }

    @Test
    public void builderDoesNotAllowToOverrideModulesWithTheSameClassName() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());

        ObjectMapper objectMapper = new ObjectMapper();
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        JacksonModule jacksonModule1 = spy(new TestJacksonModule());
        JacksonModule jacksonModule2 = spy(new TestJacksonModule());
        builder.withJacksonModules(jacksonModule1, jacksonModule2);

        // when
        builder.build();

        // then
        verify(jacksonModule1).applyTo(eq(objectMapper));
        verify(jacksonModule2, never()).applyTo(eq(objectMapper));

    }

    @Test
    public void builderCreatesExtendedObjectWriter() {

        // given
        final JacksonSerializer.Builder<Object> builder  = createDefaultTestBuilder();

        // when
        ObjectWriter writer = builder.createConfiguredWriter();

        // then
        assertEquals(ExtendedObjectWriter.class, writer.getClass());
    }

    @Test
    public void builderConfiguresExtendedObjectWriter() {

        // given
        final JacksonSerializer.Builder<Object> builder  = spy(createDefaultTestBuilder());

        // when
        builder.build();

        // then
        verify(builder).createConfiguredWriter(any(ObjectMapper.class));

    }

    @Test
    public void builderCreatesDefaultValueResolverIfNotConfigured() {

        // given
        final JacksonSerializer.Builder<Object> builder  = spy(createDefaultTestBuilder());

        // when
        ValueResolver result = builder.createValueResolver();

        // then
        assertEquals(ValueResolver.NO_OP, result);

    }

    @Test
    public void createsSingleThreadJsonFactoryIfConfigured() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());
        builder.withSingleThread(true);

        // when
        ObjectMapper defaultObjectMapper = builder.createDefaultObjectMapper();

        // then
        verify(builder).createJsonFactory();
        assertTrue(defaultObjectMapper.getFactory() instanceof SingleThreadJsonFactory);

    }

    @Test
    public void createsJsonFactoryByDefault() {

        // given
        final JacksonSerializer.Builder<Object> builder = spy(createDefaultTestBuilder());
        builder.withSingleThread(false);

        // when
        ObjectMapper defaultObjectMapper = builder.createDefaultObjectMapper();

        // then
        verify(builder).createJsonFactory();
        assertEquals(defaultObjectMapper.getFactory().getClass(), JsonFactory.class);

    }

    @Test
    public void writesAsBytes() throws Exception {

        // given
        final String expectedContent = UUID.randomUUID().toString();
        final Serializer<Object> serializer = createDefaultTestBuilder().build();

        // when
        final byte[] result = serializer.writeAsBytes(new Dummy(expectedContent));

        // then
        assertThat(new String(result), containsString(expectedContent));

    }

    @Test
    public void writesAsString() throws Exception {

        // given
        final String expectedContent = UUID.randomUUID().toString();
        final Serializer<Object> serializer = createDefaultTestBuilder().build();

        // when
        final String result = serializer.writeAsString(new Dummy(expectedContent));

        // then
        assertThat(result, containsString(expectedContent));

    }

    @Test
    public void writesToOutputStream() throws Exception {

        // given
        final String expectedContent = UUID.randomUUID().toString();
        final Serializer<Object> serializer = createDefaultTestBuilder().build();

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64);

        // when
        serializer.write(outputStream, new Dummy(expectedContent));

        System.out.println(outputStream.size());

        // then
        assertThat(outputStream.toString("UTF-8"), containsString(expectedContent));

    }

    private static class TestJacksonModule extends SimpleModule implements JacksonModule {

        @Override
        public void applyTo(ObjectMapper objectMapper) {
            objectMapper.registerModule(this);
        }

    }

    public static class Dummy {

        private String expectedContent;

        public Dummy() {

        }

        public Dummy(final String expectedContent) {
            this.expectedContent = expectedContent;
        }

        @SuppressWarnings("unused")
        public String getExpectedContent() {
            return expectedContent;
        }

        public void setExpectedContent(String expectedContent) {
            this.expectedContent = expectedContent;
        }

    }

}
