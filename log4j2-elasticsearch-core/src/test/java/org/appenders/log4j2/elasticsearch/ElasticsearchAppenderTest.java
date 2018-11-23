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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender.Builder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;


public class ElasticsearchAppenderTest {

    private static final String TEST_APPENDER_NAME = "testAppender";

    private static ItemAppenderFactory mockItemAppenderFactory;

    @Test
    public void builderReturnsNonNullObject() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();

        // when
        ElasticsearchAppender appender = builder.build();

        // then
        assertNotNull(appender);
    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsWhenAppenderNameIsNull() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withName(null);

        // when
        builder.build();
    }

    @Test
    public void builderInitializesDefaultLayoutWhenLayoutIsNotProvided() throws IllegalAccessException {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withLayout(null);

        // when
        ElasticsearchAppender appender = builder.build();

        // then
        assertNotNull(PowerMockito.field(ElasticsearchAppender.class, "layout").get(appender));

    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsWhenBatchDeliveryIsNull() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withBatchDelivery(null);

        // when
        builder.build();
    }

    @Test
    public void appenderDelegatesToItemAppender() {

        // given
        ItemAppender itemAppender = mock(ItemAppender.class);

        ItemAppenderFactory itemAppenderFactory = mockedItemAppenderFactory();
        when(itemAppenderFactory.createInstance(anyBoolean(), any(AbstractLayout.class), any(BatchDelivery.class)))
                .thenReturn(itemAppender);

        ElasticsearchAppender appender = new TestElasticsearchAppender(
                "testAppender",
                null,
                JsonLayout.newBuilder().build(),
                false,
                mock(BatchDelivery.class),
                false,
                mock(IndexNameFormatter.class)
        );

        LogEvent logEvent = mock(LogEvent.class);

        // when
        appender.append(logEvent);

        // then
        verify(itemAppender, times(1)).append(anyString(), eq(logEvent));

    }

    @Test
    public void appenderUsesProvidedLayoutWhenMessageOnlyIsSetToFalse() {

        // given
        AbstractStringLayout layout = PowerMockito.mock(AbstractStringLayout.class);

        ElasticsearchAppender.Builder builder = ElasticsearchAppenderTest.createTestElasticsearchAppenderBuilder();
        builder.withMessageOnly(false);
        builder.withLayout(layout);

        LogEvent logEvent = mock(LogEvent.class);

        ElasticsearchAppender appender = builder.build();

        // when
        appender.append(logEvent);

        // then
        ArgumentCaptor<LogEvent> captor = ArgumentCaptor.forClass(LogEvent.class);
        verify(layout, times(1)).toSerializable(captor.capture());
        assertEquals(logEvent, captor.getValue());
    }

    @Test
    public void appenderUsesProvidedIndexNameFormatter() {

        // given
        BatchDelivery<String> batchDelivery = mock(BatchDelivery.class);
        IndexNameFormatter indexNameFormatter = mock(IndexNameFormatter.class);
        when(indexNameFormatter.format(any(LogEvent.class))).thenReturn("formattedIndexName");

        ElasticsearchAppender.Builder builder = ElasticsearchAppenderTest.createTestElasticsearchAppenderBuilder();
        builder.withBatchDelivery(batchDelivery);
        builder.withMessageOnly(false);
        builder.withIndexNameFormatter(indexNameFormatter);

        LogEvent logEvent = createTestLogEvent();

        ElasticsearchAppender appender = builder.build();

        // when
        appender.append(logEvent);

        // then
        verify(batchDelivery, times(1)).add(eq("formattedIndexName"), anyString());
    }

    private LogEvent createTestLogEvent() {
        return DefaultLogEventFactory.getInstance().createEvent("testLogger",
                null,
                getClass().getName(),
                Level.INFO,
                new SimpleMessage("testMessage"),
                null,
                null);

    }

    public static Builder createTestElasticsearchAppenderBuilder() {
        Builder builder = ElasticsearchAppender.newBuilder()
                .withName(TEST_APPENDER_NAME)
                .withFilter(ThresholdFilter.createFilter(Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY))
                .withIgnoreExceptions(false)
                .withBatchDelivery(mock(AsyncBatchDelivery.class))
                .withMessageOnly(false);

        IndexNameFormatter indexNameFormatter = mock(IndexNameFormatter.class);
        when(indexNameFormatter.format(any(LogEvent.class))).thenReturn("testIndexName");
        builder.withIndexNameFormatter(indexNameFormatter);
        return builder;
    }

    static class TestElasticsearchAppender extends ElasticsearchAppender {

        protected TestElasticsearchAppender(String name, Filter filter, AbstractLayout layout, boolean ignoreExceptions, BatchDelivery batchDelivery, boolean messageOnly, IndexNameFormatter indexNameFormatter) {
            super(name, filter, layout, ignoreExceptions, batchDelivery, messageOnly, indexNameFormatter);
        }

        @Override
        protected ItemAppenderFactory createItemAppenderFactory() {
            return mockedItemAppenderFactory();
        }

    }

    private static ItemAppenderFactory mockedItemAppenderFactory() {
        if (mockItemAppenderFactory == null) {
            mockItemAppenderFactory = mock(ItemAppenderFactory.class);
        }
        return mockItemAppenderFactory;
    }

}
