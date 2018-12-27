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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.util.LoaderUtil;

@Plugin(name = JacksonMixIn.PLUGIN_NAME, category = Node.CATEGORY, elementType = JacksonMixIn.ELEMENT_TYPE, printObject = true)
public class JacksonMixIn {

    public static final String PLUGIN_NAME = "JacksonMixIn";
    public static final String ELEMENT_TYPE = "jacksonMixIn";

    private final Class targetClass;
    private final Class mixInClass;

    protected JacksonMixIn(Class targetClass, Class mixInClass) {
        this.targetClass = targetClass;
        this.mixInClass = mixInClass;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public Class getMixInClass() {
        return mixInClass;
    }

    @PluginBuilderFactory
    public static JacksonMixIn.Builder newBuilder() {
        return new JacksonMixIn.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JacksonMixIn> {

        @PluginBuilderAttribute("targetClass")
        private String targetClassName;

        @PluginBuilderAttribute("mixInClass")
        private String mixInClassName;

        @Override
        public JacksonMixIn build() {

            Class targetClass = loadClass(targetClassName, "targetClass");
            Class mixInClass = loadClass(mixInClassName, "mixInClass");

            return new JacksonMixIn(targetClass, mixInClass);

        }

        private Class loadClass(String className, String argName) {

            if (className == null) {
                throw new ConfigurationException(String.format("No %s provided for %s", argName, JacksonMixIn.PLUGIN_NAME));
            }

            try {
                return LoaderUtil.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(String.format("Cannot load %s: %s for %s", argName, className, JacksonMixIn.PLUGIN_NAME));
            }

        }

        public Builder withTargetClass(String targetClass) {
            this.targetClassName = targetClass;
            return this;
        }

        public Builder withMixInClass(String mixInClass) {
            this.mixInClassName = mixInClass;
            return this;
        }

    }
}
