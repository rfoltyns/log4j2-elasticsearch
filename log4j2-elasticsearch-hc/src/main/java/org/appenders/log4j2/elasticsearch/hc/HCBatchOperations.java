package org.appenders.log4j2.elasticsearch.hc;

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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ExtendedObjectMapper;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;

public class HCBatchOperations implements BatchOperations<BatchRequest> {

    private final PooledItemSourceFactory pooledItemSourceFactory;
    private final String mappingType;
    private final ObjectWriter objectWriter;

    public HCBatchOperations(PooledItemSourceFactory pooledItemSourceFactory, String mappingType) {
        this.pooledItemSourceFactory = pooledItemSourceFactory;
        this.mappingType = mappingType;
        this.objectWriter = configuredWriter();
    }

    @Override
    public Object createBatchItem(String indexName, Object source) {
        throw new UnsupportedOperationException("Use ItemSource based API instead");
    }

    @Override
    public Object createBatchItem(String indexName, ItemSource source) {
        return new IndexRequest.Builder(source)
                .index(indexName)
                .type(mappingType)
                .build();
    }

    @Override
    public BatchBuilder<BatchRequest> createBatchBuilder() {
        return new BatchBuilder<BatchRequest>() {

            private BatchRequest.Builder builder = new BatchRequest.Builder()
                    .withBuffer(pooledItemSourceFactory.createEmptySource())
                    .withObjectWriter(objectWriter);

            @Override
            public void add(Object item) {
                builder.add((IndexRequest)item);
            }

            @Override
            public BatchRequest build() {
                return builder.build();
            }

        };
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectWriter} to serialize {@link IndexRequest} instances
     */
    // FIXME: design - wrap with Serializer(?) to allow other implementations
    protected ObjectWriter configuredWriter() {
        return new ExtendedObjectMapper(new MappingJsonFactory())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .registerModule(new AfterburnerModule())
                .addMixIn(IndexRequest.class, IndexRequestMixIn.class)
                .writerFor(IndexRequest.class);
    }

}

