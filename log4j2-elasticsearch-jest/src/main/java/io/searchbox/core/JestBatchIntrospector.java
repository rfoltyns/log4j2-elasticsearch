package io.searchbox.core;

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


import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.action.JestActionIntrospector;
import org.appenders.log4j2.elasticsearch.BatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchItemIntrospector;

import java.util.Collection;

/**
 * Accesses {@link Bulk} non-private members
 */
public class JestBatchIntrospector implements BatchIntrospector<Bulk> {

    private final BatchItemIntrospector<AbstractDocumentTargetedAction<DocumentResult>> itemIntrospector = new JestActionIntrospector();

    @Override
    public Collection items(Bulk introspected) {
        return introspected.bulkableActions;
    }

    public BatchItemIntrospector itemIntrospector() {
        return itemIntrospector;
    }

}
