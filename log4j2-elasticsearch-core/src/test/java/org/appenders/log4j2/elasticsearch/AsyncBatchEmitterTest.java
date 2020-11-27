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


import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AsyncBatchEmitterTest {

    public static final int TEST_DELIVERY_INTERVAL = 100000000;
    public static final int TEST_BATCH_SIZE = 2;
    public static final String TEST_DATA = "dummyData";

    @BeforeEach
    public void setup() {
        System.setProperty("appenders." + AsyncBatchEmitter.EmitterLoop.class.getSimpleName() + ".startDelay", "0");
    }

    @AfterEach
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void notifiesOnBatchWithGivenSize() {

        // given
        int batchSize = 3;
        AsyncBatchEmitter emitter = createTestBulkEmitter(batchSize, TEST_DELIVERY_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        System.setProperty("appenders." + AsyncBatchEmitter.EmitterLoop.class.getSimpleName() + ".startDelay", "0");
        emitter.start();

        // when
        for (int ii = 0; ii < batchSize; ii++) {
            emitter.add(new TestBatchItem(TEST_DATA));
        }

        // then
        ArgumentCaptor<TestBatch> captor = ArgumentCaptor.forClass(TestBatch.class);
        Mockito.verify(dummyObserver, timeout(100)).apply(captor.capture());
        assertEquals(batchSize, captor.getValue().items.size());

    }

    @Test
    public void notifiesOnEveryCompletedBatch() {

        // given
        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, new TestBatchOperations());

        System.setProperty("appenders." + AsyncBatchEmitter.EmitterLoop.class.getSimpleName() + ".startDelay", "0");
        emitter.start();

        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        int expectedNumberOfBatches = 2;

        // when
        for (int ii = 0; ii < TEST_BATCH_SIZE * expectedNumberOfBatches ; ii++) {
            emitter.add(new TestBatchItem(TEST_DATA));
        }

        // then
        ArgumentCaptor<TestBatch> captor = ArgumentCaptor.forClass(TestBatch.class);
        Mockito.verify(dummyObserver, timeout(500).times(expectedNumberOfBatches)).apply(captor.capture());
        for (TestBatch batch : captor.getAllValues()) {
            assertEquals(TEST_BATCH_SIZE, batch.items.size());
        }

    }

    @Test
    public void listenerIsNotNotifiedWhenThereNoItemsToBatch() {

        // given
        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        // when
        emitter.notifyListener();

        // then
        verify(dummyObserver, never()).apply(any());

    }

    @Test
    public void listenerIsNotifiedByScheduledTask() {

        // given
        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 10, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);
        emitter.start();

        assertTrue(TEST_BATCH_SIZE > 1);

        // when
        emitter.add(new Object());

        // then
        verify(dummyObserver, timeout(100)).apply(any());

    }

    @Test
    public void listenerIsNotifiedOnLifecycleStop() {

        // given
        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 1000, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        assertTrue(TEST_BATCH_SIZE > 1);

        emitter.start();

        // when
        emitter.add(new Object());
        emitter.stop();

        // then
        verify(dummyObserver, timeout(100)).apply(any());

    }

    @Test
    public void listenerIsNotNotifiedAfterLifecycleStopCauseSchedulerIsCancelled() throws InterruptedException {

        // given
        assertTrue(TEST_BATCH_SIZE > 1);

        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 10, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        emitter.start();

        // when
        emitter.add(new Object());
        int invocations = Mockito.mockingDetails(dummyObserver).getInvocations().size();
        emitter.stop();
        Thread.sleep(50);

        // then
        // stop() notifies explicitly, hence +1
        verify(dummyObserver, times(invocations + 1)).apply(any());

    }

    @Test
    public void listenerIsNotifiedIfShutdownDecrementFitsStopTimeout() throws InterruptedException {

        // given
        final Logger logger = mockTestLogger();

        assertTrue(TEST_BATCH_SIZE > 1);

        System.setProperty("appenders." + AsyncBatchEmitter.class.getSimpleName() + ".shutdownDecrementMillis", "10");

        AsyncBatchEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 10, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        emitter.start();

        // when
        emitter.add(new Object());
        verify(logger, timeout(500)).info("Dummy notified");

        Mockito.reset(logger);

        verify(logger, never()).info("Dummy notified"); // sanity check

        emitter.add(new Object());

        emitter.stop(500, true);

        // then
        verify(logger, timeout(500)).info("Dummy notified");

        System.clearProperty("appenders." + AsyncBatchEmitter.class.getSimpleName() + ".shutdownDecrementMillis");

        setLogger(null);

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

    @Test
    public void lifecycleStartStartsOnlyOnce() {

        // given
        AsyncBatchEmitter lifeCycle = spy(createLifeCycleTestObject());

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        verify(lifeCycle).getEmitterLoop();
        assertTrue(lifeCycle.isStarted());

        lifeCycle.stop();

    }

    @Test
    public void lifecycleStopStopsOnlyOnce() {

        // given
        final AtomicInteger shutdownCaptor = new AtomicInteger();
        AsyncBatchEmitter lifeCycle = new AsyncBatchEmitter(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, new TestBatchOperations()) {
            @Override
            void shutdownExecutor() {
                shutdownCaptor.incrementAndGet();
            }
        };

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertEquals(1, shutdownCaptor.get());

    }

    private AsyncBatchEmitter createLifeCycleTestObject() {
        return createTestBulkEmitter(TEST_BATCH_SIZE, TEST_DELIVERY_INTERVAL, new TestBatchOperations());
    }

    public static AsyncBatchEmitter createTestBulkEmitter(int batchSize, int interval, BatchOperations batchOperations) {
        return new AsyncBatchEmitter(batchSize, interval, batchOperations);
    }

    private BatchOperations<Collection> createDummyBatchOperations() {
        return new BatchOperations<Collection>() {
            @Override
            public Object createBatchItem(String indexName, Object source) {
                return source;
            }

            @Override
            public Object createBatchItem(String indexName, ItemSource source) {
                return source;
            }

            @Override
            public BatchBuilder<Collection> createBatchBuilder() {
                return createTestBatchBuilder();
            }

        };
    }

    private Function<TestBatch, Boolean> dummyObserver() {
        return spy(new DummyListener());
    }

    private BatchBuilder<Collection> createTestBatchBuilder() {
        return new BatchBuilder<Collection>() {
            final Collection items = new ConcurrentLinkedQueue();

            @Override
            public void add(Object item) {
                items.add(item);
            }
            @Override
            public Collection build() {
                Iterator iterator = items.iterator();
                while (iterator.hasNext()) {
                    iterator.next();
                }
                return items;
            }

        };
    }

    static class DummyListener implements Function<TestBatch, Boolean> {

        @Override
        public Boolean apply(TestBatch arg1) {
            getLogger().info("Dummy notified");
            return true;
        }
    }

    public static class TestBatchOperations implements BatchOperations {

        @Override
        public Object createBatchItem(String indexName, Object source) {
            return new TestBatchItem(source);
        }

        @Override
        public Object createBatchItem(String indexName, ItemSource source) {
            return new TestBatchItem(source.getSource());
        }

        @Override
        public BatchBuilder createBatchBuilder() {
            return new TestBatchBuilder();
        }

    }
    public static class TestBatchBuilder implements BatchBuilder {


        private final Collection<Object> items = new ConcurrentLinkedQueue<>();

        @Override
        public void add(Object item) {
            items.add(item);
        }

        @Override
        public Object build() {
            return new TestBatch(items);
        }

    }

    public static class TestBatchItem {


        private final Object data;

        public TestBatchItem(Object data) {
            this.data = data;
        }

        public Object getData(Object o) {
            return data;
        }
    }
    public static class TestBatch {


        public Collection<Object> items;

        public TestBatch(Collection<Object> items) {
            this.items = items;
        }
    }
}
