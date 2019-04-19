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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.message.Message;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItemAppenderFactoryTest {

    @Captor
    private ArgumentCaptor<String> indexNameCaptor;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Captor
    private ArgumentCaptor<String> stringLogCaptor;

    @Captor
    private ArgumentCaptor<ItemSource> itemSourceCaptor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void messageOnlyAbstractStringLayout() {

        // given
        ItemAppenderFactory factory = new ItemAppenderFactory();
        String formattedIndexName = UUID.randomUUID().toString();
        BatchDelivery batchDelivery = mock(BatchDelivery.class);

        AbstractStringLayout abstractStringLayout = mock(AbstractStringLayout.class);
        ItemAppender itemAppender = factory.createInstance(true, abstractStringLayout, batchDelivery);

        String expectedMessage = UUID.randomUUID().toString();
        LogEvent logEvent = mock(LogEvent.class);
        Message message = mock(Message.class);
        when(message.getFormattedMessage()).thenReturn(expectedMessage);
        when(logEvent.getMessage()).thenReturn(message);

        // when
        itemAppender.append(formattedIndexName, logEvent);

        // then
        assertEquals(StringAppender.class, itemAppender.getClass());

        verify(abstractStringLayout, never()).toSerializable(any());

        verify(batchDelivery).add(indexNameCaptor.capture(), stringLogCaptor.capture());
        assertEquals(formattedIndexName, indexNameCaptor.getValue());
        assertEquals(expectedMessage, stringLogCaptor.getValue());

    }

    @Test
    public void nonMessageOnlyAbstractStringLayout() {

        // given
        ItemAppenderFactory factory = new ItemAppenderFactory();
        String formattedIndexName = UUID.randomUUID().toString();
        BatchDelivery batchDelivery = mock(BatchDelivery.class);

        AbstractStringLayout stringBasedLayout = mock(AbstractStringLayout.class);
        ItemAppender itemAppender = factory.createInstance(false, stringBasedLayout, batchDelivery);

        String expectedMessage = UUID.randomUUID().toString();
        when(stringBasedLayout.toSerializable(any(LogEvent.class))).thenReturn(expectedMessage);

        LogEvent logEvent = createDefaultTestLogEvent();

        // when
        itemAppender.append(formattedIndexName, logEvent);

        // then
        assertEquals(StringAppender.class, itemAppender.getClass());

        verify(stringBasedLayout).toSerializable(logEventCaptor.capture());
        assertEquals(logEvent, logEventCaptor.getValue());

        verify(batchDelivery).add(indexNameCaptor.capture(), stringLogCaptor.capture());
        assertEquals(formattedIndexName, indexNameCaptor.getValue());
        assertEquals(expectedMessage, stringLogCaptor.getValue());

    }

    @Test
    public void givenAbstractLayoutImplementingItemSourceLayoutDelegatesToItemSourceLayoutFactoryMethod() {

        // given
        ItemAppenderFactory factory = spy(new ItemAppenderFactory());

        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        ItemSourceLayout itemSourceLayout = spy(new TestItemSourceaLayout(mock(Configuration.class)));

        // when
        factory.createInstance(false, (AbstractLayout)itemSourceLayout, batchDelivery);

        // then
        verify(factory).createInstance(eq(false), eq(itemSourceLayout), eq(batchDelivery));

    }

    @Test
    public void nonMessageOnlyItemSourceLayout() {

        // given
        ItemAppenderFactory factory = new ItemAppenderFactory();

        String formattedIndexName = UUID.randomUUID().toString();

        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        ItemSourceLayout itemSourceLayout = spy(new TestItemSourceaLayout(mock(Configuration.class)));
        ItemSourceAppender itemAppender = (ItemSourceAppender) factory.createInstance(false, (AbstractLayout)itemSourceLayout, batchDelivery);

        String expectedMessage = UUID.randomUUID().toString();
        ItemSource itemSource = new StringItemSource(expectedMessage);
        when(itemSourceLayout.serialize(any(LogEvent.class))).thenReturn(itemSource);

        LogEvent logEvent = createDefaultTestLogEvent();

        // when
        itemAppender.append(formattedIndexName, logEvent);

        // then
        assertEquals(ItemSourceAppender.class, itemAppender.getClass());
        verify(itemSourceLayout).serialize(logEventCaptor.capture());
        assertEquals(logEvent, logEventCaptor.getValue());

        verify(batchDelivery).add(indexNameCaptor.capture(), itemSourceCaptor.capture());
        assertEquals(formattedIndexName, indexNameCaptor.getValue());
        assertEquals(expectedMessage, itemSourceCaptor.getValue().getSource());

    }

    @Test
    public void messageOnlyItemSourceLayout() {

        // given
        ItemAppenderFactory factory = new ItemAppenderFactory();
        String formattedIndexName = UUID.randomUUID().toString();
        BatchDelivery batchDelivery = mock(BatchDelivery.class);

        ItemSourceLayout itemSourceLayout = spy(new TestItemSourceaLayout(mock(Configuration.class)));
        ItemSourceAppender itemAppender = (ItemSourceAppender) factory.createInstance(true, (AbstractLayout)itemSourceLayout, batchDelivery);

        String expectedMessage = UUID.randomUUID().toString();
        ItemSource itemSource = new StringItemSource(expectedMessage);
        when(itemSourceLayout.serialize(any(Message.class))).thenReturn(itemSource);

        LogEvent logEvent = mock(LogEvent.class);
        Message message = mock(Message.class);

        when(logEvent.getMessage()).thenReturn(message);

        // when
        itemAppender.append(formattedIndexName, logEvent);

        // then
        assertEquals(ItemSourceAppender.class, itemAppender.getClass());

        verify(itemSourceLayout).serialize(messageCaptor.capture());
        assertEquals(message, messageCaptor.getValue());

        verify(batchDelivery).add(indexNameCaptor.capture(), itemSourceCaptor.capture());
        assertEquals(formattedIndexName, indexNameCaptor.getValue());
        assertEquals(expectedMessage, itemSourceCaptor.getValue().getSource());

    }

    @Test
    public void unsupportedLayout() {

        // given
        ItemAppenderFactory factory = new ItemAppenderFactory();

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("Unsupported layout");

        // when
        factory.createInstance(false, mock(AbstractLayout.class), mock(BatchDelivery.class));

    }

    private LogEvent createDefaultTestLogEvent() {
        LogEvent logEvent = mock(LogEvent.class);
        return logEvent;
    }

    private class TestItemSourceaLayout extends AbstractLayout implements ItemSourceLayout {

        public TestItemSourceaLayout(Configuration configuration) {
            super(configuration, null, null);
        }

        @Override
        public byte[] toByteArray(LogEvent event) {
            return null;
        }

        @Override
        public Serializable toSerializable(LogEvent event) {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public void encode(Object source, ByteBufferDestination destination) {

        }

        @Override
        public ItemSource serialize(LogEvent event) {
            return null;
        }

        @Override
        public ItemSource serialize(Message message) {
            return null;
        }
    }
}
