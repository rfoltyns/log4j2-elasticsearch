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

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

import static org.appenders.log4j2.elasticsearch.JacksonSerializer.Builder.DEFAULT_JACKSON_MODULES;
import static org.appenders.log4j2.elasticsearch.JacksonSerializer.Builder.DEFAULT_MIX_INS;
import static org.appenders.log4j2.elasticsearch.JacksonSerializer.Builder.DEFAULT_VIRTUAL_PROPERTIES;
import static org.appenders.log4j2.elasticsearch.JacksonSerializer.Builder.DEFAULT_VIRTUAL_PROPERTY_FILTERS;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2 file-based configuration only. If you'd like to extend it, use {@link GenericItemSourceLayout} instead.
 */
@Plugin(name = JacksonJsonLayoutPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true, deferChildren = true)
public class JacksonJsonLayoutPlugin<R> extends GenericItemSourceLayout<Object, R> implements Layout<Serializable> {

    public static final String PLUGIN_NAME = "JacksonJsonLayout";

    public JacksonJsonLayoutPlugin(GenericItemSourceLayout.Builder<Object, R> builder) {
        super(builder.serializer, builder.itemSourceFactory);
    }

    @Override
    public Map<String, String> getContentFormat() {
        throw new UnsupportedOperationException("Content format not supported");
    }

    @Override
    public byte[] getFooter() {
        throw new UnsupportedOperationException("Footer not supported");
    }

    @Override
    public byte[] getHeader() {
        throw new UnsupportedOperationException("Header not supported");
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        throw new UnsupportedOperationException("Cannot return unwrapped byte array. Use toSerializable(LogEvent) instead");
    }

    @Override
    public ItemSource<R> toSerializable(LogEvent event) {
        return serialize(event);
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void encode(LogEvent source, ByteBufferDestination destination) {
        throw new UnsupportedOperationException(ByteBufferDestination.class.getSimpleName() + " not supported");
    }

    @PluginFactory
    public static <R> JacksonJsonLayoutPlugin<R> createJacksonJsonLayout(
            @PluginConfiguration final Configuration configuration,
            @PluginElement(ItemSourceFactory.ELEMENT_TYPE) final ItemSourceFactory<Object, R> itemSourceFactory,
            @PluginElement("JacksonMixIn") final JacksonMixIn[] mixins,
            @PluginElement("JacksonModule") final JacksonModule[] jacksonModules,
            @PluginElement("VirtualProperty") final VirtualProperty[] virtualProperties,
            @PluginElement("VirtualPropertyFilter") final VirtualPropertyFilter[] virtualPropertyFilters,
            @PluginBuilderAttribute("afterburner") final boolean useAfterburner,
            @PluginBuilderAttribute("singleThread")  final boolean singleThread
    ) {
        return new JacksonJsonLayoutPlugin<>(createLayout(
                itemSourceFactory,
                createSerializerBuilder()
                        .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()))
                        .withMixins(mixins.length == 0 ? DEFAULT_MIX_INS : mixins)
                        .withJacksonModules(jacksonModules.length == 0 ? DEFAULT_JACKSON_MODULES : jacksonModules)
                        .withVirtualProperties(virtualProperties.length == 0 ? DEFAULT_VIRTUAL_PROPERTIES : virtualProperties)
                        .withVirtualPropertyFilters(virtualPropertyFilters.length == 0 ? DEFAULT_VIRTUAL_PROPERTY_FILTERS : virtualPropertyFilters)
                        .withAfterburner(useAfterburner)
                        .withSingleThread(singleThread)
                )
        );

    }

    static <R> GenericItemSourceLayout.Builder<Object, R> createLayout(final ItemSourceFactory<Object, R> factory, JacksonSerializer.Builder<Object> serializer) {
        return new GenericItemSourceLayout.Builder<Object, R>()
                .withItemSourceFactory(factory == null ? StringItemSourceFactory.newBuilder().build() : factory)
                .withSerializer(serializer.build());
    }

    @NotNull
    static JacksonSerializer.Builder<Object> createSerializerBuilder() {
        return new JacksonSerializer.Builder<>();
    }

}
