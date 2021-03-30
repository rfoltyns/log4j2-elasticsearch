package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchIntrospector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExtendedBulkTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();

        // when
        ExtendedBulk bulk = builder.build();

        // then
        assertNotNull(bulk);

    }

    @Test
    public void builderAddAddsSingleElement() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();
        BatchIntrospector<Bulk> introspector = new JestBatchIntrospector();

        String source = UUID.randomUUID().toString();
        Index action = new Index.Builder(source).build();

        // when
        builder.addAction(action);

        // then
        ExtendedBulk bulk = builder.build();
        assertEquals(1, introspector.items(bulk).size());

    }

    @Test
    public void builderAddCollectionAddsAllElements() {

        // given
        ExtendedBulk.Builder builder = new ExtendedBulk.Builder();
        BatchIntrospector<Bulk> introspector = new JestBatchIntrospector();

        String source = UUID.randomUUID().toString();
        Index action = new Index.Builder(source).build();

        int randomSize = new Random().nextInt(1000) + 10;
        Collection<BulkableAction<DocumentResult>> actions = new ArrayList<>(randomSize);
        for (int ii = 0; ii < randomSize; ii++) {
            actions.add(action);
        }

        // when
        builder.addAction(actions);

        // then
        ExtendedBulk bulk = builder.build();
        assertEquals(randomSize, introspector.items(bulk).size());

    }
}
