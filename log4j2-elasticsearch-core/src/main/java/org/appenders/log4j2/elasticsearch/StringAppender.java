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

import java.util.function.Function;

/**
 * {@link BatchDelivery#add(String, Object)} call wrapper
 */
public class StringAppender implements ItemAppender<LogEvent> {

    private final BatchDelivery batchDelivery;
    private final Function<LogEvent, String> serializer;

    public StringAppender(BatchDelivery batchDelivery, Function<LogEvent, String> serializer) {
        this.batchDelivery = batchDelivery;
        this.serializer = serializer;
    }

    /**
     * Serializes given {@link LogEvent} to {@link String} and invokes {@link BatchDelivery#add(String, Object)}
     *
     * @param formattedIndexName delivery target
     * @param logEvent event to process
     */
    @Override
    public void append(String formattedIndexName, LogEvent logEvent) {
        batchDelivery.add(formattedIndexName, serializer.apply(logEvent));
    }

}
