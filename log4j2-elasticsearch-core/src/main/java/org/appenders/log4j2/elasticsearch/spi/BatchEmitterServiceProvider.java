package org.appenders.log4j2.elasticsearch.spi;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * {@link BatchEmitterFactory} SPI loader.
 */
public class BatchEmitterServiceProvider {

    private static final Logger LOG = InternalLogging.getLogger();

    /**
     * Creates an instance of {@link BatchEmitter} using one of available {@link BatchEmitterFactory} services. A check
     * for compatibility of given {@link ClientObjectFactory} with available services is performed.
     * <p>
     * NOTE: Currently the first found and compatible {@link BatchEmitterFactory} is selected as the {@link
     * BatchEmitter} provider. This is subject to change after new config features are added in future releases (
     * priority-based selection will be available to provide more flexible extension capabilities).
     *
     * @param batchSize           number of elements in a current batch that should trigger a delivery, regardless of
     *                            the delivery interval value
     * @param deliveryInterval    number of millis between two time-triggered deliveries, regardless of the batch size
     *                            value
     * @param clientObjectFactory client-specific objects provider
     * @param failoverPolicy      sink for failed batch items
     * @return T configured {@link BatchEmitter}
     */
    public BatchEmitter createInstance(int batchSize,
                                       int deliveryInterval,
                                       ClientObjectFactory clientObjectFactory,
                                       FailoverPolicy failoverPolicy) {

        ServiceLoader<BatchEmitterFactory> loader = ServiceLoader.load(BatchEmitterFactory.class);
        Iterator<BatchEmitterFactory> it = loader.iterator();
        while (it.hasNext()) {
            BatchEmitterFactory factory = it.next();
            LOG.info("BatchEmitterFactory class found {}", factory.getClass().getName());
            if (factory.accepts(clientObjectFactory.getClass())) {
                LOG.info("Using {} as BatchEmitterFactoryProvider", factory);
                return factory.createInstance(batchSize, deliveryInterval, clientObjectFactory, failoverPolicy);
            }
        }

        throw new ConfigurationException(String.format("No compatible BatchEmitter implementations for %s found", clientObjectFactory.getClass().getName()));

    }
}
