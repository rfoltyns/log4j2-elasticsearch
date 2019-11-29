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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Manages the state of {@link KeySequenceConfig} instances and repository-wide key sequence config index: {@link KeySequenceConfigKeys}
 */
public class KeySequenceConfigRepository {

    /**
     * Default: 10 seconds
     */
    static final long DEFAULT_EXPIRE_IN_MILLIS = 10000L;

    /**
     * Key sequence config index name: {@code "00000000-0000-0000-0000-000000000000"}
     */
    static final CharSequence INDEX_KEY_NAME = new UUID(0, 0).toString();

    private final long consistencyCheckDelayNanos = TimeUnit.MILLISECONDS.toNanos(
            Long.parseLong(System.getProperty("appenders.failover.keysequence.consistencyCheckDelay", "200"))
    );

    final long id = UUID.randomUUID().getMostSignificantBits();

    final long expireInMillis;
    final Map<CharSequence, ItemSource> map;

    /**
     * {@link #expireInMillis} will be set to value of system property {@code "appenders.failover.keysequence.expireInMillis"}
     * or {@link #DEFAULT_EXPIRE_IN_MILLIS} if property is not present
     * @param dataSource
     */
    public KeySequenceConfigRepository(Map<CharSequence, ItemSource> dataSource) {
        this(
                dataSource,
                Long.parseLong(System.getProperty("appenders.failover.keysequence.expireInMillis", String.valueOf(DEFAULT_EXPIRE_IN_MILLIS)))
        );
    }

    /**
     * @param dataSource Storage instance
     * @param expireInMillis Lease duration in millis.
     *                      Amount of time to be added to current system time
     *                      and set on persisted config during {@link #persist(KeySequenceConfig)} calls.
     *                      Determines when this key sequence config becomes stale and may be picked up by other repository.
     *
     * @see KeySequenceConfig#expireAt
     */
    public KeySequenceConfigRepository(Map<CharSequence, ItemSource> dataSource, long expireInMillis) {

        this.map = dataSource;
        this.expireInMillis = expireInMillis;

        if (!map.containsKey(INDEX_KEY_NAME)) {
            // initialize key sequence config index
            map.put(INDEX_KEY_NAME, new KeySequenceConfigKeys(new ArrayList<>()));
        }

    }

    /**
     * Retrieves a {@link KeySequenceConfig} with given key. May return null.
     *
     * @param key config key
     * @return {@link KeySequenceConfig} instance or <tt>null</tt> if value was not resolved
     */
    public KeySequenceConfig get(CharSequence key) {
        return (KeySequenceConfig) map.get(key);
    }

    /**
     * Checks if given {@link KeySequenceConfig} exists in this repository
     *
     * @param keySequenceConfigKey config key
     */

    public boolean contains(CharSequence keySequenceConfigKey) {
        return map.containsKey(keySequenceConfigKey);
    }

    /**
     * Stores given {@link KeySequenceConfig}. If this repository does not contain given config, it registers the config key in the key sequence configs index ({@link #INDEX_KEY_NAME}).
     * <p>{@link KeySequenceConfig#expireAt} will be updated according to the value of {@link #expireInMillis}:
     * {@code System.currentTimeMillis() + expireInMillis}
     * <p>{@link KeySequenceConfig#ownerId} will be set to {@link #id}
     *
     * @param config {@link KeySequenceConfig} to store
     */
    public void persist(KeySequenceConfig config) {

        if (!contains(config.getKey())) {
            registerKeySequenceConfig(config.getKey());
        }

        long now = System.currentTimeMillis();
        config.setExpireAt(now + expireInMillis);
        config.setOwnerId(id);

        map.put(config.getKey(), config);

    }

    /**
     * Removes given {@link KeySequenceConfig} and de-registers it from the key sequence config index
     *
     * @param config {@link KeySequenceConfig} to remove
     */
    public void purge(KeySequenceConfig config) {
        map.remove(config.getKey());
        unregisterKeySequenceConfig(config.getKey());
    }

    /**
     * @return {@link KeySequenceConfig}s registered in key sequence config index ({@link #INDEX_KEY_NAME})
     */
    public Collection<KeySequenceConfig> getAll() {

        Collection<KeySequenceConfig> configs = new ArrayList<>();

        KeySequenceConfigKeys keySequenceConfigKeys = (KeySequenceConfigKeys) map.get(INDEX_KEY_NAME);
        for (CharSequence key : keySequenceConfigKeys.getKeys()) {
            configs.add((KeySequenceConfig) map.get(key));
        }

        return configs;

    }

    /**
     * Adds given key to key sequence configs index ({@link #INDEX_KEY_NAME}).
     * Registered key sequence configs can be retrieved with {@link #getAll()}.
     * If, in any circumstances, {@link KeySequenceConfig} will be stored in this repository,
     * but NOT present in the index, it will NOT be returned by {@link #getAll()} method
     *
     * @param keySequenceConfigKey key to be registered
     */
    void registerKeySequenceConfig(CharSequence keySequenceConfigKey) {

        KeySequenceConfigKeys keySequenceConfigKeys = (KeySequenceConfigKeys) map.get(INDEX_KEY_NAME);

        List<CharSequence> keys = keySequenceConfigKeys.getKeys();
        keys.add(keySequenceConfigKey);

        map.put(INDEX_KEY_NAME, keySequenceConfigKeys);

    }


    /**
     * Removes given key from a key sequence configs index ({@link #INDEX_KEY_NAME}).
     * Registered key sequence configs can be retrieved with {@link #getAll()}.
     * If, in any circumstances, {@link KeySequenceConfig} will be removed
     * and still present in the index, it WILL affect {@link #getAll()} calls
     *
     * @param keySequenceConfigKey key to be registered
     */
    void unregisterKeySequenceConfig(CharSequence keySequenceConfigKey) {

        KeySequenceConfigKeys keySequenceConfigKeys = (KeySequenceConfigKeys) map.get(INDEX_KEY_NAME);

        List<CharSequence> keys = keySequenceConfigKeys.getKeys();
        keys.remove(keySequenceConfigKey);

        map.put(INDEX_KEY_NAME, keySequenceConfigKeys);

    }

    /**
     * Verifies that given {@link KeySequenceConfig} is still managed by this repository instance.
     *
     * @param config {@link KeySequenceConfig} to verify
     * @return true, if this repository instance still manages given {@link KeySequenceConfig}, false otherwise
     *
     */
    boolean consistencyCheck(final KeySequenceConfig config) {

        // wait for concurrent updates
        LockSupport.parkNanos(consistencyCheckDelayNanos);

        KeySequenceConfig actual = (KeySequenceConfig) map.get(config.getKey());

        // verify that sequence still owned
        return actual.getOwnerId() == id;

    }

}
