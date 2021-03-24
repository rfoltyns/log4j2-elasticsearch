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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLog4j2JsonModule;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLogEventJacksonJsonMixIn;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.appenders.st.jackson.SingleThreadJsonFactory;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

public class JacksonJsonLayoutTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        JacksonJsonLayout.Builder builder = createDefaultTestBuilder();

        // when
        JacksonJsonLayout layout = builder.build();

        // then
        assertNotNull(layout);

    }

    @Test
    public void contentTypeIsNotNull() {

        // given
        JacksonJsonLayout layout = createDefaultTestBuilder().build();

        // when
        String contentType = layout.getContentType();

        // then
        assertNotNull(contentType);
    }

    @Test
    public void throwsOnByteArrayCreationAttempt() {

        // given
        JacksonJsonLayout layout = createDefaultTestBuilder().build();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> layout.toByteArray(new Log4jLogEvent()));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Cannot return unwrapped byte array. Use ItemSource based API"));

    }

    @Test
    public void throwsOnNullConfiguration() {

        // given
        JacksonJsonLayout.Builder builder = createDefaultTestBuilder()
                .setConfiguration(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("No Configuration instance provided for " + JacksonJsonLayout.PLUGIN_NAME));

    }

    @Test
    public void builderBuildsLayoutWithDefaultItemSourceFactoryIfNotConfigured() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        JacksonJsonLayout layout = builder.build();

        LogEvent logEvent = new Log4jLogEvent();

        // when
        ItemSource itemSource = layout.toSerializable(logEvent);

        // then
        assertEquals(StringItemSource.class, itemSource.getClass());

    }

    @Test
    public void builderBuildsLayoutWithProvidedItemSourceFactoryIfConfigured() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        builder.withItemSourceFactory(new LayoutTestItemSourceFactory());

        JacksonJsonLayout layout = builder.build();

        LogEvent logEvent = new Log4jLogEvent();

        // when
        ItemSource itemSource = layout.toSerializable(logEvent);

        // then
        assertEquals(LayoutTestItemSource.class, itemSource.getClass());

    }

    @Test
    public void builderBuildsMapperWithAfterburnerIfConfigured() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        builder.withAfterburner(true);

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
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        builder.withMixins(JacksonMixInTest.createDefaultTestBuilder().build());

        ObjectMapper objectMapper = spy(ObjectMapper.class);

        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        // when
        builder.build();

        // then
        ArgumentCaptor<ExtendedLog4j2JsonModule> captor = ArgumentCaptor.forClass(ExtendedLog4j2JsonModule.class);
        verify(objectMapper, times(1)).registerModule(captor.capture());

        Module.SetupContext setupContext = mock(Module.SetupContext.class);
        captor.getValue().setupModule(setupContext);
        verify(setupContext).setMixInAnnotations(eq(LogEvent.class), eq(ExtendedLogEventJacksonJsonMixIn.class));

    }

    @Test
    public void builderBuildsMapperWithCustomHandlerInstantiator() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

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
    public void builderResolvesNonDynamicVirtualProperties() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

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

        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder()
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
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

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
    public void builderConfiguresDefaultJacksonModules() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

        ObjectMapper objectMapper = spy(new ObjectMapper());
        when(builder.createDefaultObjectMapper()).thenReturn(objectMapper);

        JacksonModule jacksonModule1 = new TestJacksonModule();
        builder.withJacksonModules(jacksonModule1);

        // when
        builder.build();

        // then
        verify(objectMapper, times(2)).registerModule(any());

    }

    @Test
    public void builderConfiguresAdditionalJacksonModulesIfConfigured() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

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
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());

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
        JacksonJsonLayout.Builder builder  = createDefaultTestBuilder();

        // when
        ObjectWriter writer = builder.createConfiguredWriter(new ArrayList<>());

        // then
        assertEquals(ExtendedObjectWriter.class, writer.getClass());
    }

    @Test
    public void builderConfiguresExtendedObjectWriter() {

        // given
        JacksonJsonLayout.Builder builder  = spy(createDefaultTestBuilder());

        // when
        builder.build();

        // then
        verify(builder).createConfiguredWriter(any(ObjectMapper.class));

    }

    @Test
    public void builderCreatesLog4j2ValueResolver() {

        // given
        JacksonJsonLayout.Builder builder  = spy(createDefaultTestBuilder());

        // when
        ValueResolver result = builder.createValueResolver();

        // then
        assertTrue(result instanceof Log4j2Lookup);

    }

    @Test
    public void createsSingleThreadJsonFactoryIfConfigured() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
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
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        builder.withSingleThread(false);

        // when
        ObjectMapper defaultObjectMapper = builder.createDefaultObjectMapper();

        // then
        verify(builder).createJsonFactory();
        assertEquals(defaultObjectMapper.getFactory().getClass(), JsonFactory.class);

    }

    private JacksonJsonLayout.Builder createDefaultTestBuilder() {
        return JacksonJsonLayout.newBuilder()
                .setConfiguration(LoggerContext.getContext(false).getConfiguration());
    }

    @Test
    public void messageSerializationDelegatesToItemSourceFactory() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        LayoutTestItemSourceFactory itemSourceFactory = spy(new LayoutTestItemSourceFactory());
        builder.withItemSourceFactory(itemSourceFactory);

        JacksonJsonLayout layout = builder.build();

        LogEvent logEvent = new Log4jLogEvent();
        Message message = logEvent.getMessage();

        // when
        layout.serialize(message);

        // then
        verify(itemSourceFactory).create(eq(message), any(ObjectWriter.class));

    }

    @Test
    public void logEventSerializationDelegatesToItemSourceFactory() {

        // given
        JacksonJsonLayout.Builder builder = spy(createDefaultTestBuilder());
        LayoutTestItemSourceFactory itemSourceFactory = spy(new LayoutTestItemSourceFactory());
        builder.withItemSourceFactory(itemSourceFactory);

        JacksonJsonLayout layout = builder.build();

        LogEvent logEvent = new Log4jLogEvent();

        // when
        layout.toSerializable(logEvent);

        // then
        verify(itemSourceFactory).create(eq(logEvent), any(ObjectWriter.class));

    }

    @Test
    public void lifecycleStopStopsItemSourceFactoryOnlyOnce() {

        // given
        ItemSourceFactory itemSourceFactory = mock(ItemSourceFactory.class);
        when(itemSourceFactory.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        JacksonJsonLayout layout = createDefaultTestBuilder()
                .withItemSourceFactory(itemSourceFactory)
                .build();

        // when
        layout.stop();
        layout.stop();

        // then
        verify(itemSourceFactory).stop();
    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestBuilder().build();
    }

    public static class LayoutTestItemSourceFactory extends StringItemSourceFactory {

        @Override
        public ItemSource create(Object event, ObjectWriter objectWriter) {
            try {
                return new LayoutTestItemSource(objectWriter.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class LayoutTestItemSource extends StringItemSource {

        public LayoutTestItemSource(String source) {
            super(source);
        }

    }

    private class TestJacksonModule extends SimpleModule implements JacksonModule {

        @Override
        public void applyTo(ObjectMapper objectMapper) {
            objectMapper.registerModule(this);
        }

    }
}
