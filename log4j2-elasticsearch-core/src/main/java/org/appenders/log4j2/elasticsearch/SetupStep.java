package org.appenders.log4j2.elasticsearch;

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

/**
 * @param <REQ> request type
 * @param <RES> response type
 */
public abstract class SetupStep<REQ, RES> implements SetupCallback<RES> {

    /**
     * Creates client-specific request
     *
     * @return client-specific request
     */
    public abstract REQ createRequest();

    /**
     * @param setupContext context to evaluate
     * @return <i>true</i> if this step should be executed, <i>false</i> otherwise
     */
    public boolean shouldProcess(SetupContext setupContext) {
        return !Result.FAILURE.equals(setupContext.getLatestResult());
    }

}
