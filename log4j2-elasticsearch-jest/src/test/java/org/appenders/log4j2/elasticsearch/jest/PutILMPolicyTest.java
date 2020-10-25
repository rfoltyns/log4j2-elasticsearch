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

public class PutILMPolicyTest {

    public static final String TEST_ILM_POLICY_NAME = "testIlmPolicyName";
    private static final String TEST_SOURCE = "testSource";

    @After
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.FAILURE);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.SUCCESS);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.SKIP);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);

        JestResult response = mock(JestResult.class);
        when(response.getResponseCode()).thenReturn(200);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: ILM policy {} updated",
                PutILMPolicy.class.getSimpleName(),
                TEST_ILM_POLICY_NAME);

    }

    @Test
    public void onResponseLogsOnNonSuccess() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);

        JestResult response = mock(JestResult.class);
        when(response.isSucceeded()).thenReturn(false);
        String error = "test ILM policy creation error";
        when(response.getErrorMessage()).thenReturn(error);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to update ILM policy: {}",
                PutILMPolicy.class.getSimpleName(),
                error);

    }

    @Test
    public void createsActualRequest() {

        // given
        PutILMPolicy setupStep = new PutILMPolicy(TEST_ILM_POLICY_NAME, TEST_SOURCE);

        // when
        GenericJestRequest request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getRestMethodName());
        assertEquals("_ilm/policy/" + TEST_ILM_POLICY_NAME, request.buildURI());

    }

}
