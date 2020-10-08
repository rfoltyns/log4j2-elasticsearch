package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.appenders.log4j2.elasticsearch.SetupStepTest;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        SetupContext setupContext = SetupStepTest.createTestSetupContext(Result.FAILURE);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertFalse(result);

    }

    @Test
    public void executesOnSuccess() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);
        SetupContext setupContext = SetupStepTest.createTestSetupContext(Result.SUCCESS);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void executesOnSkip() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);
        SetupContext setupContext = SetupStepTest.createTestSetupContext(Result.SKIP);

        // when
        boolean result = setupStep.shouldProcess(setupContext);

        // then
        assertTrue(result);

    }

    @Test
    public void onResponseLogsOnSuccess() {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        Response response = mock(Response.class);
        when(response.getResponseCode()).thenReturn(404);

        Logger logger = mockTestLogger();

        // when
        Result result = setupStep.onResponse(response);

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

        Response response = mock(Response.class);
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

        Response response = mock(Response.class);
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
    public void createsGenericRequest() throws IOException {

        // given
        CheckBootstrapIndex setupStep = new CheckBootstrapIndex(TEST_ROLLOVER_ALIAS);

        // when
        Request request = setupStep.createRequest();

        // then
        assertEquals("HEAD", request.getHttpMethodName());
        assertEquals(TEST_ROLLOVER_ALIAS, request.getURI());
        assertNull(request.serialize().getSource());
    }

}