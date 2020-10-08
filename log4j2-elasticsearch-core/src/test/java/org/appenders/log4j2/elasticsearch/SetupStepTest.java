package org.appenders.log4j2.elasticsearch;

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.junit.After;
import org.junit.Test;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class SetupStepTest {

    @After
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void shouldExecuteReturnsFalseGivenFailureResult() {

        // given
        SetupStep<Object, Object> setupStep = createDefaultSetupStep();

        // when
        boolean result = setupStep.shouldProcess(new SetupContext(Result.FAILURE));

        // then
        assertFalse(result);

    }

    @Test
    public void shouldExecuteReturnsTrueGivenSuccessResult() {

        // given
        SetupStep<Object, Object> setupStep = createDefaultSetupStep();

        // when
        boolean result = setupStep.shouldProcess(new SetupContext(Result.SUCCESS));

        // then
        assertTrue(result);

    }

    @Test
    public void shouldExecuteReturnsTrueGivenSkipResult() {

        // given
        SetupStep<Object, Object> setupStep = createDefaultSetupStep();

        // when
        boolean result = setupStep.shouldProcess(new SetupContext(Result.SKIP));

        // then
        assertTrue(result);

    }

    @Test
    public void logsErrorOnException() {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        Exception exception = new Exception(expectedMessage);

        Logger logger = mockTestLogger();

        SetupStep<Object, Object> setupStep = createDefaultSetupStep();

        // when
        setupStep.onException(exception);

        // then
        verify(logger).error(
                "{}: {} {}",
                setupStep.getClass().getSimpleName(),
                exception.getClass().getSimpleName(),
                expectedMessage);

    }

    public static SetupStep<Object, Object> createTestSetupStep(Result execCondition, Result execResult) {
        return new TestSetupStep(execCondition, execResult);
    }

    public static SetupContext createTestSetupContext(Result failure) {
        return new SetupContext(failure);
    }

    private static SetupStep<Object, Object> createDefaultSetupStep() {
        return new SetupStep<Object, Object>() {

            @Override
            public Result onResponse(Object response) {
                return null;
            }

            @Override
            public Object createRequest() {
                return null;
            }
        };
    }

    private static class TestSetupStep extends SetupStep<Object, Object> {

        private final Result execCondition;
        private final Result execResult;

        public TestSetupStep(Result execCondition, Result execResult) {
            this.execCondition = execCondition;
            this.execResult = execResult;
        }

        @Override
        public boolean shouldProcess(SetupContext setupContext) {
            return execCondition.equals(setupContext.getLatestResult());
        }

        @Override
        public Object createRequest() {
            return null;
        }

        @Override
        public Result onResponse(Object response) {
            return execResult;
        }

    }

}