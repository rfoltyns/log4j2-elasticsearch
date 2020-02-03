package org.appenders.log4j2.elasticsearch.backoff;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 - 2020 Rafal Foltynski
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
 * Allows to accumulate data and make decision based on it.
 *
 * @param <T> data type
 */
public interface BackoffPolicy<T> {

    String NAME = "BackoffPolicy";

    /**
     * SHOULD be used to make a decision on further steps ({@link #register(Object)} or {@link #deregister(Object)}
     *
     * @param data data
     * @return true, if policy should apply, false otherwise
     */
    boolean shouldApply(T data);

    /**
     * @param data data to collect before next decision
     */
    void register(T data);

    /**
     * @param data data to remove before next decision
     */
    void deregister(T data);

}
