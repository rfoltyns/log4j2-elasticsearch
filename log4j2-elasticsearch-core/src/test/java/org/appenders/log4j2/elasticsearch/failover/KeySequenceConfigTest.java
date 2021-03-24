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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static org.appenders.log4j2.elasticsearch.failover.UUIDSequence.RESERVED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeySequenceConfigTest {

    static final int DEFAULT_OFFSET = 100;

    private static final long DEFAULT_TEST_SEQUENCE_ID = 1;
    private static final Random random = new Random();
    private static int offsetMultiplier = 1;

    public static KeySequenceConfig createDefaultTestKeySequenceConfig() {
        return createTestKeySequenceConfig(
                random.nextInt(DEFAULT_OFFSET) + DEFAULT_OFFSET * offsetMultiplier++);
    }

    public static KeySequenceConfig createTestKeySequenceConfig(long sequenceId) {
        return new KeySequenceConfig(
                sequenceId,
                UUIDSequence.RESERVED_KEYS,
                UUIDSequence.RESERVED_KEYS
        );
    }

    public static KeySequenceConfig createTestKeySequenceConfig(long sequenceId, long readerKey, long writerKey) {
        return new KeySequenceConfig(sequenceId, readerKey, writerKey);
    }

    @Test
    public void toStringShowsNecessaryInfo() throws IOException {

        // given
        KeySequenceConfig keySequenceConfig = createDefaultTestKeySequenceConfig();

        // when
        String result = keySequenceConfig.toString();

        // then
        KeySequenceConfigInfo info = new ObjectMapper().readValue(result, KeySequenceConfigInfo.class);
        assertEquals(keySequenceConfig.getClass().getSimpleName(), info.cls);
        assertEquals(keySequenceConfig.getSeqId(), info.seqId);
        assertEquals(keySequenceConfig.nextReaderIndex(), info.rIdx);
        assertEquals(keySequenceConfig.nextWriterIndex(), info.wIdx);

    }

    @Test
    public void equalsReturnsFalseIfNullProvided() {

        // given
        KeySequenceConfig keySequenceConfig = createDefaultTestKeySequenceConfig();

        // when
        boolean result = keySequenceConfig.equals(null);

        // then
        assertFalse(result);
    }

    @Test
    public void equalsReturnsFalseIfWrongTypeProvided() {

        // given
        KeySequenceConfig keySequenceConfig = createDefaultTestKeySequenceConfig();

        Object object = new Object();

        // when
        boolean result = keySequenceConfig.equals(object);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfSequenceIdsAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID + 1, RESERVED_KEYS, RESERVED_KEYS);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfOwnerIdsAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setOwnerId(1);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig2.setOwnerId(2);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfReaderKeysAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS + 1, RESERVED_KEYS);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfWriterKeysAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS + 1);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfExpiriesAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setExpireAt(1);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig2.setExpireAt(2);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsTrueIfOtherStateIsEqual() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setOwnerId(1);
        keySequenceConfig1.setExpireAt(1);

        KeySequenceConfig keySequenceConfig2 =
                createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig2.setOwnerId(1);
        keySequenceConfig2.setExpireAt(1);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertTrue(result);

    }

    @Test
    public void hashCodeReturnsValidValues() {

        // given
        KeySequenceConfig config =
                        createTestKeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        Random random = new Random();

        // when
        for (int i = 0; i < 10000; i++) {
            config.setExpireAt(random.nextLong());
            assertNotEquals(0, config.hashCode());
        }

    }

    static class KeySequenceConfigInfo {
        public long seqId;
        public long rIdx;
        public long wIdx;
        public String cls;
    }
}
