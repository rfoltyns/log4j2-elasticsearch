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
 * Index template definition. Supports both composable index templates and templates deprecated in 7.8.
 * Set {@link #apiVersion} to 8 to indicate that this template is composable
 * Set {@link #apiVersion} to 7 to use legacy Index Template API
 *
 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html">Composable index templates</a>
 * and <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates-v1.html">Deprecated index templates</a>
 */
public class IndexTemplate implements OpSource {

    public static final String TYPE_NAME = "IndexTemplate";
    public static final int DEFAULT_API_VERSION = 8;

    private final int apiVersion;
    private final String name;
    private final String source;

    /**
     * @param name Index template name
     * @param source Index template document
     */
    public IndexTemplate(String name, String source) {
        this(DEFAULT_API_VERSION, name, source);
    }

    /**
     * @param apiVersion Elasticsearch Index Template API version
     * @param name Index template name
     * @param source Index template document
     */
    public IndexTemplate(int apiVersion, String name, String source) {
        this.apiVersion = apiVersion;
        this.name = name;
        this.source = source;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    /**
     * @return Index template name
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
     * @return Index template document
     */
    @Override
    public String getSource() {
        return this.source;
    }

    public static IndexTemplate.Builder newBuilder() {
        return new IndexTemplate.Builder();
    }

    public static class Builder {

        protected int apiVersion = DEFAULT_API_VERSION;
        protected String name;
        protected String path;
        protected String source;

        public IndexTemplate build() {
            validate();
            return new IndexTemplate(apiVersion, name, loadSource());
        }

        void validate() {
            if (name == null) {
                throw new IllegalArgumentException("No name provided for " + IndexTemplate.class.getSimpleName());
            }

            final boolean noneSet = path == null && source == null;
            final boolean moreThanOneSet = path != null && source != null;
            if (noneSet || moreThanOneSet) {
                throw new IllegalArgumentException("Either path or source must to be provided for " + IndexTemplate.class.getSimpleName());
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
         * @param name Index template name
         * @return this
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param path {@code "classpath:<path>"} or file path of index template document
         * @return this
         */
        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * @param source Index template document
         * @return this
         */
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        /**
         * @param apiVersion Elasticsearch API version
         * @return this
         */
        public IndexTemplate.Builder withApiVersion(int apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

    }

}
