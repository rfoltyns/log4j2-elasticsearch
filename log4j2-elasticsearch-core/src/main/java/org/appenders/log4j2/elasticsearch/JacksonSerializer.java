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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLog4j2JsonModule;
import org.appenders.st.jackson.SingleThreadJsonFactory;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class JacksonSerializer<T> implements Serializer<T> {

    protected final ObjectWriter objectWriter;

    public JacksonSerializer(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Override
    public void write(OutputStream outputStream, T source) throws Exception {
        objectWriter.writeValue(outputStream, source);
    }

    @Override
    public String writeAsString(T event) throws Exception {
        return objectWriter.writeValueAsString(event);
    }

    public static class Builder<T> {

        /**
         * Default: {@code [ExtendedLog4j2JsonModule]}
         */
        public static final JacksonModule[] DEFAULT_JACKSON_MODULES = new JacksonModule[]{
                new ExtendedLog4j2JsonModule()
        };

        /**
         * Default: {@code []}
         */

        public static final VirtualProperty[] DEFAULT_VIRTUAL_PROPERTIES = new VirtualProperty[0];

        /**
         * Default: {@code []}
         */

        public static final VirtualPropertyFilter[] DEFAULT_VIRTUAL_PROPERTY_FILTERS = new VirtualPropertyFilter[0];

        /**
         * Default: {@code []}
         */
        public static final JacksonMixIn[] DEFAULT_MIX_INS = new JacksonMixIn[0];

        private JacksonMixIn[] mixins = DEFAULT_MIX_INS;
        private JacksonModule[] jacksonModules = DEFAULT_JACKSON_MODULES;
        private VirtualProperty[] virtualProperties = DEFAULT_VIRTUAL_PROPERTIES;
        private VirtualPropertyFilter[] virtualPropertyFilters = DEFAULT_VIRTUAL_PROPERTY_FILTERS;
        private ValueResolver valueResolver = ValueResolver.NO_OP;
        private boolean useAfterburner;
        private boolean singleThread;

        public Serializer<T> build() {
            return new JacksonSerializer<>(createConfiguredWriter());
        }

        protected ObjectWriter createConfiguredWriter() {

            final ObjectMapper objectMapper = createDefaultObjectMapper();

            return configureModules(objectMapper, getJacksonModules())
                    .configureMixins(objectMapper, Arrays.asList(mixins))
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

            return new JacksonSerializer.Builder.JacksonModulesList(linkedList);

        }

        protected ObjectWriter createConfiguredWriter(ObjectMapper objectMapper) {
            return objectMapper.writer(new MinimalPrettyPrinter());
        }

        protected JacksonSerializer.Builder<T> configureModules(ObjectMapper objectMapper, Collection<JacksonModule> modules) {

            for (JacksonModule module : modules) {
                module.applyTo(objectMapper);
            }

            return this;
        }

        protected JacksonSerializer.Builder<T> configureMixins(ObjectMapper objectMapper, List<JacksonMixIn> mixins) {

            for (JacksonMixIn mixin : mixins) {
                objectMapper.addMixIn(mixin.getTargetClass(), mixin.getMixInClass());
            }
            return this;

        }

        protected JacksonSerializer.Builder<T> configureVirtualProperties(ObjectMapper objectMapper, VirtualProperty[] virtualProperties, VirtualPropertyFilter[] virtualPropertyFilters) {

            final ValueResolver valueResolver = createValueResolver();

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
            return valueResolver;
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
         * Allows to customize serialization
         *
         * @param mixins mixins to be applied
         * @return this
         */
        public JacksonSerializer.Builder<T> withMixins(JacksonMixIn... mixins) {
            this.mixins = mixins;
            return this;
        }

        /**
         * Allows to append properties to serialized objects.
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
        public JacksonSerializer.Builder<T> withVirtualProperties(VirtualProperty... virtualProperties) {
            this.virtualProperties = virtualProperties;
            return this;
        }

        /**
         * Allows to define inclusion/exclusion filters for {@link VirtualProperty}-ies.
         *
         * @param virtualPropertyFilters filters to be applied to each configured {@link VirtualProperty}
         * @return this
         */
        public JacksonSerializer.Builder<T> withVirtualPropertyFilters(VirtualPropertyFilter[] virtualPropertyFilters) {
            this.virtualPropertyFilters = virtualPropertyFilters;
            return this;
        }

        /**
         * Allows to configure {@link com.fasterxml.jackson.module.afterburner.AfterburnerModule} - (de)serialization optimizer
         *
         * @param useAfterburner if true, {@link com.fasterxml.jackson.module.afterburner.AfterburnerModule} will be used, false otherwise
         * @return this
         */
        public JacksonSerializer.Builder<T> withAfterburner(boolean useAfterburner) {
            this.useAfterburner = useAfterburner;
            return this;
        }

        /**
         * Allows to configure {@link SingleThreadJsonFactory}
         *
         * NOTE: Use ONLY when {@link GenericItemSourceLayout#serialize(Object)}
         * are called exclusively by a one thread at a time, e.g. with AsyncLogger
         *
         * @param singleThread if true, {@link SingleThreadJsonFactory} will be used to create serializers,
         *                    otherwise {@code com.fasterxml.jackson.core.JsonFactory} will be used
         * @return this
         */
        public JacksonSerializer.Builder<T> withSingleThread(boolean singleThread) {
            this.singleThread = singleThread;
            return this;
        }

        /**
         * Allow to configure additional {@code com.fasterxml.jackson.databind.Module} implementations
         *
         * @param modules Jackson modules to register on {@link #build()}
         * @return this
         */
        public JacksonSerializer.Builder<T> withJacksonModules(JacksonModule... modules) {
            this.jacksonModules = Stream.of(modules)
                    .toArray(JacksonModule[]::new);
            return this;
        }

        /**
         * Allows to configure value resolver
         *
         * @param valueResolver value resolver to use
         * @return this
         */
        public JacksonSerializer.Builder<T> withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
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

}
