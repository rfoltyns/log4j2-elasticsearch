package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.JestResult;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.junit.After;
import org.junit.Test;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckBootstrapIndexTest {

    public static final String TEST_ROLLOVER_ALIAS = "testRolloverAlias";

    @After
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);
        SetupContext setupContext = new SetupContext(Result.FAILURE);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);
        SetupContext setupContext = new SetupContext(Result.SUCCESS);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);
        SetupContext setupContext = new SetupContext(Result.SKIP);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        JestResult jestResult = mock(JestResult.class);
        when(jestResult.getResponseCode()).thenReturn(404);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(jestResult);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: Index {} does not exist",
                CheckBootstrapIndex.class.getSimpleName(),
                TEST_ROLLOVER_ALIAS);

    }

    @Test
    public void onResponseCode200LogsAndSkips() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        JestResult response = mock(JestResult.class);
        when(response.getResponseCode()).thenReturn(200);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.SKIP, result);
        verify(logger).info(
                "{}: Index {} already exists",
                CheckBootstrapIndex.class.getSimpleName(),
                TEST_ROLLOVER_ALIAS);

    }

    @Test
    public void onResponseCodeZeroLogsAndFails() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        JestResult response = mock(JestResult.class);
        when(response.getResponseCode()).thenReturn(0);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to determine if {} index already exists",
                CheckBootstrapIndex.class.getSimpleName(),
                TEST_ROLLOVER_ALIAS);

    }

    @Test
    public void createsGenericJestRequest() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        // when
        GenericJestRequest request = setupStep.createRequest();

        // then
        assertEquals("HEAD", request.getRestMethodName());
        assertEquals(TEST_ROLLOVER_ALIAS, request.buildURI());

    }

}
