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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.appenders.log4j2.elasticsearch.failover.KeySequenceConfigTest.createTestKeySequenceConfig;
import static org.appenders.log4j2.elasticsearch.failover.UUIDSequence.RESERVED_KEYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UUIDSequenceTest {

    public static final int DEFAULT_TEST_SEQ_ID = 1;
    public static final int EXPECTED_INITIAL_DIFF = 0;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnInitialReaderIndexLowerThanReservedKeys() {

        // given
        KeySequenceConfig sequenceConfig = createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS - 1, RESERVED_KEYS);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("readerIndex cannot be lower than");

        // when
        createDefaultTestUUIDSequence(sequenceConfig);

    }

    @Test
    public void throwsOnInitialWriterIndexLowerThanReservedKeys() {

        // given
        KeySequenceConfig sequenceConfig = createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS, RESERVED_KEYS - 1);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("writerIndex cannot be lower than");

        // when
        createDefaultTestUUIDSequence(sequenceConfig);

    }

    @Test
    public void equalsReturnsFalseIfNullProvided() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        boolean result = sequence.equals(null);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfWrongTypeProvided() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        boolean result = sequence.equals(Object.class);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfSequenceIdsAreDifferent() {

        // given
        KeySequenceConfig config1 = createDefaultTestKeySequenceConfig();
        KeySequence sequence1 = createDefaultTestUUIDSequence(config1);

        KeySequenceConfig config2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID + 1);
        KeySequence sequence2 = createDefaultTestUUIDSequence(config2);

        // when
        boolean result = sequence1.equals(sequence2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsTrueIfSequenceIdsAreEqual() {

        // given
        KeySequenceConfig config1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID);
        KeySequence sequence1 = createDefaultTestUUIDSequence(config1);

        KeySequenceConfig config2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID);
        KeySequence sequence2 = createDefaultTestUUIDSequence(config2);

        // when
        boolean result = sequence1.equals(sequence2);

        // then
        assertTrue(result);

    }

    @Test
    public void equalsReturnsTrueIfReaderIndicesAreDifferent() {

        // given
        KeySequenceConfig config1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID);
        KeySequence sequence1 = createDefaultTestUUIDSequence(config1);

        KeySequenceConfig config2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS + 1, RESERVED_KEYS);
        KeySequence sequence2 = createDefaultTestUUIDSequence(config2);

        // when
        boolean result = sequence1.equals(sequence2);

        // then
        assertTrue(result);

    }

    @Test
    public void hashCodesAreEqualWhenSequencesAreEqual() {

        // given
        KeySequenceConfig config1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID);
        KeySequence sequence1 = createDefaultTestUUIDSequence(config1);

        KeySequenceConfig config2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS + 1, RESERVED_KEYS + 1);
        KeySequence sequence2 = createDefaultTestUUIDSequence(config2);

        assertTrue(sequence1.equals(sequence2));

        // when
        int hashCode1 = sequence1.hashCode();
        int hashCode2 = sequence1.hashCode();

        // then
        assertEquals(hashCode1, hashCode2);

    }

    @Test
    public void equalsReturnsTrueIfWriterIndicesAreDifferent() {

        // given
        KeySequenceConfig config1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID);
        KeySequence sequence1 = createDefaultTestUUIDSequence(config1);

        KeySequenceConfig config2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS, RESERVED_KEYS + 1);
        KeySequence sequence2 = createDefaultTestUUIDSequence(config2);

        // when
        boolean result = sequence1.equals(sequence2);

        // then
        assertTrue(result);

    }

    @Test
    public void returnsNextWriterKeyExcludingCurrent() {

        // given
        CharSequence expectedFirstKey = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedSecondKey = getNextExpectedKey(RESERVED_KEYS + 2);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        CharSequence key1 = sequence.nextWriterKey();
        CharSequence key2 = sequence.nextWriterKey();

        // then
        assertEquals(expectedFirstKey, key1);
        assertEquals(expectedSecondKey, key2);

    }

    @Test
    public void returnsNextReaderKeyExcludingCurrent() {

        // given
        CharSequence expectedFirstKey = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedSecondKey = getNextExpectedKey(RESERVED_KEYS + 2);

        KeySequenceConfig config = createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS, RESERVED_KEYS + 2);
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        CharSequence key1 = sequence.nextReaderKey();
        CharSequence key2 = sequence.nextReaderKey();

        // then
        assertEquals(expectedFirstKey, key1);
        assertEquals(expectedSecondKey, key2);

    }

    @Test
    public void returnsNullReaderKeyIfReaderKeyEqualsInitialWriterKey() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        CharSequence readerKey1 = sequence.nextReaderKey();

        // then
        assertNull(readerKey1);

    }

    @Test
    public void returnsNullReaderKeyIfReaderKeyEqualsWriterKey() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        CharSequence readerKey1 = sequence.nextReaderKey();
        CharSequence readerKey2 = sequence.nextReaderKey();

        // then
        assertNull(readerKey1);
        assertNull(readerKey2);

    }

    @Test
    public void returnsNullReaderKeyIfReaderKeyModifiedConcurrently() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = spy(createDefaultTestUUIDSequence(config));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        when(sequence.readerKeysAvailable()).thenAnswer(new Answer<Long>() {

            private boolean firstCall = true;

            @Override
            public Long answer(InvocationOnMock invocation) {
                if (firstCall) {
                    firstCall = false;
                    new Thread(() -> {
                        sequence.nextReaderKey();
                        countDownLatch.countDown();
                    }).run();
                }
                return 1L;
            }

        });

        // when
        sequence.nextWriterKey();

        CharSequence readerKey1 = sequence.nextReaderKey();

        // then
        assertNull(readerKey1);

    }

    @Test
    public void diffReturnsZeroIfWriterIndexEqualsReaderIndex() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        long diff1 = sequence.readerKeysAvailable();
        sequence.nextWriterKey();
        sequence.nextReaderKey();

        long diff2 = sequence.readerKeysAvailable();

        // then
        assertEquals(0, diff1);
        assertEquals(0, diff2);

    }

    @Test
    public void diffDoesNotReturnZeroIfWriterIndexDoesNotEqualReaderIndex() {


        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        assertEquals(EXPECTED_INITIAL_DIFF, sequence.readerKeysAvailable());

        // when
        sequence.nextWriterKey();
        long diff1 = sequence.readerKeysAvailable();

        sequence.nextReaderKey();
        long diff2 = sequence.readerKeysAvailable();

        sequence.nextWriterKey();
        long diff3 = sequence.readerKeysAvailable();

        sequence.nextWriterKey();
        long diff4 = sequence.readerKeysAvailable();

        // then
        assertEquals(1, diff1);
        assertEquals(0, diff2);
        assertEquals(1, diff3);
        assertEquals(2, diff4);

    }

    @Test
    public void returnsIteratorOverNextAvailableKeys() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        assertEquals(EXPECTED_INITIAL_DIFF, sequence.readerKeysAvailable());

        // when
        Iterator iterator = sequence.nextReaderKeys(0);

        // then
        assertNotNull(iterator);

    }

    @Test
    public void nextAvailableKeysNextReturnsNullIfReaderIndexEqualsInitialWriterIndex() {

        // given
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        Iterator<CharSequence> iterator = sequence.nextReaderKeys(1);

        // when
        CharSequence next = iterator.next();

        // then
        assertNull(next);

    }

    @Test
    public void nextAvailableKeysNextReturnsNextReaderKeyIfReaderIndexLowerThanWriterIndex() {

        // given
        CharSequence expectedFirstKey = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedSecondKey = getNextExpectedKey(RESERVED_KEYS + 2);

        KeySequenceConfig config = createTestKeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS, RESERVED_KEYS + 2);
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        Iterator<CharSequence> iterator = sequence.nextReaderKeys(2);

        // when
        CharSequence readerKey1 = iterator.next();
        CharSequence readerKey2 = iterator.next();

        // then
        assertEquals(expectedFirstKey, readerKey1);
        assertEquals(expectedSecondKey, readerKey2);

    }

    @Test
    public void nextAvailableKeysNextReturnsNullIfReaderIndexLowerThanWriterIndex() {

        // given
        CharSequence expectedFirstKey = getNextExpectedKey(RESERVED_KEYS + 1);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        Iterator<CharSequence> iterator = sequence.nextReaderKeys(2);

        // when
        CharSequence next1 = iterator.next();
        sequence.nextWriterKey();
        CharSequence next2 = iterator.next();

        // then
        assertNull(next1);
        assertEquals(expectedFirstKey, next2);

    }

    @Test
    public void nextAvailableKeysHasNextReturnsTrueIfRemainingIsHigherThasZero() {

        // given
        CharSequence expectedFirstKey = getNextExpectedKey(RESERVED_KEYS + 1);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        Iterator<CharSequence> iterator = sequence.nextReaderKeys(1);

        // when
        boolean hasNext1 = iterator.hasNext();
        sequence.nextWriterKey();
        boolean hasNext2 = iterator.hasNext();
        CharSequence next1 = iterator.next();
        boolean hasNext3 = iterator.hasNext();


        // then
        assertEquals(expectedFirstKey, next1);
        assertFalse(hasNext1);
        assertTrue(hasNext2);
        assertFalse(hasNext3);

    }

    @Test
    public void configSnapshotReturnsASnapshotOfCurrentState() {

        // given
        CharSequence expectedReaderKey1 = getNextExpectedKey(RESERVED_KEYS);
        CharSequence expectedReaderKey2 = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedReaderKey3 = getNextExpectedKey(RESERVED_KEYS + 2);
        CharSequence expectedWriterKey1 = getNextExpectedKey(RESERVED_KEYS);
        CharSequence expectedWriterKey2 = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedWriterKey3 = getNextExpectedKey(RESERVED_KEYS + 2);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        KeySequenceConfig snapshot1 = sequence.getConfig(false);

        sequence.nextWriterKey();
        KeySequenceConfig snapshot2 = sequence.getConfig(false);

        sequence.nextWriterKey();
        KeySequenceConfig snapshot3 = sequence.getConfig(false);

        sequence.nextReaderKey();
        KeySequenceConfig snapshot4 = sequence.getConfig(false);

        sequence.nextReaderKey();
        KeySequenceConfig snapshot5 = sequence.getConfig(false);

        sequence.nextReaderKey();
        KeySequenceConfig snapshot6 = sequence.getConfig(false);

        // then
        assertEquals(getSequenceIndex(expectedReaderKey1), snapshot1.nextReaderIndex());
        assertEquals(getSequenceIndex(expectedWriterKey1), snapshot1.nextWriterIndex());
        assertEquals(snapshot1.nextReaderIndex(), snapshot1.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey2), snapshot2.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey1), snapshot2.nextReaderIndex());
        assertNotEquals(snapshot2.nextReaderIndex(), snapshot2.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey3), snapshot3.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey1), snapshot3.nextReaderIndex());
        assertNotEquals(snapshot3.nextReaderIndex(), snapshot3.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey3), snapshot4.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey2), snapshot4.nextReaderIndex());
        assertNotEquals(snapshot4.nextReaderIndex(), snapshot4.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey3), snapshot5.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey3), snapshot5.nextReaderIndex());
        assertEquals(snapshot5.nextReaderIndex(), snapshot5.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey3), snapshot6.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey3), snapshot6.nextReaderIndex());
        assertEquals(snapshot6.nextReaderIndex(), snapshot6.nextWriterIndex());

    }

    @Test
    public void sharedConfigReturnsLatestState() {

        // given
        CharSequence expectedReaderKey = getNextExpectedKey(RESERVED_KEYS + 1);
        CharSequence expectedWriterKey = getNextExpectedKey(RESERVED_KEYS + 1);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        KeySequence sequence = createDefaultTestUUIDSequence(config);

        // when
        KeySequenceConfig snapshot1 = sequence.getConfig(true);

        sequence.nextWriterKey();
        KeySequenceConfig snapshot2 = sequence.getConfig(true);

        sequence.nextReaderKey();
        KeySequenceConfig snapshot3 = sequence.getConfig(true);

        // then
        assertEquals(getSequenceIndex(expectedReaderKey), snapshot1.nextReaderIndex());
        assertEquals(getSequenceIndex(expectedWriterKey), snapshot1.nextWriterIndex());

        assertEquals(getSequenceIndex(expectedWriterKey), snapshot2.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey), snapshot2.nextReaderIndex());

        assertEquals(getSequenceIndex(expectedWriterKey), snapshot3.nextWriterIndex());
        assertEquals(getSequenceIndex(expectedReaderKey), snapshot3.nextReaderIndex());

        assertEquals(snapshot1.nextReaderIndex(), snapshot1.nextWriterIndex());
        assertEquals(snapshot2.nextReaderIndex(), snapshot2.nextWriterIndex());
        assertEquals(snapshot3.nextReaderIndex(), snapshot3.nextWriterIndex());

        assertEquals(snapshot1.getSource(), snapshot2.getSource());
        assertEquals(snapshot2.getSource(), snapshot3.getSource());

    }

    public long getSequenceIndex(CharSequence expectedReaderKey1) {
        return UUID.fromString((String) expectedReaderKey1).getLeastSignificantBits();
    }

    public String getNextExpectedKey(int reservedKeys) {
        return new UUID(DEFAULT_TEST_SEQ_ID, reservedKeys).toString();
    }

    public static KeySequenceConfig createDefaultTestKeySequenceConfig() {
        return new KeySequenceConfig(DEFAULT_TEST_SEQ_ID, RESERVED_KEYS, RESERVED_KEYS);
    }

    public static KeySequence createDefaultTestKeySequence() {
        return new UUIDSequence(createDefaultTestKeySequenceConfig());
    }

    private UUIDSequence createDefaultTestUUIDSequence(KeySequenceConfig sequenceConfig) {
        return new UUIDSequence(sequenceConfig);
    }

}
