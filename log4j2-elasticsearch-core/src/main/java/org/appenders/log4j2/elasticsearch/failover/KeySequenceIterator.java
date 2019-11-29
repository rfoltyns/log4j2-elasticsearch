package org.appenders.log4j2.elasticsearch.failover;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 - 2020 Rafal Foltynski
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
import java.util.concurrent.atomic.AtomicLong;

class KeySequenceIterator implements Iterator<CharSequence> {

    private final KeySequence keySequence;
    private final AtomicLong remaining;

    public KeySequenceIterator(KeySequence keySequence, long maxKeys) {
        this.keySequence = keySequence;
        this.remaining = new AtomicLong(maxKeys);
    }

    @Override
    public boolean hasNext() {
        return remaining.get() > 0 && keySequence.readerKeysAvailable() > 0;
    }

    @Override
    public CharSequence next() {
        remaining.decrementAndGet();
        return keySequence.nextReaderKey();
    }

}
