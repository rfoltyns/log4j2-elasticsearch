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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SkippingSetupStepChainTest {

    @Test
    public void executesOnlyIfAllowedByShouldExecute() {

        // given
        SetupStep<Object, Object> expected1 = spy(SetupStepTest.createTestSetupStep(Result.SUCCESS, Result.SKIP));
        SetupStep<Object, Object> expected2 = spy(SetupStepTest.createTestSetupStep(Result.SKIP, Result.FAILURE));
        SetupStep<Object, Object> expected3 = spy(SetupStepTest.createTestSetupStep(Result.FAILURE, Result.SUCCESS));

        ResultCapturingStepProcessor execOnStep = new ResultCapturingStepProcessor();
        SkippingSetupStepChain<SetupStep<Object, Object>> skippingSetupStepChain = new SkippingSetupStepChain<>(
                Arrays.asList(expected1, expected2, expected3), execOnStep
        );

        // when
        skippingSetupStepChain.execute();

        // then
        verify(expected1).shouldProcess(any());
        verify(expected1).onResponse(any());
        verify(expected2).shouldProcess(any());
        verify(expected2).onResponse(any());
        verify(expected3).shouldProcess(any());
        verify(expected3).onResponse(any());

        assertEquals(Arrays.asList(Result.SKIP, Result.FAILURE, Result.SUCCESS), execOnStep.getResultList());

    }

    @Test
    public void skipsRequestDependingOnPreviousResult() {

        // given
        SetupStep<Object, Object> expected1 = spy(SetupStepTest.createTestSetupStep(Result.SUCCESS, Result.SKIP));
        SetupStep<Object, Object> notExpected1 = spy(SetupStepTest.createTestSetupStep(Result.SUCCESS, Result.FAILURE)); // expected SUCCESS, skipping
        SetupStep<Object, Object> notExpected2 = spy(SetupStepTest.createTestSetupStep(Result.FAILURE, Result.SUCCESS)); // expected FAILURE, skipping
        SetupStep<Object, Object> expected2 = spy(SetupStepTest.createTestSetupStep(Result.SKIP, Result.SUCCESS)); // expected SKIP

        ResultCapturingStepProcessor stepProcessor = new ResultCapturingStepProcessor();
        SkippingSetupStepChain<SetupStep<Object, Object>> skippingSetupStepChain = new SkippingSetupStepChain<>(
                Arrays.asList(expected1, notExpected1, notExpected2, expected2), stepProcessor
        );

        // when
        skippingSetupStepChain.execute();

        // then
        verify(expected1).shouldProcess(any());
        verify(expected1).createRequest();
        verify(expected1).onResponse(any());
        verify(notExpected1).shouldProcess(any());
        verify(notExpected1, never()).createRequest();
        verify(notExpected1, never()).onResponse(any());
        verify(notExpected2).shouldProcess(any());
        verify(notExpected2, never()).createRequest();
        verify(notExpected2, never()).onResponse(any());
        verify(expected2).shouldProcess(any());
        verify(expected2).createRequest();
        verify(expected2).onResponse(any());

        assertEquals(Arrays.asList(Result.SKIP, Result.SUCCESS), stepProcessor.getResultList());

    }

    private static class ResultCapturingStepProcessor implements StepProcessor<SetupStep<Object, Object>> {

        private final List<Result> resultList = new ArrayList<>();

        @Override
        public Result process(SetupStep<Object, Object> request) {

            request.createRequest();

            Result result = request.onResponse(null);
            resultList.add(result); // capturing here as SetupContext is reused, so Mockito will not capture it properly

            return result;

        }

        public List<Result> getResultList() {
            return resultList;
        }

    }

}
