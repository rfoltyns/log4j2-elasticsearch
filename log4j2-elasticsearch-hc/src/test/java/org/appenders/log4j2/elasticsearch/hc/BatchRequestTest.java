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
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.appenders.log4j2.elasticsearch.hc.IndexRequestTest.createIndexRequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BatchRequestTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        // when
        BatchRequest batchRequest = builder.build();

        // then
        Assert.assertNotNull(batchRequest);

    }

    @Test
    public void builderFailsWhenBufferIsNull() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withBuffer(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("buffer cannot be null");

        // when
        builder.build();
    }

    @Test
    public void builderFailsWhenObjectWriterIsNull() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withObjectWriter(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("objectWriter cannot be null");

        // when
        builder.build();
    }

    @Test
    public void builderCanStoreAction() {

        // given
        BatchRequest.Builder builder = createDefaultTestObjectBuilder();

        builder.withObjectWriter(null);

        // when
        builder.add(mock(IndexRequest.class));

        // then
        assertEquals(1, builder.items.size());
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
    public void canSerializeUniqueItemsSeparately() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(IndexRequest.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index1 = UUID.randomUUID().toString();
        String mappingType = UUID.randomUUID().toString();
        IndexRequest action1 = createIndexRequestBuilder(source1)
                .index(index1)
                .type(mappingType)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        String index2 = UUID.randomUUID().toString();

        IndexRequest action2 = createIndexRequestBuilder(source2)
                .index(index2)
                .type(mappingType)
                .build();

        BatchRequest request = new BatchRequest.Builder()
                .withObjectWriter(writer)
                .withBuffer(createTestItemSource())
                .add(action1)
                .add(action2)
                .build();

        // when
        request.serialize();

        // then
        ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(writer, times(2)).writeValue((OutputStream)any(), captor.capture());
        List<IndexRequest> allValues = captor.getAllValues();
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

    public static BatchRequest createTestBatch(BatchRequest.Builder builder, ItemSource<ByteBuf>... payloads) {
        builder.withBuffer(createTestItemSource());
        builder.withObjectWriter(mock(ObjectWriter.class));

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.add(createIndexRequestBuilder(payload)
                    .build());
        }
        return spy(builder.build());
    }

    @Test
    public void canSerializeOnceIfAllItemsAreTheSame() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(IndexRequest.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index = UUID.randomUUID().toString();
        String mappingType = UUID.randomUUID().toString();
        IndexRequest action1 = createIndexRequestBuilder(source1)
                .index(index)
                .type(mappingType)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        IndexRequest action2 = createIndexRequestBuilder(source2)
                .index(index)
                .type(mappingType)
                .build();

        BatchRequest batchRequest = new BatchRequest.Builder()
                .withObjectWriter(writer)
                .withBuffer(createTestItemSource())
                .add(action1)
                .add(action2)
                .build();

        // when
        batchRequest.serialize();

        // then
        ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(writer, times(1)).writeValueAsBytes(captor.capture());

        List<IndexRequest> allValues = captor.getAllValues();
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

        BatchRequest request = builder.build();

        Collection<IndexRequest> actions = new ArrayList<>();
        IndexRequest indexRequest = spy(createIndexRequestBuilder(mock(ByteBufItemSource.class))
                .build());
        actions.add(indexRequest);
        builder.add(actions);

        // when
        request.completed();

        // then
        verify(indexRequest).release();

    }

    public static BatchRequest.Builder createDefaultTestObjectBuilder() {
        BatchRequest.Builder builder = new BatchRequest.Builder()
                .withObjectWriter(mock(ObjectWriter.class))
                .withBuffer(mock(ByteBufItemSource.class));
        return builder;
    }

}
