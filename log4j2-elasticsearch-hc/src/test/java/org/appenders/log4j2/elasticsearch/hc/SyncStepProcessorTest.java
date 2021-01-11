package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncStepProcessorTest {

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithExceptionMessage() {

        //given
        SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        Function<Exception, BasicResponse> blockingResponseExceptionHandler = stepProcessor.createBlockingResponseFallbackHandler();

        String expectedMessage = UUID.randomUUID().toString();

        // when
        BasicResponse basicResponse = blockingResponseExceptionHandler.apply(new Exception(expectedMessage));

        // then
        assertEquals(expectedMessage, basicResponse.getErrorMessage());

    }

    @Test
    public void defaultBlockingResponseFallbackHandlerCreatesBasicResponseWithNoExceptionMessage() {

        //given
        SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        Function<Exception, BasicResponse> blockingResponseExceptionHandler = stepProcessor.createBlockingResponseFallbackHandler();

        // when
        BasicResponse basicResponse = blockingResponseExceptionHandler.apply(null);

        // then
        assertNull(basicResponse.getErrorMessage());

    }

    @Test
    public void createsBlockingResponseHandler() {

        // given
        SyncStepProcessor stepProcessor = createTestStepProcessor(mock(HttpClient.class));

        // when
        BlockingResponseHandler<BasicResponse> responseHandler = stepProcessor.createBlockingResponseHandler();

        // then
        assertNotNull(responseHandler);

    }

    @Test
    public void processExecutesGivenStep() {

        // given
        SetupStep<Request, Response> setupStep = mock(SetupStep.class);
        GenericRequest request = mock(GenericRequest.class);
        BasicResponse response = new BasicResponse();

        when(setupStep.createRequest()).thenReturn(request);

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(eq(request), any())).thenReturn(response);

        SyncStepProcessor stepProcessor = createTestStepProcessor(httpClient);

        // when
        stepProcessor.process(setupStep);

        // then
        verify(setupStep).createRequest();
        verify(setupStep).onResponse(response);

    }

    private SyncStepProcessor createTestStepProcessor(HttpClient mock) {
        return new SyncStepProcessor(new HttpClientProvider(null) {
            @Override
            public HttpClient createClient() {
                return mock;
            }
        }, new ObjectMapper().readerFor(BatchResult.class));
    }

}
