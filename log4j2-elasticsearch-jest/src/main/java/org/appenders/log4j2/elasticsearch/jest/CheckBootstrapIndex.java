package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Checks if bootstrap index exists
 */
public class CheckBootstrapIndex extends SetupStep<GenericJestRequest, JestResult> {

    protected final String rolloverAlias;

    public CheckBootstrapIndex(String rolloverAlias) {
        this.rolloverAlias = rolloverAlias;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if index does NOT exist,
     *          {@link Result#SKIP} if index exists,
     *          {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

        if (response.getResponseCode() == 404) {

            getLogger().info("{}: Index {} does not exist",
                    getClass().getSimpleName(), rolloverAlias);

            return Result.SUCCESS;

        } else if (response.getResponseCode() == 200) {

            getLogger().info("{}: Index {} already exists",
                    getClass().getSimpleName(), rolloverAlias);

            return Result.SKIP;

        } else {

            getLogger().error("{}: Unable to determine if {} index already exists",
                    getClass().getSimpleName(), rolloverAlias);
            return Result.FAILURE;

        }

    }

    @Override
    public GenericJestRequest createRequest() {

        return new GenericJestRequest("HEAD", null) {

            @Override
            public String buildURI() {
                return rolloverAlias;
            }

        };

    }
}
