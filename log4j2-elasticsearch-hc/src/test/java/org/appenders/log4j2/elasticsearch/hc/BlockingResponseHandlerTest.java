package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlockingResponseHandlerTest {

    @Test
    public void canBeInterrupted() {

        // given
        final BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                mock(ObjectReader.class),
                ex -> null);

        Thread thread = new Thread(() -> handler.getResult());

        // when
        thread.start();
        thread.interrupt();

        // then
        assertTrue(thread.isInterrupted());

    }

    @Test
    public void setsResultOnCompletion() {

        // given
        final BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                mock(ObjectReader.class),
                ex -> null);
        Response expectedResult = mock(Response.class);

        // when
        handler.completed(expectedResult);

        // then
        assertEquals(expectedResult, handler.getResult());

    }

    @Test
    public void setsTemplateErrorResultOnFailure() {

        // given
        Response expectedResult = mock(Response.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(expectedResult.getErrorMessage()).thenReturn(expectedMessage);

        final BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                mock(ObjectReader.class),
                ex -> expectedResult);

        // when
        handler.failed(new IOException(expectedMessage));

        // then
        assertEquals(expectedResult.getErrorMessage(), handler.getResult().getErrorMessage());

    }

    @Test
    public void deserializesResponseUsinGivenReader() throws IOException {

        // given
        ObjectReader mockedObjectReader = mock(ObjectReader.class);
        BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                mockedObjectReader, ex -> null);

        InputStream inputStream = mock(InputStream.class);

        // when
        handler.deserializeResponse(inputStream);

        // then
        verify(mockedObjectReader).readValue(any(InputStream.class));

    }

}
