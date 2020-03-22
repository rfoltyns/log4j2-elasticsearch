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


import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.failover.FailoverListener;

/**
 * Provides a failure handler interface. Implementation of this class MUST handle failed items gracefully.
 *
 * @param <T> type of batch item payload
 */
public interface FailoverPolicy<T> {

    String ELEMENT_TYPE = "failoverPolicy";

    /**
     * SHOULD provide an alternate method of delivery
     *
     * @param failedPayload payload to be handled
     */
    void deliver(T failedPayload);

    /**
     * SHOULD provide an alternate method of delivery
     *
     * @param failedPayload payload to be handled
     * @deprecated {@link ItemSource} parameter was added in 1.4. It was a mistake. Method will be removed in 1.5. Use {@link #deliver(FailedItemSource)} instead.
     */
    @Deprecated
    default void deliver(ItemSource<T> failedPayload) {
        deliver(failedPayload.getSource()); // fallback to existing API for backwards compatibility
    }

    /**
     * SHOULD provide an alternate method of delivery
     *
     * @param failedPayload
     */
    default void deliver(FailedItemSource<T> failedPayload) {
        deliver((ItemSource<T>)failedPayload); // fallback to existing API for backwards compatibility
    }

    /**
     * @param failoverListener listener to be notified about events of policy's choice
     */
    default <U extends FailoverListener> void addListener(U failoverListener) {
        // noop
    }

}
