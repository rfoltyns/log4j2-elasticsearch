package org.appenders.log4j2.elasticsearch;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery.Builder;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.TEST_INDEX_TEMPLATE;
import static org.appenders.log4j2.elasticsearch.IndexTemplateTest.TEST_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchDeliveryTest {

    private static final int TEST_BATCH_SIZE = 100;
    private static final int TEST_DELIVERY_INTERVAL = 100;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    @Test
    public void batchDeliveryAddObjectDelegatesToProvidedBatchOperationsObjectApi() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery batchDelivery = batchDeliveryBuilder.build();

        String indexName = UUID.randomUUID().toString();
        String logObject = UUID.randomUUID().toString();

        // when
        batchDelivery.add(indexName, logObject);

        // then
        verify(batchOperations).createBatchItem(eq(indexName), eq(logObject));

    }

    @Test
    public void batchDeliveryAddItemSourceDelegatesToProvidedBatchOperationsItemSourceApi() {

        // given
        Builder batchDeliveryBuilder = createTestBatchDeliveryBuilder();

        ClientObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        BatchOperations batchOperations = spy(clientObjectFactory.createBatchOperations());
        when(clientObjectFactory.createBatchOperations()).thenReturn(batchOperations);

        batchDeliveryBuilder.withClientObjectFactory(clientObjectFactory);

        BatchDelivery batchDelivery = batchDeliveryBuilder.build();

        String indexName = UUID.randomUUID().toString();
        ItemSource itemSource = mock(ItemSource.class);

        // when
        batchDelivery.add(indexName, itemSource);

        // then
        verify(batchOperations).createBatchItem(eq(indexName), eq(itemSource));

    }

    @Test
    public void deliveryAddsBatchItemToBatchEmitter() {

        // given
        TestHttpObjectFactory objectFactory = createTestObjectFactoryBuilder().build();

        TestBatchEmitterFactory batchEmitterFactory = spy(new TestBatchEmitterFactory());

        BatchEmitter emitter = batchEmitterFactory.createInstance(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, objectFactory, new NoopFailoverPolicy());

        TestAsyncBatchDelivery delivery = spy(new TestAsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                objectFactory,
                new NoopFailoverPolicy(),
                null) {
            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }
        });

        String testMessage = "test message";

        // when
        delivery.add("testIndexName", testMessage);

        // then
        ArgumentCaptor<BulkEmitterTest.TestBatchItem> captor = ArgumentCaptor.forClass(BulkEmitterTest.TestBatchItem.class);

        verify(emitter, times(1)).add(captor.capture());
        assertEquals(testMessage, captor.getValue().getData(null));
    }

    @Test
    public void lifecycleStartSetsUpIndexTemplateExecutionIfIndexTemplateIsConfigured() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());

        IndexTemplate indexTemplate = mock(IndexTemplate.class);

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .withIndexTemplate(indexTemplate)
                .build();

        // when
        batchDelivery.start();

        // then
        verify(objectFactory).addOperation(any());

        

    }

    @Test
    public void lifecycleStartDoesntSetUpIndexTemplateExecutionIfIndexTemplateIsNotConfigured() {

        // given
        TestHttpObjectFactory objectFactory = spy(createTestObjectFactoryBuilder().build());

        BatchDelivery batchDelivery = createTestBatchDeliveryBuilder()
                .withClientObjectFactory(objectFactory)
                .withIndexTemplate(null)
                .build();

        // when
        batchDelivery.start();

        // then
        verify(objectFactory, never()).addOperation(any());

    }

    @Test
    public void lifecycleStartStartsBatchEmitter() {

        // given
        BatchEmitter batchEmitter = mock(BatchEmitter.class);

        BatchEmitterServiceProvider batchEmitterFactory = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return batchEmitter;
            }
        };

        TestAsyncBatchDelivery batchDelivery = spy(new TestAsyncBatchDelivery(
                TEST_BATCH_SIZE,
                TEST_DELIVERY_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy(),
                null) {
            @Override
            protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
                return batchEmitterFactory;
            }
        });

        // when
        batchDelivery.start();

        // then
        verify(batchEmitter).start();

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createTestBatchDeliveryBuilder().build();
    }

    static class TestAsyncBatchDelivery extends AsyncBatchDelivery {

        private BatchEmitterServiceProvider mockedProvider;

        public TestAsyncBatchDelivery(int batchSize, int deliveryInterval, ClientObjectFactory objectFactory, FailoverPolicy failoverPolicy, IndexTemplate indexTemplate) {
            super(batchSize, deliveryInterval, objectFactory, failoverPolicy, indexTemplate);
        }

        @Override
        protected BatchEmitterServiceProvider createBatchEmitterServiceProvider() {
            return mockedProvider;
        }

    }


}
