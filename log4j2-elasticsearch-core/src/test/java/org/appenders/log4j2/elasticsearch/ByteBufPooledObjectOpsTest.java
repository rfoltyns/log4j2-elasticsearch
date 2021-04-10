package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import io.netty.buffer.CompositeByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.byteBufAllocator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ByteBufPooledObjectOpsTest {

    public static final int DEFAULT_TEST_SOURCE_SIZE = 1024;

    @Test
    public void createsByteBufItemSource() {

        // given
        int expectedEstimatedSourceSize = 1024;

        PooledObjectOps<ByteBuf> pooledObjectOps = createTestPooledObjectOps(expectedEstimatedSourceSize);

        // when
        ItemSource<ByteBuf> itemSource = pooledObjectOps.createItemSource(ItemSource::release);

        // then
        assertNotNull(itemSource);
        assertEquals(expectedEstimatedSourceSize, itemSource.getSource().writableBytes());

    }

    @Test
    public void resetShrinksBufferToMaxSizeIfOversized() {

        // given
        PooledObjectOps<ByteBuf> pooledObjectOps = createTestPooledObjectOps();

        ItemSource<ByteBuf> itemSource = pooledObjectOps.createItemSource(pooled -> {
        });

        int postWriteCapacity = DEFAULT_TEST_SOURCE_SIZE * 2;
        byte[] bytes = new byte[postWriteCapacity];
        Arrays.fill(bytes, (byte) 1);

        itemSource.getSource().writeBytes(bytes);

        // sanity check
        assertEquals(postWriteCapacity, itemSource.getSource().capacity());

        // when
        pooledObjectOps.reset(itemSource);

        // then
        assertEquals(DEFAULT_TEST_SOURCE_SIZE, itemSource.getSource().capacity());
        assertEquals(0, itemSource.getSource().readerIndex());
        assertEquals(0, itemSource.getSource().writerIndex());

    }

    @Test
    public void createsBufferWithMinSize() {

        // given
        PooledObjectOps<ByteBuf> pooledObjectOps = new ByteBufPooledObjectOps(byteBufAllocator,
                new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_SOURCE_SIZE / 2, DEFAULT_TEST_SOURCE_SIZE));

        // when
        ItemSource<ByteBuf> itemSource = pooledObjectOps.createItemSource(pooled -> {});

        // then
        assertEquals(DEFAULT_TEST_SOURCE_SIZE / 2, itemSource.getSource().capacity());

    }

    @Test
    public void purgeReleasesItemSource() {

        // given
        CompositeByteBuf testByteBuf = spy(ByteBufItemSourceTest.createDefaultTestByteBuf());

        PooledObjectOps<ByteBuf> pooledObjectOps = new ByteBufPooledObjectOps(byteBufAllocator,
                new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_SOURCE_SIZE, DEFAULT_TEST_SOURCE_SIZE)) {
            @Override
            public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {
                return spy(new ByteBufItemSource(testByteBuf, releaseCallback));
            }
        };

        ItemSource<ByteBuf> itemSource = pooledObjectOps.createItemSource(pooled -> {});

        // when
        pooledObjectOps.purge(itemSource);

        // then
        verify(testByteBuf).release();

    }

    @Test
    public void createsMetricsSupplier() {

        // given
        PooledObjectOps<ByteBuf> pooledObjectOps = createTestPooledObjectOps();

        // when
        Supplier<String> metricsSupplier = pooledObjectOps.createMetricsSupplier();

        // then
        assertNotNull(metricsSupplier);
        assertNotNull(metricsSupplier.get());

    }

    public static ByteBufPooledObjectOps createTestPooledObjectOps() {
        return createTestPooledObjectOps(DEFAULT_TEST_SOURCE_SIZE);
    }

    public static ByteBufPooledObjectOps createTestPooledObjectOps(int expectedEstimatedSourceSize) {
        return new ByteBufPooledObjectOps(
                byteBufAllocator,
                new ByteBufBoundedSizeLimitPolicy(expectedEstimatedSourceSize, expectedEstimatedSourceSize));
    }

}
