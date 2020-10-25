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
 * Allows to create client-specific administrative {@link Operation}s
 */
public abstract class SetupOperationFactory implements OperationFactory {

    /**
     * Index template setup
     *
     * @param indexTemplate {@link IndexTemplate} definition
     * @return {@link Operation} that executes given index template
     */
    public abstract Operation indexTemplate(IndexTemplate indexTemplate);

    /**
     * ILM policy setup
     *
     * @param ilmPolicy {@link ILMPolicy} definition
     * @return {@link Operation} that executes given ILM policy
     */
    public abstract Operation ilmPolicy(ILMPolicy ilmPolicy);

    /**
     * Handles unsupported {@link Operation}s
     *
     * @param opSource operation definition
     * @return throws by default
     */
    public Operation handleUnsupported(OpSource opSource) {
        throw new IllegalArgumentException(opSource.getClass().getSimpleName() + " is not supported");
    }

    /**
     * Dispatches given {@link OpSource} by {@link OpSource#getType()}.
     * <br>
     * Falls back to {@link #handleUnsupported(OpSource)} if none of supported types matches given {@link OpSource#getType()}
     *
     * @param opSource operation definition
     * @return If supported {@link Operation} that's ready to execute, throws otherwise
     */
    @Override
    public final Operation create(OpSource opSource) {

        final Operation operation;

        switch (opSource.getType()) {
            case IndexTemplate.TYPE_NAME : {
                operation = indexTemplate((IndexTemplate) opSource);
                break;
            }
            case ILMPolicy.TYPE_NAME: {
                operation = ilmPolicy((ILMPolicy) opSource);
                break;
            }
            default:
                operation = handleUnsupported(opSource);
        }

        return operation;

     }

}
