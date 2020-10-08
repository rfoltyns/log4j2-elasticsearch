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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Searches for available {@link KeySequence} with configured {@link #sequenceId} in provided {@link KeySequenceConfigRepository}.
 * There should always be only one instance of this class with the same {@link #sequenceId} when sharing the same {@link KeySequenceConfigRepository}.
 * Otherwise they may compete for the same {@link KeySequence} and result in data loss.
 * <p>First available {@link KeySequenceConfig} can be retrieved with {@link #firstAvailable()} call where "first available" meets following criteria:
 * <ul>
 * <li>Has the same sequence id as configured {@link #sequenceId}
 * <li>Is managed by provided {@link KeySequenceConfigRepository}: {@code repo.ID == keySequenceConfig.ownerId} or expired where "expired" means: currentTimeInMillis &gt;= {@link KeySequenceConfig#getExpireAt()})
 * </ul>
 *
 * <p>NOTE: Once provided with {@link KeySequenceConfigRepository}, {@link #firstAvailable()} MUST be called to
 * configure current {@link KeySequence}. {@link #currentKeySequence()} may be used afterwards.
 */
public class SingleKeySequenceSelector implements KeySequenceSelector {

    private KeySequenceConfigRepository repository;

    final AtomicReference<KeySequence> current = new AtomicReference<>(null);
    final long sequenceId;

    /**
     * @param sequenceId id of key sequence to use.
     *                   There should always be only one instance of this class with the same {@link #sequenceId} when sharing the same {@link KeySequenceConfigRepository}.
     *                   Otherwise they may compete for the same {@link KeySequence} and result in data loss.
     */
    public SingleKeySequenceSelector(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public SingleKeySequenceSelector withRepository(KeySequenceConfigRepository keySequenceConfigRepository) {
        this.repository = keySequenceConfigRepository;
        return this;
    }

    @Override
    public void close() {
        repository.persist(current.get().getConfig(false));
    }

    /**
     * Fast path. Provides access to current {@link KeySequence}. Returned value MAY be null if {@link #firstAvailable()} was not called before or
     * {@link #firstAvailable()} didn't find a valid key sequence in configured repository
     *
     * @return current {@link KeySequence} supplier
     */
    @Override
    public Supplier<KeySequence> currentKeySequence() {
        return current::get;
    }

    /**
     * <p>On first call, if {@link KeySequenceConfig} with matching {@link #sequenceId} exists, it will be retrieved and checked against conditions mentioned above.
     * If it does NOT exist, new {@link KeySequenceConfig} with configured {@link #sequenceId} will be created.
     * <p>If first {@link #firstAvailable()} call succeeded, lookup SHOULD never happen again
     * and {@link #currentKeySequence()} SHOULD be used to get the {@link KeySequence} instance.
     * However, {@link #firstAvailable()} MAY be used to persist the state of currently owned, valid {@link KeySequenceConfig}
     * <p>WARNING! If first {@link #firstAvailable()} call failed, it SHOULD NOT be called again as it indicates that
     * {@link KeySequenceConfig} is used by other {@link KeySequenceConfigRepository} and this instance was not configured correctly.
     *
     * @return valid {@link KeySequence} if found. Otherwise <tt>null</tt>.
     * @throws IllegalStateException if {@link #repository} is null. See {@link #withRepository(KeySequenceConfigRepository)}
     */
    @Override
    public final KeySequence firstAvailable() {

        if (repository == null) {
            throw new IllegalStateException(KeySequenceConfigRepository.class.getSimpleName() +
                    " was not provided for " + SingleKeySequenceSelector.class.getSimpleName());
        }

        KeySequence keySequence = current.get();
        if (keySequence != null) {

            getLogger().debug("Reusing current key sequence: {}", keySequence.getConfig(true));

            // persist current state
            repository.persist(keySequence.getConfig(true));

            return keySequence;

        }

        KeySequenceConfig existing = findMatchingKeySequenceConfig();

        if (existing == null) {

            KeySequenceConfig newConfig = createKeySequenceConfig(sequenceId);

            getLogger().info("No matching keys sequences found. Creating new {}", newConfig.getKey());
            setCurrent(newConfig);

            return current.get();
        }

        if (isExpired(existing) || isOwned(existing)) {

            getLogger().info("Reusing expired key sequence: {}", existing.getKey());
            setCurrent(existing);

        }

        return current.get();

    }

    private boolean isOwned(KeySequenceConfig existing) {
        return repository.ID == existing.ownerId;
     }

    private KeySequenceConfig findMatchingKeySequenceConfig() {

        Collection<KeySequenceConfig> configs = repository.getAll();

        for (KeySequenceConfig config : configs) {
            if (config.getSeqId() == sequenceId) {
                return config;
            }
        }

        return null;

    }

    /*
     * visible for testing
     */
    boolean isExpired(KeySequenceConfig config) {
        return config.getExpireAt() <= System.currentTimeMillis();
    }

    private void setCurrent(KeySequenceConfig config) {

        repository.persist(config);

        if (repository.consistencyCheck(config)) {
            getLogger().info("Current key sequence: {}", config.getKey());
            current.set(createKeySequence(config));
            return;
        }

        getLogger().warn(String.format("Cannot reuse key sequence %s. Consistency check failed. It seems that other process is using this sequence right now.", config.getKey()));

    }

    // FIXME: extract to factory and allow to define ReaderKeySequence and WriterKeySequence separately
    private KeySequence createKeySequence(KeySequenceConfig config) {
        return new UUIDSequence(config);
    }

    private KeySequenceConfig createKeySequenceConfig(long sequenceId) {
        return new KeySequenceConfig(
                sequenceId,
                UUIDSequence.RESERVED_KEYS,
                UUIDSequence.RESERVED_KEYS
        );
    }

}
