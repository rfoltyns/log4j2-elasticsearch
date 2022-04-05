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


import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * {@link BatchEmitterFactory} SPI loader.
 */
public class BatchEmitterServiceProvider {

    private static final Comparator<BatchEmitterFactory> LOADING_ORDER = new NaturalLoadingOrder();

    private final Collection<Iterable<BatchEmitterFactory>> serviceLoaders;

    public BatchEmitterServiceProvider() {
        this(Arrays.asList(serviceLoader(Thread.currentThread().getContextClassLoader()),
                serviceLoader(BatchEmitterServiceProvider.class.getClassLoader())));
    }

    BatchEmitterServiceProvider(Collection<Iterable<BatchEmitterFactory>> serviceLoaders) {
        this.serviceLoaders = Collections.unmodifiableList(
                serviceLoaders.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

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

        final Set<BatchEmitterFactory> batchEmitterFactories = new TreeSet<>(LOADING_ORDER);

        for (Iterable<BatchEmitterFactory> serviceLoader : serviceLoaders) {

            batchEmitterFactories.addAll(getCompatibleFactories(
                    clientObjectFactory,
                    serviceLoader));

        }

        for (BatchEmitterFactory factory : batchEmitterFactories) {

            BatchEmitter batchEmitter = factory.createInstance(batchSize, deliveryInterval, clientObjectFactory, failoverPolicy);
            if (batchEmitter != null) {
                getLogger().info("Using {} as {}", factory.getClass().getName(), getClass().getSimpleName());
                return batchEmitter;
            }

        }

        throw new IllegalStateException(String.format(
                "No compatible BatchEmitter implementations for %s found",
                clientObjectFactory.getClass().getName()));
    }

    private List<BatchEmitterFactory> getCompatibleFactories(
            ClientObjectFactory clientObjectFactory,
            Iterable<BatchEmitterFactory> serviceLoader){

        final List<BatchEmitterFactory> factories = new ArrayList<>();

        for (BatchEmitterFactory factory : serviceLoader) {
            getLogger().info("{} class found {}", BatchEmitterFactory.class.getSimpleName(), factory.getClass().getName());
            if (factory.accepts(clientObjectFactory.getClass())) {
                factories.add(factory);
            }
        }

        return factories;
    }

    private static Iterable<BatchEmitterFactory> serviceLoader(ClassLoader classLoader) {
        return ServiceLoader.load(BatchEmitterFactory.class, classLoader);
    }

    private static class NaturalLoadingOrder implements Comparator<BatchEmitterFactory> {

        @Override
        public int compare(BatchEmitterFactory o1, BatchEmitterFactory o2) {

            if (o1.loadingOrder() == o2.loadingOrder()) {
                return 0;
            }

            return o1.loadingOrder() > o2.loadingOrder() ? 1 : -1;

        }

    }
}
