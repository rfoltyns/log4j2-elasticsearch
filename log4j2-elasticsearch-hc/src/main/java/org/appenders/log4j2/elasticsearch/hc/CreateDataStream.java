package org.appenders.log4j2.elasticsearch.hc;

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

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupContext;
import org.appenders.log4j2.elasticsearch.SetupStep;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Creates bootstrap index
 */
public class CreateDataStream extends SetupStep<Request, Response> {

    protected final String name;
    protected final ItemSource itemSource;

    public CreateDataStream(String name, ItemSource itemSource) {
        this.name = name;
        this.itemSource = itemSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldProcess(SetupContext setupContext) {

        boolean shouldExecute = Result.SUCCESS.equals(setupContext.getLatestResult());

        if (!shouldExecute) {
            getLogger().info("{}: Skipping data stream creation",
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

            getLogger().info("{}: Data stream {} created",
                    getClass().getSimpleName(), name);

            return Result.SUCCESS;

        }

        getLogger().error("{}: Unable to create data stream: {}",
                getClass().getSimpleName(), response.getErrorMessage());

        return Result.FAILURE;

    }

    @Override
    public Request createRequest() {
        return new GenericRequest("PUT", "_data_stream/" + name, itemSource);
    }

}
