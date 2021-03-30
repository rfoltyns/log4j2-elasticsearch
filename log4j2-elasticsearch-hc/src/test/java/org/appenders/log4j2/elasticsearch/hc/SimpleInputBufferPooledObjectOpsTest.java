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
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ReleaseCallback;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SimpleInputBufferPooledObjectOpsTest {

    private static final int TEST_BUFFER_SIZE = 34;

    @Test
    public void createsItemSource() {

        // given
        SimpleInputBufferPooledObjectOps ops = new SimpleInputBufferPooledObjectOps(
                HeapByteBufferAllocator.INSTANCE,
                TEST_BUFFER_SIZE
        );

        // when
        ItemSource result = ops.createItemSource(mock(ReleaseCallback.class));

        // then
        assertNotNull(result);

    }

    @Test
    public void purgeDoesNotAffectItemSource() {

        // given
        SimpleInputBufferPooledObjectOps ops = new SimpleInputBufferPooledObjectOps(
                HeapByteBufferAllocator.INSTANCE,
                TEST_BUFFER_SIZE
        );

        ItemSource result = spy(ops.createItemSource(mock(ReleaseCallback.class)));

        // when
        ops.purge(result);

        // then
        verifyNoMoreInteractions(result);

    }

    @Test
    public void resetDelegatesToUnderlyingItem() {

        // given
        SimpleInputBufferPooledObjectOps ops = new SimpleInputBufferPooledObjectOps(
                HeapByteBufferAllocator.INSTANCE,
                TEST_BUFFER_SIZE
        );

        ItemSource<SimpleInputBuffer> result = spy(ops.createItemSource(source -> {}));
        SimpleInputBuffer simpleInputBuffer = mock(SimpleInputBuffer.class);
        when(result.getSource()).thenReturn(simpleInputBuffer);

        // when
        ops.reset(result);

        // then
        verify(simpleInputBuffer).reset();

    }

    @Test
    public void metricsSupplierReturnsNull() {

        // given
        SimpleInputBufferPooledObjectOps ops = new SimpleInputBufferPooledObjectOps(
                HeapByteBufferAllocator.INSTANCE,
                TEST_BUFFER_SIZE
        );

        Supplier<String> metricsSupplier = ops.createMetricsSupplier();

        // when
        String result = metricsSupplier.get();

        // then
        assertNull(result);

    }
}
