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


@Plugin(name = IndexTemplatePlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class IndexTemplatePlugin extends IndexTemplate {

    public static final String TYPE_NAME = "IndexTemplate";
    public static final String PLUGIN_NAME = "IndexTemplate";

    /**
     * @param name Index template name
     * @param source Index template document
     */
    protected IndexTemplatePlugin(String name, String source) {
        super(name, source);
    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @PluginFactory
    public static IndexTemplatePlugin createIndexTemplate(
            @PluginAttribute("name") @Required String name,
            @PluginAttribute("path") String path,
            @PluginValue("sourceString") String source) {

        Builder builder = newBuilder()
                .withName(name)
                .withPath(path)
                .withSource(source);

        try {
            builder.validate();
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

        return new IndexTemplatePlugin(name, builder.loadSource());

    }

}
