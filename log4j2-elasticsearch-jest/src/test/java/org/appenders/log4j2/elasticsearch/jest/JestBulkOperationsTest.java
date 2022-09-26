package org.appenders.log4j2.elasticsearch.jest;

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


import io.searchbox.action.AbstractAction;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.StringItemSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.StringItemSourceTest.createTestStringItemSource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JestBulkOperationsTest {

    @Test
    public void nonStringItemSourceIsNotSupported() {

        // given
        BatchOperations<Bulk> bulkOperations = JestHttpObjectFactoryTest.createTestObjectFactoryBuilder().build().createBatchOperations();

        ItemSource itemSource = Object::new;

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> bulkOperations.createBatchItem("testIndex", itemSource));

        // then
        assertThat(exception.getMessage(), containsString("Non String payloads are not supported"));

    }

    @Test
    public void bulkContainsAddedStringItem() {

        // given
        BatchOperations<Bulk> bulkOperations = JestHttpObjectFactoryTest.createTestObjectFactoryBuilder().build().createBatchOperations();
        BatchBuilder<Bulk> batchBuilder = bulkOperations.createBatchBuilder();

        String testPayload = "{ \"testfield\": \"testvalue\" }";
        Index item = (Index) bulkOperations.createBatchItem("testIndex", testPayload);

        // when
        batchBuilder.add(item);
        Bulk bulk = batchBuilder.build();

        // then
        JestBatchIntrospector introspector = new JestBatchIntrospector();
        AbstractAction action = (AbstractAction) introspector.items(bulk).iterator().next();
        assertEquals(testPayload, introspector.itemIntrospector().getPayload(action));

    }

    @Test
    public void bulkContainsAddedSourceItem() {

        // given
        BatchOperations<Bulk> bulkOperations = JestHttpObjectFactoryTest.createTestObjectFactoryBuilder().build().createBatchOperations();
        BatchBuilder<Bulk> batchBuilder = bulkOperations.createBatchBuilder();

        String testPayload = "{ \"testfield\": \"testvalue\" }";
        StringItemSource itemSource = spy(createTestStringItemSource(testPayload));
        Index item = (Index) bulkOperations.createBatchItem("testIndex", itemSource);

        // when
        batchBuilder.add(item);
        Bulk bulk = batchBuilder.build();

        // then
        verify(itemSource, times(2)).getSource();
        JestBatchIntrospector introspector = new JestBatchIntrospector();
        AbstractAction action = (AbstractAction) introspector.items(bulk).iterator().next();
        assertEquals(testPayload, introspector.itemIntrospector().getPayload(action));

    }

    @Test
    public void defaultMappingTypeIsNull() {

        // given
        BatchOperations<Bulk> bulkOperations = new JestBulkOperations();

        String testPayload = "{ \"testfield\": \"testvalue\" }";
        StringItemSource itemSource = spy(createTestStringItemSource(testPayload));
        Index item = (Index) bulkOperations.createBatchItem("testIndex", itemSource);

        // when
        String type = item.getType();

        // then
        assertNull(type);

    }

    @Test
    public void mappingTypeCanBeSet() {

        // given
        String expectedMappingType = UUID.randomUUID().toString();
        BatchOperations<Bulk> bulkOperations = new JestBulkOperations(expectedMappingType);

        String testPayload = "{ \"testfield\": \"testvalue\" }";
        StringItemSource itemSource = spy(createTestStringItemSource(testPayload));
        Index item = (Index) bulkOperations.createBatchItem("testIndex", itemSource);

        // when
        String type = item.getType();

        // then
        assertEquals(expectedMappingType, type);

    }

}
