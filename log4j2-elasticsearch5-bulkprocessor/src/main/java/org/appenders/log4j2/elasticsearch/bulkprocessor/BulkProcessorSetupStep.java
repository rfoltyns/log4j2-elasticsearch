package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

