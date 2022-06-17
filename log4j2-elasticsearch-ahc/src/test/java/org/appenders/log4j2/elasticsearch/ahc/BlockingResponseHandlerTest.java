package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlockingResponseHandlerTest {
    static BlockingResponseHandler<Response> createDefaultTestBlockingResponseHandler() {
        //noinspection unchecked
        return new BlockingResponseHandler<Response>(
                mock(Deserializer.class),
                ex -> null);
    }

    @Test
    public void canBeInterrupted() throws InterruptedException {

        // given
        final BlockingResponseHandler<Response> handler = createDefaultTestBlockingResponseHandler();

        final Thread thread = new Thread(handler::getResult);

        // when
        thread.start();
        Thread.sleep(500);
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
        final Response expectedResult = mock(Response.class);

        // when
        handler.completed(expectedResult);

        // then
        assertEquals(expectedResult, handler.getResult());

    }

    @Test
    public void setsTemplateErrorResultOnFailure() {

        // given
        final Response expectedResult = mock(Response.class);

        final String expectedMessage = UUID.randomUUID().toString();
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
    public void deserializesResponseUsingGivenDeserializer() throws IOException {

        // given
        final Deserializer deserializer = mock(Deserializer.class);
        final BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                deserializer, ex -> null);

        final InputStream inputStream = mock(InputStream.class);

        // when
        handler.deserializeResponse(inputStream);

        // then
        verify(deserializer).read(any(InputStream.class));

    }

    @Test
    public void deserializeResponseFallsBackOnNoInputStream() throws IOException {

        // given
        final ObjectReader mockedObjectReader = mock(ObjectReader.class);
        final Response expectedResponse = mock(Response.class);
        final BlockingResponseHandler<Response> handler = new BlockingResponseHandler<>(
                mockedObjectReader, ex -> expectedResponse);

        // when
        final Response response = handler.deserializeResponse(null);

        // then
        assertEquals(expectedResponse, response);

    }

}
