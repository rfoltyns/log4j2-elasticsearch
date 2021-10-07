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

/**
 * Implementation of this interface SHOULD resize given {@link ItemSourcePool}
 * and SHOULD be used as a fail-over when {@link ItemSourcePool} runs out of elements
 */
public interface ResizePolicy {

    String ELEMENT_TYPE = "resizePolicy";

    /**
     * @param itemSourcePool pool to be resized
     * @return true, if any resizing was performed, false otherwise
     */
    boolean increase(ItemSourcePool itemSourcePool);

    /**
     * @param itemSourcePool pool to be resized
     * @return true, if any resizing was performed, false otherwise
     */
    boolean decrease(ItemSourcePool itemSourcePool);


    /**
     * @param itemSourcePool pool to be resized
     * @return true, if any resizing is possible, false otherwise
     */
    default boolean canResize(ItemSourcePool itemSourcePool) {
        return true;
    }

}
