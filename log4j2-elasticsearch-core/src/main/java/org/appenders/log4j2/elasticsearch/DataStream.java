package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
    private static final String EMPTY_SOURCE = "";

    private final String name;

    /**
     * @param name Data stream name
     */
    public DataStream(final String name) {
        this.name = name;
    }

    /**
     * @return Data stream name
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
     * @return Empty String {@code ""}
     */
    @Override
    public String getSource() {
        return EMPTY_SOURCE;
    }

    public static class Builder {

        protected String name;

        public DataStream build() {
            validate();
            return new DataStream(name);
        }

        void validate() {

            if (name == null) {
                throw new IllegalArgumentException("No name provided for " + DataStream.class.getSimpleName());
            }

        }

        /**
         * @param name Data stream name
         * @return this
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

    }

}
