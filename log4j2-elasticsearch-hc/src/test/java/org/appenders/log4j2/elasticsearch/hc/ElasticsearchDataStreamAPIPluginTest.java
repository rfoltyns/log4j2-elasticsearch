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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElasticsearchDataStreamAPIPluginTest extends ElasticsearchBulkAPITest {

    public static final String TEST_PAYLOAD_STRING = "{}";
    public static final String TEST_NAME = UUID.randomUUID().toString();

    @Test
    public void defaultBuilderBuildsSuccessfully() {

        // given
        final ElasticsearchDataStreamAPIPlugin.Builder builder = ElasticsearchDataStreamAPIPlugin.newBuilder();

        // when
        final ElasticsearchDataStreamAPIPlugin plugin = builder.build();

        // then
        assertNotNull(plugin);

    }

    @Test
    public void defaultBuilderDoesNotSetMappingType() {

        // given
        final ElasticsearchDataStreamAPIPlugin.Builder builder = ElasticsearchDataStreamAPIPlugin.newBuilder();

        final ElasticsearchDataStreamAPIPlugin plugin = builder.build();

        // when
        final IndexRequest indexRequest = plugin.itemBuilder(TEST_NAME, createTestItemSource(TEST_PAYLOAD_STRING)).build();

        // then
        assertNull(indexRequest.id);
        assertNull(indexRequest.type);
        assertNotNull(indexRequest.index);

    }

    @Test
    public void builderSetsConfiguredFilterPath() {

        // given
        final String expectedFilterPath = UUID.randomUUID().toString();
        final ElasticsearchDataStreamAPIPlugin.Builder builder = ElasticsearchDataStreamAPIPlugin.newBuilder()
                .withFilterPath(expectedFilterPath);

        final ElasticsearchDataStreamAPIPlugin plugin = builder.build();
        final BatchRequest.Builder batchBuilder = plugin.batchBuilder();
        batchBuilder.add(IndexRequestTest.createIndexRequestBuilder().build());

        // when
        final BatchRequest batchRequest = batchBuilder
                .withBuffer(ByteBufItemSourceTest.createTestItemSource())
                .build();

        // then
        assertTrue(batchRequest.getURI().endsWith(expectedFilterPath));

    }

    @Test
    public void builderThrowsWhenItemSerializerIsNull() {

        // given
        final ElasticsearchDataStreamAPIPlugin.Builder builder = ElasticsearchDataStreamAPIPlugin.newBuilder();
        builder.withItemSerializer(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSerializer cannot be null"));

    }

    @Test
    public void builderThrowsWhenResultDeserializerIsNull() {

        // given
        final ElasticsearchDataStreamAPIPlugin.Builder builder = ElasticsearchDataStreamAPIPlugin.newBuilder();
        builder.withResultDeserializer(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resultDeserializer cannot be null"));

    }

}
