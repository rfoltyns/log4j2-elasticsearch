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
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.appenders.log4j2.elasticsearch.failover.UUIDSequence.RESERVED_KEYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class KeySequenceConfigTest {

    private static final long DEFAULT_TEST_SEQUENCE_ID = 1;

    @Test
    public void toStringShowsNecessaryInfo() throws IOException {

        // given
        KeySequenceConfig keySequenceConfig =
                KeySequenceConfigRepositoryTest.createDefaultTestKeySequenceConfig();

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
        KeySequenceConfig keySequenceConfig =
                KeySequenceConfigRepositoryTest.createDefaultTestKeySequenceConfig();

        // when
        boolean result = keySequenceConfig.equals(null);

        // then
        assertFalse(result);
    }

    @Test
    public void equalsReturnsFalseIfWrongTypeProvided() {

        // given
        KeySequenceConfig keySequenceConfig =
                KeySequenceConfigRepositoryTest.createDefaultTestKeySequenceConfig();

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
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID + 1, RESERVED_KEYS, RESERVED_KEYS);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfOwnerIdsAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setOwnerId(1);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
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
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS + 1, RESERVED_KEYS);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfWriterKeysAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS + 1);

        // when
        boolean result = keySequenceConfig1.equals(keySequenceConfig2);

        // then
        assertFalse(result);

    }

    @Test
    public void equalsReturnsFalseIfExpiriesAreDifferent() {

        // given
        KeySequenceConfig keySequenceConfig1 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setExpireAt(1);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
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
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
        keySequenceConfig1.setOwnerId(1);
        keySequenceConfig1.setExpireAt(1);

        KeySequenceConfig keySequenceConfig2 =
                new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);
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
                        new KeySequenceConfig(DEFAULT_TEST_SEQUENCE_ID, RESERVED_KEYS, RESERVED_KEYS);

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
