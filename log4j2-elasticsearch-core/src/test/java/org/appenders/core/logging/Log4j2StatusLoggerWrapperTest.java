package org.appenders.core.logging;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
public class Log4j2StatusLoggerWrapperTest {

    @Test
    public void errorDelegatesProperly() {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        AbstractLogger logger = mock(AbstractLogger.class);
        Logger wrapper = new Log4j2StatusLoggerWrapper(logger);
        
        String arg1 = UUID.randomUUID().toString();
        String arg2 = UUID.randomUUID().toString();

        // when
        wrapper.error(expectedMessage, arg1, arg2);

        // then
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).error(eq(expectedMessage), captor.capture());

        assertEquals(arg1, captor.getAllValues().get(0));
        assertEquals(arg2, captor.getAllValues().get(1));

    }

    @Test
    public void warnDelegatesProperly() {
        
        // given
        String expectedMessage = UUID.randomUUID().toString();
        AbstractLogger logger = mock(AbstractLogger.class);
        Logger wrapper = new Log4j2StatusLoggerWrapper(logger);

        String arg1 = UUID.randomUUID().toString();
        String arg2 = UUID.randomUUID().toString();

        // when
        wrapper.error(expectedMessage, arg1, arg2);

        // then
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).error(eq(expectedMessage), captor.capture());

        assertEquals(arg1, captor.getAllValues().get(0));
        assertEquals(arg2, captor.getAllValues().get(1));

    }

    @Test
    public void infoDelegatesProperly() {
        // given
        String expectedMessage = UUID.randomUUID().toString();
        AbstractLogger logger = mock(AbstractLogger.class);
        Logger wrapper = new Log4j2StatusLoggerWrapper(logger);

        String arg1 = UUID.randomUUID().toString();
        String arg2 = UUID.randomUUID().toString();

        // when
        wrapper.info(expectedMessage, arg1, arg2);

        // then
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).info(eq(expectedMessage), captor.capture());

        assertEquals(arg1, captor.getAllValues().get(0));
        assertEquals(arg2, captor.getAllValues().get(1));

    }

    @Test
    public void debugDelegatesProperly() {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        AbstractLogger logger = mock(AbstractLogger.class);
        Logger wrapper = new Log4j2StatusLoggerWrapper(logger);

        String arg1 = UUID.randomUUID().toString();
        String arg2 = UUID.randomUUID().toString();

        // when
        wrapper.debug(expectedMessage, arg1, arg2);

        // then
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).debug(eq(expectedMessage), captor.capture());

        assertEquals(arg1, captor.getAllValues().get(0));
        assertEquals(arg2, captor.getAllValues().get(1));

    }

    @Test
    public void traceDelegatesProperly() {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        AbstractLogger logger = mock(AbstractLogger.class);
        Logger wrapper = new Log4j2StatusLoggerWrapper(logger);

        String arg1 = UUID.randomUUID().toString();
        String arg2 = UUID.randomUUID().toString();

        // when
        wrapper.trace(expectedMessage, arg1, arg2);

        // then
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(logger).trace(eq(expectedMessage), captor.capture());

        assertEquals(arg1, captor.getAllValues().get(0));
        assertEquals(arg2, captor.getAllValues().get(1));

    }

}
