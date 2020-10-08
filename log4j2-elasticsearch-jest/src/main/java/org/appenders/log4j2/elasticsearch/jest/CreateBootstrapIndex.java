package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates bootstrap index
 */
public class CreateBootstrapIndex extends SetupStep<GenericJestRequest, JestResult> {

    public static final String BOOTSTRAP_TEMPLATE = "{\"aliases\": {\"%s\":{\"is_write_index\":true}}}";

    protected final String rolloverAlias;
    protected final String bootstrapIndexName;

    public CreateBootstrapIndex(String rolloverAlias) {
        this.rolloverAlias = rolloverAlias;
        this.bootstrapIndexName = String.format("%s-000001", rolloverAlias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldProcess(SetupContext setupContext) {

        boolean shouldExecute = Result.SUCCESS.equals(setupContext.getLatestResult());

        if (!shouldExecute) {
            getLogger().info("{}: Skipping bootstrap index creation",
                    getClass().getSimpleName());
        }

        return shouldExecute;

    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if bootstrap index was created, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

        if (response.getResponseCode() == 200) {

            getLogger().info("{}: Bootstrap index {} created",
                    getClass().getSimpleName(), bootstrapIndexName);

            return Result.SUCCESS;

        }

        getLogger().error("{}: Unable to create bootstrap index: {}",
                getClass().getSimpleName(), response.getErrorMessage());

        return Result.FAILURE;

    }

    @Override
    public GenericJestRequest createRequest() {

        String source = String.format(BOOTSTRAP_TEMPLATE, rolloverAlias);

        return new GenericJestRequest("PUT", source) {

            @Override
            public String buildURI() {
                return bootstrapIndexName;
            }

        };

    }

}
