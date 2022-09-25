package org.appenders.log4j2.elasticsearch.hc;

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

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ItemSource;

/**
 * {@link ItemSource} based document to be indexed.
 * When it's no longer needed, {@link #completed()} MUST be called to release underlying resources.
 */
public class IndexRequest implements Item<ItemSource<ByteBuf>> {

    protected final String id;
    protected final String type;
    protected final String index;
    protected final ItemSource<ByteBuf> source;

    protected IndexRequest(Builder builder) {
        this.id = builder.id;
        this.index = builder.index;
        this.type = builder.type;
        this.source = builder.source;
    }

    public String getId() {
        return id;
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    @Override
    public final ItemSource<ByteBuf> getSource() {
        return this.source;
    }

    @Override
    public void completed() {
        this.source.release();
    }

    public final boolean sameIndex(final IndexRequest other) {
        return this.index.equals(other.index);
    }

    public final boolean sameType(final IndexRequest other) {

        if (this.type == null) {
            return other.type == null;
        }

        return this.type.equals(other.type);

    }

    public static class Builder {

        private final ItemSource<ByteBuf> source;

        private String id;
        private String index;
        private String type;

        public Builder(ItemSource<ByteBuf> source) {
            this.source = source;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder index(String index) {
            this.index = index;
            return this;
        }

        public Builder type(String mappingType) {
            this.type = mappingType;
            return this;
        }

        public IndexRequest build() {
            validate();
            return new IndexRequest(this);
        }

        protected void validate() {

            if (source == null) {
                throw new IllegalArgumentException("source cannot be null");
            }

            if (index == null) {
                throw new IllegalArgumentException("index cannot be null");
            }

            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }

        }

    }

}
