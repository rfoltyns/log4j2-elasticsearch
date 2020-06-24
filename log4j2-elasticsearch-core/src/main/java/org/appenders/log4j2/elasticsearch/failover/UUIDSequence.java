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

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides a sequence of UUID(n, m), where n &gt;= 0 and m &gt;= 0
 */
public class UUIDSequence implements KeySequence {

    /**
     * Key sequence keys start from "00000000-0000-000[seqId]-0000-000000000010".
     * This allows to define internal data between "...00" and "...0f" keys
     */
    public static final int RESERVED_KEYS = 16;

    private final long seqId;
    private final AtomicLong readerIndex;
    private final AtomicLong writerIndex;
    private final KeySequenceConfig config;

    private final int hashCode;

    /**
     * @param keySequenceConfig key sequence configuration
     */
    public UUIDSequence(KeySequenceConfig keySequenceConfig) {

        if (keySequenceConfig.nextReaderIndex() < RESERVED_KEYS) {
            throw new IllegalArgumentException("readerIndex cannot be lower than " + RESERVED_KEYS);
        }
        if (keySequenceConfig.nextWriterIndex() < RESERVED_KEYS) {
            throw new IllegalArgumentException("writerIndex cannot be lower than " + RESERVED_KEYS);
        }

        this.config = keySequenceConfig;
        this.seqId = keySequenceConfig.getSeqId();
        this.readerIndex = new AtomicLong(keySequenceConfig.nextReaderIndex());
        this.writerIndex = new AtomicLong(keySequenceConfig.nextWriterIndex());

        this.hashCode = 31 * 37 + Long.hashCode(seqId);

    }

    /**
     * Returns UUID-formatted, HEX representation of next readerIndex value including initial {@link #seqId},
     * e.g. when {@link #seqId} equals 5 and {@link #readerIndex} equals 254, returned value is {@code "00000000-0000-0005-0000-0000000000ff"}
     * e.g. when {@link #seqId} equals 5 and {@link #readerIndex} equals 255, returned value is {@code "00000000-0000-0005-0000-000000000100"}
     *
     * @return next reader key in sequence.
     *          <tt>null</tt>, if {@link #readerIndex} is equal to {@link #writerIndex}
     */
    @Override
    public CharSequence nextReaderKey() {

        long current = readerIndex.get();
        if (readerKeysAvailable() > 0 && readerIndex.compareAndSet(current, current + 1L)) {
            return new UUID(seqId, current + 1L).toString();
        }
        return null;
    }

    /**
     * Returns UUID-formatted, HEX representation of next writerIndex starting from current including initial {@link #seqId},
     * e.g. when {@link #seqId} equals 5 and initial {@link #writerIndex} equals 254, returned value is {@code "00000000-0000-0005-0000-0000000000ff"}
     * e.g. when {@link #seqId} equals 5 and initial {@link #writerIndex} equals 255, returned value is {@code "00000000-0000-0005-0000-000000000100"}
     *
     * @return next reader key in sequence.
     *          <tt>null</tt>, if {@link #readerIndex} is equal to {@link #writerIndex}
     */
    @Override
    public CharSequence nextWriterKey() {
        // TODO: replace with sth smaller if feasible..
        return new UUID(seqId, writerIndex.incrementAndGet()).toString();
    }

    @Override
    public long readerKeysAvailable() {
        return writerIndex.get() - readerIndex.get();
    }

    @Override
    public KeySequenceConfig getConfig(boolean sharedInstance) {

        if (!sharedInstance) {
            return new KeySequenceConfig(this.seqId, readerIndex.get(), writerIndex.get());
        }

        config.setWriterIndex(writerIndex.get());
        config.setReaderIndex(readerIndex.get());
        return config;
    }

    /**
     * @param maxKeys maximum number of keys available in returned iterator
     * @return iterator over a batch of reader keys
     */
    @Override
    public Iterator<CharSequence> nextReaderKeys(long maxKeys) {
        return new KeySequenceIterator(this, maxKeys);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof UUIDSequence)) {
            return false;
        }

        UUIDSequence that = (UUIDSequence) obj;

        // readerIndex and writerIndex values don't matter here.
        // If key sequence is used simultaneously, only one should survive anyway.
        return this.seqId == that.seqId;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
