package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
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
import static org.mockito.Mockito.times;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.Message;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.JestBatchDelivery;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender.Builder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

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

    @Test(expected = ConfigurationException.class)
    public void builderFailsWhenLayoutIsNull() {

        // given
        ElasticsearchAppender.Builder builder = createTestElasticsearchAppenderBuilder();
        builder.withLayout(null);

        // when
        builder.build();
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
        BatchDelivery<String> batchDelivery = Mockito.mock(BatchDelivery.class);

        ElasticsearchAppender.Builder builder = ElasticsearchAppenderTest.createTestElasticsearchAppenderBuilder();
        builder.withMessageOnly(true);
        builder.withBatchDelivery(batchDelivery );

        String testMessageString = "formattedTestMessage";

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getFormattedMessage()).thenReturn(testMessageString);

        LogEvent logEvent = Mockito.mock(LogEvent.class);
        Mockito.when(logEvent.getMessage()).thenReturn(message);

        ElasticsearchAppender appender = builder.build();

        // when
        appender.append(logEvent);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(batchDelivery, times(1)).add(captor.capture());
        assertEquals(testMessageString, captor.getValue());
    }

    public static Builder createTestElasticsearchAppenderBuilder() {
        Builder builder = ElasticsearchAppender.newBuilder();

        builder.withName(TEST_APPENDER_NAME);
        builder.withLayout(JsonLayout.createDefaultLayout());
        builder.withIgnoreExceptions(false);
        builder.withBatchDelivery(Mockito.mock(JestBatchDelivery.class));
        builder.withMessageOnly(false);

        return builder;
    }
}
