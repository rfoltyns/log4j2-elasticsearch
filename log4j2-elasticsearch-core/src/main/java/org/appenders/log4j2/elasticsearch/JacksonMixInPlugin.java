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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.util.LoaderUtil;

@Plugin(name = JacksonMixInPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = JacksonMixInPlugin.ELEMENT_TYPE, printObject = true)
public class JacksonMixInPlugin extends JacksonMixIn {

    public static final String PLUGIN_NAME = "JacksonMixIn";
    public static final String ELEMENT_TYPE = "jacksonMixIn";

    protected JacksonMixInPlugin(Class targetClass, Class mixInClass) {
        super(targetClass, mixInClass);
    }

    @PluginBuilderFactory
    public static JacksonMixInPlugin.Builder newBuilder() {
        return new JacksonMixInPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JacksonMixInPlugin> {

        @PluginBuilderAttribute("targetClass")
        private String targetClassName;

        @PluginBuilderAttribute("mixInClass")
        private String mixInClassName;

        @Override
        public JacksonMixInPlugin build() {

            Class targetClass = loadClass(targetClassName, "targetClass");
            Class mixInClass = loadClass(mixInClassName, "mixInClass");

            return new JacksonMixInPlugin(targetClass, mixInClass);

        }

        private Class loadClass(String className, String argName) {

            if (className == null) {
                throw new ConfigurationException(String.format("No %s provided for %s", argName, JacksonMixInPlugin.PLUGIN_NAME));
            }

            try {
                return LoaderUtil.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(String.format("Cannot load %s: %s for %s", argName, className, JacksonMixInPlugin.PLUGIN_NAME));
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
