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

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class IndexRequestTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderFailsWhenSourceIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("source cannot be null");

        // when
        builder.build();

    }

    @Test
    public void builderFailsWhenIndexIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder()
                .index(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("index cannot be null");

        // when
        builder.build();

    }

    @Test
    public void builderFailsWhenMappingTypeIsNull() {

        // given
        IndexRequest.Builder builder = createIndexRequestBuilder()
                .type(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("type cannot be null");

        // when
        builder.build();

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

    @Test
    public void getRestMethodNameReturnsStaticVariable() {

        // given
        IndexRequest indexRequest = createIndexRequestBuilder()
                .index(UUID.randomUUID().toString())
                .build();

        // when
        String restMethodName = indexRequest.getHttpMethodName();

        // then
        assertEquals(IndexRequest.HTTP_METHOD_NAME, restMethodName);

    }

    @Test
    public void urlContainsIdIfSpecified() {

        // given
        ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        String expectedId = UUID.randomUUID().toString();
        String expectedIndex = UUID.randomUUID().toString();
        String expectedMappingType = UUID.randomUUID().toString();

        // when
        IndexRequest.Builder builder = createIndexRequestBuilder(
                expectedItemSource, expectedId, expectedIndex, expectedMappingType);

        IndexRequest request = builder.build();

        // then
        assertTrue(request.getURI().contains(expectedId));

    }

    @Test
    public void urlDoesNotContainIdIfIdNotSpecified() {

        // given
        ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        String expectedId = UUID.randomUUID().toString();
        String expectedIndex = UUID.randomUUID().toString();
        String expectedMappingType = UUID.randomUUID().toString();

        IndexRequest.Builder builder = createIndexRequestBuilder(
                expectedItemSource, null, expectedIndex, expectedMappingType);

        // when
        IndexRequest request = builder.build();

        // then
        assertFalse(request.getURI().contains(expectedId));

    }

    @Test
    public void urlContainsType() {

        // given
        ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        String expectedId = UUID.randomUUID().toString();
        String expectedIndex = UUID.randomUUID().toString();
        String expectedType = UUID.randomUUID().toString();

        // when
        IndexRequest.Builder builder = createIndexRequestBuilder(
                expectedItemSource, expectedId, expectedIndex, expectedType);

        IndexRequest request = builder.build();

        // then
        assertTrue(request.getURI().contains(expectedType));

    }

    @Test
    public void urlContainsIndex() {

        // given
        ByteBufItemSource expectedItemSource = mock(ByteBufItemSource.class);

        String expectedIndexName = UUID.randomUUID().toString();

        // when
        IndexRequest.Builder builder = createIndexRequestBuilder(expectedItemSource)
                .index(expectedIndexName);

        IndexRequest request = builder.build();

        // then
        assertTrue(request.getURI().startsWith(expectedIndexName));

    }

    @Test
    public void unsupportedEncodingIsThrownAsIllegalArgument() throws UnsupportedEncodingException {

        // given
        String expectedIndex = UUID.randomUUID().toString();
        IndexRequest.Builder builder = createIndexRequestBuilder(mock(ByteBufItemSource.class))
                .index(expectedIndex);

        IndexRequest request = spy(builder.build());

        String expectedMessage = UUID.randomUUID().toString();
        when(request.encode(matches(expectedIndex)))
                .thenThrow(new UnsupportedEncodingException(expectedMessage));

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        request.getURI();

    }

    @Test
    public void serializeRequestReturnsGivenSource() {

        // given
        ByteBufItemSource expectedSource = mock(ByteBufItemSource.class);
        IndexRequest request = createIndexRequestBuilder(expectedSource)
                .build();

        // when
        ItemSource<ByteBuf> result = request.serialize();

        // then
        Assert.assertEquals(expectedSource, result);
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
