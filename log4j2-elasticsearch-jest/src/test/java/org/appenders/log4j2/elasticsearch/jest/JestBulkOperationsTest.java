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



import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.junit.Assert;
import org.junit.Test;

public class JestBulkOperationsTest {

    @Test
    public void bulkContainsAddedItem() {
        // given
        BatchOperations<Bulk> bulkOperations = JestHttpObjectFactoryTest.createTestObjectFactoryBuilder().build().createBatchOperations();
        BatchBuilder<Bulk> batchBuilder = bulkOperations.createBatchBuilder();

        String testPayload = "{ \"testfield\": \"testvalue\" }";
        Index item = (Index) bulkOperations.createBatchItem("testIndex", testPayload);

        // when
        batchBuilder.add(item);
        Bulk bulk = batchBuilder.build();

        // then
        JestBatchIntrospector introspector = new JestBatchIntrospector();
        Assert.assertEquals(testPayload, introspector.items(bulk).get(0));
    }

}
