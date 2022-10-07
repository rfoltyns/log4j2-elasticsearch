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
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender.Builder;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
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

    @Test
    public void builderFailsWhenAppenderNameIsNull() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withName(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No name provided"));

    }

    @Test
    public void builderFailsLayoutIsNotProvided() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withLayout(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No layout provided"));

    }

    @Test
    public void builderFailsWhenBatchDeliveryIsNull() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withBatchDelivery(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No batchDelivery [AsyncBatchDelivery] provided"));

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
        when(logEvent.getTimeMillis()).thenReturn(System.currentTimeMillis());

        // when
        appender.append(logEvent);

        // then
        verify(itemAppender, times(1)).append(anyString(), eq(logEvent));

    }

    @Test
    public void appenderUsesProvidedLayoutWhenMessageOnlyIsSetToFalse() {

        // given
        Layout layout = mock(Layout.class);

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
            public ItemSourceAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
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
    public void lifecycleStopStopsItemAppenderOnlyOnce() {

        // given
        ItemSourceAppender mockItemAppender = mock(ItemSourceAppender.class);
        when(mockItemAppender.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemSourceAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory);

        // when
        appender.stop();
        appender.stop();

        // then
        verify(mockItemAppender).stop();

    }

    @Test
    public void lifecycleStopWithTimeoutStopsItemAppenderOnlyOnce() {

        // given
        ItemSourceAppender mockItemAppender = mock(ItemSourceAppender.class);
        when(mockItemAppender.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemSourceAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory);

        // when
        appender.stop(0, TimeUnit.MILLISECONDS);
        appender.stop(0, TimeUnit.MILLISECONDS);

        // then
        verify(mockItemAppender).stop();
        assertFalse(appender.isStarted());
        assertTrue(appender.isStopped());

    }

    @Test
    public void lifecycleStopStopsItemSourceLayout() {

        // given
        StringAppender mockItemAppender = mock(StringAppender.class);
        when(mockItemAppender.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        JacksonJsonLayout layout = mock(JacksonJsonLayout.class);
        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory, layout);

        // when
        appender.stop(0, TimeUnit.MILLISECONDS);

        // then
        assertFalse(appender.isStarted());
        assertTrue(appender.isStopped());

        verify(layout).stop();

    }

    @Test
    public void lifecycleStopDeregistersItemSourceLayout() {

        // given
        StringAppender mockItemAppender = mock(StringAppender.class);
        when(mockItemAppender.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        JacksonJsonLayoutPlugin layout = mock(JacksonJsonLayoutPlugin.class);
        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory, layout);

        // when
        appender.stop(0, TimeUnit.MILLISECONDS);

        // then
        assertFalse(appender.isStarted());
        assertTrue(appender.isStopped());

        verify(layout).deregister();

    }

    @Test
    public void lifecycleStopDoesntInteractWithAbstractLayout() {

        // given
        StringAppender mockItemAppender = mock(StringAppender.class);
        when(mockItemAppender.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        ItemAppenderFactory itemAppenderFactory = new ItemAppenderFactory() {
            @Override
            public ItemAppender createInstance(boolean messageOnly, Layout layout, BatchDelivery batchDelivery) {
                return mockItemAppender;
            }
        };

        AbstractLayout abstractLayout = mock(AbstractLayout.class);
        TestElasticsearchAppender appender = createTestElasticsearchAppender(itemAppenderFactory, abstractLayout);

        // when
        appender.stop(0, TimeUnit.MILLISECONDS);

        // then
        assertTrue(mockingDetails(abstractLayout).getInvocations().size() == 0);
        assertFalse(appender.isStarted());
        assertTrue(appender.isStopped());

    }

    @Test
    public void lifecycleStopResetsInternalLogging() {

        // given
        Logger initialLogger = getLogger();
        assertNotNull(initialLogger);

        LifeCycle lifeCycle = createLifeCycleTestObject();

        lifeCycle.start();

        assertSame(initialLogger, getLogger());

        // when
        lifeCycle.stop();

        // then
        assertNotSame(initialLogger, getLogger());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createTestElasticsearchAppenderBuilder(JacksonJsonLayout.newBuilder()
                .setConfiguration(LoggerContext.getContext(false).getConfiguration())
                .build()).build();
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

    private TestElasticsearchAppender createTestElasticsearchAppender(ItemAppenderFactory mockItemAppenderFactory) {
        return createTestElasticsearchAppender(mockItemAppenderFactory, JsonLayout.newBuilder().build());
    }

    private TestElasticsearchAppender createTestElasticsearchAppender(ItemAppenderFactory mockItemAppenderFactory, Layout layout) {
        IndexNameFormatter indexNameFormatter = mock(IndexNameFormatter.class);
        when(indexNameFormatter.format(any())).thenReturn(UUID.randomUUID().toString());

        return new TestElasticsearchAppender(
                "testAppender",
                null,
                layout,
                false,
                mock(BatchDelivery.class),
                false,
                indexNameFormatter
        ) {
            @Override
            protected ItemAppenderFactory createItemAppenderFactory() {
                return mockItemAppenderFactory;
            }
        };
    }

    public static Builder createTestElasticsearchAppenderBuilder() {
        return createTestElasticsearchAppenderBuilder(new JacksonJsonLayout.Builder()
                .setConfiguration(LoggerContext.getContext(false).getConfiguration())
                .build()
        );
    }

    public static Builder createTestElasticsearchAppenderBuilder(AbstractLayout layout) {
        Builder builder = ElasticsearchAppender.newBuilder()
                .withName(TEST_APPENDER_NAME)
                .withFilter(ThresholdFilter.createFilter(Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY))
                .withIgnoreExceptions(false)
                .withBatchDelivery(mock(AsyncBatchDelivery.class))
                .withMessageOnly(false)
                .withLayout(layout);

        IndexNameFormatter indexNameFormatter = mock(IndexNameFormatter.class);
        when(indexNameFormatter.format(any(LogEvent.class))).thenReturn("testIndexName");
        builder.withIndexNameFormatter(indexNameFormatter);
        return builder;
    }

    static class TestElasticsearchAppender extends ElasticsearchAppender {

        protected TestElasticsearchAppender(String name, Filter filter, Layout layout, boolean ignoreExceptions, BatchDelivery batchDelivery, boolean messageOnly, IndexNameFormatter indexNameFormatter) {
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
