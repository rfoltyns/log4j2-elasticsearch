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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.jackson.ExtendedLog4j2JsonModule;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.message.Message;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Allows to customize serialization of incoming events. See {@link Builder} API docs for more details
 */
@Plugin(name = JacksonJsonLayout.PLUGIN_NAME, category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class JacksonJsonLayout extends AbstractLayout<ItemSource> implements ItemSourceLayout, LifeCycle {

    public static final String PLUGIN_NAME = "JacksonJsonLayout";

    private volatile State state = State.STOPPED;

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

        /**
         * Default: {@code [ExtendedLog4j2JsonModule]}
         */
        static final JacksonModule[] DEFAULT_JACKSON_MODULES = new JacksonModule[]{
                new ExtendedLog4j2JsonModule()
        };

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        private ItemSourceFactory itemSourceFactory = DEFAULT_SOURCE_FACTORY;

        @PluginElement(JacksonMixIn.ELEMENT_TYPE)
        private JacksonMixIn[] mixins = new JacksonMixIn[0];

        @PluginElement("JacksonModule")
        private JacksonModule[] jacksonModules = DEFAULT_JACKSON_MODULES;

        @PluginElement("VirtualProperty")
        private VirtualProperty[] virtualProperties = new VirtualProperty[0];

        @PluginElement("VirtualPropertyFilter")
        private VirtualPropertyFilter[] virtualPropertyFilters = new VirtualPropertyFilter[0];

        @PluginBuilderAttribute("afterburner")
        private boolean useAfterburner;

        @PluginBuilderAttribute("singleThread")
        private boolean singleThread;

        @Override
        public JacksonJsonLayout build() {

            if (getConfiguration() == null) {
                throw new ConfigurationException("No Configuration instance provided for " + PLUGIN_NAME);
            }

            return new JacksonJsonLayout(
                    getConfiguration(),
                    createConfiguredWriter(),
                    itemSourceFactory
            );
        }

        protected ObjectWriter createConfiguredWriter() {
            return createConfiguredWriter(Arrays.asList(mixins)); // this method can be inlined in 1.6
        }

        /**
         * @deprecated As of 1.6, this method will be removed, use {@link #createConfiguredWriter()} instead
          */
        @Deprecated
        protected ObjectWriter createConfiguredWriter(List<JacksonMixIn> mixins) {

            ObjectMapper objectMapper = createDefaultObjectMapper();

            return configureModules(objectMapper, getJacksonModules())
                    .configureMixins(objectMapper, mixins)
                    .configureVirtualProperties(objectMapper, virtualProperties, virtualPropertyFilters)
                    .createConfiguredWriter(objectMapper);

        }

        private Collection<JacksonModule> getJacksonModules() {

            LinkedList<JacksonModule> linkedList = new LinkedList<>(Arrays.asList(DEFAULT_JACKSON_MODULES));
            linkedList.addAll(Arrays.asList(this.jacksonModules));

            if (useAfterburner) {
                // com.fasterxml.jackson.module:jackson-module-afterburner required here
                linkedList.add(new JacksonAfterburnerModuleConfigurer());
            }

            return new JacksonModulesList(linkedList);

        }

        protected ObjectWriter createConfiguredWriter(ObjectMapper objectMapper) {
            return objectMapper.writer(new MinimalPrettyPrinter());
        }

        protected Builder configureModules(ObjectMapper objectMapper, Collection<JacksonModule> modules) {

            for (JacksonModule module : modules) {
                module.applyTo(objectMapper);
            }

            return this;
        }

        protected Builder configureMixins(ObjectMapper objectMapper, List<JacksonMixIn> mixins) {

            for (JacksonMixIn mixin : mixins) {
                objectMapper.addMixIn(mixin.getTargetClass(), mixin.getMixInClass());
            }
            return this;

        }

        protected Builder configureVirtualProperties(ObjectMapper objectMapper, VirtualProperty[] virtualProperties, VirtualPropertyFilter[] virtualPropertyFilters) {

            ValueResolver valueResolver = createValueResolver();

            for (VirtualProperty property : virtualProperties) {
                if (!property.isDynamic()) {
                    property.setValue(valueResolver.resolve(property.getValue()));
                }
            }

            SerializationConfig customConfig = objectMapper.getSerializationConfig()
                    .with(new JacksonHandlerInstantiator(
                            virtualProperties,
                            valueResolver,
                            virtualPropertyFilters
                    ));

            objectMapper.setConfig(customConfig);

            return this;
        }

        /**
         * @return resolver used when {@link VirtualProperty}(-ies) configured
         */
        protected ValueResolver createValueResolver() {
            return new Log4j2Lookup(getConfiguration().getStrSubstitutor());
        }

        protected ObjectMapper createDefaultObjectMapper() {
            return new ExtendedObjectMapper(createJsonFactory())
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .configure(SerializationFeature.CLOSE_CLOSEABLE, false);
        }

        protected JsonFactory createJsonFactory() {
            if (singleThread) {
                return new SingleThreadJsonFactory();
            }
            return new JsonFactory();
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
         * Allows to append properties to serialized {@link LogEvent} and {@link Message}.
         *
         * Non-dynamic properties ({@code VirtualProperty#dynamic == false}) will be resolved on {@link #build()} call.
         *
         * Dynamic properties ({@code VirtualProperty#isDynamic == true}) will NOT be resolved on {@link #build()} call and resolution will be deferred to underlying {@link VirtualPropertiesWriter}.
         *
         * Similar to Log4j2 {@code KeyValuePair}.
         *
         * @param virtualProperties properties to be appended to JSON output
         * @return this
         */
        public Builder withVirtualProperties(VirtualProperty... virtualProperties) {
            this.virtualProperties = virtualProperties;
            return this;
        }

        /**
         * Allows to define inclusion/exclusion filters for {@link VirtualProperty}-ies.
         *
         * @param virtualPropertyFilters filters to be applied to each configured {@link VirtualProperty}
         * @return this
         */
        public Builder withVirtualPropertyFilters(VirtualPropertyFilter[] virtualPropertyFilters) {
            this.virtualPropertyFilters = virtualPropertyFilters;
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

        /**
         * Allows to configure {@link SingleThreadJsonFactory}
         *
         * NOTE: Use ONLY when {@link JacksonJsonLayout#serialize(LogEvent)}/{@link JacksonJsonLayout#serialize(Message)}
         * are called exclusively by a one thread at a time, e.g. with AsyncLogger
         *
         * @param singleThread if true, {@link SingleThreadJsonFactory} will be used to create serializers,
         *                    otherwise {@code com.fasterxml.jackson.core.JsonFactory} will be used
         * @return this
         */
        public Builder withSingleThread(boolean singleThread) {
            this.singleThread = singleThread;
            return this;
        }

        /**
         * Allow to configure additional {@code com.fasterxml.jackson.databind.Module} implementations
         *
         * @param modules Jackson modules to register on {@link #build()}
         * @return this
         */
        public Builder withJacksonModules(JacksonModule... modules) {
            this.jacksonModules = Stream.of(modules).flatMap(Stream::of)
                    .toArray(JacksonModule[]::new);
            return this;
        }

        /**
         * First-comes, first-served {@link JacksonModule} linked list. Ensures that no more than one element of specific type is present.
         */
        private static class JacksonModulesList extends LinkedHashSet<JacksonModule> {

            public JacksonModulesList(LinkedList<JacksonModule> linkedList) {
                super(linkedList);
            }

            /**
             * Adds given {@link JacksonModule} to the list if element of the same type is not already present.
             *
             * @param jacksonModule {@link JacksonModule} to add
             * @return <i>true</i>, if element was added, <i>false</i> otherwise
             */
            @Override
            public boolean add(JacksonModule jacksonModule) {

                if (this.contains(jacksonModule)) {
                    return false;
                }

                return super.add(jacksonModule);

            }

            /**
             * Checks if this list contains an element with the same class name.
             *
             * @param o element to check
             * @return <i>true</i> if element is present, <i>false</i> otherwise
             */
            @Override
            public boolean contains(Object o) {

                for (JacksonModule jacksonModule : this) {
                    if (jacksonModule.getClass().getName().equals(o.getClass().getName())) {
                        return true;
                    }
                }

                return false;

            }

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
