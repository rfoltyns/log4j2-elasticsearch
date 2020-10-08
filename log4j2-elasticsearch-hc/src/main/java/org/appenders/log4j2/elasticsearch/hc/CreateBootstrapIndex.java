package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates bootstrap index
 */
public class CreateBootstrapIndex extends SetupStep<Request, Response> {

    static final String BOOTSTRAP_TEMPLATE = "{\"aliases\": {\"%s\":{\"is_write_index\":true}}}";

    protected final String rolloverAlias;
    protected final String bootstrapIndexName;
    protected final ItemSource itemSource;

    public CreateBootstrapIndex(String rolloverAlias, ItemSource itemSource) {
        this.rolloverAlias = rolloverAlias;
        this.bootstrapIndexName = String.format("%s-000001", rolloverAlias);
        this.itemSource = itemSource;
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
    public Result onResponse(Response response) {

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
    public Request createRequest() {
        return new GenericRequest("PUT", bootstrapIndexName, itemSource);
    }

}
