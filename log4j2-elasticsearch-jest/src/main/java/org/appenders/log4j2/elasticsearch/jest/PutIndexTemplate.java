package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates or updates index template
 */
public class PutIndexTemplate extends SetupStep<GenericJestRequest, JestResult> {

    protected final String templateName;
    protected final String source;

    public PutIndexTemplate(String templateName, String source) {
        this.templateName = templateName;
        this.source = source;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if index template was processed successfully, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

        if (response.isSucceeded()) {

            getLogger().info("{}: Index template {} updated",
                    getClass().getSimpleName(), templateName);

            return Result.SUCCESS;

        }

        getLogger().error("{}: Unable to update index template: {}",
                getClass().getSimpleName(), response.getErrorMessage());

        return Result.FAILURE;

    }

    @Override
    public GenericJestRequest createRequest() {

        GenericJestRequest request = new GenericJestRequest("PUT", source) {
            @Override
            public String buildURI() {
                return "_template/" + templateName;
            }
        };

        return request;

    }

}
