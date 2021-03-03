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
 * When it's no longer needed, {@link #release()} MUST be called to release underlying resources.
 */
public class ElasticsearchDataStreamItem extends IndexRequest {

    protected ElasticsearchDataStreamItem(Builder builder) {
        super(builder);
    }

    public static class Builder extends IndexRequest.Builder {

        public Builder(ItemSource<ByteBuf> source) {
            super(source);
        }

        public ElasticsearchDataStreamItem build() {
            if (source == null) {
                throw new IllegalArgumentException("source cannot be null");
            }
            return new ElasticsearchDataStreamItem(this);
        }
    }

}
