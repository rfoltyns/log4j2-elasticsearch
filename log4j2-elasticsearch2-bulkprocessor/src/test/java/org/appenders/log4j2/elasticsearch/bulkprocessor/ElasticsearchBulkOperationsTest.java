package org.appenders.log4j2.elasticsearch.bulkprocessor;

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



import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.StringItemSource;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.BulkActionIntrospector;
import org.elasticsearch.action.index.IndexRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.appenders.log4j2.elasticsearch.StringItemSourceTest.createTestStringItemSource;
import static org.mockito.Mockito.spy;

public class ElasticsearchBulkOperationsTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnBatchBuilderCreate() {

        // given
        BatchOperations<BulkRequest> batchOperations = createDefaultTestBulkRequestBatchOperations();

        expectedException.expect(UnsupportedOperationException.class);

        // when
        batchOperations.createBatchBuilder();

    }

    @Test
    public void createsBatchItemWithStringSource() {

        // given
        BatchOperations<BulkRequest> batchOperations = createDefaultTestBulkRequestBatchOperations();


        String expectedPayload = "expectedPayload";

        // when
        IndexRequest batchItem = (IndexRequest) batchOperations.createBatchItem("testIndex", expectedPayload);

        // then
        Assert.assertEquals(expectedPayload, new BulkActionIntrospector().getPayload(batchItem));
        Assert.assertEquals("index", batchItem.opType().lowercase());

    }

    @Test
    public void createsBatchItemWithItemSource() {

        // given
        BatchOperations<BulkRequest> batchOperations = createDefaultTestBulkRequestBatchOperations();

        String expectedPayload = "expectedPayload";
        StringItemSource itemSource = spy(createTestStringItemSource(expectedPayload));

        // when
        IndexRequest batchItem = (IndexRequest) batchOperations.createBatchItem("testIndex", itemSource);

        // then
        Assert.assertEquals(expectedPayload, new BulkActionIntrospector().getPayload(batchItem));
        Assert.assertEquals("index", batchItem.opType().lowercase());

    }

    private BatchOperations<BulkRequest> createDefaultTestBulkRequestBatchOperations() {
        BulkProcessorObjectFactory factory = BulkProcessorObjectFactoryTest
                .createTestObjectFactoryBuilder()
                .build();

        return spy(factory.createBatchOperations());
    }

}
