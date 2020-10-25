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
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * Creates or updates index template
 */
public class PutIndexTemplate extends BulkProcessorSetupStep<AcknowledgedResponse> {

    private final String name;
    private final byte[] source;

    public PutIndexTemplate(String name, byte[] source) {
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
                .source(source, XContentType.JSON);
    }

}
