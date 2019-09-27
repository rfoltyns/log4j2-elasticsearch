package org.appenders.log4j2.elasticsearch;

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

import org.apache.logging.log4j.core.lookup.StrSubstitutor;

public class Log4j2Lookup implements ValueResolver {

    private final StrSubstitutor strSubstitutor;

    public Log4j2Lookup(StrSubstitutor strSubstitutor) {
        this.strSubstitutor = strSubstitutor;
    }

    /**
     * Resolves given {@link VirtualProperty} if {@link VirtualProperty#isDynamic()} is true
     *
     * @param property property to resolve
     * @return resolved value
     */
    @Override
    public String resolve(VirtualProperty property) {
        if (property.isDynamic()) {
            return resolve(property.getValue());
        }
        return property.getValue();
    }

    @Override
    public String resolve(String unresolved) {
        return strSubstitutor.replace(unresolved);
    }

}
