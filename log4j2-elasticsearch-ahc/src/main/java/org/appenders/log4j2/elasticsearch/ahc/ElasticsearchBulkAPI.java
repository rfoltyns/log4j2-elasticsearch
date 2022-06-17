package org.appenders.log4j2.elasticsearch.ahc;

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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.ExtendedObjectMapper;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;

public class ElasticsearchBulkAPI implements ClientAPIFactory<IndexRequest.Builder, BatchRequest.Builder, BatchResult> {

    private final String mappingType;
    private final Serializer<Object> itemSerializer;
    private final Deserializer<BatchResult> resultDeserializer;

    public ElasticsearchBulkAPI(final String mappingType) {
        this.mappingType = mappingType;
        this.itemSerializer = createItemSerializer();
        this.resultDeserializer = createResultDeserializer();
    }

    /**
     * @param itemSerializer index request metadata serializer
     * @param resultDeserializer batch response deserializer
     * @param mappingType Elasticsearch mapping type
     */
    public ElasticsearchBulkAPI(
            final String mappingType,
            final Serializer<Object> itemSerializer,
            final Deserializer<BatchResult> resultDeserializer
    ) {
        this.mappingType = mappingType;
        this.itemSerializer = itemSerializer;
        this.resultDeserializer = resultDeserializer;
    }

    @Override
    public IndexRequest.Builder itemBuilder(final String target, final ItemSource payload) {
        return new IndexRequest.Builder(payload)
                .index(target)
                .type(mappingType);
    }

    @Override
    public BatchRequest.Builder batchBuilder() {
        return new BatchRequest.Builder()
                .withItemSerializer(itemSerializer)
                .withResultDeserializer(resultDeserializer);
    }

    public Serializer<Object> createItemSerializer() {

        final ObjectWriter objectWriter = defaultObjectMapper()
                .writerFor(IndexRequest.class);

        return new JacksonSerializer<>(objectWriter);

    }

    public Deserializer<BatchResult> createResultDeserializer() {

        final ObjectReader objectReader = defaultObjectMapper()
                .readerFor(BatchResult.class);

        return new JacksonDeserializer<>(objectReader);

    }

    public static ObjectMapper defaultObjectMapper() {
        return new ExtendedObjectMapper(new MappingJsonFactory())
                .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .addMixIn(IndexRequest.class, IndexRequestMixIn.class)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .addMixIn(BatchResult.class, BatchResultMixIn.class)
                .addMixIn(Error.class, ErrorMixIn.class)
                .addMixIn(BatchItemResult.class, BatchItemResultMixIn.class);
    }

}
