package org.appenders.log4j2.elasticsearch.hc;

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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
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

public class PutIndexTemplateTest {

    public static final String TEST_TEMPLATE_NAME = "testRolloverAlias";
    private static final ItemSource TEST_SOURCE = ByteBufItemSourceTest.createTestItemSource();

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void doesNotExecuteOnFailure() {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.FAILURE);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.SUCCESS);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);
        SetupContext setupContext = new SetupContext(Result.SKIP);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        Response response = mock(Response.class);
        when(response.getResponseCode()).thenReturn(200);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.SUCCESS, result);
        verify(logger).info(
                "{}: Index template {} updated",
                PutIndexTemplate.class.getSimpleName(),
                TEST_TEMPLATE_NAME);

    }

    @Test
    public void onResponseLogsOnNonSuccess() {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        Response response = mock(Response.class);
        when(response.isSucceeded()).thenReturn(false);
        String error = "test index template creation error";
        when(response.getErrorMessage()).thenReturn(error);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);
        verify(logger).error(
                "{}: Unable to update index template: {}",
                PutIndexTemplate.class.getSimpleName(),
                error);

    }

    @Test
    public void createsActualRequestIfComposable() throws Exception {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(IndexTemplate.DEFAULT_API_VERSION + 1, TEST_TEMPLATE_NAME, TEST_SOURCE);

        // when
        Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals("_index_template/" + TEST_TEMPLATE_NAME, request.getURI());
        assertTrue(request.serialize() == TEST_SOURCE);

    }

    @Test
    public void createsActualRequestIfNotComposable() throws Exception {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(6, TEST_TEMPLATE_NAME, TEST_SOURCE);

        // when
        Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals("_template/" + TEST_TEMPLATE_NAME, request.getURI());
        assertTrue(request.serialize() == TEST_SOURCE);

    }

    @Test
    public void defaultRequestIsComposable() throws Exception {

        // given
        PutIndexTemplate setupStep = new PutIndexTemplate(TEST_TEMPLATE_NAME, TEST_SOURCE);

        // when
        Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals("_index_template/" + TEST_TEMPLATE_NAME, request.getURI());
        assertTrue(request.serialize() == TEST_SOURCE);

    }

}
