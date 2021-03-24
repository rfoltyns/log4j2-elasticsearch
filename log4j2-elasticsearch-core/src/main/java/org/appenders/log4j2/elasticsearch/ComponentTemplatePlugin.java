package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;


@Plugin(name = ComponentTemplatePlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class ComponentTemplatePlugin extends ComponentTemplate {

    public static final String PLUGIN_NAME = "ComponentTemplate";

    /**
     * @param name Component template name
     * @param source Component template document
     */
    protected ComponentTemplatePlugin(String name, String source) {
        super(name, source);
    }

    @PluginFactory
    public static ComponentTemplatePlugin createComponentTemplate(
            @PluginAttribute("name") @Required String name,
            @PluginAttribute("path") String path,
            @PluginValue("sourceString") String source) {

        ComponentTemplate.Builder builder = ComponentTemplate.newBuilder()
                .withName(name)
                .withPath(path)
                .withSource(source);

        try {
            builder.validate();
            return new ComponentTemplatePlugin(name, builder.loadSource());
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

    }

}
