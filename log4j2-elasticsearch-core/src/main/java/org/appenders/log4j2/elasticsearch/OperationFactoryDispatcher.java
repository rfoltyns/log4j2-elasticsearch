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

import java.util.HashMap;
import java.util.Map;

/**
 * Allows to create client-specific administrative {@link Operation}s
 */
public class OperationFactoryDispatcher implements OperationFactory {

    private final Map<String, OperationFactory> registry = new HashMap<>();

    /**
     * Dispatches given {@link OpSource} by {@link OpSource#getType()}.
     * <br>
     * Calls {@link #handleMissing(OpSource)} if none of supported types matches given {@link OpSource#getType()}
     *
     * @param opSource operation definition
     * @return If supported {@link Operation} that's ready to execute, throws otherwise
     */
    @Override
    public final Operation create(OpSource opSource) {

        if (!registry.containsKey(opSource.getType())) {
            return handleMissing(opSource);
        }

        return registry.get(opSource.getType()).create(opSource);

    }

    /**
     * Registers {@link OperationFactory} for given type.
     * Replaces previously registered factory, if present.
     *
     * @param type {@link OpSource#getType()}
     * @param factory factory for given type
     * @return <i>true</i> if this dispatcher already had a factory registered for given type
     */
    protected final boolean register(String type, OperationFactory factory) {
        return registry.put(type, factory) != null;
    }

    /**
     * Handles unsupported {@link Operation}s
     *
     * @param opSource operation definition
     * @return throws by default
     */
    public Operation handleMissing(OpSource opSource) {
        throw new IllegalArgumentException(opSource.getClass().getSimpleName() + " is not supported");
    }

}
