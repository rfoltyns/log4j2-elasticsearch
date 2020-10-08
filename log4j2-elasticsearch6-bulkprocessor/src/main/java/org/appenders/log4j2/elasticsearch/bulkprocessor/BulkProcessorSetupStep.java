package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.transport.TransportClient;

public abstract class BulkProcessorSetupStep<RES extends AcknowledgedResponse> extends SetupStep<ActionRequest, RES> {

    /**
     * Executes with given {@code TransportClient}
     *
     * @param client client to use when executing {@link #createRequest()}
     * @return {@link Result} outcome
     */
    public abstract Result execute(TransportClient client);

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if operation was acknowledged, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(AcknowledgedResponse response) {
        return response.isAcknowledged() ? Result.SUCCESS : Result.FAILURE;
    }

}

