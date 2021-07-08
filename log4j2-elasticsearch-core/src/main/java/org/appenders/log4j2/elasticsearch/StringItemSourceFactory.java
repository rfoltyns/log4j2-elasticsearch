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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import static org.appenders.core.logging.InternalLogging.getLogger;

@Plugin(name = StringItemSourceFactory.PLUGIN_NAME, category = Node.CATEGORY, elementType = ItemSourceFactory.ELEMENT_TYPE, printObject = true)
public class StringItemSourceFactory<T> implements ItemSourceFactory<T, String> {

    static final String PLUGIN_NAME = "StringItemSourceAppender";

    private volatile State state = State.STOPPED;

    StringItemSourceFactory() {
        // prefer builder
    }

    @Override
    public final boolean isBuffered() {
        return false;
    }

    /**
     *
     * @param event processed item
     * @param objectWriter writer to be used to serialize given item
     * @return null, if {@link JsonProcessingException} occured, {@link ItemSource} otherwise
     */
    @Override
    public ItemSource create(Object event, ObjectWriter objectWriter) {
        try {
            return new StringItemSource(objectWriter.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            // dev's error. returning null to resurface
            getLogger().error("Cannot write item source: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ItemSource<String> create(T source, Serializer<T> serializer) {
        try {
            return new StringItemSource(serializer.writeAsString(source));
        } catch (Exception e) {
            // dev's error. returning null to resurface
            getLogger().error("Cannot write item source: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ItemSource createEmptySource() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " cannot create empty source. Use buffer-based classes instead");
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<StringItemSourceFactory> {

        @Override
        public StringItemSourceFactory build() {
            return new StringItemSourceFactory<>();
        }

    }


    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {
        state = State.STARTED;
    }

    @Override
    public void stop() {
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
