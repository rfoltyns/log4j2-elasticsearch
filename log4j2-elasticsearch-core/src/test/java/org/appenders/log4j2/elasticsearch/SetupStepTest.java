package org.appenders.log4j2.elasticsearch;

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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class SetupStepTest {

    @AfterEach
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
