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
 * @see org.apache.logging.log4j.core.jackson.LogEventJacksonJsonMixIn
 * @see org.appenders.log4j2.elasticsearch.JacksonJsonLayout.Builder
 */
public class VirtualPropertiesWriter extends VirtualBeanPropertyWriter {

    protected final VirtualProperty[] virtualProperties;
    protected final ValueResolver valueResolver;

    VirtualPropertiesWriter() {
        throw new UnsupportedOperationException(String.format(
                "Invalid use of %s. Use virtualProperties based constructors",
                VirtualPropertiesWriter.class.getSimpleName())
        );
    }

    public VirtualPropertiesWriter(VirtualProperty[] virtualProperties, ValueResolver valueResolver) {
        this.virtualProperties = virtualProperties;
        this.valueResolver = valueResolver;
    }

    public VirtualPropertiesWriter(
            BeanPropertyDefinition propDef,
            Annotations annotations,
            JavaType type,
            VirtualProperty[] virtualProperties,
            ValueResolver valueResolver
    ) {
        super(propDef, annotations, type);
        this.virtualProperties = virtualProperties;
        this.valueResolver = valueResolver;
    }

    @Override
    protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) {
        throw new UnsupportedOperationException("Should not be used with this implementation. Use serializeAsField() to write value directly.");
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        if (virtualProperties.length > 0) {
            for (int i = 0; i < virtualProperties.length; i++) {
                gen.writeFieldName(virtualProperties[i].getName());
                gen.writeString(valueResolver.resolve(virtualProperties[i]));
            }
        }
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
                valueResolver
        );
    }

    @Override
    public void fixAccess(SerializationConfig config) {
        // noop - fast path as super.getMember() returns null anyway
    }

}
