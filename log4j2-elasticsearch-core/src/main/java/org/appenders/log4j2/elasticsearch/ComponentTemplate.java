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

/**
 * Component template definition. See
 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-component-template.html">Component templates</a>
 * and <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html">Composable index templates</a>
 */
public class ComponentTemplate implements OpSource {

    public static final String TYPE_NAME = "ComponentTemplate";

    private final String name;
    private final String source;

    /**
     * @param name Component template name
     * @param source Component template document
     */
    protected ComponentTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @return Component template name
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@link #TYPE_NAME}
     */
    @Override
    public String getType() {
        return TYPE_NAME;
    }

    /**
     * @return Component template document
     */
    @Override
    public String getSource() {
        return this.source;
    }

    public static ComponentTemplate.Builder newBuilder() {
        return new ComponentTemplate.Builder();
    }

    public static class Builder {

        protected String name;
        protected String path;
        protected String source;

        public ComponentTemplate build() {
            validate();
            return new ComponentTemplate(name, loadSource());
        }

        void validate() {
            if (name == null) {
                throw new IllegalArgumentException("No name provided for " + getClass().getSimpleName());
            }
            if ((path == null && source == null) || (path != null && source != null)) {
                throw new IllegalArgumentException("Either path or source have to be provided for " + getClass().getSimpleName());
            }
        }

        /**
         * @return {@link #source} if configured, {@link ResourceUtil#loadResource(String)} otherwise
         */
        protected String loadSource() {

            if (source != null) {
                return source;
            }

            return ResourceUtil.loadResource(path);

        }

        /**
         * @param name Component template name
         * @return this
         */
        public ComponentTemplate.Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param path {@code "classpath:<path>"} or file path of component template document
         * @return this
         */
        public ComponentTemplate.Builder withPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * @param source Component template document
         * @return this
         */
        public ComponentTemplate.Builder withSource(String source) {
            this.source = source;
            return this;
        }
    }

}
