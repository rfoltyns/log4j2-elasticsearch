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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.jackson.ExtendedLog4j2JsonModule;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.message.Message;

import java.util.Arrays;
import java.util.List;

/**
 * Allows to customize serialization of incoming events
 */
@Plugin(name = JacksonJsonLayout.PLUGIN_NAME, category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class JacksonJsonLayout extends AbstractLayout<ItemSource> implements ItemSourceLayout {

    public static final String PLUGIN_NAME = "JacksonJsonLayout";

    private final ObjectWriter objectWriter;
    private final ItemSourceFactory itemSourceFactory;

    protected JacksonJsonLayout(Configuration config, ObjectWriter configuredWriter, ItemSourceFactory itemSourceFactory) {
        super(config, null, null);
        this.objectWriter = configuredWriter;
        this.itemSourceFactory = itemSourceFactory;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        throw new UnsupportedOperationException("Cannot return unwrapped byte array. Use ItemSource based API");
    }

    @Override
    public final ItemSource toSerializable(LogEvent event) {
        return serialize(event);
    }

    @Override
    public final ItemSource serialize(LogEvent event) {
        return itemSourceFactory.create(event, objectWriter);
    }

    @Override
    public final ItemSource serialize(Message message) {
        return itemSourceFactory.create(message, objectWriter);
    }

    @PluginBuilderFactory
    public static JacksonJsonLayout.Builder newBuilder() {
        return new JacksonJsonLayout.Builder();
    }

    public static class Builder extends org.apache.logging.log4j.core.layout.AbstractLayout.Builder<JacksonJsonLayout.Builder> implements org.apache.logging.log4j.core.util.Builder<JacksonJsonLayout> {

        /**
         * Default: {@link StringItemSourceFactory}
         */
        static final ItemSourceFactory DEFAULT_SOURCE_FACTORY = StringItemSourceFactory.newBuilder().build();

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        private ItemSourceFactory itemSourceFactory = DEFAULT_SOURCE_FACTORY;

        @PluginElement(JacksonMixIn.ELEMENT_TYPE)
        private JacksonMixIn[] mixins = new JacksonMixIn[0];

        @PluginBuilderAttribute("afterburner")
        private boolean useAfterburner;

        @Override
        public JacksonJsonLayout build() {
            return new JacksonJsonLayout(
                    getConfiguration(),
                    createConfiguredWriter(Arrays.asList(mixins)),
                    itemSourceFactory
            );
        }

        protected ObjectWriter createConfiguredWriter(List<JacksonMixIn> mixins) {

            ObjectMapper objectMapper = createDefaultObjectMapper();
            objectMapper.registerModule(new ExtendedLog4j2JsonModule());

            if (useAfterburner) {
                // com.fasterxml.jackson.module:jackson-module-afterburner required here
                new JacksonAfterburnerModuleConfigurer().configure(objectMapper);
            }

            for (JacksonMixIn mixin : mixins) {
                objectMapper.addMixIn(mixin.getTargetClass(), mixin.getMixInClass());
            }

            return objectMapper.writer(new MinimalPrettyPrinter());
        }

        protected ObjectMapper createDefaultObjectMapper() {
            return new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .configure(SerializationFeature.CLOSE_CLOSEABLE, false);
        }

        /**
         * @param itemSourceFactory {@link ItemSource} producer
         * @return this
         */
        public Builder withItemSourceFactory(ItemSourceFactory itemSourceFactory) {
            this.itemSourceFactory = itemSourceFactory;
            return this;
        }

        /**
         * Allows to customize {@link LogEvent} and {@link Message} serialization,
         * including user-provided {@link org.apache.logging.log4j.message.ObjectMessage}
         *
         * @param mixins mixins to be applied
         * @return this
         */
        public Builder withMixins(JacksonMixIn... mixins) {
            this.mixins = mixins;
            return this;
        }

        /**
         * Allows to configure {@link AfterburnerModule} - (de)serialization optimizer
         *
         * @param useAfterburner if true, {@link AfterburnerModule} will be used, false otherwise
         * @return this
         */
        public Builder withAfterburner(boolean useAfterburner) {
            this.useAfterburner = useAfterburner;
            return this;
        }
    }
}
