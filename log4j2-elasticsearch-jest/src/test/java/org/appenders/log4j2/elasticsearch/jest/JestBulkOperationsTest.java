package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
