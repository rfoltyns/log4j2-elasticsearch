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

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;

public class ElasticsearchDataStreamAPI implements ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> {

    private final Serializer<Object> itemSerializer;
    private final Deserializer<BatchResult> resultDeserializer;
    private final String filterPath;

    public ElasticsearchDataStreamAPI() {
        this.itemSerializer = createItemSerializer();
        this.resultDeserializer = createResultDeserializer();
        this.filterPath = null;
    }

    public ElasticsearchDataStreamAPI(final String filterPath) {
        this.itemSerializer = createItemSerializer();
        this.resultDeserializer = createResultDeserializer();
        this.filterPath = filterPath;
    }

    /**
     * @param itemSerializer index request metadata serializer
     * @param resultDeserializer batch response deserializer
     */
    public ElasticsearchDataStreamAPI(
            final Serializer<Object> itemSerializer,
            final Deserializer<BatchResult> resultDeserializer
    ) {
        this.itemSerializer = itemSerializer;
        this.resultDeserializer = resultDeserializer;
        this.filterPath = null;
    }

    /**
     * @param filterPath Elasticsearch {@code filter_path}
     * @param itemSerializer index request metadata serializer
     * @param resultDeserializer batch response deserializer
     */
    public ElasticsearchDataStreamAPI(
            final Serializer<Object> itemSerializer,
            final Deserializer<BatchResult> resultDeserializer,
            final String filterPath
    ) {
        this.itemSerializer = itemSerializer;
        this.resultDeserializer = resultDeserializer;
        this.filterPath = filterPath;
    }

    @Override
    public IndexRequest.Builder itemBuilder(final String target, final ItemSource payload) {
        return new DataStreamItem.Builder(payload)
                .index(target);
    }

    @Override
    public BatchRequest.Builder batchBuilder() {
        return new DataStreamBatchRequest.Builder()
                .withFilterPath(filterPath)
                .withItemSerializer(itemSerializer)
                .withResultDeserializer(resultDeserializer);
    }

    private Serializer<Object> createItemSerializer() {

        final ObjectWriter objectWriter = ElasticsearchBulkAPI.defaultObjectMapper()
                .addMixIn(IndexRequest.class, DataStreamItemMixIn.class)
                .writerFor(IndexRequest.class);

        return new JacksonSerializer<>(objectWriter);

    }

    @SuppressWarnings("DuplicatedCode")
    protected Deserializer<BatchResult> createResultDeserializer() {

        final ObjectReader objectReader = ElasticsearchBulkAPI.defaultObjectMapper()
                .addMixIn(BatchItemResult.class, DataStreamItemResultMixIn.class)
                .readerFor(BatchResult.class);

        return new JacksonDeserializer<>(objectReader);

    }

}
