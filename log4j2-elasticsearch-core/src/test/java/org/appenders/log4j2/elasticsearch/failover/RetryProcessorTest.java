package org.appenders.log4j2.elasticsearch.failover;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static org.appenders.log4j2.elasticsearch.failover.SingleKeySequenceSelectorTest.DEFAULT_TEST_SEQUENCE_ID;
import static org.appenders.log4j2.elasticsearch.failover.UUIDSequence.RESERVED_KEYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryProcessorTest {

    public static final int DEFAULT_TEST_MAX_RETRY_SIZE = 10;

    private Random random = new Random();

    @Before
    public void setup() {
        System.setProperty("appenders.retry.backoff.millis", "0");
    }

    @Test
    public void runnableRunDelegatesToRetryBatch() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(ItemSource.class);
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, 0, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = spy(new RetryProcessor(DEFAULT_TEST_MAX_RETRY_SIZE, items, listeners, keySequenceSelector));

        // when
        retryProcessor.run();

        // then
        verify(retryProcessor).retry();

    }

    @Test
    public void exceptionsAreNotRethrown() {

        // given
        RetryListener listener = mock(RetryListener.class);
        NullPointerException expectedException = spy(new NullPointerException("test exception"));
        when(listener.notify(any())).thenThrow(expectedException);

        ItemSource itemSource = mock(FailedItemSource.class);
        int expectedSize = 1;
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, expectedSize, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(expectedSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener).notify(any(FailedItemSource.class));
        verify(expectedException).getMessage();

    }

    @Test
    public void retryReturnsGracefullyIfKeySequenceIsNull() {

        // given
        RetryListener listener = mock(RetryListener.class);

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = mock(KeySequenceSelector.class);
        when(keySequenceSelector.firstAvailable()).thenReturn(null);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(DEFAULT_TEST_MAX_RETRY_SIZE, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, never()).notify(any());

    }

    @Test
    public void doesNotRetryWhenMapIsEmpty() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(FailedItemSource.class);
        int maxRetryBatchSize = 10;
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, 0, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(maxRetryBatchSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, times(0)).notify(any(FailedItemSource.class));

    }

    @Test
    public void retryListSizeIsConfigurable() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(FailedItemSource.class);
        int maxRetryBatchSize = DEFAULT_TEST_MAX_RETRY_SIZE;
        int mapSize = maxRetryBatchSize * 2;
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, mapSize, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(maxRetryBatchSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, times(maxRetryBatchSize)).notify(any(FailedItemSource.class));

    }

    @Test
    public void retryListSizeIsLowerThanMaxSizeIfNumberOfAvailableElementsIsLowerThanMaxSize() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(FailedItemSource.class);
        int maxRetryBatchSize = DEFAULT_TEST_MAX_RETRY_SIZE;
        int mapSize = maxRetryBatchSize / 2;
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, mapSize, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(maxRetryBatchSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, times(mapSize)).notify(any(FailedItemSource.class));

    }

    @Test
    public void retryListSizeIsLowerThanNumberOfAvailableElementsIfElementsAreNull() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(FailedItemSource.class);
        int maxRetryBatchSize = DEFAULT_TEST_MAX_RETRY_SIZE;
        int mapSize = maxRetryBatchSize / 2;

        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, mapSize, keySequenceSelector, () -> itemSource);

        UUIDSequence keySequence = new UUIDSequence(UUIDSequenceTest.createDefaultTestKeySequenceConfig());
        keySequence.nextWriterKey(); // progress key sequence
        items.remove(keySequence.nextReaderKey()); // have to remove AFTER keySequenceSelector initialization

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(maxRetryBatchSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, times(mapSize - 1)).notify(any(FailedItemSource.class));
        assertEquals(1, retryProcessor.orphanedKeyCount.get());

    }

    @Test
    public void waitsIfBackoffMillisConfigured() {

        // given
        System.setProperty("appenders.retry.backoff.millis", "100");

        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(ItemSource.class);
        int sequenceId = random.nextInt(1000) + RESERVED_KEYS;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(sequenceId, items);
        fillMap(items, 1, keySequenceSelector, () -> itemSource);

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(DEFAULT_TEST_MAX_RETRY_SIZE, items, listeners, keySequenceSelector);

        // when
        long start = System.currentTimeMillis();
        retryProcessor.run();
        long end = System.currentTimeMillis();

        // then
        assertTrue(end - start >= 100);

    }

    @Test
    public void nonFailedItemSourceIsReleasedAndNotRetried() {

        // given
        RetryListener listener = mock(RetryListener.class);

        ItemSource itemSource = mock(FailedItemSource.class);
        int maxRetryBatchSize = DEFAULT_TEST_MAX_RETRY_SIZE;
        int mapSize = maxRetryBatchSize / 2;

        Map<CharSequence, ItemSource> items = new HashMap<>();
        KeySequenceSelector keySequenceSelector = createDefaultTestKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID, items);
        fillMap(items, mapSize, keySequenceSelector, () -> itemSource);

        UUIDSequence keySequence = new UUIDSequence(UUIDSequenceTest.createDefaultTestKeySequenceConfig());
        keySequence.nextWriterKey(); // progress key sequence

        ItemSource invalidItemSource = mock(ItemSource.class);
        CharSequence readerKey = keySequence.nextReaderKey();
        items.put(readerKey, invalidItemSource); // replace 1 element

        RetryListener[] listeners = {listener};
        RetryProcessor retryProcessor = new RetryProcessor(maxRetryBatchSize, items, listeners, keySequenceSelector);

        // when
        retryProcessor.retry();

        // then
        verify(listener, times(mapSize - 1)).notify(any(FailedItemSource.class));
        verify(invalidItemSource).release();

    }

    static Map<CharSequence, ItemSource> fillMap(
            Map<CharSequence, ItemSource> map,
            int expectedSize,
            KeySequenceSelector keySequenceSelector,
            Supplier<ItemSource> itemSourceSupplier
            ) {

        KeySequence keySequence = keySequenceSelector.firstAvailable();

        for (int i = 0; i < expectedSize; i++) {
            map.put(keySequence.nextWriterKey(), itemSourceSupplier.get());
        }
        new KeySequenceConfigRepository(map).persist(keySequence.getConfig(true));

        return map;
    }

    public KeySequenceSelector createDefaultTestKeySequenceSelector(long sequenceId, Map<CharSequence, ItemSource> items) {
        return new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID)
                .withRepository(new KeySequenceConfigRepository(items));
    }

}

