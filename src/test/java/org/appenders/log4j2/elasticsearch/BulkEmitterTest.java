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


import java.util.Observable;
import java.util.Observer;
import java.util.function.Function;

import org.appenders.log4j2.elasticsearch.BulkEmitter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.searchbox.core.Bulk;
import io.searchbox.core.Index;

public class BulkEmitterTest {

    public static final int LARGE_TEST_INTERVAL = 10000;
    public static final int TEST_BATCH_SIZE = 2;

    @Test
    public void emitsBatchWithGivenSize() {

        // given
        int batchSize = 3;
        BulkEmitter emitter = createTestBulkEmitter(batchSize, LARGE_TEST_INTERVAL);
        Function<Bulk, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        // when
        for (int ii = 0; ii < batchSize; ii++) {
            emitter.add(new Index.Builder("test").build());
        }

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        Mockito.verify(dummyObserver, Mockito.times(1)).apply(captor.capture());

    }

    @Test
    public void emitsOnEveryCompletedBatch() {

        // given
        BulkEmitter emitter = createTestBulkEmitter(TEST_BATCH_SIZE, LARGE_TEST_INTERVAL);
        Function<Bulk, Boolean> dummyObserver = dummyObserver();
        emitter.addListener(dummyObserver);

        int expectedNumberOfBatches = 4;

        // when
        for (int ii = 0; ii < TEST_BATCH_SIZE * expectedNumberOfBatches ; ii++) {
            emitter.add(new Index.Builder("test").build());
        }

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        Mockito.verify(dummyObserver, Mockito.times(expectedNumberOfBatches)).apply(captor.capture());

    }

    public static BulkEmitter createTestBulkEmitter(int batchSize, int interval) {
        return Mockito.spy(new BulkEmitter(batchSize, LARGE_TEST_INTERVAL));
    }

    private Function<Bulk, Boolean> dummyObserver() {
        return Mockito.spy(new DummyListener());
    }

    class DummyListener implements Function<Bulk, Boolean> {
        @Override
        public Boolean apply(Bulk arg1) {
            return true;
        }
    }
}
