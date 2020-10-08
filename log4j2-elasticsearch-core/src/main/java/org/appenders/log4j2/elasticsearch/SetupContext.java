package org.appenders.log4j2.elasticsearch;

/**
 * MAY be used to hold shared data between {@link SetupStep}s
 */
public class SetupContext {

    private Result latestResult;

    public SetupContext(Result latestResult) {
        this.latestResult = latestResult;
    }

    public Result getLatestResult() {
        return latestResult;
    }

    public void setLatestResult(Result latestResult) {
        this.latestResult = latestResult;
    }

}
