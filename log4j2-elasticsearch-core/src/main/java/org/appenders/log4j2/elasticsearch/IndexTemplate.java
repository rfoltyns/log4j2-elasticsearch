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


public class IndexTemplate implements OpSource {

    public static final String TYPE_NAME = "IndexTemplate";

    private final String name;
    private final String source;

    /**
     * @param name Index template name
     * @param source Index template document
     */
    public IndexTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    /**
     * @return Index template name
     */
    public String getName() {
        return name;
    }

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

        protected String name;
        protected String path;
        protected String source;

        public IndexTemplate build() {
            validate();
            return new IndexTemplate(name, loadSource());
        }

        void validate() {
            if (name == null) {
                throw new IllegalArgumentException("No name provided for IndexTemplate");
            }
            if ((path == null && source == null) || (path != null && source != null)) {
                throw new IllegalArgumentException("Either path or source have to be provided for IndexTemplate");
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
         * @return
         */
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }
    }

}
