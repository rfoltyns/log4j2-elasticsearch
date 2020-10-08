package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates or updates ILM policy
 */
public class PutILMPolicy extends SetupStep<Request, Response> {

    protected final String name;
    protected final ItemSource source;

    public PutILMPolicy(String name, ItemSource source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if ILM policy was processed successfully, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(Response response) {

        source.release();

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
    public Request createRequest() {
        String uri = String.format("_ilm/policy/%s", name);
        return new GenericRequest("PUT", uri, source);
    }

}
