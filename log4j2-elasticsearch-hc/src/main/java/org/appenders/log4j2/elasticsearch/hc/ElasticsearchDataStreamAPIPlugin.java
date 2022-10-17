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


import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;

@Plugin(name = "ElasticsearchDataStream", category = Node.CATEGORY, elementType = "clientAPIFactory", printObject = true)
public class ElasticsearchDataStreamAPIPlugin extends ElasticsearchDataStreamAPI {

    private ElasticsearchDataStreamAPIPlugin(
            final Serializer<Object> serializer,
            final Deserializer<BatchResult> deserializer
    ) {
        super(serializer, deserializer);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ElasticsearchDataStreamAPIPlugin> {

        private Serializer<Object> itemSerializer = createItemSerializer();
        private Deserializer<BatchResult> resultDeserializer = createResultDeserializer();

        @Override
        public ElasticsearchDataStreamAPIPlugin build()
        {
            if (itemSerializer == null) {
                throw new ConfigurationException("itemSerializer cannot be null");
            }

            if (resultDeserializer == null) {
                throw new ConfigurationException("resultDeserializer cannot be null");
            }

            return new ElasticsearchDataStreamAPIPlugin(itemSerializer, resultDeserializer);
        }

        /**
         * @return index request metadata serializer
         */
        protected Serializer<Object> createItemSerializer() {
            final ObjectWriter objectWriter = ElasticsearchBulkAPI.defaultObjectMapper()
                    .addMixIn(IndexRequest.class, DataStreamItemMixIn.class)
                    .writerFor(IndexRequest.class);
            return new JacksonSerializer<>(objectWriter);
        }

        /**
         * @return batch response deserializer
         */
        @SuppressWarnings("DuplicatedCode")
        protected Deserializer<BatchResult> createResultDeserializer() {
            return new JacksonDeserializer<>(ElasticsearchBulkAPI.defaultObjectMapper()
                    .addMixIn(BatchItemResult.class, DataStreamItemResultMixIn.class)
                    .readerFor(BatchResult.class));
        }

    public Builder withItemSerializer(final Serializer<Object> itemSerializer) {
            this.itemSerializer = itemSerializer;
            return this;
        }

        public Builder withResultDeserializer(final Deserializer<BatchResult> resultDeserializer) {
            this.resultDeserializer = resultDeserializer;
            return this;
        }

    }

}

