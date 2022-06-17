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
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
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

public class PutComponentTemplateTest {

    public static final String TEST_TEMPLATE_NAME = "testComponentTemplateName";
    private static final ItemSource TEST_SOURCE = ByteBufItemSourceTest.createTestItemSource();

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        final SetupContext setupContext = new SetupContext(Result.FAILURE);

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        final SetupContext setupContext = new SetupContext(Result.SUCCESS);

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        final SetupContext setupContext = new SetupContext(Result.SKIP);

        // when
        final boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        final Response response = mock(Response.class);
        when(response.getResponseCode()).thenReturn(200);

        final Logger logger = mockTestLogger();

        // when
        final Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: Component template {} updated",
                PutComponentTemplate.class.getSimpleName(),
                TEST_TEMPLATE_NAME);

    }

    @Test
    public void onResponseLogsOnNonSuccess() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        final Response response = mock(Response.class);
        when(response.isSucceeded()).thenReturn(false);
        final String error = "test component template creation error";
        when(response.getErrorMessage()).thenReturn(error);

        final Logger logger = mockTestLogger();

        // when
        final Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to update component template: {}",
                PutComponentTemplate.class.getSimpleName(),
                error);

    }

    @Test
    public void createsActualRequest() {

        // given
        final PutComponentTemplate setupStep = new PutComponentTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        // when
        final Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals("_component_template/" + TEST_TEMPLATE_NAME, request.getURI());

    }

}
