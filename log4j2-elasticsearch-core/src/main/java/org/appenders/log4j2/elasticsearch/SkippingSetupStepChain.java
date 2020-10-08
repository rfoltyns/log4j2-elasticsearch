package org.appenders.log4j2.elasticsearch;

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