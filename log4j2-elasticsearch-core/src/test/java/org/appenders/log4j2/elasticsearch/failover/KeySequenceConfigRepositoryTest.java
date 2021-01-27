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

import net.openhft.chronicle.map.ChronicleMap;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.failover.KeySequenceConfigTest.createDefaultTestKeySequenceConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeySequenceConfigRepositoryTest {

    static final long DEFAULT_TEST_KEY_SEQUENCE_EXPIRY = 1000;

    private static final Random random = new Random();

    @Test
    public void keySequenceConfigListIsCreatedIfItIsAbsentInGivenMap() {

        // given
        ChronicleMap<CharSequence, ItemSource> failedItems = mock(ChronicleMap.class);
        when(failedItems.containsKey(eq(KeySequenceConfigRepository.INDEX_KEY_NAME)))
                .thenReturn(false);

        // when
        new KeySequenceConfigRepository(failedItems, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        // then
        ArgumentCaptor<KeySequenceConfigKeys> configRegistryCaptor =
                ArgumentCaptor.forClass(KeySequenceConfigKeys.class);

        verify(failedItems).put(eq(KeySequenceConfigRepository.INDEX_KEY_NAME), configRegistryCaptor.capture());

        assertNotNull(configRegistryCaptor.getValue());

    }

    @Test
    public void keySequenceRegistryIsReusedIfItIsAlreadyPresentInGivenMap() {

        // given
        ChronicleMap<CharSequence, ItemSource> failedItems = mock(ChronicleMap.class);
        when(failedItems.containsKey(eq(KeySequenceConfigRepository.INDEX_KEY_NAME)))
                .thenReturn(true);

        // when
        new KeySequenceConfigRepository(failedItems, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        // then
        verify(failedItems, never()).put(eq(KeySequenceConfigRepository.INDEX_KEY_NAME), any());

    }

    @Test
    public void getReturnsElementIfPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        KeySequenceConfig expected = createDefaultTestKeySequenceConfig();
        map.put(expected.getKey(), expected);

        // when
        KeySequenceConfig result = repository.get(expected.getKey());

        // then
        assertEquals(expected, result);

    }

    @Test
    public void getReturnsNullIfElementNotPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        // when
        KeySequenceConfig result = repository.get(UUID.randomUUID().toString());

        // then
        assertNull(result);

    }

    @Test
    public void containsReturnsTrueIfElementPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        KeySequenceConfig expected = createDefaultTestKeySequenceConfig();
        map.put(expected.getKey(), expected);

        // when
        boolean result = repository.contains(expected.getKey());

        // then
        assertTrue(result);

    }

    @Test
    public void containsReturnsFalseIfElementNotPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        // when
        boolean result = repository.contains(UUID.randomUUID().toString());

        // then
        assertFalse(result);

    }

    @Test
    public void persistRegistersNewKeySequenceConfigKeyIfNotPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        CharSequence expectedKey = config.getKey();

        // when
        repository.persist(config);

        // then
        KeySequenceConfigKeys keys =
                (KeySequenceConfigKeys) map.get(KeySequenceConfigRepository.INDEX_KEY_NAME);
        assertTrue(keys.getSource().getKeys().contains(expectedKey));

    }

    @Test
    public void persistDoesNotRegisterNewKeySequenceConfigKeyIfAlreadyPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = spy(new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY));

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        CharSequence expectedKey = config.getKey();

        map.put(expectedKey, config);

        KeySequenceConfigKeys keys =
                (KeySequenceConfigKeys) map.get(KeySequenceConfigRepository.INDEX_KEY_NAME);
        assertFalse(keys.getSource().getKeys().contains(expectedKey)); // orphaned AND NOT-registered key sequences may never get registered again..

        // when
        repository.persist(config);

        // then
        verify(repository, never()).registerKeySequenceConfig(eq(expectedKey));

    }

    @Test
    public void persistStoresKeySequenceConfigIfNotPresentInGivenMap() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        CharSequence expectedKey = config.getKey();

        // when
        repository.persist(config);

        // then
        assertEquals(config, map.get(expectedKey));

    }

    @Test
    public void purgeUnregistersKeySequenceConfigKey() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = spy(new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY));

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        CharSequence nonExpectedKey = config.getKey();
        KeySequenceConfigKeys keys =
                (KeySequenceConfigKeys) map.get(KeySequenceConfigRepository.INDEX_KEY_NAME);
        assertTrue(keys.getSource().getKeys().contains(nonExpectedKey));

        // when
        repository.purge(config);

        // then
        verify(repository).unregisterKeySequenceConfig(eq(nonExpectedKey));
        assertFalse(keys.getSource().getKeys().contains(nonExpectedKey));

    }

    @Test
    public void purgeRemovesKeySequenceConfig() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = spy(new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY));

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        CharSequence nonExpectedKey = config.getKey();
        assertTrue(map.containsKey(nonExpectedKey));

        // when
        repository.purge(config);

        // then
        assertFalse(map.containsKey(nonExpectedKey));

    }

    @Test
    public void getAllReturnsOnlyRegisteredKeySequenceConfigs() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = spy(new KeySequenceConfigRepository(map, DEFAULT_TEST_KEY_SEQUENCE_EXPIRY));

        KeySequenceConfig config1 = createDefaultTestKeySequenceConfig();
        KeySequenceConfig config2 = createDefaultTestKeySequenceConfig();
        KeySequenceConfig config3 = createDefaultTestKeySequenceConfig();
        repository.persist(config1);
        repository.persist(config2);

        map.put(config3.getKey(), config3); // this should never happen outside persist() scope

        // when
        Collection<KeySequenceConfig> configs = repository.getAll();

        // then
        assertTrue(configs.contains(config1));
        assertTrue(configs.contains(config2));
        assertFalse(configs.contains(config3));

    }

    @Test
    public void persistSetsExpiryMillis() {

        // given
        long minimumExpectedExpiry = 11000L;
        System.setProperty("appenders.failover.keysequence.expireInMillis", String.valueOf(minimumExpectedExpiry));

        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        CharSequence expectedKey = config.getKey();

        // when
        repository.persist(config);

        // then
        KeySequenceConfig result = (KeySequenceConfig) map.get(expectedKey);
        assertTrue(System.currentTimeMillis() + minimumExpectedExpiry >= result.getExpireAt());

    }

    @Test
    public void persistSetsProvidedExpiryIfProvidedWasLowerThanDefault() {

        // given
        long defaultExpiry = 20000L;
        System.setProperty("appenders.failover.keysequence.expireInMillis", String.valueOf(defaultExpiry));

        Map<CharSequence, ItemSource> map = createDefaultTestMap();

        long providedExpiry = 15000L;
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, providedExpiry);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        CharSequence expectedKey = config.getKey();

        // when
        repository.persist(config);

        // then
        KeySequenceConfig result = (KeySequenceConfig) map.get(expectedKey);
        assertTrue(System.currentTimeMillis() + providedExpiry >= result.getExpireAt());
        assertTrue(System.currentTimeMillis() + defaultExpiry > result.getExpireAt() - providedExpiry);

    }

    @Test
    public void persistSetsGivenExpiryIfHigherThanDefault() {

        // given
        Map<CharSequence, ItemSource> map = createDefaultTestMap();

        long minimumExpectedExpiry = 20000L;
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, minimumExpectedExpiry);

        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        CharSequence expectedKey = config.getKey();

        // when
        repository.persist(config);

        // then
        KeySequenceConfig result = (KeySequenceConfig) map.get(expectedKey);
        assertTrue(System.currentTimeMillis() + minimumExpectedExpiry >= result.getExpireAt());

    }

    @Test
    public void consistencyCheckReturnsTrueIfKeySequenceConfigOwned() {

        // given
        System.setProperty("appenders.failover.keysequence.expireInMillis", "0");

        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0);
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();

        repository.persist(config);

        // when
        boolean result = repository.consistencyCheck(config);

        // then
        assertTrue(result);

    }

    @Test
    public void consistencyCheckReturnsFalseIfKeySequenceConfigNotOwned() {

        // given
        System.setProperty("appenders.failover.keysequence.expireInMillis", "0");

        Map<CharSequence, ItemSource> map = createDefaultTestMap();
        KeySequenceConfigRepository repository = new KeySequenceConfigRepository(map, 0);
        KeySequenceConfig config = createDefaultTestKeySequenceConfig();
        repository.persist(config);

        // when
        config.setOwnerId(KeySequenceConfigRepository.ID + 1);
        map.put(config.getKey(), config);

        boolean result = repository.consistencyCheck(config);

        // then
        assertFalse(result);

    }

    public HashMap<CharSequence, ItemSource> createDefaultTestMap() {
        return new HashMap<>();
    }

}
