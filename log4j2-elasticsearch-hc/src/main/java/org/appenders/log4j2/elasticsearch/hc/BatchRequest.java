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
import org.appenders.log4j2.elasticsearch.ItemSource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

/**
 * {@link org.appenders.log4j2.elasticsearch.ByteBufItemSource}-backed /_bulk request.
 * Allows to index multiple {@link org.appenders.log4j2.elasticsearch.hc.IndexRequest} documents
 * in a single request.
 */
public class BatchRequest implements Batch<IndexRequest> {

    public static final String HTTP_METHOD_NAME = "POST";
    public static final char LINE_SEPARATOR = '\n';

    private final ObjectWriter objectWriter;
    private ItemSource<ByteBuf> itemSource;

    protected final Collection<IndexRequest> indexRequests;

    protected BatchRequest(Builder builder) {
        this.indexRequests = getQueueFactoryInstance().toIterable(builder.items);
        this.objectWriter = builder.objectWriter;
        this.itemSource = builder.itemSource;
    }

    /**
     * Serializes and writes {@link #indexRequests} into {@link #itemSource}
     *
     * @return underlying buffer filled with serialized indexRequests
     * @throws IOException if serialization failed
     */
    public ItemSource serialize() throws IOException {

        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(itemSource.getSource());

        // in current impl with no IDs, it's possible to reduce serialization by reusing first action
        IndexRequest identicalAction = uniformAction(indexRequests);
        byte[] actionTemplate = identicalAction != null ? objectWriter.writeValueAsBytes(identicalAction) : null;

        for (IndexRequest action : indexRequests) {

            if (actionTemplate == null) {
                objectWriter.writeValue((OutputStream) byteBufOutputStream, action);
            } else {
                byteBufOutputStream.write(actionTemplate);
            }
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

            ByteBuf source = action.getSource().getSource();
            itemSource.getSource().writeBytes(source);
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

        }
        return itemSource;
    }

    /**
     * Checks if all items in given collection are equal
     * ({@link IndexRequest#index} and {@link IndexRequest#type} are the same for all elements)
     *
     * @param indexRequests collection of items to be checked
     * @return {@link IndexRequest} first action in given collection if all items are equal, null otherwise
     */
    IndexRequest uniformAction(Collection<IndexRequest> indexRequests) {

        IndexRequest current = null;
        for (IndexRequest indexRequest : indexRequests) {
            if (current == null) {
                current = indexRequest;
                continue;
            }
            if (!current.index.equals(indexRequest.index) || !current.type.equals(indexRequest.type)) {
                // fail fast and serialize each item
                return null;
            }
        }
        return current;
    }

    /**
     * Clears underlying collection of indexRequests and releases all {@link ItemSource} instances.
     * <p>MUST be called when request is completed. Otherwise it may lead to excessive resource usage and memory leaks
     */
    public void completed() {
        for (IndexRequest indexRequest : indexRequests) {
            indexRequest.completed();
        }
        indexRequests.clear();

        itemSource.release();
        itemSource = null;

    }

    /**
     * @return collection of batch items
     */
    @Override
    public Collection<IndexRequest> getItems() {
        return indexRequests;
    }

    /**
     * @return collection of batch items
     * @deprecated As of 1.6, this method will be removed. Use {@link #getItems()} instead
     */
    @Deprecated
    public Collection<IndexRequest> getIndexRequests() {
        return getItems();
    }

    @Override
    public String getURI() {
        return "/_bulk";
    }

    @Override
    public String getHttpMethodName() {
        return HTTP_METHOD_NAME;
    }

    public static class Builder {

        protected final Collection<IndexRequest> items = getQueueFactoryInstance().tryCreateMpscQueue(
                BatchRequest.class.getSimpleName(),
                Integer.parseInt(System.getProperty("appenders." + BatchRequest.class.getSimpleName() + ".initialSize", "10000")));

        private ItemSource<ByteBuf> itemSource;
        private ObjectWriter objectWriter;

        public Builder add(IndexRequest item) {
            this.items.add(item);
            return this;
        }

        public Builder add(Collection<? extends IndexRequest> items) {
            this.items.addAll(items);
            return this;
        }

        public BatchRequest build() {
            if (itemSource == null) {
                throw new IllegalArgumentException("buffer cannot be null");
            }

            if (objectWriter == null) {
                throw new IllegalArgumentException("objectWriter cannot be null");
            }

            return new BatchRequest(this);
        }

        public Builder withBuffer(ItemSource<ByteBuf> buffer) {
            this.itemSource = buffer;
            return this;
        }

        public Builder withObjectWriter(ObjectWriter objectWriter) {
            this.objectWriter = objectWriter;
            return this;
        }

    }
}
