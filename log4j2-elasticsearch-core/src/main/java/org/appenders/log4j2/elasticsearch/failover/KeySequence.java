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

/**
 * Allows to define a sequence of reader and writer keys.
 */
public interface KeySequence {

    /**
     * @return next reader key in sequence
     */
    CharSequence nextReaderKey();

    /**
     * @return next writer key in sequence
     */
    CharSequence nextWriterKey();

    /**
     * @param maxKeys Max number of items in returned iterator
     * @return iterator over a batch of next reader keys
     */
    Iterator<CharSequence> nextReaderKeys(long maxKeys);

    /**
     * @return number of available reader keys. MUST be equal to n where readerIndex + n equals writerIndex
     */
    long readerKeysAvailable();

    /**
     * Returns a snapshot or shared, mutable holder of the current state
     *
     * @param sharedInstance if true, returned config will be updated by sub-sequent calls, false otherwise
     * @return representation of current state
     */
    KeySequenceConfig getConfig(boolean sharedInstance);

}
