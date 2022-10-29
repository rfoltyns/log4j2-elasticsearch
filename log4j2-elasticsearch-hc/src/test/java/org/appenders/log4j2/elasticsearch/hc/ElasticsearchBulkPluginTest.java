package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElasticsearchBulkPluginTest extends ElasticsearchBulkAPITest {

    public static final String TEST_PAYLOAD_STRING = "{}";
    public static final String TEST_INDEX_NAME = UUID.randomUUID().toString();

    @Test
    public void defaultBuilderBuildsSuccessfully() {

        // given
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder();

        // when
        final ElasticsearchBulkPlugin plugin = builder.build();

        // then
        assertNotNull(plugin);

    }

    @Test
    public void defaultBuilderDoesNotSetMappingType() {

        // given
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder();

        final ElasticsearchBulkPlugin plugin = builder.build();

        // when
        final IndexRequest indexRequest = plugin.itemBuilder(TEST_INDEX_NAME, createTestItemSource(TEST_PAYLOAD_STRING)).build();

        // then
        assertNull(indexRequest.type);

    }

    @Test
    public void builderBuildsWhenMappingTypeIsNull() {

        // given
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder()
                .withMappingType(null);

        final ElasticsearchBulkPlugin plugin = builder.build();

        // when
        final IndexRequest indexRequest = plugin.itemBuilder(TEST_INDEX_NAME, createTestItemSource(TEST_PAYLOAD_STRING)).build();

        // then
        assertNull(indexRequest.type);

    }

    @Test
    public void builderSetsConfiguredMappingType() {

        // given
        final String expectedMappingType = UUID.randomUUID().toString();
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder()
                .withMappingType(expectedMappingType);

        final ElasticsearchBulkPlugin plugin = builder.build();

        // when
        final IndexRequest indexRequest = plugin.itemBuilder(TEST_INDEX_NAME, createTestItemSource(TEST_PAYLOAD_STRING)).build();

        // then
        assertEquals(expectedMappingType, indexRequest.type);

    }

    @Test
    public void builderSetsConfiguredFilterPath() {

        // given
        final String expectedFilterPath = UUID.randomUUID().toString();
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder()
                .withFilterPath(expectedFilterPath);

        final ElasticsearchBulkPlugin plugin = builder.build();

        // when
        final BatchRequest batchRequest = plugin.batchBuilder()
                .withBuffer(ByteBufItemSourceTest.createTestItemSource())
                .build();

        // then
        assertTrue(batchRequest.getURI().endsWith(expectedFilterPath));

    }

    @Test
    public void builderThrowsWhenItemSerializerIsNull() {

        // given
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder()
                .withItemSerializer(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSerializer cannot be null"));

    }

    @Test
    public void builderThrowsWhenResultDeserializerIsNull() {

        // given
        final ElasticsearchBulkPlugin.Builder builder = ElasticsearchBulkPlugin.newBuilder()
                .withResultDeserializer(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resultDeserializer cannot be null"));

    }

}
