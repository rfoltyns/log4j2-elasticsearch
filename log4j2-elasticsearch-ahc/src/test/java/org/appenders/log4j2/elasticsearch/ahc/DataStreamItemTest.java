package org.appenders.log4j2.elasticsearch.ahc;

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

import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class DataStreamItemTest {

    @Test
    public void builderFailsWhenSourceIsNull() {

        // given
        final IndexRequest.Builder builder = createDataStreamItemRequestBuilder(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("source cannot be null"));

    }

    @Test
    public void builderFailsWhenIndexIsNull() {

        // given
        final IndexRequest.Builder builder = createDataStreamItemRequestBuilder()
                .index(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("index cannot be null"));

    }

    @Test
    public void builderBuildsWhenMappingTypeIsNull() {

        // given
        final IndexRequest.Builder builder = createDataStreamItemRequestBuilder()
                .type(null);

        // when
        final IndexRequest request = builder.build();

        // then
        assertNotNull(request);

    }

    @Test
    public void builderSetsAllFields() {

        // given
        final String expectedIndex = UUID.randomUUID().toString();
        final ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        // when
        final IndexRequest.Builder builder = createDataStreamItemRequestBuilder(
                expectedItemSource, expectedIndex);

        final IndexRequest request = builder.build();

        // then
        assertEquals(expectedIndex, request.getIndex());
        assertEquals(expectedItemSource, request.getSource());

    }

    @Test
    public void requestTypesAreSameIfBothAreNull() {

        // given
        final IndexRequest request1 = createDataStreamItemRequestBuilder()
                .type(null)
                .build();

        final IndexRequest request2 = createDataStreamItemRequestBuilder()
                .type(null)
                .build();

        // when
        final boolean result = request1.sameType(request2);

        // then
        assertTrue(result);

    }

    @Test
    public void requestTypesAreSameIfBothAreEqual() {

        // given
        final String type = UUID.randomUUID().toString();
        final IndexRequest request1 = createDataStreamItemRequestBuilder()
                .type(type)
                .build();

        final IndexRequest request2 = createDataStreamItemRequestBuilder()
                .type(type)
                .build();

        // when
        final boolean result = request1.sameType(request2);

        // then
        assertTrue(result);

    }

    @Test
    public void requestTypesAreNotSameIfOneIsNullAndOtherIsNot() {

        // given
        final String type = UUID.randomUUID().toString();
        final IndexRequest request1 = createDataStreamItemRequestBuilder()
                .type(null)
                .build();

        final IndexRequest request2 = createDataStreamItemRequestBuilder()
                .type(type)
                .build();

        // when
        final boolean result = request1.sameType(request2);

        // then
        assertFalse(result);

    }

    public static IndexRequest.Builder createDataStreamItemRequestBuilder() {
        return createDataStreamItemRequestBuilder(mock(ByteBufItemSource.class));

    }

    public static IndexRequest.Builder createDataStreamItemRequestBuilder(
            final ItemSource expectedItemSource
    ) {
        return createDataStreamItemRequestBuilder(
                expectedItemSource,
                UUID.randomUUID().toString()
        );
    }

    private static IndexRequest.Builder createDataStreamItemRequestBuilder(
            final ItemSource expectedItemSource,
            final String expectedIndex
    ) {
        return new DataStreamItem.Builder(expectedItemSource)
                .index(expectedIndex);
    }

}
