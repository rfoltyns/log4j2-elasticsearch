package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.JestResult;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckDataStreamTest {

    public static final String TEST_NAME = "testRolloverAlias";

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);
        SetupContext setupContext = new SetupContext(Result.FAILURE);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);
        SetupContext setupContext = new SetupContext(Result.SUCCESS);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);
        SetupContext setupContext = new SetupContext(Result.SKIP);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);

        JestResult jestResult = mock(JestResult.class);
        when(jestResult.getResponseCode()).thenReturn(404);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(jestResult);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: Data stream {} does not exist",
                CheckDataStream.class.getSimpleName(),
                TEST_NAME);

    }

    @Test
    public void onResponseCode200LogsAndSkips() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);

        JestResult response = mock(JestResult.class);
        when(response.getResponseCode()).thenReturn(200);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.SKIP, result);
        verify(logger).info(
                "{}: Data stream {} already exists",
                CheckDataStream.class.getSimpleName(),
                TEST_NAME);

    }

    @Test
    public void onResponseCodeZeroLogsAndFails() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);

        JestResult response = mock(JestResult.class);
        when(response.getResponseCode()).thenReturn(0);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to determine if {} data stream already exists",
                CheckDataStream.class.getSimpleName(),
                TEST_NAME);

    }

    @Test
    public void createsGenericJestRequest() {

        // given
        CheckDataStream setupStep = new CheckDataStream(TEST_NAME);

        // when
        GenericJestRequest request = setupStep.createRequest();

        // then
        assertEquals("GET", request.getRestMethodName());
        assertEquals("_data_stream/" + TEST_NAME, request.buildURI());

    }

}
