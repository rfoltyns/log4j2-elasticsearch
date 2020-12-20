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

import org.appenders.log4j2.elasticsearch.ClientProvider;

/**
 * {@link ClientProvider} reuse strategy.
 *
 * @param <T> client type
 */
public interface ClientProviderPolicy<T> {

    /**
     * @param source {@link ClientProvider} that MAY be used by implementations of this policy
     * @return {@link ClientProvider} after policy was applied. MAY not be the same instance as given {@code source}
     */
    ClientProvider<T> apply(ClientProvider<T> source);

}
