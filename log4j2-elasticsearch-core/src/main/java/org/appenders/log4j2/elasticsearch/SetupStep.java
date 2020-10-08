package org.appenders.log4j2.elasticsearch;

/**
 * @param <REQ> request type
 * @param <RES> response type
 */
public abstract class SetupStep<REQ, RES> implements SetupCallback<RES> {

    /**
     * Creates client-specific request
     *
     * @return client-specific request
     */
    public abstract REQ createRequest();

    /**
     * @param setupContext context to evaluate
     * @return <i>true</i> if this step should be executed, <i>false</i> otherwise
     */
    public boolean shouldProcess(SetupContext setupContext) {
        return !Result.FAILURE.equals(setupContext.getLatestResult());
    }

}
