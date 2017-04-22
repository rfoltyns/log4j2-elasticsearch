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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareOnlyThisForTest(JsonLayout.class)
@RunWith(PowerMockRunner.class)
public class ElasticsearchAppenderJsonLayoutTest {

    @Test
    public void appenderUsesProvidedLayoutWhenMessageOnlyIsSetToFalse() throws IOException, NoSuchMethodException, SecurityException {

        // given
        JsonLayout layout = PowerMockito.mock(JsonLayout.class);
        PowerMockito.when(layout.toSerializable(Mockito.any(LogEvent.class))).thenReturn("test message");

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

}
