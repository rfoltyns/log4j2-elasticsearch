package org.appenders.log4j2.elasticsearch.ahc;

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

import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Checks if bootstrap index exists
 */
public class CheckBootstrapIndex extends SetupStep<Request, Response> {

    protected final String rolloverAlias;

    public CheckBootstrapIndex(final String rolloverAlias) {
        this.rolloverAlias = rolloverAlias;
    }

    /**
     * @param response client response
     * @return {@link Result#SUCCESS} if index does NOT exist,
     *          {@link Result#SKIP} if index exists,
     *          {@link Result#FAILURE} otherwise
     */
    @Override
    public Result onResponse(final Response response) {

        if (response.getResponseCode() == 404) {

            getLogger().info("{}: Index {} does not exist",
                    getClass().getSimpleName(), rolloverAlias);

            return Result.SUCCESS;

        } else if (response.getResponseCode() == 200) {

            getLogger().info("{}: Index {} already exists",
                    getClass().getSimpleName(), rolloverAlias);

            return Result.SKIP;

        } else {

            getLogger().error("{}: Unable to determine if {} index already exists",
                    getClass().getSimpleName(), rolloverAlias);

            return Result.FAILURE;

        }

    }

    @Override
    public Request createRequest() {
        return new GenericRequest("HEAD", rolloverAlias, () -> null);
    }

}
