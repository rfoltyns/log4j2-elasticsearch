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


import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.IndexNamePluginTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Serializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ElasticsearchBulkAPITest {

    @Test
    public void createsIndexRequestBuilder() {

        // given
        final String payloadString = UUID.randomUUID().toString();
        final ItemSource payload = createTestItemSource(payloadString);

        @SuppressWarnings("unchecked") final Serializer<Object> serializer = mock(Serializer.class);
        final Deserializer<BatchResult> deserializer = mock(Deserializer.class);

        final String mappingType = UUID.randomUUID().toString();
        final ElasticsearchBulkAPI builder = new ElasticsearchBulkAPI(mappingType, null, serializer, deserializer);

        final String target = IndexNamePluginTest.TEST_INDEX_NAME;

        // when
        final IndexRequest.Builder requestBuilder = builder.itemBuilder(target, payload);
        final IndexRequest request = requestBuilder.build();

        // then
        assertNotNull(request);
        assertEquals(target, request.index);
        assertEquals(payload, request.source);

    }

    @Test
    public void createsBatchRequestBuilder() throws Exception {

        // given
        final Serializer<Object> serializer = spy(ElasticsearchBulkPlugin.newBuilder().createItemSerializer());
        final Deserializer<BatchResult> deserializer = mock(Deserializer.class);

        final String mappingType = UUID.randomUUID().toString();
        final String filterPath = UUID.randomUUID().toString();
        final ElasticsearchBulkAPI builder = new ElasticsearchBulkAPI(mappingType, filterPath, serializer, deserializer);

        final ItemSource<ByteBuf> batchBuffer = createDefaultTestBatchBuffer();

        final String target = IndexNamePluginTest.TEST_INDEX_NAME;

        final String payloadString = UUID.randomUUID().toString();
        final ItemSource payload = createTestItemSource(payloadString);
        final IndexRequest.Builder indexRequestBuilder = builder.itemBuilder(target, payload);
        final IndexRequest indexRequest = spy(indexRequestBuilder.build());

        // when
        final BatchRequest.Builder requestBuilder = builder.batchBuilder();
        requestBuilder.withBuffer(batchBuffer);
        requestBuilder.add(indexRequest);

        final BatchRequest request = requestBuilder.build();
        final ItemSource serialized = request.serialize();

        // then
        verify(serializer).writeAsBytes(eq(indexRequest));

        final ByteBuf source = (ByteBuf) serialized.getSource();
        final String batchString = source.toString(StandardCharsets.UTF_8);

        assertThat(batchString, containsString(target));
        assertThat(batchString, containsString(mappingType));
        assertThat(batchString, containsString(payloadString));

        assertEquals("/_bulk?filter_path=" + filterPath, request.getURI());

    }

    ItemSource createTestItemSource(final String payloadString) {

        final CompositeByteBuf buffer = ByteBufItemSourceTest.createDefaultTestByteBuf();
        buffer.writeBytes(payloadString.getBytes(StandardCharsets.UTF_8));

        return ByteBufItemSourceTest.createTestItemSource(buffer, itemSource -> {});

    }

    private ItemSource<ByteBuf> createDefaultTestBatchBuffer() {
        final CompositeByteBuf batchByteBuf = ByteBufItemSourceTest.createDefaultTestByteBuf();
        return ByteBufItemSourceTest.createTestItemSource(batchByteBuf, itemSource -> {});
    }

}
