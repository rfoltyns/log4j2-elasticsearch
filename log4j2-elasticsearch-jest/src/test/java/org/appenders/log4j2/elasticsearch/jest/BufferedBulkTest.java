package org.appenders.log4j2.elasticsearch.jest;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.searchbox.action.BulkableAction;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BufferedBulkTest {

    @Test
    public void builderBuildsSuccessfully() {

        // then
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        // when
        BufferedBulk bufferedBulk = builder.build();

        // then
        assertNotNull(bufferedBulk);

    }

    @Test
    public void builderFailsWhenBufferedSourceIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withBuffer(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("bufferedSource cannot be null"));

    }

    @Test
    public void builderFailsWhenObjectReaderIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectReader(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("objectReader cannot be null"));

    }

    @Test
    public void builderFailsWhenObjectWriterIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectWriter(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("objectWriter cannot be null"));

    }

    @Test
    public void builderCanStoreAction() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectWriter(null);

        // when
        builder.addAction(mock(BulkableAction.class));

        // then
        assertEquals(1, builder.actions.size());

    }

    @Test
    public void builderCanStoreMultipleActionsAtOnce() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        List<BulkableAction> actions = new ArrayList<>();
        int expectedSize = new Random().nextInt(100) + 1;
        for (int i = 0; i < expectedSize; i++) {
            actions.add(mock(BulkableAction.class));
        }

        // when
        builder.addAction(actions);

        // then
        assertEquals(expectedSize, builder.actions.size());

    }

    @Test
    public void canSerializeUniqueItemsSeparately() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(BufferedIndex.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index1 = UUID.randomUUID().toString();
        BulkableAction action1 = new BufferedIndex.Builder(source1)
                .index(index1)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        String index2 = UUID.randomUUID().toString();
        BulkableAction action2 = new BufferedIndex.Builder(source2)
                .index(index2)
                .build();

        BufferedBulk bulk = (BufferedBulk) new BufferedBulk.Builder()
                .withObjectWriter(writer)
                .withObjectReader(mock(ObjectReader.class))
                .withBuffer(createTestItemSource())
                .addAction(action1)
                .addAction(action2)
                .build();

        // when
        bulk.serializeRequest();

        // then
        ArgumentCaptor<BufferedIndex> captor = ArgumentCaptor.forClass(BufferedIndex.class);
        verify(writer, times(2)).writeValue((OutputStream)any(), captor.capture());
        List<BufferedIndex> allValues = captor.getAllValues();
        assertEquals(2, allValues.size());
        assertEquals(2, bulk.getActions().size());
        assertEquals(index1, allValues.get(0).getIndex());
        assertEquals(index2, allValues.get(1).getIndex());

    }

    @Test
    public void canSerializeBulk() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(BufferedIndex.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index1 = UUID.randomUUID().toString();
        BulkableAction action1 = new BufferedIndex.Builder(source1)
                .index(index1)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        String index2 = UUID.randomUUID().toString();
        BulkableAction action2 = new BufferedIndex.Builder(source2)
                .index(index2)
                .build();

        BufferedBulk bulk = (BufferedBulk) new BufferedBulk.Builder()
                .withObjectWriter(writer)
                .withObjectReader(mock(ObjectReader.class))
                .withBuffer(createTestItemSource())
                .addAction(action1)
                .addAction(action2)
                .build();

        // when
        bulk.serializeRequest();

        // then
        assertEquals("/_bulk", bulk.getURI());
        assertFalse(bulk.getURI().contains(index1));
        assertFalse(bulk.getURI().contains(index2));
        ArgumentCaptor<BufferedIndex> captor = ArgumentCaptor.forClass(BufferedIndex.class);
        verify(writer, times(2)).writeValue((OutputStream)any(), captor.capture());
        List<BufferedIndex> allValues = captor.getAllValues();
        assertEquals(2, allValues.size());
        assertEquals(index1, allValues.get(0).getIndex());
        assertEquals(index2, allValues.get(1).getIndex());

    }

    @Test
    public void canSerializeOnceIfAllItemsAreTheSame() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(BufferedIndex.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index = UUID.randomUUID().toString();
        BulkableAction action1 = new BufferedIndex.Builder(source1)
                .index(index)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        BulkableAction action2 = new BufferedIndex.Builder(source2)
                .index(index)
                .build();

        BufferedBulk bulk = (BufferedBulk) new BufferedBulk.Builder()
                .withObjectWriter(writer)
                .withObjectReader(mock(ObjectReader.class))
                .withBuffer(createTestItemSource())
                .addAction(action1)
                .addAction(action2)
                .build();

        // when
        bulk.serializeRequest();

        // then
        assertEquals("/_bulk", bulk.getURI());
        ArgumentCaptor<BufferedIndex> captor = ArgumentCaptor.forClass(BufferedIndex.class);
        verify(writer, times(1)).writeValueAsBytes(captor.capture());

        List<BufferedIndex> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        assertEquals(index, allValues.get(0).getIndex());

    }

    @Test
    public void canSerializeDataStreamBulk() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(BufferedIndex.class));

        ItemSource<ByteBuf> source1 = createTestItemSource();
        String index = UUID.randomUUID().toString();
        BulkableAction action1 = new BufferedIndex.Builder(source1)
                .index(index)
                .build();

        ItemSource<ByteBuf> source2 = createTestItemSource();
        BulkableAction action2 = new BufferedIndex.Builder(source2)
                .index(index)
                .build();

        BufferedBulk bulk = (BufferedBulk) new BufferedBulk.Builder()
                .withDataStreamsEnabled(true)
                .withObjectWriter(writer)
                .withObjectReader(mock(ObjectReader.class))
                .withBuffer(createTestItemSource())
                .addAction(action1)
                .addAction(action2)
                .build();

        // when
        bulk.serializeRequest();

        // then
        assertTrue(bulk.getURI().endsWith(index + "/_bulk"));
        ArgumentCaptor<BufferedIndex> captor = ArgumentCaptor.forClass(BufferedIndex.class);
        verify(writer, times(1)).writeValueAsBytes(captor.capture());

        List<BufferedIndex> allValues = captor.getAllValues();
        assertEquals(1, allValues.size());
        assertEquals(index, allValues.get(0).getIndex());

    }

    @Test
    public void throwsIfCannotDeriveIndexOnGetURI() throws IOException {

        // given
        ObjectWriter writer = spy(new ObjectMapper().writerFor(BufferedIndex.class));

        BufferedBulk bulk = new BufferedBulk.Builder()
                .withDataStreamsEnabled(true)
                .withObjectWriter(writer)
                .withObjectReader(mock(ObjectReader.class))
                .withBuffer(createTestItemSource())
                .build();

        // when
        bulk.serializeRequest();
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, bulk::getURI);

        // then
        assertThat(exception.getMessage(), containsString("Unable to derive index name from empty batch"));

    }

    @Test
    public void canDeserializeErrorResponse() throws IOException {

        // given
        TestResult testResult = new TestResult();
        testResult.errors = true;
        testResult.took = new Random().nextInt(1000);

        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectReader(new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredReader());

        BufferedBulk bulk = builder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        InputStream inputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(testResult));

        // when
        BufferedBulkResult result = bulk.deserializeResponse(inputStream);

        // then
        assertFalse(result.isSucceeded());
        assertEquals(testResult.took, result.getTook());

    }

    @Test
    public void canDeserializeSuccessResponse() throws IOException {

        // given
        TestResult testResult = new TestResult();
        testResult.errors = false;
        testResult.took = new Random().nextInt(1000);

        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectReader(new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredReader());

        BufferedBulk bulk = builder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        InputStream inputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(testResult));

        // when
        BufferedBulkResult result = bulk.deserializeResponse(inputStream);

        // then
        assertTrue(result.isSucceeded());
        assertEquals(testResult.took, result.getTook());

    }

    @Test
    public void canDeserializeSuccessWithFailedItemsResponse() throws IOException {

        // given
        Random random = new Random();

        BulkResultItem bulkResultItem = createTestErrorBulkResultItem(random.nextInt(1000));

        int took = random.nextInt(1000);

        TestResult testResult = new TestResult();
        testResult.errors = true;
        testResult.took = took;
        testResult.items = new ArrayList<>();
        testResult.items.add(bulkResultItem);

        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();
        builder.withObjectReader(new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredReader());

        BufferedBulk bulk = builder.build();

        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .addMixIn(BulkResultItem.class, BulkResultItemMixIn.class);

        InputStream inputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(testResult));

        // when
        BufferedBulkResult result = bulk.deserializeResponse(inputStream);

        // then
        assertFalse(result.isSucceeded());
        assertEquals(testResult.took, result.getTook());

        BulkResultItem resultItem = testResult.items.get(0);
        assertEquals(bulkResultItem, resultItem);

    }

    @Test
    public void canDeserializeSuccessWithErrorResponse() throws IOException {

        // given
        TestResult testResult = new TestResult();

        testResult.error = createTestBulkError();
        testResult.status = new Random().nextInt(1000);

        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();
        builder.withObjectReader(new BufferedBulkOperations(mock(PooledItemSourceFactory.class)).configuredReader());

        BufferedBulk bulk = builder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        InputStream inputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(testResult));

        // when
        BufferedBulkResult result = bulk.deserializeResponse(inputStream);

        // then
        assertFalse(result.isSucceeded());
        assertEquals(testResult.took, result.getTook());
        assertEquals(testResult.error.getType(), result.getError().getType());
        assertEquals(testResult.error.getReason(), result.getError().getReason());
        assertEquals(testResult.status, result.getStatus());

    }

    @Test
    public void callingCompletedReleasesBulkItemSource() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);

        BufferedBulk bulk = builder.build();

        // when
        bulk.completed();

        // then
        verify(buffer).release();

    }

    @Test
    public void callingCompletedReleasesActions() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        ByteBufItemSource buffer = mock(ByteBufItemSource.class);
        builder.withBuffer(buffer);


        List<BulkableAction> actions = new ArrayList<>();
        BufferedIndex bufferedIndex = spy(new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build());
        actions.add(bufferedIndex);
        builder.addAction(actions);

        BufferedBulk bulk = builder.build();

        // when
        bulk.completed();

        // then
        verify(bufferedIndex).release();

     }

    private BufferedBulk.Builder createDefaultTestMockedBuilder() {
        BufferedBulk.Builder builder = new BufferedBulk.Builder();
        builder.withObjectReader(mock(ObjectReader.class));
        builder.withObjectWriter(mock(ObjectWriter.class));
        builder.withBuffer(mock(ByteBufItemSource.class));
        return builder;
    }

    private BulkError createTestBulkError() {
        BulkError bulkError = new BulkError();
        String errorReason = UUID.randomUUID().toString();
        String errorType = UUID.randomUUID().toString();
        bulkError.setReason(errorReason);
        bulkError.setType(errorType);
        return bulkError;
    }

    private BulkResultItem createTestErrorBulkResultItem(int status) {
        String id = UUID.randomUUID().toString();
        String index = UUID.randomUUID().toString();
        String type = UUID.randomUUID().toString();
        String errorType = UUID.randomUUID().toString();
        String errorReason = UUID.randomUUID().toString();
        String causedByType = UUID.randomUUID().toString();
        String causedByReason = UUID.randomUUID().toString();

        BulkResultItem bulkResultItem = new BulkResultItem();
        bulkResultItem.setId(id);
        bulkResultItem.setIndex(index);
        bulkResultItem.setType(type);
        bulkResultItem.setStatus(status);

        BulkError bulkError = new BulkError();
        bulkError.setType(errorType);
        bulkError.setReason(errorReason);

        BulkError causedByError = new BulkError();
        causedByError.setType(causedByType);
        causedByError.setReason(causedByReason);
        bulkError.setCausedBy(causedByError);

        bulkResultItem.setBulkError(bulkError);

        return bulkResultItem;
    }

}
