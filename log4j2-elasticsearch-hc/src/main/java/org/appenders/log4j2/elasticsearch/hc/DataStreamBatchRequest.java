package org.appenders.log4j2.elasticsearch.hc;

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

import java.util.Collection;

/**
 * {@link org.appenders.log4j2.elasticsearch.ByteBufItemSource}-backed /_bulk request.
 * Allows to index multiple {@link IndexRequest} documents
 * in a single request.
 */
public class DataStreamBatchRequest extends BatchRequest {

    private IndexRequest first;

    protected DataStreamBatchRequest(Builder builder) {
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

        for (IndexRequest indexRequest : indexRequests) {

            if (first == null) {
                first = indexRequest;
                continue;
            }

            final boolean sameIndex = first.sameIndex(indexRequest);

            if (!sameIndex) {
                // fail fast and serialize each item
                throw new IllegalArgumentException("Items for different indices found: " + first.getIndex() + " != " + indexRequest.getIndex());
            }

        }

        return first;

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

        @Override
        public DataStreamBatchRequest build() {

            validate();

            return new DataStreamBatchRequest(this);

        }

    }
}
