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

import java.util.Collection;

/**
 * Allows to execute a series of steps within a single {@link #execute()} call.
 * <br>
 * Execution of each step is skipped if {@code SetupStep.shouldProcess()} returns <i>false</i>
 *
 * @param <T> {@link SetupStep} types
 */
public class SkippingSetupStepChain<T extends SetupStep<?, ?>> implements Operation {

    protected final Collection<T> steps;
    protected final StepProcessor<T> stepProcessor;

    /**
     * @param steps {@link SetupStep}s to process
     * @param stepProcessor called for each {@link SetupStep}
     */
    public SkippingSetupStepChain(
            Collection<T> steps,
            StepProcessor<T> stepProcessor) {
        this.steps = steps;
        this.stepProcessor = stepProcessor;
    }

    /**
     * Executes a series of steps within a single call.
     * <br>
     * {@link SetupStep} will be omitted if, {@link SetupStep#shouldProcess(SetupContext)} returns false.
     * <br>
     * Otherwise, {@link SetupStep} will be processed by {@link StepProcessor#process(Object)}}
     */
    @Override
    public void execute() {
        SetupContext setupContext = new SetupContext(Result.SUCCESS);
        for (T step : steps) {
            if (step.shouldProcess(setupContext)) {
                setupContext.setLatestResult(stepProcessor.process(step));
            }
        }
    }

}
