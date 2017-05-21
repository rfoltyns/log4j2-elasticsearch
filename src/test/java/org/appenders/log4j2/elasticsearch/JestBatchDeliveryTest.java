package org.appenders.log4j2.elasticsearch;

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


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery.Builder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;

import io.searchbox.action.BulkableAction;

public class JestBatchDeliveryTest {

    private static final int TEST_BATCH_SIZE = 100;
    private static final int TEST_DELIVERY_INTERVAL = 100;
    private static final String TEST_INDEX_NAME = "test_index";
    
    public static AsyncBatchDelivery.Builder createTestBatchDeliveryBuilder() {
        return spy(AsyncBatchDelivery.newBuilder()
                .withIndexName(TEST_INDEX_NAME)
                .withBatchSize(TEST_BATCH_SIZE)
                .withDeliveryInterval(TEST_DELIVERY_INTERVAL)
                .withClientObjectFactory(JestHttpObjectFactoryTest.createTestObjectFactoryBuilder().build()))
                .withFailoverPolicy(new NoopFailoverPolicy());
    }

    @Test
    public void builderReturnsNonNullObject() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        // when
        BatchDelivery<String> delivery = batchDeliveryBuilder.build();

        // then
        Assert.assertNotNull(delivery);
    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsWhenIndexNameIsNull() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withIndexName(null);

        // when
        batchDeliveryBuilder.build();

    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsWhenClientObjectFactoryIsNull() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withClientObjectFactory(null);

        // when
        batchDeliveryBuilder.build();

    }

    @Test
    public void deliveryAddsBatchItemToBatchEmitter() throws Exception {

        // given
        BulkEmitter emitter = BulkEmitterTest.createTestBulkEmitter(
                BulkEmitterTest.TEST_BATCH_SIZE, BulkEmitterTest.LARGE_TEST_INTERVAL);

        AsyncBatchDelivery delivery = createTestBatchDeliveryBuilder().build();
        PowerMockito.field(AsyncBatchDelivery.class, "bulkEmitter").set(delivery, emitter);

        String testMessage = "test message";

        // when
        delivery.add(testMessage);

        // then
        ArgumentCaptor<BulkableAction> captor = ArgumentCaptor.forClass(BulkableAction.class);

        verify(emitter, times(1)).add(captor.capture());
        assertEquals(testMessage, captor.getValue().getData(null));
    }
}
