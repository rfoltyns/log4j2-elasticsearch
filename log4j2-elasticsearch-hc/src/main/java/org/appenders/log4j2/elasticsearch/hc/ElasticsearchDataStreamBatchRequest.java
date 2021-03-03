package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

/**
 * {@link org.appenders.log4j2.elasticsearch.ByteBufItemSource}-backed /_bulk request.
 * Allows to index multiple {@link IndexRequest} documents
 * in a single request.
 */
public class ElasticsearchDataStreamBatchRequest extends BatchRequest {

    private IndexRequest action;

    protected ElasticsearchDataStreamBatchRequest(Builder builder) {
        super(builder);
    }

    /**
     * Checks if all items in given collection are equal
     * ({@link IndexRequest#index} and {@link IndexRequest#type} are the same for all elements)
     *
     * @param indexRequests collection of items to be checked
     * @return {@link IndexRequest} first action in given collection if all items are equal, null otherwise
     */
    IndexRequest uniformAction(Collection<IndexRequest> indexRequests) {
        if (action == null) {
            action = indexRequests.stream().limit(1).findFirst().orElse(null);
        }
        return action;
    }

    @Override
    public String getURI() {
        return uniformAction(indexRequests).getIndex() + "/_bulk";
    }

    @Override
    public String getHttpMethodName() {
        return HTTP_METHOD_NAME;
    }

    public static class Builder extends BatchRequest.Builder {

        public BatchRequest.Builder add(IndexRequest item) {
            super.add(item);
            return this;
        }

        public BatchRequest.Builder add(Collection<? extends IndexRequest> items) {
            super.add(items);
            return this;
        }

        public ElasticsearchDataStreamBatchRequest build() {
//            super.build();
            return new ElasticsearchDataStreamBatchRequest(this);
        }

    }
}
