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
 * Implementation of this interface SHOULD provide {@link ItemSource} pooling capabilities
 */
public interface ItemSourcePool<T> extends LifeCycle {

    /**
     * Creates pooled {@link ItemSource} instances
     *
     * @param delta number of elements to be pooled
     */
    void incrementPoolSize(int delta);

    /**
     * Creates ONE pooled {@link ItemSource}
     */
    void incrementPoolSize();

    /**
     * Retrieves pooled element from the pool
     *
     * @throws PoolResourceException if pooled resource couldn't be obtained
     * @return pooled {@link ItemSource}
     */
    ItemSource<T> getPooled() throws PoolResourceException;

    /**
     * Removes ONE pooled element from the pool
     *
     * @return true, if element was successfully removed, false otherwise
     */
    boolean remove();

    /**
     * @return pool identifier
     */
    String getName();

    /**
     * @return Number of elements initially created by the pool
     */
    int getInitialSize();

    /**
     * @return Total number of elements managed by this pool
     */
    int getTotalSize();

    /**
     * @return Number of pooled elements available to offer
     */
    int getAvailableSize();

    /**
     * MUST clean up/close underlying resources
     */
    void shutdown();

}
