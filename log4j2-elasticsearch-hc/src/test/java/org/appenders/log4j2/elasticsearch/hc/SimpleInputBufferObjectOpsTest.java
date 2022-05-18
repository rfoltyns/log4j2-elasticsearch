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

import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ResizePolicy;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.junit.jupiter.api.Test;

import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.DEFAULT_TEST_ITEM_POOL_NAME;
import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.DEFAULT_TEST_ITEM_SIZE_IN_BYTES;
import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.DEFAULT_TEST_MONITOR_TASK_INTERVAL;
import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.DEFAULT_TEST_RESIZE_TIMEOUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SimpleInputBufferObjectOpsTest {

    @Test
    public void resetDelegatesToSimpleInputBuffer() {

        // given
        SimpleInputBufferPooledObjectOps pooledObjectOps = spy(createDefaultTestObject());
        SimpleInputBuffer inputBuffer = mock(SimpleInputBuffer.class);

        ItemSource<SimpleInputBuffer> itemSourceMock = mock(ItemSource.class);
        when(pooledObjectOps.createItemSource(any())).thenReturn(itemSourceMock);

        ItemSource<SimpleInputBuffer> itemSource = pooledObjectOps.createItemSource(null);
        when(itemSourceMock.getSource()).thenReturn(inputBuffer);

        // when
        pooledObjectOps.reset(itemSource);

        // then
        verify(inputBuffer, times(1)).reset();

    }

    @Test
    public void purgeHasNoSideEffects() {

        // given
        SimpleInputBufferPooledObjectOps pooledObjectOps = spy(createDefaultTestObject());
        SimpleInputBuffer inputBuffer = mock(SimpleInputBuffer.class);

        ItemSource<SimpleInputBuffer> itemSourceMock = mock(ItemSource.class);
        when(pooledObjectOps.createItemSource(any())).thenReturn(itemSourceMock);

        ItemSource<SimpleInputBuffer> itemSource = pooledObjectOps.createItemSource(null);
        when(itemSourceMock.getSource()).thenReturn(inputBuffer);

        // when
        pooledObjectOps.purge(itemSource);

        // then
        verifyNoInteractions(itemSourceMock);
        verifyNoInteractions(inputBuffer);

    }

    public static GenericItemSourcePool<SimpleInputBuffer> createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored) {
        ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        return createDefaultTestGenericItemSourcePool(initialSize, monitored, resizePolicy);
    }

    public static GenericItemSourcePool<SimpleInputBuffer> createDefaultTestGenericItemSourcePool(
            int initialSize,
            boolean monitored,
            ResizePolicy resizePolicy
    ) {

        SimpleInputBufferPooledObjectOps pooledObjectOps = new SimpleInputBufferPooledObjectOps(
                HeapByteBufferAllocator.INSTANCE,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        return new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                monitored,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                initialSize
        );
    }

    public static SimpleInputBufferPooledObjectOps createDefaultTestObject() {
        return new SimpleInputBufferPooledObjectOps(HeapByteBufferAllocator.INSTANCE, DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }
}
