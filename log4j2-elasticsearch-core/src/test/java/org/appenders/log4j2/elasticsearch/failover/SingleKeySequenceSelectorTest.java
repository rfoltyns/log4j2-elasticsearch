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
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.log4j2.elasticsearch.failover.KeySequenceConfigTest.createDefaultTestKeySequenceConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleKeySequenceSelectorTest {

    static final int DEFAULT_TEST_SEQUENCE_ID = 1;

    // ==============================
    // After setup (after firstAvailable() call)
    // ==============================

    @Test
    public void keySequenceIsReusedAfterFirstCall() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        sleep(1);
        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId());
        keySequenceSelector.withRepository(repository);

        // when
        KeySequence keySequence1 = keySequenceSelector.firstAvailable();
        KeySequence keySequence2 = keySequenceSelector.firstAvailable();

        // then
        KeySequenceConfig config1 = keySequence1.getConfig(true);
        KeySequenceConfig config2 = keySequence2.getConfig(true);

        assertEquals(config.getSeqId(), config1.getSeqId());
        assertEquals(config.getSeqId(), config2.getSeqId());
        assertEquals(config1.getKey(), config2.getKey());
        assertSame(config1, config2);

    }

    // ==============================
    // Before setup (before first firstAvailable() call)
    // ==============================

    @Test
    public void repositoryMustBeSet() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID);

        // when
        keySequenceSelector.withRepository(repository);

        // then
        KeySequence keySequence = keySequenceSelector.firstAvailable();
        assertNotNull(keySequence);

    }

    @Test
    public void throwsWhenRepositoryNotSet() {

        // given
        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> keySequenceSelector.firstAvailable());

        // then
        assertThat(exception.getMessage(), equalTo(KeySequenceConfigRepository.class.getSimpleName() +
                " was not provided for " + SingleKeySequenceSelector.class.getSimpleName()));

    }

    @Test
    public void newKeySequenceIsCreatedOnFirstCallIfNoConfigsAvailableInProvidedRepository() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID);
        keySequenceSelector.withRepository(repository);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertNotNull(keySequence);
        assertSame(keySequence, keySequenceSelector.currentKeySequence().get());

    }

    @Test
    public void newKeySequenceIsCreatedOnFirstCallIfNoMatchingKeySequenceIsAvailableInProvidedRepository() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId() + 1);
        keySequenceSelector.withRepository(repository);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertNotNull(keySequence);
        assertSame(keySequence, keySequenceSelector.currentKeySequence().get());

    }

    @Test
    public void keySequenceIsReusedOnFirstCallIfThereIsAnExpiredKeySequenceWithMatchingSequenceIdInProvidedRepository() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        sleep(1);

        CharSequence expectedKey = config.getKey();

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(UUID.fromString((String)expectedKey).getMostSignificantBits());
        keySequenceSelector.withRepository(repository);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertEquals(expectedKey, keySequence.getConfig(true).getKey());

    }

    @Test
    public void keySequenceIsReusedOnFirstCallIfThereIsAnExpiredAndNotOwnedKeySequenceWithMatchingSequenceIdInProvidedRepository() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        sleep(1);

        CharSequence expectedKey = config.getKey();

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(UUID.fromString((String)expectedKey).getMostSignificantBits());
        keySequenceSelector.withRepository(repository);

        config.setOwnerId(KeySequenceConfigRepository.ID + 1);
        map.put(config.getKey(), config);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertEquals(expectedKey, keySequence.getConfig(true).getKey());

    }

    @Test
    public void keySequenceIsNotReusedOnFirstCallWhenMatchingNonExpiredAndNotOwnedKeySequenceFoundInProvidedRepository() {

        // given
        System.setProperty("appenders.failover.keysequence.consistencyCheckDelay", "1");

        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 1000);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId());
        keySequenceSelector.withRepository(repository);

        config.setOwnerId(KeySequenceConfigRepository.ID + 1);
        map.put(config.getKey(), config);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertNull(keySequence);

    }

    @Test
    public void keySequenceIsReusedOnFirstCallWhenMatchingNonExpiredAndOwnedKeySequenceFoundInProvidedRepository() {

        // given
        System.setProperty("appenders.failover.keysequence.consistencyCheckDelay", "1");

        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 1000);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        CharSequence expectedKey = config.getKey();

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId());
        keySequenceSelector.withRepository(repository);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertEquals(expectedKey, keySequence.getConfig(true).getKey());

    }

    @Test
    public void newKeySequenceIsNotCreatedOnFirstCallWhenNotExpiredAndNotOwnedKeySequenceFoundInProvidedRepository() {

        // given
        System.setProperty("appenders.failover.keysequence.consistencyCheckDelay", "1");

        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 1000);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId());
        keySequenceSelector.withRepository(repository);

        int size = map.size();

        config.setOwnerId(KeySequenceConfigRepository.ID + 1);
        map.put(config.getKey(), config);

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertNull(keySequence);
        assertEquals(size, map.size());

    }

    @Test
    public void newKeySequenceIsNotCreatedOnFirstCallWhenConsistencyCheckFailed() {

        // given
        Map<CharSequence, ItemSource> map = new HashMap<>();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0) {
            @Override
            boolean consistencyCheck(KeySequenceConfig config) {
                return false;
            }
        };

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        sleep(1);

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(config.getSeqId());
        keySequenceSelector.withRepository(repository);

        int size = map.size();

        // when
        KeySequence keySequence = keySequenceSelector.firstAvailable();

        // then
        assertNull(keySequence);
        assertEquals(size, map.size());

    }

    private void sleep(long millis) {
        LockSupport.parkNanos(millis * 1000000L);
    }

}
