package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.netty.buffer.ByteBuf;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;

/**
 * Allows to create buffered versions of Jest action classes
 */
public class BufferedBulkOperations implements BatchOperations<Bulk> {

    private final PooledItemSourceFactory pooledItemSourceFactory;
    private final ObjectWriter objectWriter;
    private final ObjectReader objectReader;

    // FIXME: design - writer and reader should be configurable here(?)
    public BufferedBulkOperations(PooledItemSourceFactory pooledItemSourceFactory) {
        this.pooledItemSourceFactory = pooledItemSourceFactory;
        this.objectWriter = configuredWriter();
        this.objectReader = configuredReader();
    }

    @Override
    public Object createBatchItem(String indexName, Object source) {
        throw new UnsupportedOperationException("Use ItemSource based API instead");
    }

    @Override
    public Object createBatchItem(String indexName, ItemSource source) {
        return new BufferedIndex.Builder((ItemSource<ByteBuf>) source)
                .index(indexName)
                .build();
    }

    @Override
    public BatchBuilder<Bulk> createBatchBuilder() {
        return new BatchBuilder<Bulk>() {

            private final BufferedBulk.Builder builder = new BufferedBulk.Builder()
                    .withBuffer(pooledItemSourceFactory.createEmptySource())
                    .withObjectWriter(objectWriter)
                    .withObjectReader(objectReader);

            @Override
            public void add(Object item) {
                builder.addAction((BulkableAction) item);
            }

            @Override
            public Bulk build() {
                return builder.build();
            }

        };
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectWriter} to serialize {@link BufferedIndex} instances
     */
    protected ObjectWriter configuredWriter() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .addMixIn(BufferedIndex.class, BulkableActionMixIn.class)
                .writerFor(BufferedIndex.class);
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectReader} to deserialize {@link BufferedBulkResult}
     */
    protected ObjectReader configuredReader() {
        return new ObjectMapper()
                .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class)
                .addMixIn(BulkError.class, BulkErrorMixIn.class)
                .addMixIn(BulkResultItem.class, BulkResultItemMixIn.class)
                .readerFor(BufferedBulkResult.class);
    }

}

