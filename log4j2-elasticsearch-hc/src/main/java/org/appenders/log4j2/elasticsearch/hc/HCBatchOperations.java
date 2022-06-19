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


import com.fasterxml.jackson.databind.ObjectWriter;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;

public class HCBatchOperations implements BatchOperations<BatchRequest>, LifeCycle {

    private volatile State state = State.STOPPED;

    protected final PooledItemSourceFactory batchBufferFactory;
    private final ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> builderFactory;
    private final String mappingType;

    /**
     * @param batchBufferFactory batch buffer factory
     * @param mappingType Elasticsearch mapping type
     * @deprecated This constructor will be removed in future releases. Use {@link #HCBatchOperations(PooledItemSourceFactory, ClientAPIFactory)} instead
     */
    @Deprecated
    public HCBatchOperations(final PooledItemSourceFactory batchBufferFactory, final String mappingType) {
        this.batchBufferFactory = batchBufferFactory;
        this.builderFactory = new ElasticsearchBulkAPI(mappingType);

        // bad decisions pit..
        this.mappingType = mappingType;
    }

    /**
     * @param batchBufferFactory batch buffer factory
     * @deprecated This constructor will be removed in future releases. Use {@link #HCBatchOperations(PooledItemSourceFactory, ClientAPIFactory)} instead
     */
    @Deprecated
    public HCBatchOperations(final PooledItemSourceFactory batchBufferFactory) {
        this(batchBufferFactory, (String) null);
    }

    public HCBatchOperations(final PooledItemSourceFactory batchBufferFactory, final ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> builderFactory) {
        this.batchBufferFactory = batchBufferFactory;
        this.builderFactory = builderFactory;
        this.mappingType = null; // irrelevant in this context
    }

    /**
     * @return Elasticsearch mapping type
     * @deprecated This method will be removed in future releases. Use {@link ClientAPIFactory} instead.
     */
    @Deprecated
    public String getMappingType() {
        return mappingType;
    }

    @Override
    public Object createBatchItem(final String target, final Object source) {
        throw new UnsupportedOperationException("Use ItemSource based API instead");
    }

    @Override
    public Object createBatchItem(final String target, final ItemSource payload) {
        return builderFactory.itemBuilder(target, payload).build();
    }

    @Override
    public BatchBuilder<BatchRequest> createBatchBuilder() {
        return new BatchBuilder<BatchRequest>() {

            private final BatchRequest.Builder builder = builderFactory.batchBuilder()
                    .withBuffer(batchBufferFactory.createEmptySource());

            @Override
            public void add(Object item) {
                builder.add(item);
            }

            @Override
            public BatchRequest build() {
                return builder.build();
            }

        };
    }

    /**
     * @return {@code com.fasterxml.jackson.databind.ObjectWriter} to serialize {@link IndexRequest} instances
     * @deprecated This method will be removed along with {@link #HCBatchOperations(PooledItemSourceFactory, String)} constructor. Use {@link #HCBatchOperations(PooledItemSourceFactory, ClientAPIFactory)} instead.
     */
    @Deprecated
    protected ObjectWriter configuredWriter() {
        throw new UnsupportedOperationException("Moved to ElasticsearchBulk or peer");
    }

    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        batchBufferFactory.start();

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        batchBufferFactory.stop();

        state = State.STOPPED;

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

}

