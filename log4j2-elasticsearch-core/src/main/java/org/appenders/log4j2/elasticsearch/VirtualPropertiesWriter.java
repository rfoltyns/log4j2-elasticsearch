package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * This custom FasterXML Jackson {@code com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter}
 * allows to append {@link VirtualProperty}-ies at the end of JSON output
 *
 * @see org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLogEventJacksonJsonMixIn
 * @see org.appenders.log4j2.elasticsearch.JacksonJsonLayout.Builder
 */
public class VirtualPropertiesWriter extends VirtualBeanPropertyWriter {

    protected final VirtualProperty[] virtualProperties;
    protected final ValueResolver valueResolver;
    protected final VirtualPropertyFilter[] filters;

    VirtualPropertiesWriter() {
        throw new UnsupportedOperationException(String.format(
                "Invalid use of %s. Use virtualProperties based constructors",
                VirtualPropertiesWriter.class.getSimpleName())
        );
    }

    /**
     * Initializes writer with no filters
     *
     * @param virtualProperties {@link VirtualProperty}-ies to append
     * @param valueResolver {@link ValueResolver} dynamic variables resolver
     */
    public VirtualPropertiesWriter(VirtualProperty[] virtualProperties, ValueResolver valueResolver) {
        this(virtualProperties, valueResolver, new VirtualPropertyFilter[0]);
    }

    /**
     * @param virtualProperties {@link VirtualProperty}-ies to append
     * @param valueResolver {@link ValueResolver} dynamic variables resolver
     * @param filters {@link VirtualPropertyFilter} inclusion filters. Allow to include/exclude
     * {@link VirtualProperty} by name or value returned by {@link ValueResolver}
     */
    public VirtualPropertiesWriter(VirtualProperty[] virtualProperties, ValueResolver valueResolver, VirtualPropertyFilter[] filters) {
        this.virtualProperties = virtualProperties;
        this.valueResolver = valueResolver;
        this.filters = filters;
    }

    /**
     * This constructor should not be invoked directly and should only be used within
     * {@link #withConfig(MapperConfig, AnnotatedClass, BeanPropertyDefinition, JavaType)} call.
     *
     * @param propDef property definition created by {@code by com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector}
     * @param annotations contains only @JsonAppend at the moment
     * @param type {@link VirtualProperty}[]
     * @param virtualProperties {@link VirtualProperty}-ies to append
     * @param valueResolver {@link ValueResolver} dynamic variables resolver
     * @param filters {@link VirtualPropertyFilter} inclusion filters. Allow to include/exclude
     * {@link VirtualProperty} by name or value returned by {@link ValueResolver}
     */
    VirtualPropertiesWriter(
            BeanPropertyDefinition propDef,
            Annotations annotations,
            JavaType type,
            VirtualProperty[] virtualProperties,
            ValueResolver valueResolver,
            VirtualPropertyFilter[] filters
    ) {
        super(propDef, annotations, type);
        this.virtualProperties = virtualProperties;
        this.valueResolver = valueResolver;
        this.filters = filters;
    }

    @Override
    protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) {
        throw new UnsupportedOperationException("Should not be used with this implementation. Use serializeAsField() to write value directly.");
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {

        for (int i = 0; i < virtualProperties.length; i++) {

            VirtualProperty property = virtualProperties[i];

            String resolved = valueResolver.resolve(property);
            if (isExcluded(property, resolved)) {
                continue;
            }

            gen.writeFieldName(property.getName());
            if (property.isWriteRaw()) {
                gen.writeRawValue(resolved);
            } else {
                gen.writeString(resolved);
            }

        }
    }

    private boolean isExcluded(VirtualProperty property, String resolved) {

        for (int i = 0; i < filters.length; i++) {
            if (!filters[i].isIncluded(property.getName(), resolved)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VirtualPropertiesWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass, BeanPropertyDefinition propDef, JavaType type) {
        return new VirtualPropertiesWriter(
                propDef,
                new AnnotationCollector.OneAnnotation(
                        declaringClass.getRawType(),
                        declaringClass.getAnnotations().get(JsonAppend.class)
                ),
                type,
                virtualProperties,
                valueResolver,
                filters
        );
    }

    @Override
    public void fixAccess(SerializationConfig config) {
        // noop - fast path as super.getMember() returns null anyway
    }

}
