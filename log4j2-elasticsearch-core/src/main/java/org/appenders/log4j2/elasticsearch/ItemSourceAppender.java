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

import org.apache.logging.log4j.core.LogEvent;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;

import java.util.function.Function;

/**
 * {@link BatchDelivery#add(String, ItemSource)} call wrapper
 */
public class ItemSourceAppender implements ItemAppender<LogEvent> {

    static final Logger LOG = InternalLogging.getLogger();

    private volatile State state = State.STOPPED;

    private final BatchDelivery batchDelivery;
    private final Function<LogEvent, ItemSource> serializer;

    public ItemSourceAppender(BatchDelivery batchDelivery, Function<LogEvent, ItemSource> serializer) {
        this.batchDelivery = batchDelivery;
        this.serializer = serializer;
    }

    /**
     * Serializes given {@link LogEvent} to {@link ItemSource} and invokes {@link BatchDelivery#add(String, ItemSource)}
     *
     * @param formattedIndexName delivery target
     * @param event event to process
     */
    @Override
    public final void append(String formattedIndexName, LogEvent event) {
        batchDelivery.add(formattedIndexName, serializer.apply(event));
    }

    @Override
    public void start() {
        batchDelivery.start();
        state = State.STARTED;

        LOG.debug("{} started", getClass().getSimpleName());

    }

    @Override
    public void stop() {

        LOG.debug("Stopping {}", getClass().getSimpleName());

        if (!batchDelivery.isStopped()) {
            batchDelivery.stop();
        }
        state = State.STOPPED;

        LOG.debug("{} stopped", getClass().getSimpleName());

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
