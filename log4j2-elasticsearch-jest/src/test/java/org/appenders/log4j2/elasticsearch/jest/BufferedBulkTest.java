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
import io.searchbox.action.BulkableAction;
import org.appenders.log4j2.elasticsearch.BufferedItemSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BufferedBulkTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // then
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        // when
        BufferedBulk bufferedBulk = builder.build();

        // then
        Assert.assertNotNull(bufferedBulk);

    }

    @Test
    public void builderFailsWhenBufferedSourceIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withBuffer(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("bufferedSource cannot be null");

        // when
        builder.build();
    }

    @Test
    public void builderFailsWhenObjectReaderIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectReader(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("objectReader cannot be null");

        // when
        builder.build();
    }

    @Test
    public void builderFailsWhenObjectWriterIsNull() {

        // given
        BufferedBulk.Builder builder = createDefaultTestMockedBuilder();

        builder.withObjectWriter(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("objectWriter cannot be null");

        // when
        builder.build();
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

    private BufferedBulk.Builder createDefaultTestMockedBuilder() {
        BufferedBulk.Builder builder = new BufferedBulk.Builder();
        builder.withObjectReader(mock(ObjectReader.class));
        builder.withObjectWriter(mock(ObjectWriter.class));
        builder.withBuffer(mock(BufferedItemSource.class));
        return builder;
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

        BufferedItemSource buffer = mock(BufferedItemSource.class);
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

        BufferedItemSource buffer = mock(BufferedItemSource.class);
        builder.withBuffer(buffer);

        BufferedBulk bulk = builder.build();

        List<BulkableAction> actions = new ArrayList<>();
        BufferedIndex bufferedIndex = spy(new BufferedIndex.Builder(mock(BufferedItemSource.class)).build());
        actions.add(bufferedIndex);
        builder.addAction(actions);

        // when
        bulk.completed();

        // then
        verify(bufferedIndex).release();

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
