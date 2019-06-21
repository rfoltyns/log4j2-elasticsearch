package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourcePool;
import org.appenders.log4j2.elasticsearch.PoolResourceException;
import org.appenders.log4j2.elasticsearch.ResizePolicy;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.hc.SimpleInputBufferObjectOpsTest.createDefaultTestGenericItemSourcePool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PoolingAsyncResponseConsumerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void onResponseReceivedReturnsTheSameResponse() throws IOException {

        // given
        HttpResponse response = mock(HttpResponse.class);
        PoolingAsyncResponseConsumer consumer = createDefaultTestObject();

        // when
        consumer.onResponseReceived(response);
        HttpResponse actual = consumer.buildResult(null);

        // then
        assertEquals(response, actual);
    }

    @Test
    public void onEntityEnclosedSetsResponseInputStream() throws IOException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                        GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                        false
                );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        // when
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        // then
        HttpResponse response = consumer.buildResult(null);
        verify(response, times(1)).setEntity(any());

    }

    @Test
    public void onEntityEnclosedPoolsTheBufferOnce() throws IOException, PoolResourceException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = spy(createDefaultTestGenericItemSourcePool(
                GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                false
        ));

        PoolingAsyncResponseConsumer consumer = spy(createDefaultTestObject(itemSourcePool));
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        // when
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        // then
        verify(consumer, times(1)).getPooled();

    }

    @Test
    public void onContentReceivedPassedDecoderToBuffer() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = mock(ItemSourcePool.class);

        ItemSource<SimpleInputBuffer> itemSource = mock(ItemSource.class);
        when(itemSourcePool.getPooled()).thenReturn(itemSource);

        SimpleInputBuffer buffer = mock(SimpleInputBuffer.class);
        when(itemSource.getSource()).thenReturn(buffer);

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        ContentDecoder contentDecoder = mock(ContentDecoder.class);

        // when
        consumer.onContentReceived(contentDecoder, mock(IOControl.class));

        // then
        verify(buffer, times(1)).consumeContent(eq(contentDecoder));

    }

    @Test
    public void onContentReceivedThrowsIOExceptionOnEmptyPool() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                0,
                false,
                new ResizePolicy() {
                    @Override
                    public boolean increase(ItemSourcePool itemSourcePool) {
                        return false;
                    }

                    @Override
                    public boolean decrease(ItemSourcePool itemSourcePool) {
                        return false;
                    }
                }
        );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);

        expectedException.expect(IOException.class);
        expectedException.expectMessage("Unable to resize. Creation of ItemSource was unsuccessful");

        // when
         consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

    }

    @Test
    public void releaseResourcesNullifiesTheResponse() throws IOException {

        // given
        GenericItemSourcePool<SimpleInputBuffer> itemSourcePool = createDefaultTestGenericItemSourcePool(
                GenericItemSourcePoolTest.DEFAULT_TEST_INITIAL_POOL_SIZE,
                false
        );

        PoolingAsyncResponseConsumer consumer = createDefaultTestObject(itemSourcePool);
        consumer.onResponseReceived(mock(HttpResponse.class));

        HttpEntity httpEntity = mock(HttpEntity.class);
        consumer.onEntityEnclosed(httpEntity, ContentType.create("application/json"));

        HttpResponse before = consumer.buildResult(null);
        assertNotNull(before);

        // when
        consumer.releaseResources();

        // then
        HttpResponse response = consumer.buildResult(null);
        assertNull(response);

    }

    private PoolingAsyncResponseConsumer createDefaultTestObject(ItemSourcePool<SimpleInputBuffer> itemSourcePool) {
        return new PoolingAsyncResponseConsumer(itemSourcePool);
    }

    private PoolingAsyncResponseConsumer createDefaultTestObject() {
        ItemSourcePool<SimpleInputBuffer> itemSourcePool = mock(ItemSourcePool.class);
        return createDefaultTestObject(itemSourcePool);
    }

}
