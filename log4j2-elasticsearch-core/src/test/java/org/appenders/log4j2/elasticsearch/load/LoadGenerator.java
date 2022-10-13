package org.appenders.log4j2.elasticsearch.load;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadGenerator {
    private final Queue<ConditionalLoop> producers;
    private final LoadProducerFactory producerFactory;
    private final AtomicInteger numberOfProducers;
    private final AtomicInteger producerSleepMillis;
    private final AtomicInteger producerBatchSize;
    private final ThrottlingPolicy throttlingPolicy;

    public LoadGenerator(final Queue<ConditionalLoop> initialProducers,
                         final LoadProducerFactory producerFactory,
                         final ThrottlingPolicy throttlingPolicy,
                         final int numberOfProducers,
                         final int producerSleepMillis,
                         final int producerBatchSize) {
        this.producers = initialProducers;
        this.producerFactory = producerFactory;
        this.throttlingPolicy = throttlingPolicy;
        this.numberOfProducers = new AtomicInteger(numberOfProducers);
        this.producerSleepMillis = new AtomicInteger(producerSleepMillis);
        this.producerBatchSize = new AtomicInteger(producerBatchSize);
    }

    public int reconfigure(final double currentLoad, final int limitPerSec) {

        if (currentLoad <= 0) {
            return 0;
        }

        final int sleepMillis = producerSleepMillis.get();

        double targetLoad = throttlingPolicy.throttle(currentLoad, limitPerSec);

        final int newSleepMillis = (int) (sleepMillis * currentLoad);
        producerSleepMillis.set(Math.max(newSleepMillis, 5)); // 5ms for now. That's 200 runs per second.

        while (projectedLoad(limitPerSec) > 1.05d) {
            producerSleepMillis.incrementAndGet();
        }

        if (projectedLoad(limitPerSec) < 1.0) {

            final int batchSize = producerBatchSize.get();
            final int producers = numberOfProducers.get();

            if (batchSize != 100) { // 100 logs per call at this point is 20000/sec per producer at targetLoad == 1
                final int newBatchSize = (int) (batchSize / targetLoad);
                producerBatchSize.set(Math.min(newBatchSize, 100));
            } else if (producers < 32) {

                long projectedRate = projectedCountPerMillis(1000);

                final int moreProducers = (int) (limitPerSec / projectedRate);
                numberOfProducers.addAndGet(moreProducers);

                final int lessProducers = (int) (projectedRate / limitPerSec);
                numberOfProducers.addAndGet(-lessProducers);
            }

        }

        while (producers.size() > numberOfProducers.get()) {
            producers.remove().stopLoop();
        }
        while (producers.size() < numberOfProducers.get()) {
            producers.add(createInternal());
        }
        if (currentLoad - projectedLoad(limitPerSec) > 0.05) {
            System.out.println("Stopping over-producing loop");
            producers.remove().stopLoop();
        }

        return numberOfProducers.get();

    }

    private double projectedLoad(int limitPerSec) {
        return projectedCountPerMillis(1000) / (double) limitPerSec;
    }

    public long projectedCountPerMillis(long millis) {
        return  (millis / producerSleepMillis.get()) * numberOfProducers.get() * producerBatchSize.get();
    }

    public int getNumberOfProducers() {
        return producers.size();
    }

    public int getProducerBatchSize() {
        return producerBatchSize.get();
    }

    public int getProducerSleepMillis() {
        return producerSleepMillis.get();
    }

    public void start() {

        while (producers.size() < getNumberOfProducers()) {
            producers.add(createInternal());
        }

    }

    private ConditionalLoop createInternal() {
        return producerFactory.createProducer(true, this::getProducerBatchSize, this::getProducerSleepMillis);
    }

}
