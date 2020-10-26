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


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BulkEmitterTest {

    public static final int LARGE_TEST_INTERVAL = 10000;
    public static final int TEST_BATCH_SIZE = 2;
    public static final String TEST_DATA = "dummyData";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void interruptedExceptionIsHandled() throws InterruptedException {

        // given
        int slackTime = 10000;
        Function listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                try {
                    Thread.currentThread().sleep(slackTime);
                } catch (InterruptedException e) {
                    Assert.fail();
                }
                return true;
            }
        });

        BulkEmitter<Collection> emitter = new BulkEmitter<>(1, 10000, new BatchOperations<Collection>() {
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

        });

        emitter.addListener(listener);

        Thread t1 = new Thread(() -> emitter.add(0));
        Thread t2 = new Thread(() -> emitter.add(1));

        // when
        t1.start();
        Thread.currentThread().sleep(100);
        t2.start();
        Thread.currentThread().sleep(100);
        t2.interrupt();

        // then
        verify(listener, times(1)).apply(any());

    }

    @Test
    public void threadsAwaitingAtLatchAreEventuallyReleased() {

        // given
        int slackTime = 100;
        Function listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                try {
                    Thread.currentThread().sleep(slackTime);
                } catch (InterruptedException e) {
                    Assert.fail();
                }
                return true;
            }
        });

        BulkEmitter<Collection> emitter = new BulkEmitter<>(1, 10000, new BatchOperations<Collection>() {
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

        });

        emitter.addListener(listener);

        Thread t1 = new Thread(() -> emitter.add(0));
        Thread t2 = new Thread(() -> emitter.add(1));

        // when
        t1.start();
        long start = System.currentTimeMillis();
        t2.run();
        long end = System.currentTimeMillis();

        // then
        verify(listener, times(1)).apply(any());
        System.out.println(end - start);
        assertTrue(end - start >= slackTime);

    }

    private BatchBuilder<Collection> createTestBatchBuilder() {
        return new BatchBuilder<Collection>() {
            Collection items = new ConcurrentLinkedQueue();

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

    @Test
    public void notifiesOnBatchWithGivenSize() {

        // given
        int batchSize = 3;
        BulkEmitter emitter = createTestBulkEmitter(batchSize, LARGE_TEST_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        // when
        for (int ii = 0; ii < batchSize; ii++) {
            emitter.add(new TestBatchItem(TEST_DATA));
        }

        // then
        ArgumentCaptor<TestBatch> captor = ArgumentCaptor.forClass(TestBatch.class);
        Mockito.verify(dummyObserver, Mockito.times(1)).apply(captor.capture());
        Assert.assertEquals(batchSize, captor.getValue().items.size());
    }

    @Test
    public void notifiesOnEveryCompletedBatch() throws InterruptedException {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, LARGE_TEST_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        int expectedNumberOfBatches = 4;

        // when
        for (int ii = 0; ii < TEST_BATCH_SIZE * expectedNumberOfBatches ; ii++) {
            emitter.add(new TestBatchItem(TEST_DATA));
            Thread.sleep(100);
        }

        // then
        ArgumentCaptor<TestBatch> captor = ArgumentCaptor.forClass(TestBatch.class);
        Mockito.verify(dummyObserver, Mockito.times(expectedNumberOfBatches)).apply(captor.capture());
        for (TestBatch batch : captor.getAllValues()) {
            Assert.assertEquals(TEST_BATCH_SIZE, batch.items.size());
        }

    }

    @Test
    public void listenerIsNotNotifiedWhenThereNoItemsToBatch() {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, LARGE_TEST_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        // when
        emitter.notifyListener();

        // then
        verify(dummyObserver, never()).apply(any());

    }

    @Test
    public void listenerIsNotifiedByScheduledTask() throws InterruptedException {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 1000, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);
        emitter.start();

        assertTrue(TEST_BATCH_SIZE > 1);

        // when
        emitter.add(new Object());

        // then
        verify(dummyObserver, timeout(2000)).apply(any());

    }

    @Test
    public void listenerIsNotifiedOnLifecycleStop() {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 1000, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        assertTrue(TEST_BATCH_SIZE > 1);

        emitter.start();

        // when
        emitter.add(new Object());
        emitter.stop();

        // then
        verify(dummyObserver).apply(any());

    }

    @Test
    public void listenerIsNotNotifiedAfterLifecycleStopCauseSchedulerIsCancelled() throws InterruptedException {

        // given
        assertTrue(TEST_BATCH_SIZE > 1);

        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, 10, new TestBatchOperations());
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

    private BulkEmitter createLifeCycleTestObject() {
        return createTestBulkEmitter(TEST_BATCH_SIZE, LARGE_TEST_INTERVAL, new TestBatchOperations());
    }

    public static BulkEmitter createTestBulkEmitter(int batchSize, int interval, BatchOperations batchOperations) {
        BulkEmitter bulkEmitter = new BulkEmitter(batchSize, interval, batchOperations);
        return bulkEmitter;
    }

    private Function<TestBatch, Boolean> dummyObserver() {
        return spy(new DummyListener());
    }

    class DummyListener implements Function<TestBatch, Boolean> {
        @Override
        public Boolean apply(TestBatch arg1) {
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

        private Collection<Object> items = new ConcurrentLinkedQueue<>();

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

        private Object data;

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
