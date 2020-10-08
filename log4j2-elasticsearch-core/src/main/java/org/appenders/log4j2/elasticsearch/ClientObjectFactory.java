package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import java.util.Collection;
import java.util.function.Function;

/**
 * Factory for client-specific objects:
 * <ul>
 * <li> the client itself
 * <li> batch items creators (client-specific batch objects)
 * <li> batch processing listeners
 * <li> pre-batch setup operations (client-specific setup requests)
 * </ul>
 * <p>
 * Implementations of this class MUST provide a set of client- and {@link BatchEmitter}-compatible objects
 *
 * @param <CLIENT_TYPE> type of client object produced by this factory
 * @param <BATCH_TYPE> type of batch objects handled by the client
 */
public interface ClientObjectFactory<CLIENT_TYPE, BATCH_TYPE> extends LifeCycle {

    String ELEMENT_TYPE = "objectFactory";

    /**
     * @return Collection of configured addresses
     */
    Collection<String> getServerList();

    /**
     * @return CLIENT_TYPE Fully configured client
     */
    CLIENT_TYPE createClient();

    /**
     * Listener that MUST accept and send prepared batch and handle the exceptions
     * @param failoverPolicy sink for failed batch items
     * @return prepared batch handler
     */
    Function<BATCH_TYPE, Boolean> createBatchListener(FailoverPolicy failoverPolicy);

    /**
     * Failed batch handler. SHOULD deliver the batch to alternate target or provided failover policy
     * @param failover optional failover strategy
     * @return prepared failed batch handler
     */
    Function<BATCH_TYPE, Boolean> createFailureHandler(FailoverPolicy failover);

    /**
     * @return batch items creator
     */
    BatchOperations<BATCH_TYPE> createBatchOperations();

    /**
     * Updates target with index template
     * @param indexTemplate index template request
     * @deprecated As of 1.6, this method will be removed, use {@link #addOperation(Operation)} instead
     */
    @Deprecated
    void execute(IndexTemplate indexTemplate);

    /**
     * Allows to add operation to be executed before next batch. Exact time of the execution depends on implementation of this factory.
     *
     * NOTE: {@code default} added for backwards compatibility. {@code default} will be removed future releases
     * @param operation operation to be executed
     */
    default void addOperation(Operation operation) {}

    /**
     * MUST return an instance of {@link OperationFactory}
     *
     * Since 1.5
     *
     * @return {@link OperationFactory} instance
     */
    OperationFactory setupOperationFactory();

    /*
     * LIFECYCLE
     *
     * {@code default} added for backwards compatibility. {@code default} will be removed future releases
     *
     */

    @Override
    default void start() {}

    @Override
    default void stop() {}

    @Override
    default boolean isStarted() { return false; }

    @Override
    default boolean isStopped() { return true; }
}
