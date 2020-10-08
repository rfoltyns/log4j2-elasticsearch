package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.Result;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;

/**
 * Creates or updates index template
 */
public class PutIndexTemplate extends BulkProcessorSetupStep<AcknowledgedResponse> {

    private final String name;
    private final String source;

    public PutIndexTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * Executes with given {@code TransportClient}
     *
     * @param client client to use when executing {@link #createRequest()}
     * @return Always {@link Result#SUCCESS}
     */
    @Override
    public Result execute(TransportClient client) {
        IndicesAdminClient indices = client.admin().indices();

        PutIndexTemplateRequest request = createRequest();
        indices.putTemplate(request, new LoggingActionListener<>(request.getClass().getSimpleName()));

        return Result.SUCCESS;
    }

    @Override
    public PutIndexTemplateRequest createRequest() {
        return new PutIndexTemplateRequest()
                .name(name)
                .source(source);
    }

}
