package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.metrics.Measured;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

/**
 * Wraps {@link ItemSource} creation
 */
public class GenericItemSourceLayout<T, R> implements ItemSourceLayout<T, R>, LifeCycle, Measured {

    private volatile State state = State.STOPPED;

    final Serializer<T> serializer;
    final ItemSourceFactory<T, R> itemSourceFactory;

    public GenericItemSourceLayout(final Serializer<T> serializer, final ItemSourceFactory<T, R> itemSourceFactory) {
        this.serializer = serializer;
        this.itemSourceFactory = itemSourceFactory;
    }

    @Override
    public final ItemSource<R> serialize(final T source) {
        return itemSourceFactory.create(source, serializer);
    }

    @Override
    public void register(final MetricsRegistry registry) {
        Measured.of(itemSourceFactory).register(registry);
    }

    @Override
    public void deregister() {
        Measured.of(itemSourceFactory).deregister();
    }

    public static class Builder<T, R> {

        protected Serializer<T> serializer;
        protected ItemSourceFactory<T, R> itemSourceFactory;

        public GenericItemSourceLayout<T, R> build() {

            if (serializer == null) {
                throw new IllegalArgumentException("No Serializer provided for " + GenericItemSourceLayout.class.getSimpleName());
            }

            if (itemSourceFactory == null) {
                throw new IllegalArgumentException("No ItemSourceFactory provided for " + GenericItemSourceLayout.class.getSimpleName());
            }

            return new GenericItemSourceLayout<>(
                    serializer,
                    itemSourceFactory
            );
        }

        /**
         * @param serializer {@link ItemSource} producer
         * @return this
         */
        public Builder<T, R> withSerializer(final Serializer<T> serializer) {
            this.serializer = serializer;
            return this;
        }

        /**
         * @param itemSourceFactory {@link ItemSource} producer
         * @return this
         */
        public Builder<T, R> withItemSourceFactory(final ItemSourceFactory<T, R> itemSourceFactory) {
            this.itemSourceFactory = itemSourceFactory;
            return this;
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {
        itemSourceFactory.start();
        state = State.STARTED;
    }

    @Override
    public void stop() {

        if (!itemSourceFactory.isStopped()) {
            itemSourceFactory.stop();
        }

        state = State.STOPPED;

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

}
