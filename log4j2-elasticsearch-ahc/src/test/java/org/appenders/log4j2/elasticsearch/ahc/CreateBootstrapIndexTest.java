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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.SetupStepTest.createTestSetupContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateBootstrapIndexTest {

    public static final String TEST_BOOTSTRAP_INDEX_NAME = "testBootstrapIndexName";

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());
        final SetupContext setupContext = createTestSetupContext(Result.FAILURE);

        final Logger logger = mockTestLogger();

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);
        verify(logger).info("{}: Skipping bootstrap index creation",
                CreateBootstrapIndex.class.getSimpleName());

    }

    @Test
    public void executesOnSuccess() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());
        final SetupContext setupContext = createTestSetupContext(Result.SUCCESS);

        final Logger logger = mockTestLogger();

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);
        verify(logger, never()).info("{}: Skipping bootstrap index creation",
                CreateBootstrapIndex.class.getSimpleName());

    }

    @Test
    public void doesNotExecuteOnSkip() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());
        final SetupContext setupContext = createTestSetupContext(Result.SKIP);

        final Logger logger = mockTestLogger();

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);
        verify(logger).info("{}: Skipping bootstrap index creation",
                CreateBootstrapIndex.class.getSimpleName());

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());

        final Response jestResult = mock(Response.class);
        when(jestResult.getResponseCode()).thenReturn(200);

        final Logger logger = mockTestLogger();

        // when
        final Result result = setupStep.onResponse(jestResult);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: Bootstrap index {} created",
                CreateBootstrapIndex.class.getSimpleName(),
                TEST_BOOTSTRAP_INDEX_NAME + "-000001");

    }

    @Test
    public void onResponseLogsOnNonSuccess() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());

        final Response jestResult = mock(Response.class);
        when(jestResult.getResponseCode()).thenReturn(400);
        final String error = "test bootstrap index creation error";
        when(jestResult.getErrorMessage()).thenReturn(error);

        final Logger logger = mockTestLogger();

        // when
        final Result result = setupStep.onResponse(jestResult);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to create bootstrap index: {}",
                CreateBootstrapIndex.class.getSimpleName(),
                error);

    }

    @Test
    public void createsGenericRequest() {

        // given
        final CreateBootstrapIndex setupStep = new CreateBootstrapIndex(
                TEST_BOOTSTRAP_INDEX_NAME,
                createTestItemSource());

        // when
        final Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals(TEST_BOOTSTRAP_INDEX_NAME + "-000001", request.getURI());

    }

}
