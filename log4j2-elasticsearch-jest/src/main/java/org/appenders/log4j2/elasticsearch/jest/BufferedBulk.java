package org.appenders.log4j2.elasticsearch.jest;

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

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

/**
 * Extended Jest {@code io.searchbox.core.Bulk} using {@link ByteBufItemSource}
 */
public class BufferedBulk extends Bulk {

    public static final char LINE_SEPARATOR = '\n';

    private final ObjectWriter objectWriter;
    private final ObjectReader objectReader;
    private final ItemSource<ByteBuf> bulkSource;

    protected final Collection<BulkableAction> actions;

    public BufferedBulk(BufferedBulk.Builder builder) {
        super(builder);
        this.actions = getQueueFactoryInstance().toIterable(builder.actions);
        this.objectWriter = builder.objectWriter;
        this.objectReader = builder.objectReader;
        this.bulkSource = builder.bufferedSource;
    }

    public BufferedBulkResult deserializeResponse(InputStream responseBody) throws IOException {
        return objectReader.readValue(responseBody);
    }

    /**
     * Serializes and writes {@link #actions} into {@link #bulkSource}
     * 
     * @return underlying buffer filled with serialized actions
     * @throws IOException if serialization failed
     */
    public ByteBuf serializeRequest() throws IOException {

        ReusableByteBufOutputStream byteBufOutputStream = new ReusableByteBufOutputStream(bulkSource.getSource());

        // in current impl with no IDs, it's possible to reduce serialization by reusing first action
        BulkableAction sameAction = getSameItem(actions);
        byte[] actionTemplate = sameAction != null ? objectWriter.writeValueAsBytes(sameAction) : null;

        for (BulkableAction action : actions) {

            if (actionTemplate == null) {
                objectWriter.writeValue((OutputStream) byteBufOutputStream, action);
            } else {
                byteBufOutputStream.write(actionTemplate);
            }
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

            ByteBuf source = ((BufferedIndex)action).getSource().getSource();
            bulkSource.getSource().writeBytes(source);
            byteBufOutputStream.writeByte(LINE_SEPARATOR);

        }

        return bulkSource.getSource();
    }

    public String getURI() {
        return "/_bulk";
    }

    /**
     * Checks if all actions in given collection are equal
     * ({@link BulkableAction#getIndex()} and {@link BulkableAction#getType()} are the same for all elements)
     *
     * @param bulkableActions collection of actions to be checked
     * @return {@link BulkableAction} first action in given collection if all actions are equal, null otherwise
     */
    private BulkableAction getSameItem(Collection<BulkableAction> bulkableActions) {

        BulkableAction current = null;
        for (BulkableAction bulkableAction : bulkableActions) {
            if (current == null) {
                current = bulkableAction;
                continue;
            }
            // fail fast and serialize each item
            // no need to check type for org.appenders.log4j2.elasticsearch.jest.BufferedIndex
            if (!current.getIndex().equals(bulkableAction.getIndex())) {
                return null;
            }
        }
        return current;
    }

    /**
     * Clears underlying collection of actions and releases all {@link ItemSource} instances.
     * <p>MUST be called when request is completed. Otherwise it may lead to excessive resource usage and memory leaks
     */
    public void completed() {
        for (BulkableAction bulkableAction : actions) {
            ((BufferedIndex)bulkableAction).release();
        }
        actions.clear();
        bulkSource.release();
    }

    public Collection<BulkableAction> getActions() {
        return actions;
    }

    public static class Builder extends Bulk.Builder {

        protected final Collection<BulkableAction> actions = getQueueFactoryInstance().tryCreateMpscQueue(
                BufferedBulk.class.getSimpleName(),
                Integer.parseInt(System.getProperty("appenders." + BufferedBulk.class.getSimpleName() + ".initialSize", "10000"))
        );

        private ItemSource<ByteBuf> bufferedSource;
        private ObjectWriter objectWriter;
        private ObjectReader objectReader;

        @Override
        public Bulk.Builder addAction(BulkableAction action) {
            this.actions.add(action);
            return this;
        }

        @Override
        public Bulk.Builder addAction(Collection<? extends BulkableAction> actions) {
            this.actions.addAll(actions);
            return this;
        }

        @Override
        public BufferedBulk build() {
            if (bufferedSource == null) {
                throw new IllegalArgumentException("bufferedSource cannot be null");
            }

            if (objectReader == null) {
                throw new IllegalArgumentException("objectReader cannot be null");
            }

            if (objectWriter == null) {
                throw new IllegalArgumentException("objectWriter cannot be null");
            }

            return new BufferedBulk(this);
        }

        public Builder withBuffer(ItemSource<ByteBuf> buffer) {
            this.bufferedSource = buffer;
            return this;
        }

        public Builder withObjectWriter(ObjectWriter objectWriter) {
            this.objectWriter = objectWriter;
            return this;
        }

        public Builder withObjectReader(ObjectReader objectReader) {
            this.objectReader = objectReader;
            return this;
        }

    }
}
