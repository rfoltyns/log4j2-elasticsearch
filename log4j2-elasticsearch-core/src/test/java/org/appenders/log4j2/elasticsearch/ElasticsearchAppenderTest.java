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


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender.Builder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;


public class ElasticsearchAppenderTest {

    private static final String TEST_APPENDER_NAME = "testAppender";

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

        ElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory);

        LogEvent logEvent = mock(LogEvent.class);

        // when
        appender.append(logEvent);

        // then
        verify(itemAppender, times(1)).append(anyString(), eq(logEvent));

    }

    private TestElasticsearchAppender createTestElasticsearchAppender() {
        return createTestElasticsearchAppender(mockedItemAppenderFactory());
    }

    private TestElasticsearchAppender createTestElasticsearchAppender(ItemAppenderFactory mockItemAppenderFactory) {
        return new TestElasticsearchAppender(
                    "testAppender",
                    null,
                    JsonLayout.newBuilder().build(),
                    false,
                    mock(BatchDelivery.class),
                    false,
                    mock(IndexNameFormatter.class)
            ) {
            @Override
            protected ItemAppenderFactory createItemAppenderFactory() {
                return mockItemAppenderFactory;
            }
        };
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
        verify(batchDelivery, times(1)).add(eq("formattedIndexName"), any(ItemSource.class));
    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

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

        assertFalse(lifeCycle.isStarted());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    @Test
    public void lifecycleStartStartsItemAppender() {

        // given
        ItemSourceAppender mockItemAppender = mock(ItemSourceAppender.class);

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemSourceAppender createInstance(boolean messageOnly, AbstractLayout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory);

        // when
        appender.start();

        // then
        verify(mockItemAppender).start();


    }

    @Test
    public void lifecycleStopStopsItemAppender() {

        // given
        ItemSourceAppender mockItemAppender = mock(ItemSourceAppender.class);

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemSourceAppender createInstance(boolean messageOnly, AbstractLayout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory);

        // when
        appender.stop();

        // then
        verify(mockItemAppender).stop();

    }

    private LifeCycle createLifeCycleTestObject() {
        return createTestElasticsearchAppenderBuilder().build();
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
        return mock(ItemAppenderFactory.class);
    }

}
