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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class StringItemSourceFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        StringItemSourceFactory.Builder builder = StringItemSourceFactory.newBuilder();

        // when
        StringItemSourceFactory factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void createEmptySourceThrows() {

        // given
        StringItemSourceFactory factory = createDefaultTestStringItemSourceFactory();

        expectedException.expect(UnsupportedOperationException.class);

        // when
        factory.createEmptySource();

    }

    @Test
    public void createWritesItemSource() throws IOException {

        // given
        StringItemSourceFactory factory = createDefaultTestStringItemSourceFactory();

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        // when
        ItemSource itemSource = factory.create(logEvent, objectWriter);

        // then
        assertNotNull(itemSource);
        verify(objectWriter).writeValueAsString(eq(logEvent));

    }

    @Test
    public void createFailureIsHandled() throws IOException {

        // given
        StringItemSourceFactory factory = createDefaultTestStringItemSourceFactory();

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        doThrow(JsonMappingException.fromUnexpectedIOE(new IOException("test exception"))).when(objectWriter).writeValueAsString(eq(logEvent));

        // when
        ItemSource itemSource = factory.create(logEvent, objectWriter);

        // then
        assertNull(itemSource);

    }

    @Test
    public void isBufferedReturnsFalse() {

        // given
        StringItemSourceFactory.Builder builder = StringItemSourceFactory.newBuilder();

        // when
        StringItemSourceFactory factory = builder.build();

        // then
        assertEquals(false, factory.isBuffered());

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
        return createDefaultTestStringItemSourceFactory();
    }

    private StringItemSourceFactory createDefaultTestStringItemSourceFactory() {
        return StringItemSourceFactory.newBuilder().build();
    }

}
