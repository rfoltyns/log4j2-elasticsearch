package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates or updates ILM policy
 */
public class PutILMPolicy extends SetupStep<GenericJestRequest, JestResult> {

    protected final String name;
    protected final String source;

    public PutILMPolicy(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if ILM policy was processed successfully, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

        if (response.getResponseCode() == 200) {

            getLogger().info("{}: ILM policy {} updated",
                    getClass().getSimpleName(), name);

            return Result.SUCCESS;

        }

        getLogger().error("{}: Unable to update ILM policy: {}",
                getClass().getSimpleName(), response.getErrorMessage());

        return Result.FAILURE;

    }

    @Override
    public GenericJestRequest createRequest() {

        return new GenericJestRequest("PUT", source) {

            @Override
            public String buildURI() {
                return String.format("_ilm/policy/%s", name);
            }

        };

    }
}
