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

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents current state of {@link KeySequence}.
 */
public class KeySequenceConfig implements ItemSource<KeySequenceConfig>, Serializable {

    protected final int initialHashTotal;
    protected final int hashConstant;

    protected final long seqId;
    protected final CharSequence key;

    protected long ownerId;
    protected long readerIndex;
    protected long writerIndex;
    protected long expireAt;

    public KeySequenceConfig(long seqId, long readerIndex, long writerIndex) {
        this.hashConstant = 17;
        this.initialHashTotal = 37 * this.hashConstant + ((int) (seqId ^ (seqId >> 32)));

        this.seqId = seqId;
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
        this.key = new UUID(seqId, 0).toString();

    }

    public CharSequence getKey() {
        return key;
    }

    public long getSeqId() {
        return seqId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(long expireAt) {
        this.expireAt = expireAt;
    }

    public long nextReaderIndex() {
        return readerIndex;
    }

    public void setReaderIndex(long readerIndex) {
        this.readerIndex = readerIndex;
    }

    public long nextWriterIndex() {
        return writerIndex;
    }

    public void setWriterIndex(long writerIndex) {
        this.writerIndex = writerIndex;
    }

    @Override
    public KeySequenceConfig getSource() {
        return this;
    }

    @Override
    public String toString() {
        //noinspection StringBufferReplaceableByString
        return new StringBuilder(128)
                .append("{\"cls\": \"").append(KeySequenceConfig.class.getSimpleName()).append("\",")
                .append("\"seqId\":").append(seqId).append(",")
                .append("\"rIdx\":").append(readerIndex).append(",")
                .append("\"wIdx\":").append(writerIndex).append("}")
                .toString();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof KeySequenceConfig)) {
            return false;
        }

        KeySequenceConfig that = (KeySequenceConfig)obj;
        if (this.seqId != that.seqId) {
            return false;
        }

        if (this.ownerId != that.ownerId) {
            return false;
        }

        if (this.expireAt != that.expireAt) {
            return false;
        }

        if (this.readerIndex != that.readerIndex) {
            return false;
        }

        if (this.writerIndex != that.writerIndex) {
            return false;
        }

        return true;

    }

    @Override
    public int hashCode() {
        Objects.hash(seqId, expireAt, ownerId, readerIndex, writerIndex);
        int total = initialHashTotal;
        total = total * hashConstant + Long.hashCode(expireAt);
        total = total * hashConstant + Long.hashCode(ownerId);
        total = total * hashConstant + Long.hashCode(readerIndex);
        return total * hashConstant + Long.hashCode(writerIndex);
    }

}
