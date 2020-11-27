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
 * SPI for all {@link BatchEmitter} factories.
 * <p>
 * Instances of implementing classes are created by {@link org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider}
 * using {@link java.util.ServiceLoader}.
 * <p>
 * Given that multiple factories might be available in runtime:
 * <ul>
 * <li>{@link BatchEmitterFactory#accepts accepts()} can validate compatibility with {@link ClientObjectFactory}.</li>
 * <li>{@link BatchEmitterFactory#loadingOrder loadingOrder()} can determine which compatible factory takes precedence</li>
 * </ul>
 *
 * @param <T> return type
 */
public interface BatchEmitterFactory<T extends BatchEmitter> {

    int DEFAULT_LOADING_ORDER = 100;

    /**
     * Validates given {@link ClientObjectFactory} class
     *
     * @param clientObjectFactoryClass class implementing {@link ClientObjectFactory}
     * @return true if this factory can produce a {@link BatchEmitter} compatible with given {@link
     * ClientObjectFactory}, false otherwise
     */
    boolean accepts(Class<? extends ClientObjectFactory> clientObjectFactoryClass);

    /**
     * Determines loading priority. Factories with lower order SHOULD be loaded before factories with higher order
     *
     * @return loading order of this factory
     */
    default int loadingOrder() {
        return DEFAULT_LOADING_ORDER;
    }

    /**
     * Creates an instance of {@link BatchEmitter}
     *
     * @param batchSize           number of elements in a current batch that should trigger a delivery, regardless of
     *                            the deliveryInterval
     * @param deliveryInterval    number of millis between two time-triggered deliveries, regardless of the batchSize
     * @param clientObjectFactory client-specific objects provider
     * @param failoverPolicy      sink for failed batch items
     * @return T configured and {@link BatchEmitter}
     */
    T createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy);

}
