package org.appenders.log4j2.elasticsearch.hc;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Serializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.hc.IndexRequestTest.createIndexRequestBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BatchRequestTest {

    @SuppressWarnings("unchecked")
    public static BatchRequest createTestBatch(final BatchRequest.Builder builder, final ItemSource<ByteBuf>... payloads) {

        //noinspection unchecked
        builder.withBuffer(createTestItemSource())
                .withItemSerializer(mock(Serializer.class))
                .withResultDeserializer(mock(Deserializer.class));

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.add(createIndexRequestBuilder(payload)
                    .build());
        }
        return spy(builder.build());
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        // when
        BatchRequest batchRequest = builder.build();

        // then
        assertNotNull(batchRequest);
        assertEquals("/_bulk", batchRequest.getURI());
        assertEquals("POST", batchRequest.getHttpMethodName());

    }

    @Test
    public void builderFailsWhenBufferIsNull() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withBuffer(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("buffer cannot be null"));

    }

    @Test
    public void builderFailsWhenSerializerIsNull() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();
        builder.withItemSerializer(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSerializer cannot be null"));

    }

    @Test
    public void builderFailsWhenResultDeserializerIsNull() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();
        builder.withResultDeserializer(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resultDeserializer cannot be null"));

    }

    @Test
    public void builderFailsWhenObjectWriterIsNull() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withObjectWriter(null));

        // then
        assertThat(exception.getMessage(), containsString("objectWriter cannot be null"));

    }

    @Test
    public void builderSupportsLegacyObjectWriterSetter() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder()
                .withItemSerializer(null);

        final ObjectWriter objectWriter = mock(ObjectWriter.class);

        // when
        builder.withObjectWriter(objectWriter);
        final BatchRequest batchRequest = builder.build();

        // then
        assertNotNull(batchRequest);

    }

    @Test
    public void builderCanStoreAction() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withItemSerializer(null);

        // when
        builder.add(mock(IndexRequest.class));

        // then
        assertEquals(1, builder.items.size());

    }

    @Test
    public void builderCanStoreActionWithProvidedCollection() {

        // given
        final Collection<IndexRequest> expectedItems = new ArrayList<>();
        final BatchRequest.Builder builder = new BatchRequest.Builder(expectedItems)
                .withItemSerializer(mock(Serializer.class))
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(mock(ByteBufItemSource.class));

        builder.withItemSerializer(null);

        // when
        builder.add(mock(IndexRequest.class));

        // then
        assertEquals(1, expectedItems.size());

    }

    @Test
    public void builderCanStoreMultipleActionsAtOnce() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        Collection<IndexRequest> actions = new ArrayList<>();
        int expectedSize = new Random().nextInt(100) + 1;
        for (int i = 0; i < expectedSize; i++) {
            actions.add(mock(IndexRequest.class));
        }

        // when
        builder.add(actions);

        // then
        assertEquals(expectedSize, builder.items.size());

    }

    @Test
    public void canSerializeUniqueItemsSeparately() throws Exception {

        // given
        final Serializer<Object> serializer = spy(new JacksonSerializer<>(new ObjectMapper().writerFor(IndexRequest.class)));

        final ItemSource<ByteBuf> source1 = createTestItemSource();
        final String index1 = UUID.randomUUID().toString();
        final String mappingType = UUID.randomUUID().toString();
        final IndexRequest action1 = createIndexRequestBuilder(source1)
                .index(index1)
                .type(mappingType)
                .build();

        final ItemSource<ByteBuf> source2 = createTestItemSource();
        final String index2 = UUID.randomUUID().toString();

        final IndexRequest action2 = createIndexRequestBuilder(source2)
                .index(index2)
                .type(mappingType)
                .build();

        @SuppressWarnings("unchecked")
        final BatchRequest request = new BatchRequest.Builder()
                .withItemSerializer(serializer)
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(createTestItemSource())
                .add(action1)
                .add(action2)
                .build();

        // when
        request.serialize();

        // then
        final ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(serializer, times(2)).write(any(), captor.capture());
        final List<IndexRequest> allValues = captor.getAllValues();
        assertEquals(2, allValues.size());
        assertEquals(index1, allValues.get(0).getIndex());
        assertEquals(index2, allValues.get(1).getIndex());

        assertEquals(mappingType, allValues.get(0).getType());
        assertEquals(mappingType, allValues.get(1).getType());

    }

    @Test
    public void itemsAreIdenticalIfIndicesAndTypeAreTheSame() {

        // given
        String index1 = UUID.randomUUID().toString();
        String type1 = UUID.randomUUID().toString();

        IndexRequest item1 = IndexRequestTest.createIndexRequestBuilder()
                .type(type1)
                .index(index1)
                .build();

        IndexRequest item2 = IndexRequestTest.createIndexRequestBuilder()
                .type(type1)
                .index(index1)
                .build();

        Collection<IndexRequest> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        // when
        BatchRequest batchRequest = createDefaultTestObjectBuilder().build();
        IndexRequest result = batchRequest.uniformAction(items);

        // then
        assertEquals(result, item1);

    }

    @Test
    public void itemsAreNotIdenticalIfIndicesAreDifferent() {

        // given
        String type1 = UUID.randomUUID().toString();

        IndexRequest item1 = IndexRequestTest.createIndexRequestBuilder()
                .type(type1)
                .index(UUID.randomUUID().toString())
                .build();

        IndexRequest item2 = IndexRequestTest.createIndexRequestBuilder()
                .type(type1)
                .index(UUID.randomUUID().toString())
                .build();

        Collection<IndexRequest> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        BatchRequest batchRequest = createDefaultTestObjectBuilder().build();

        // when
        IndexRequest result = batchRequest.uniformAction(items);

        // then
        assertNull(result);

    }

    @Test
    public void itemsAreNotIdenticalIfTypesAreDifferent() {

        // given
        String index = UUID.randomUUID().toString();

        IndexRequest item1 = IndexRequestTest.createIndexRequestBuilder()
                .type(UUID.randomUUID().toString())
                .index(index)
                .build();

        IndexRequest item2 = IndexRequestTest.createIndexRequestBuilder()
                .type(UUID.randomUUID().toString())
                .index(index)
                .build();

        Collection<IndexRequest> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        BatchRequest batchRequest = createDefaultTestObjectBuilder().build();

        // when
        IndexRequest result = batchRequest.uniformAction(items);

        // then
        assertNull(result);

    }

    @Test
    public void itemsAreNotIdenticalIfBothIndicesAndTypesAreDifferent() {

        // given
        IndexRequest item1 = IndexRequestTest.createIndexRequestBuilder()
                .type(UUID.randomUUID().toString())
                .index(UUID.randomUUID().toString())
                .build();

        IndexRequest item2 = IndexRequestTest.createIndexRequestBuilder()
                .type(UUID.randomUUID().toString())
                .index(UUID.randomUUID().toString())
                .build();

        Collection<IndexRequest> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        BatchRequest batchRequest = createDefaultTestObjectBuilder().build();

        // when
        IndexRequest result = batchRequest.uniformAction(items);

        // then
        assertNull(result);

    }

    @Test
    public void canSerializeOnceIfAllItemsAreTheSame() throws Exception {

        // given
        final Serializer<Object> serializer = spy(new JacksonSerializer<>(new ObjectMapper().writerFor(IndexRequest.class)));

        final ItemSource<ByteBuf> source1 = createTestItemSource();
        final String index = UUID.randomUUID().toString();
        final String mappingType = UUID.randomUUID().toString();
        final IndexRequest action1 = createIndexRequestBuilder(source1)
                .index(index)
                .type(mappingType)
                .build();

        final ItemSource<ByteBuf> source2 = createTestItemSource();
        final IndexRequest action2 = createIndexRequestBuilder(source2)
                .index(index)
                .type(mappingType)
                .build();

        @SuppressWarnings("unchecked")
        final BatchRequest batchRequest = new BatchRequest.Builder()
                .withItemSerializer(serializer)
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(createTestItemSource())
                .add(action1)
                .add(action2)
                .build();

        // when
        batchRequest.serialize();

        // then
        final ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(serializer, times(1)).writeAsBytes(captor.capture());

        final List<IndexRequest> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        assertEquals(index, allValues.get(0).getIndex());

    }

    @Test
    public void callingCompletedReleasesItemSource() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);

        BatchRequest request = builder.build();

        // when
        request.completed();

        // then
        verify(buffer).release();

    }

    @Test
    public void callingCompletedReleasesActions() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);

        Collection<IndexRequest> actions = new ArrayList<>();
        IndexRequest indexRequest = spy(createIndexRequestBuilder(mock(ByteBufItemSource.class))
                .build());
        actions.add(indexRequest);
        builder.add(actions);

        BatchRequest request = builder.build();

        // when
        request.completed();

        // then
        verify(indexRequest).completed();

    }

    public static BatchRequest.Builder createDefaultTestObjectBuilder() {
        //noinspection unchecked
        return new BatchRequest.Builder()
                .withItemSerializer(mock(Serializer.class))
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(mock(ByteBufItemSource.class));
    }

}
