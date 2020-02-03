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
 * Default implementation. SHOULD have no impact on caller's behaviour
 *
 * @param <T> client-specific type
 */
public class NoopBackoffPolicy<T> implements BackoffPolicy<T> {

    /**
     * @param request not used
     * @return false
     */
    @Override
    public final boolean shouldApply(T request) {
        return false;
    }

    @Override
    public void register(T request) {
        // noop
    }

    @Override
    public void deregister(Object result) {
        // noop
    }

}
