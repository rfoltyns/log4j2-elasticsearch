package org.appenders.log4j2.elasticsearch;

/**
 * @param <T> {@link SetupStep} type
 */
public interface StepProcessor<T> {

    /**
     * Processes given step
     *
     * @param step Step to process
     * @return SHOULD return a non-null {@link Result}
     */
    Result process(T step);

}
