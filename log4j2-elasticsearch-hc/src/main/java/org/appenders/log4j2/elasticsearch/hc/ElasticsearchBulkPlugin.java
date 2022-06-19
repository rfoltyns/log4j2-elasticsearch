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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.ExtendedObjectMapper;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;

@Plugin(name = "ElasticsearchBulk", category = Node.CATEGORY, elementType = "clientAPIFactory", printObject = true)
public class ElasticsearchBulkPlugin extends ElasticsearchBulkAPI {

    private ElasticsearchBulkPlugin(
            final Serializer<Object> itemSerializer,
            final Deserializer<BatchResult> resultDeserializer,
            final String mappingType
    ) {
        super(mappingType, itemSerializer, resultDeserializer);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ElasticsearchBulkPlugin> {

        @PluginAttribute(value = "mappingType")
        protected String mappingType;

        private Serializer<Object> itemSerializer = createItemSerializer();
        private Deserializer<BatchResult> resultDeserializer = createResultDeserializer();

        @Override
        public ElasticsearchBulkPlugin build()
        {
            if (itemSerializer == null) {
                throw new ConfigurationException("itemSerializer cannot be null");
            }

            if (resultDeserializer == null) {
                throw new ConfigurationException("resultDeserializer cannot be null");
            }

            return new ElasticsearchBulkPlugin(itemSerializer, resultDeserializer, mappingType);
        }

        public Builder withMappingType(final String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        public Builder withItemSerializer(final Serializer<Object> itemSerializer) {
            this.itemSerializer = itemSerializer;
            return this;
        }

        public Builder withResultDeserializer(final Deserializer<BatchResult> resultDeserializer) {
            this.resultDeserializer = resultDeserializer;
            return this;
        }

        /**
         * @return index request metadata serializer
         */
        protected Serializer<Object> createItemSerializer() {
            final ObjectWriter objectWriter = new ExtendedObjectMapper(new MappingJsonFactory())
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .addMixIn(IndexRequest.class, IndexRequestMixIn.class)
                    .writerFor(IndexRequest.class);
            return new JacksonSerializer<>(objectWriter);
        }

        /**
         * @return batch response deserializer
         */
        @SuppressWarnings("DuplicatedCode")
        protected Deserializer<BatchResult> createResultDeserializer() {

            final ObjectReader objectReader = new ObjectMapper()
                    .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .addMixIn(BatchResult.class, BatchResultMixIn.class)
                    .addMixIn(Error.class, ErrorMixIn.class)
                    .addMixIn(BatchItemResult.class, BatchItemResultMixIn.class)
                    .readerFor(BatchResult.class);

            return new JacksonDeserializer<>(objectReader);

        }

    }

}

