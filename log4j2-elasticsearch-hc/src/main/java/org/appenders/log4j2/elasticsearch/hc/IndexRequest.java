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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * {@link ItemSource} based document to be indexed.
 * When it's no longer needed, {@link #release()} MUST be called to release underlying resources.
 */
public class IndexRequest implements Request {

    private static final String CHARSET = "utf-8";
    public static final String HTTP_METHOD_NAME = "POST";
    public static final int ESTIMATED_URI_LENGTH = 64;
    public static final String FORWARD_SLASH = "/";

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

    public final ItemSource<ByteBuf> getSource() {
        return this.source;
    }

    @Override
    public String getURI() {

        StringBuilder sb = new StringBuilder(ESTIMATED_URI_LENGTH);

        try {

            sb.append(encode(index))
                    .append(FORWARD_SLASH)
                    .append(encode(type));

            if (id != null) {
                sb.append(FORWARD_SLASH).append(encode(id));
            }

        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        return sb.toString();

    }

    @Override
    public String getHttpMethodName() {
        return HTTP_METHOD_NAME;
    }

    @Override
    public ItemSource serialize() {
        return source;
    }

    public void release() {
        source.release();
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
            if (source == null) {
                throw new IllegalArgumentException("source cannot be null");
            }
            if (index == null) {
                throw new IllegalArgumentException("index cannot be null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }
            return new IndexRequest(this);
        }
    }

    /* visible for testing */
    String encode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, CHARSET);
    }

}
