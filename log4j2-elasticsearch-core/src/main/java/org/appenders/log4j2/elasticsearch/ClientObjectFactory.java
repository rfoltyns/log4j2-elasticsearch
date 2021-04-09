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
public interface ClientObjectFactory<CLIENT_TYPE, BATCH_TYPE> extends LifeCycle, BatchListenerFactory<BATCH_TYPE>, ClientFactory<CLIENT_TYPE> {

    String ELEMENT_TYPE = "objectFactory";

    /**
     * @return batch items creator
     */
    BatchOperations<BATCH_TYPE> createBatchOperations();

    /**
     * Allows to add operation to be executed before next batch. Exact time of the execution depends on implementation of this factory.
     *
     * NOTE: {@code default} added for backwards compatibility. {@code default} will be removed future releases
     * @param operation operation to be executed
     * @deprecated As of 1.6, this method will be wrapped by batch-phase-based queue
     */
    @Deprecated
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
