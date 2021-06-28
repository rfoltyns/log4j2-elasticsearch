package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import static org.appenders.core.logging.InternalLogging.getLogger;

import org.apache.logging.log4j.core.LogEvent;

public class NonEmptyFilter implements VirtualPropertyFilter {

    /**
     * Allows to determine inclusion based on presence and length of given value.
     *
     * @param fieldName Name to be logged on exclusion
     * @param resolvedValue result of {@link ValueResolver#resolve(VirtualProperty, LogEvent)}
     *
     * @return <i>true</i>, if {@code resolvedValue} is not null and it's length is greater than 0, <i>false</i> otherwise
     */
    @Override
    public final boolean isIncluded(String fieldName, String resolvedValue) {

        if (resolvedValue == null) {
            getLogger().debug("VirtualProperty with excluded. Value was null. Name: {}", fieldName);
            return false;
        }

        if (resolvedValue.isEmpty()) {
            getLogger().debug("VirtualProperty with excluded. Value was empty. Name: {}", fieldName);
            return false;
        }

        return true;
    }

}
