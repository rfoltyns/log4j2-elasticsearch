package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery.Builder;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.TEST_INDEX_TEMPLATE;
import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.TEST_PATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BatchDeliveryTest {

    private static final int TEST_BATCH_SIZE = 100;
    private static final int TEST_DELIVERY_INTERVAL = 100;
    private static final String TEST_INDEX_NAME = "test_index";

    public static final String TEST_SERVER_URIS = "http://localhost:9200";

    public static TestHttpObjectFactory.Builder createTestObjectFactoryBuilder() {
        TestHttpObjectFactory.Builder builder = TestHttpObjectFactory.newBuilder();
        builder.withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    public static Builder createTestBatchDeliveryBuilder() {
        return spy(AsyncBatchDelivery.newBuilder()
                .withBatchSize(TEST_BATCH_SIZE)
                .withDeliveryInterval(TEST_DELIVERY_INTERVAL)
                .withClientObjectFactory(createTestObjectFactoryBuilder().build()))
                .withFailoverPolicy(new NoopFailoverPolicy())
                .withIndexTemplate(new IndexTemplate(TEST_INDEX_TEMPLATE, TEST_PATH));
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
    public void builderFailsWhenClientObjectFactoryIsNull() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withClientObjectFactory(null);

        // when
        batchDeliveryBuilder.build();

    }

    @Test(expected = ConfigurationException.class)
    public void deprecatedDeliveryFailsWhenIndexNameIsNull() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();
        batchDeliveryBuilder.withIndexName(null);
        BatchDelivery<String> delivery = batchDeliveryBuilder.build();

        // when
        delivery.add("testLog");

    }

    @Test
    public void deliveryAddsBatchItemToBatchEmitter() throws Exception {

        // given
        TestHttpObjectFactory objectFactory = createTestObjectFactoryBuilder().build();

        TestBatchEmitterFactory batchEmitterFactory = spy(new TestBatchEmitterFactory());
        TestAsyncBatchDelivery.mockedProvider = batchEmitterFactory;

        BatchEmitter emitter = batchEmitterFactory.createInstance(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, objectFactory, new NoopFailoverPolicy());

        AsyncBatchDelivery.Builder builder = createTestBatchDeliveryBuilder();
        TestAsyncBatchDelivery testAsyncBatchDelivery = spy(new TestAsyncBatchDelivery(TEST_INDEX_NAME,
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                objectFactory,
                new NoopFailoverPolicy(),
                null));
        Mockito.when(builder.build()).thenReturn(testAsyncBatchDelivery);

        AsyncBatchDelivery delivery = builder.build();

        String testMessage = "test message";

        // when
        delivery.add("testIndexName", testMessage);

        // then
        ArgumentCaptor<BulkEmitterTest.TestBatchItem> captor = ArgumentCaptor.forClass(BulkEmitterTest.TestBatchItem.class);

        verify(emitter, times(1)).add(captor.capture());
        assertEquals(testMessage, captor.getValue().getData(null));
    }

    @Test
    public void batchDeliveryExecutesIndexTemplateDuringStartupWhenIndexTemplatesNotNull() {

        // given
        TestBatchEmitterFactory batchEmitterFactory = spy(new TestBatchEmitterFactory());
        TestAsyncBatchDelivery.mockedProvider = batchEmitterFactory;

        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());
        IndexTemplate testIndexTemplate = spy(new IndexTemplate(TEST_INDEX_TEMPLATE, TEST_PATH));

        new TestAsyncBatchDelivery(TEST_INDEX_NAME,
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                objectFactory,
                new NoopFailoverPolicy(),
                testIndexTemplate);

        // then
        Mockito.verify(objectFactory, times(1)).execute(eq(testIndexTemplate));
    }

    private static class TestAsyncBatchDelivery extends AsyncBatchDelivery {

        public static BatchEmitterServiceProvider mockedProvider;

        public TestAsyncBatchDelivery(String indexName, int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, IndexTemplate indexTemplate) {
            super(indexName, batchSize, deliveryInterval, objectFactory, failoverPolicy, indexTemplate);
        }

        @Override
        protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
            return mockedProvider;
        }

    }


}
