package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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

import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * NOTE: As of 2.0, this interface will no longer extend {@link LifeCycle}.
 * All existing implementations that use {@link LifeCycle} API should implement it directly and
 * use {@link LifeCycle#of(Object)} instead to access the API.
 */
public interface ItemSourceFactory extends EmptyItemSourceFactory, LifeCycle {

    String ELEMENT_TYPE = "itemSourceFactory";

    /**
     * Indicates whether {@link ItemSource} lifecycle has to be taken care of
     *
     * @return true, if returned {@link ItemSource} instances are buffered, false otherwise
     */
    boolean isBuffered();

    /**
     * Creates {@link ItemSource} from given source using provided {@link ObjectWriter}
     *
     * @param source log item
     * @param objectWriter writer to be used to serialize given item
     * @return {@link ItemSource} containing serialized log
     */
    ItemSource create(Object source, ObjectWriter objectWriter);

}
