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

import java.util.function.Supplier;

/**
 * Allows to retrieve an instance of {@link KeySequence}.
 */
public interface KeySequenceSelector {

    /**
     * Returned {@code Supplier} SHOULD return a current {@link KeySequence}
     *
     * @return supplier with current {@link KeySequence}
     */
    Supplier<KeySequence> currentKeySequence();

    /**
     * @return available {@link KeySequence}
     */
    KeySequence firstAvailable();

    /**
     * @param keySequenceConfigRepository repository to check against
     * @return this
     */
    KeySequenceSelector withRepository(KeySequenceConfigRepository keySequenceConfigRepository);

    /**
     * Cleanup underlying resources and reset
     */
    void close();

}
