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
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.ExtendedLog4j2JsonModule;
import org.apache.logging.log4j.core.jackson.LogEventJacksonJsonMixIn;
import org.apache.logging.log4j.message.Message;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacksonJsonLayoutTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        JacksonJsonLayout.Builder builder = createDefaultTestBuilder();

        // when
        JacksonJsonLayout layout = builder.build();

        // then
        Assert.assertNotNull(layout);

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

        expectedException.expect(UnsupportedOperationException.class);

        // when
        layout.toByteArray(new Log4jLogEvent());

    }

    @Test
    public void throwsOnNullConfiguration() {

        // given
        JacksonJsonLayout.Builder builder = createDefaultTestBuilder()
                .setConfiguration(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No Configuration instance provided for " + JacksonJsonLayout.PLUGIN_NAME);

        // when
        builder.build();

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
        verify(objectMapper).registerModule(captor.capture());

        Module.SetupContext setupContext = mock(Module.SetupContext.class);
        captor.getValue().setupModule(setupContext);
        verify(setupContext).setMixInAnnotations(eq(LogEvent.class), eq(LogEventJacksonJsonMixIn.class));

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
        verify(builder).createConfiguredWriter(any());

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
}
