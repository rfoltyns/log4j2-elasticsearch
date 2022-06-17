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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncStepProcessorTest {

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithExceptionMessage() {

        //given
        final SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        final Function<Exception, BatchResult> blockingResponseExceptionHandler = stepProcessor.createBlockingResponseFallbackHandler();

        final String expectedMessage = UUID.randomUUID().toString();

        // when
        final BatchResult basicResponse = blockingResponseExceptionHandler.apply(new Exception(expectedMessage));

        // then
        assertEquals(expectedMessage, basicResponse.getErrorMessage());

    }

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithNoExceptionMessage() {

        //given
        final SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        final Function<Exception, BatchResult> blockingResponseExceptionHandler = stepProcessor.createBlockingResponseFallbackHandler();

        // when
        final BatchResult basicResponse = blockingResponseExceptionHandler.apply(null);

        // then
        assertNull(basicResponse.getErrorMessage());

    }

    @Test
    public void createsBlockingResponseHandler() {

        // given
        final SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        // when
        final BlockingResponseHandler<BatchResult> responseHandler = stepProcessor.createBlockingResponseHandler();

        // then
        assertNotNull(responseHandler);

    }

    @Test
    public void processExecutesGivenStep() {

        // given
        final SetupStep<Request, Response> setupStep = mock(SetupStep.class);
        final GenericRequest request = mock(GenericRequest.class);
        final BasicResponse response = new BasicResponse();

        when(setupStep.createRequest()).thenReturn(request);

        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(eq(request), any())).thenReturn(response);

        final SyncStepProcessor stepProcessor = createTestStepProcessor(httpClient);

        // when
        stepProcessor.process(setupStep);

        // then
        verify(setupStep).createRequest();
        verify(setupStep).onResponse(response);

    }

    private SyncStepProcessor createTestStepProcessor(final HttpClient mock) {
        return new SyncStepProcessor(new HttpClientProvider(null) {
            @Override
            public HttpClient createClient() {
                return mock;
            }
        }, new JacksonDeserializer<>(new ObjectMapper().readerFor(BatchResult.class)));
    }

}
