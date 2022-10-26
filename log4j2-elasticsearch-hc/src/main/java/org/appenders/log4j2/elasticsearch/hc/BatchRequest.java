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
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;

import java.io.IOException;
import java.io.InputStream;
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

    private final Serializer<Object> itemSerializer;
    private final Deserializer<BatchResult> resultDeserializer;
    private ItemSource<ByteBuf> buffer;

    protected final Collection<IndexRequest> indexRequests;
    private final int size;

    protected BatchRequest(final Builder builder) {
        this.indexRequests = getQueueFactoryInstance(BatchRequest.class.getSimpleName()).toIterable(builder.items);
        this.size = this.indexRequests.size();
        this.itemSerializer = builder.itemSerializer;
        this.resultDeserializer = builder.resultDeserializer;
        this.buffer = builder.itemSource;
    }

    /**
     * Serializes and writes {@link #indexRequests} into {@link #buffer}
     *
     * @return underlying buffer filled with serialized indexRequests
     * @throws IOException if serialization failed
     */
    public ItemSource serialize() throws Exception {

        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(buffer.getSource());

        // in current impl with no IDs, it's possible to reduce serialization by reusing first action
        IndexRequest identicalAction = uniformAction(indexRequests);
        byte[] actionTemplate = identicalAction != null ? itemSerializer.writeAsBytes(identicalAction) : null;

        for (IndexRequest action : indexRequests) {

            if (actionTemplate == null) {
                itemSerializer.write(byteBufOutputStream, action);
            } else {
                byteBufOutputStream.write(actionTemplate);
            }
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

            ByteBuf source = action.getSource().getSource();
            buffer.getSource().writeBytes(source);
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

        }

        return buffer;

    }

    public BatchResult deserialize(final InputStream responseBody) throws IOException {
        return resultDeserializer.read(responseBody);
    }

    /**
     * Checks if all items in given collection are equal
     * ({@link IndexRequest#index} and {@link IndexRequest#type} are the same for all elements)
     *
     * @param indexRequests collection of items to be checked
     * @return {@link IndexRequest} first action in given collection if all items are equal, null otherwise
     */
    IndexRequest uniformAction(final Collection<IndexRequest> indexRequests) {

        IndexRequest current = null;
        for (IndexRequest indexRequest : indexRequests) {

            if (current == null) {
                current = indexRequest;
                continue;
            }

            final boolean sameIndex = current.sameIndex(indexRequest);
            final boolean sameType = current.sameType(indexRequest);

            if (!sameIndex || !sameType) {
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

        buffer.release();
        buffer = null;

    }

    /**
     * @return collection of batch items
     */
    @Override
    public Collection<IndexRequest> getItems() {
        return indexRequests;
    }

    @Override
    public int size() {
        return size;
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

        private static final int INITIAL_SIZE = Integer.parseInt(System.getProperty("appenders." + BatchRequest.class.getSimpleName() + ".initialSize", "8192"));

        protected final Collection<IndexRequest> items;

        private ItemSource<ByteBuf> itemSource;
        private Serializer<Object> itemSerializer;
        private Deserializer<BatchResult> resultDeserializer;

        public Builder() {
            this(getQueueFactoryInstance(BatchRequest.class.getSimpleName()).tryCreateMpscQueue(INITIAL_SIZE));
        }

        Builder(final Collection<IndexRequest> items) {
            this.items = items;
        }

        public Builder add(final Object item) {
            add((IndexRequest)item);
            return this;
        }

        public Builder add(final IndexRequest item) {
            this.items.add(item);
            return this;
        }

        public Builder add(final Collection<? extends IndexRequest> items) {
            this.items.addAll(items);
            return this;
        }

        public BatchRequest build() {

            validate();

            return new BatchRequest(this);

        }

        protected void validate() {

            if (itemSource == null) {
                throw new IllegalArgumentException("buffer cannot be null");
            }

            if (itemSerializer == null) {
                throw new IllegalArgumentException("itemSerializer cannot be null");
            }

            if (resultDeserializer == null) {
                throw new IllegalArgumentException("resultDeserializer cannot be null");
            }

        }

        public Builder withBuffer(final ItemSource<ByteBuf> buffer) {
            this.itemSource = buffer;
            return this;
        }

        /**
         * Will be replaced with Serializer. Use {@link #withItemSerializer(Serializer)}
         *
         * @param objectWriter item serializer
         * @return this
         * @deprecated As of 1.7, this method will be removed. Use {@link #withItemSerializer(Serializer)} instead
         */
        @Deprecated
        public Builder withObjectWriter(final ObjectWriter objectWriter) {
            if (objectWriter == null) {
                throw new IllegalArgumentException("objectWriter cannot be null");
            }
            this.itemSerializer = new JacksonSerializer<>(objectWriter);
            return this;
        }

        public Builder withItemSerializer(final Serializer<Object> serializer) {
            this.itemSerializer = serializer;
            return this;
        }

        public Builder withResultDeserializer(final Deserializer<BatchResult> deserializer) {
            this.resultDeserializer = deserializer;
            return this;
        }
    }
}
