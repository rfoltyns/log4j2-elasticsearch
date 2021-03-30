package org.appenders.log4j2.elasticsearch.hc;

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

import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class IndexRequestTest {

    @Test
    public void builderFailsWhenSourceIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("source cannot be null"));

    }

    @Test
    public void builderFailsWhenIndexIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder()
                .index(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("index cannot be null"));

    }

    @Test
    public void builderFailsWhenMappingTypeIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder()
                .type(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("type cannot be null"));

    }

    @Test
    public void builderCreatesRequest() {

        // given
        String expectedId = UUID.randomUUID().toString();
        String expectedIndex = UUID.randomUUID().toString();
        String expectedType = UUID.randomUUID().toString();
        ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        // when
        IndexRequest.Builder builder = createIndexRequestBuilder(
                expectedItemSource, expectedId, expectedIndex, expectedType);

        IndexRequest request = builder.build();

        // then
        assertEquals(expectedId, request.getId());
        assertEquals(expectedIndex, request.getIndex());
        assertEquals(expectedType, request.getType());
        assertEquals(expectedItemSource, request.getSource());

    }

    public static IndexRequest.Builder createIndexRequestBuilder() {
        return createIndexRequestBuilder(mock(ByteBufItemSource.class));

    }

    public static IndexRequest.Builder createIndexRequestBuilder(
            ItemSource expectedItemSource
    ) {
        return createIndexRequestBuilder(
                expectedItemSource,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
    }

    private static IndexRequest.Builder createIndexRequestBuilder(
            ItemSource expectedItemSource,
            String expectedId,
            String expectedIndex,
            String expectedMappingType
    ) {
        return new IndexRequest.Builder(expectedItemSource)
                .id(expectedId)
                .index(expectedIndex)
                .type(expectedMappingType);
    }

}
