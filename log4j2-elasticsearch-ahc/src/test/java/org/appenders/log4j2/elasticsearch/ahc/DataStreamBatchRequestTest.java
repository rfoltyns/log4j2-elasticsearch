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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.appenders.log4j2.elasticsearch.ahc.DataStreamItemTest.createDataStreamItemRequestBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class DataStreamBatchRequestTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        // when
        final BatchRequest batchRequest = builder.build();

        // then
        assertNotNull(batchRequest);

    }

    @Test
    public void uriIsBasedOnBatchItems() {

        // given
        final String expectedIndex = UUID.randomUUID().toString();
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        final DataStreamItem dataStreamItem = mock(DataStreamItem.class);
        when(dataStreamItem.getIndex()).thenReturn(expectedIndex);

        builder.add(dataStreamItem);

        // when
        final BatchRequest batchRequest = builder.build();

        // then
        assertNotNull(batchRequest);
        assertEquals(expectedIndex + "/_bulk", batchRequest.getURI());

    }

    @Test
    public void uriContainsFilterPath() {

        // given
        final String expectedIndex = UUID.randomUUID().toString();
        final String expectedFilterPath = UUID.randomUUID().toString();
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder()
                .withFilterPath(expectedFilterPath);

        final DataStreamItem dataStreamItem = mock(DataStreamItem.class);
        when(dataStreamItem.getIndex()).thenReturn(expectedIndex);

        builder.add(dataStreamItem);

        // when
        final BatchRequest batchRequest = builder.build();

        // then
        assertNotNull(batchRequest);
        assertEquals(expectedIndex + "/_bulk?filter_path=" + expectedFilterPath, batchRequest.getURI());

    }

    @Test
    public void builderFailsWhenBufferIsNull() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

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
    public void builderCanStoreAction() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withItemSerializer(null);

        // when
        builder.add(mock(DataStreamItem.class));

        // then
        assertEquals(1, builder.items.size());

    }

    @Test
    public void builderCanStoreMultipleActionsAtOnce() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        final Collection<IndexRequest> actions = new ArrayList<>();
        final int expectedSize = new Random().nextInt(100) + 1;
        for (int i = 0; i < expectedSize; i++) {
            actions.add(mock(DataStreamItem.class));
        }

        // when
        builder.add(actions);

        // then
        assertEquals(expectedSize, builder.items.size());

    }

    @Test
    public void throwsOnItemsWithDifferentIndicesInSameBatch() {

        // given
        final Serializer<Object> serializer = spy(new JacksonSerializer<>(new ObjectMapper().writerFor(IndexRequest.class)));

        final ItemSource<ByteBuf> source1 = createTestItemSource();
        final String index1 = UUID.randomUUID().toString();
        final IndexRequest action1 = createDataStreamItemRequestBuilder(source1)
                .index(index1)
                .build();

        final ItemSource<ByteBuf> source2 = createTestItemSource();
        final String index2 = UUID.randomUUID().toString();

        final IndexRequest action2 = createDataStreamItemRequestBuilder(source2)
                .index(index2)
                .build();

        @SuppressWarnings("unchecked")
        final BatchRequest request = new DataStreamBatchRequest.Builder()
                .withItemSerializer(serializer)
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(createTestItemSource())
                .add(action1)
                .add(action2)
                .build();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, request::serialize);

        // then
        assertThat(exception.getMessage(), containsString("Items for different indices found"));
        assertThat(exception.getMessage(), containsString(index1));
        assertThat(exception.getMessage(), containsString(index2));

    }

    @Test
    public void canSerializeOnceIfAllItemsAreTheSame() throws Exception {

        // given
        final Serializer<Object> serializer = spy(new JacksonSerializer<>(new ObjectMapper().writerFor(IndexRequest.class)));

        final ItemSource<ByteBuf> source1 = createTestItemSource();
        final String index = UUID.randomUUID().toString();
        final String mappingType = UUID.randomUUID().toString();
        final IndexRequest action1 = createDataStreamItemRequestBuilder(source1)
                .index(index)
                .type(mappingType)
                .build();

        final ItemSource<ByteBuf> source2 = createTestItemSource();
        final IndexRequest action2 = createDataStreamItemRequestBuilder(source2)
                .index(index)
                .type(mappingType)
                .build();

        @SuppressWarnings("unchecked")
        final BatchRequest batchRequest = new DataStreamBatchRequest.Builder()
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

        assertEquals(index + "/_bulk", batchRequest.getURI());
        assertEquals("POST", batchRequest.getHttpMethodName());

    }

    @Test
    public void callingCompletedReleasesItemSource() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        final ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);

        final BatchRequest request = builder.build();

        // when
        request.completed();

        // then
        verify(buffer).release();

    }

    @Test
    public void callingCompletedReleasesActions() {

        // given
        final BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        final ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);

        final Collection<IndexRequest> actions = new ArrayList<>();
        final IndexRequest indexRequest = spy(createDataStreamItemRequestBuilder(mock(ByteBufItemSource.class))
                .build());
        actions.add(indexRequest);
        builder.add(actions);

        final BatchRequest request = builder.build();

        // when
        request.completed();

        // then
        verify(indexRequest).completed();

    }

    public static BatchRequest.Builder createDefaultTestObjectBuilder() {
        //noinspection unchecked
        return new DataStreamBatchRequest.Builder()
                .withItemSerializer(mock(Serializer.class))
                .withResultDeserializer(mock(Deserializer.class))
                .withBuffer(mock(ByteBufItemSource.class));
    }

}
