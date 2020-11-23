package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;


/**
 * Creates or updates component template
 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-component-template.html">Component templates</a>
 * and <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html">Composable index templates</a>
 */
public class PutComponentTemplate extends SetupStep<Request, Response> {

    protected final String name;
    protected final ItemSource source;

    public PutComponentTemplate(String name, ItemSource itemSource) {
        this.name = name;
        this.source = itemSource;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if component template was processed successfully, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(Response response) {

        source.release();

        if (response.getResponseCode() == 200) {

            getLogger().info("{}: Component template {} updated",
                    getClass().getSimpleName(), name);

            return Result.SUCCESS;

        }

        getLogger().error("{}: Unable to update component template: {}",
                getClass().getSimpleName(), response.getErrorMessage());

        return Result.FAILURE;

    }

    @Override
    public Request createRequest() {
        String uri = "_component_template/" + name;
        return new GenericRequest("PUT", uri, source);
    }

}
