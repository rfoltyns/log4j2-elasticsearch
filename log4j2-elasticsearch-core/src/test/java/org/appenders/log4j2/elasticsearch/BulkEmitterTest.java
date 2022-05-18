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


import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BulkEmitterTest {

    public static final int LARGE_TEST_INTERVAL = 10000;
    public static final int TEST_BATCH_SIZE = 2;
    public static final String TEST_DATA = "dummyData";

    @Test
    public void interruptedExceptionIsHandled() throws InterruptedException {

        // given
        int slackTime = 10000;
        Function<Collection, Boolean> listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                try {
                    Thread.sleep(slackTime);
                } catch (InterruptedException e) {
                    Assertions.fail();
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
        Thread.sleep(100);
        t2.start();
        Thread.sleep(100);
        t2.interrupt();

        // then
        verify(listener, times(1)).apply(any());

    }

    @Test
    public void interruptedExceptionOnShutdownIsHandled() {

        // given
        Function<Collection, Boolean> listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        BulkEmitter<Collection> emitter = new BulkEmitter<>(2, 10000, new BatchOperations<Collection>() {
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

        final Logger logger = InternalLoggingTest.mockTestLogger();

        System.setProperty("appenders." + BulkEmitter.class.getSimpleName() + ".startDelay", "0");

        // when
        emitter.start();
        emitter.add(new Object());

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));

        final Thread stoppingThread = new Thread(() -> emitter.shutdownExecutor(100));

        stoppingThread.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        stoppingThread.interrupt();

        // then
        verify(listener, times(1)).apply(any());

        verify(logger, timeout(1000)).error("{}: Executor shutdown interrupted", BulkEmitter.class.getSimpleName());

        emitter.stop();

    }

    @Test
    public void threadsAwaitingAtLatchAreEventuallyReleased() {

        // given
        int slackTime = 100;
        Function<Collection, Boolean> listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                try {
                    Thread.sleep(slackTime);
                } catch (InterruptedException e) {
                    Assertions.fail();
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
        assertTrue(end - start >= slackTime);

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
        verify(dummyObserver, Mockito.times(1)).apply(captor.capture());
        assertEquals(batchSize, captor.getValue().items.size());
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
        verify(dummyObserver, Mockito.times(expectedNumberOfBatches)).apply(captor.capture());
        for (TestBatch batch : captor.getAllValues()) {
            assertEquals(TEST_BATCH_SIZE, batch.items.size());
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
    public void listenerIsNotifiedByScheduledTask() {

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
    public void listenerIsNotifiedIsShutdownDecrementFitsStopTimeout() {

        // given
        assertTrue(TEST_BATCH_SIZE > 1);

        System.setProperty("appenders." + BulkEmitter.class.getSimpleName() + ".shutdownDecrementMillis", "10");

        BulkEmitter emitter = createTestBulkEmitter(2, 10, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        emitter.start();

        final Logger logger = InternalLoggingTest.mockTestLogger();

        // when
        emitter.add(new Object());
        int invocations = Mockito.mockingDetails(dummyObserver).getInvocations().size();
        emitter.stop(5000, true);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        emitter.add(new Object());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeastOnce()).info(captor.capture(), any());

        verify(dummyObserver, times(invocations + 2)).apply(any());

        System.clearProperty("appenders." + BulkEmitter.class.getSimpleName() + ".shutdownDecrementMillis");

    }

    @Test
    public void listenerIsNotifiedWithStartDelayIfConfigured() {

        // given
        Function<Collection, Boolean> listener = spy(new Function<Collection, Boolean>() {
            @Override
            public Boolean apply(Collection collection) {
                return true;
            }
        });

        BulkEmitter<Collection> emitter = new BulkEmitter<>(2, 10000, new BatchOperations<Collection>() {
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

        System.setProperty("appenders." + BulkEmitter.class.getSimpleName() + ".startDelay", "100");

        // when
        emitter.start();
        emitter.add(new Object());

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        verify(listener, never()).apply(any());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        // then
        verify(listener, timeout(500)).apply(any());

        emitter.stop();

        System.clearProperty("appenders." + BulkEmitter.class.getSimpleName() + ".startDelay");

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
        return new BulkEmitter(batchSize, interval, batchOperations);
    }

    private Function<TestBatch, Boolean> dummyObserver() {
        return spy(new DummyListener());
    }

    static class DummyListener implements Function<TestBatch, Boolean> {
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
