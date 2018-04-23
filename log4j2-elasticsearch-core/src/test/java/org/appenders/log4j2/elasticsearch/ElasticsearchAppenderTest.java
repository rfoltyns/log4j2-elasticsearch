package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
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
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender.Builder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;

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
    public void appenderUsedFormattedMessageDirectlyWhenMessageOnlyIsSetToTrue() {

        // given
        BatchDelivery<String> batchDelivery = mock(BatchDelivery.class);

        ElasticsearchAppender.Builder builder = ElasticsearchAppenderTest.createTestElasticsearchAppenderBuilder();
        builder.withMessageOnly(true);
        builder.withBatchDelivery(batchDelivery );

        String testMessageString = "formattedTestMessage";

        Message message = mock(Message.class);
        when(message.getFormattedMessage()).thenReturn(testMessageString);

        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getMessage()).thenReturn(message);

        ElasticsearchAppender appender = builder.build();

        // when
        appender.append(logEvent);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(batchDelivery, times(1)).add(anyString(), captor.capture());
        assertEquals(testMessageString, captor.getValue());
    }

    @Test
    public void appenderUsesProvidedLayoutWhenMessageOnlyIsSetToFalse() throws IOException, NoSuchMethodException, SecurityException {

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
    public void appenderUsesProvidedIndexNameFormatter() throws IOException, NoSuchMethodException, SecurityException {

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
}
