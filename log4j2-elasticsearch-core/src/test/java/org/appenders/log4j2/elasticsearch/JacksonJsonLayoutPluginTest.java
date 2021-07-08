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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.appenders.core.logging.InternalLogging;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JacksonJsonLayoutPluginTest {

    @Test
    public void builderBuildsSuccessfullyWithMinimalParams() {

        // when
        final JacksonJsonLayoutPlugin<Object> layoutPlugin = JacksonJsonLayoutPlugin.createJacksonJsonLayout(
                LoggerContext.getContext(true).getConfiguration(),
                null,
                new JacksonMixIn[0],
                new JacksonModule[0],
                new VirtualProperty[0],
                new VirtualPropertyFilter[0],
                false,
                false);

        final LogEvent logEvent = mock(LogEvent.class);
        final ItemSource<Object> result = layoutPlugin.serialize(logEvent);

        // then
        assertEquals(StringItemSourceFactory.class, layoutPlugin.itemSourceFactory.getClass());
        assertEquals("{\"timeMillis\":0}", result.getSource());

    }

    @Test
    public void buildsWithConfiguredParams() {

        // given
        mockTestLogger();

        final String expectedValue = UUID.randomUUID().toString();

        final StrSubstitutor strSubstitutor = mock(StrSubstitutor.class);
        when(strSubstitutor.replace(any(String.class))).thenReturn(expectedValue);

        final Configuration configuration = mock(Configuration.class);
        when(configuration.getStrSubstitutor()).thenReturn(strSubstitutor);

        final ItemSourceFactory<Object, Object> itemSourceFactory = spy((ItemSourceFactory) new StringItemSourceFactory<>());
        final JacksonMixIn mixin = spy(JacksonMixInTest.createDefaultTestBuilder().build());
        final JacksonModule jacksonModule = spy(new TestJacksonModule());
        final String expectedFieldName = UUID.randomUUID().toString();
        final String expectedUnresolvedValue = UUID.randomUUID().toString();
        final VirtualProperty virtualProperty = spy(VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withName(expectedFieldName)
                .withValue(expectedUnresolvedValue)
                .build());
        final VirtualPropertyFilter virtualPropertyFilter = mock(VirtualPropertyFilter.class);
        when(virtualPropertyFilter.isIncluded(any(), any())).thenReturn(true);

        // when
        final JacksonJsonLayoutPlugin<Object> layoutPlugin = JacksonJsonLayoutPlugin.createJacksonJsonLayout(
                configuration,
                itemSourceFactory,
                new JacksonMixIn[] { mixin },
                new JacksonModule[] { jacksonModule },
                new VirtualProperty[] { virtualProperty },
                new VirtualPropertyFilter[] { virtualPropertyFilter },
                false,
                false);

        final LogEvent logEvent = mock(LogEvent.class);
        final ItemSource<Object> result = layoutPlugin.serialize(logEvent);

        // then
        verify(mixin).getMixInClass();
        verify(jacksonModule).applyTo(any());
        verify(virtualProperty, times(2)).isDynamic();
        verify(virtualPropertyFilter).isIncluded(eq(expectedFieldName), eq(expectedValue));
        verify(itemSourceFactory).create(eq(logEvent), any(Serializer.class));
        assertTrue(result.getSource().toString().contains(expectedValue));

        InternalLogging.setLogger(null);
    }

    @Test
    public void throwsOnGetHeaderAttempt() {

        // given
        final JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, layout::getHeader);

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Header not supported"));

    }

    @Test
    public void throwsOnGetContentFormatAttempt() {

        // given
        final JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, layout::getContentFormat);

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Content format not supported"));

    }

    @Test
    public void throwsOnEncodeAttempt() {

        // given
        final JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();
        final LogEvent mock = mock(LogEvent.class);

        final ByteBufferDestination byteBufferDestination = mock(ByteBufferDestination.class);
        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            layout.encode(mock, byteBufferDestination);
        });

        // then
        assertThat(exception.getMessage(), containsString(ByteBufferDestination.class.getSimpleName()));
        assertThat(exception.getMessage(), containsString("not supported"));

    }

    @Test
    public void toSerializableDelegates() {

        // given
        final ItemSourceFactory itemSourceFactory = mock(ItemSourceFactory.class);
        final StringItemSource itemSource = new StringItemSource(UUID.randomUUID().toString());
        when(itemSourceFactory.create(any(), (Serializer) any())).thenReturn(itemSource);

        final JacksonJsonLayoutPlugin<Object> layout = JacksonJsonLayoutPlugin.createJacksonJsonLayout(
                mock(Configuration.class),
                itemSourceFactory,
                new JacksonMixIn[0],
                new JacksonModule[0],
                new VirtualProperty[0],
                new VirtualPropertyFilter[0],
                false,
                false);

        final LogEvent logEvent = mock(LogEvent.class);

        // when
        final ItemSource<Object> result = layout.toSerializable(logEvent);

        // then
        assertEquals(layout.serialize(logEvent), result);

    }

    @Test
    public void throwsOnGetFooterAttempt() {

        // given
        JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, layout::getFooter);

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Footer not supported"));

    }

    @Test
    public void contentTypeIsNotNull() {

        // given
        JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();

        // when
        String contentType = layout.getContentType();

        // then
        assertNull(contentType);
    }

    @Test
    public void throwsOnByteArrayCreationAttempt() {

        // given
        JacksonJsonLayoutPlugin<Object> layout = createDefaultTestLayout();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> layout.toByteArray(new Log4jLogEvent()));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Cannot return unwrapped byte array. Use toSerializable(LogEvent) instead"));

    }

    private JacksonJsonLayoutPlugin<Object> createDefaultTestLayout() {
        return JacksonJsonLayoutPlugin.createJacksonJsonLayout(
                mock(Configuration.class),
                null,
                new JacksonMixIn[0],
                new JacksonModule[0],
                new VirtualProperty[0],
                new VirtualPropertyFilter[0],
                false,
                false);
    }

    private static class TestJacksonModule extends SimpleModule implements JacksonModule {

        @Override
        public void applyTo(ObjectMapper objectMapper) {
            objectMapper.registerModule(this);
        }

    }
}
