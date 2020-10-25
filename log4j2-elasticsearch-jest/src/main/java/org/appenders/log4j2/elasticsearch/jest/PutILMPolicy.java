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
 * Creates or updates ILM policy
 */
public class PutILMPolicy extends SetupStep<GenericJestRequest, JestResult> {

    protected final String name;
    protected final String source;

    public PutILMPolicy(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if ILM policy was processed successfully, {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

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
    public GenericJestRequest createRequest() {

        return new GenericJestRequest("PUT", source) {

            @Override
            public String buildURI() {
                return String.format("_ilm/policy/%s", name);
            }

        };

    }
}
