package org.appenders.log4j2.elasticsearch.jest;

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
