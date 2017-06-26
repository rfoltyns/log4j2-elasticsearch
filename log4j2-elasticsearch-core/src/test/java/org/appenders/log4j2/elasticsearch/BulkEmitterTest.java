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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class BulkEmitterTest {

    public static final int LARGE_TEST_INTERVAL = 10000;
    public static final int TEST_BATCH_SIZE = 2;
    public static final String TEST_DATA = "dummyData";

    @Test
    public void emitsBatchWithGivenSize() {

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
    public void emitsOnEveryCompletedBatch() {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, LARGE_TEST_INTERVAL, new TestBatchOperations());
        Function<TestBatch, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        int expectedNumberOfBatches = 4;

        // when
        for (int ii = 0; ii < TEST_BATCH_SIZE * expectedNumberOfBatches ; ii++) {
            emitter.add(new TestBatchItem(TEST_DATA));
        }

        // then
        ArgumentCaptor<TestBatch> captor = ArgumentCaptor.forClass(TestBatch.class);
        Mockito.verify(dummyObserver, Mockito.times(expectedNumberOfBatches)).apply(captor.capture());
        for (TestBatch batch : captor.getAllValues()) {
            Assert.assertEquals(TEST_BATCH_SIZE, batch.items.size());
        }

    }

    public static BulkEmitter createTestBulkEmitter(int batchSize, int interval, BatchOperations batchOperations) {
        return Mockito.spy(new BulkEmitter(batchSize, interval, batchOperations));
    }

    private Function<TestBatch, Boolean> dummyObserver() {
        return Mockito.spy(new DummyListener());
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
