package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
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
    private static final ItemSource TEST_SOURCE = ByteBufItemSourceTest.createTestItemSource();

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

        Response response = mock(Response.class);
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

        Response response = mock(Response.class);
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
        Request request = setupStep.createRequest();

        // then
        assertEquals("PUT", request.getHttpMethodName());
        assertEquals("_ilm/policy/" + TEST_ILM_POLICY_NAME, request.getURI());

    }

}