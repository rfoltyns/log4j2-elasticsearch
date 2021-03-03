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

public class DataStream implements OpSource {

    public static final String TYPE_NAME = "DataStream";

    private final String name;

    /**
     * @param name Index template name
     */
    public DataStream(String name) {
        this.name = name;
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
        return "{}";
    }

    public static DataStream.Builder newBuilder() {
        return new DataStream.Builder();
    }

    public static class Builder {

        protected String name;

        public DataStream build() {
            validate();
            return new DataStream(name);
        }

        void validate() {
            if (name == null) {
                throw new IllegalArgumentException("No name provided for " + getClass().getSimpleName());
            }
        }

        /**
         * @param name Index template name
         * @return this
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

    }

}