package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
 * Checks if data stream exists
 */
public class CheckDataStream extends SetupStep<GenericJestRequest, JestResult> {

    protected final String name;

    public CheckDataStream(String name) {
        this.name = name;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if data stream does NOT exist,
     *          {@link Result#SKIP} if data stream exists,
     *          {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(JestResult response) {

        if (response.getResponseCode() == 404) {

            getLogger().info("{}: Data stream {} does not exist",
                    getClass().getSimpleName(), name);

            return Result.SUCCESS;

        } else if (response.getResponseCode() == 200) {

            getLogger().info("{}: Data stream {} already exists",
                    getClass().getSimpleName(), name);

            return Result.SKIP;

        } else {

            getLogger().error("{}: Unable to determine if {} data stream already exists",
                    getClass().getSimpleName(), name);

            return Result.FAILURE;

        }

    }

    @Override
    public GenericJestRequest createRequest() {

        return new GenericJestRequest("GET", null) {

            @Override
            public String buildURI() {
                return "_data_stream/" + name;
            }

        };

    }

}
