package org.appenders.log4j2.elasticsearch;

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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.function.Supplier;

class ByteBufPooledObjectOps implements PooledObjectOps<ByteBuf> {

    private static final int NAX_NUM_COMPONENTS = Integer.parseInt(
            System.getProperty("appenders.io.netty.buffer.CompositeByteBuf.naxNumComponents", "2"));

    private final UnpooledByteBufAllocator byteBufAllocator;
    private final SizeLimitPolicy<ByteBuf> sizeLimitPolicy;

    /**
     * @param byteBufAllocator {@code io.netty.buffer.UnpooledByteBufAllocator} to use
     * @param initialSize initial buffer size
     *
     * @deprecated As of 1.6, use {@link #ByteBufPooledObjectOps(UnpooledByteBufAllocator, SizeLimitPolicy)} instead.
     */
    @Deprecated
    ByteBufPooledObjectOps(UnpooledByteBufAllocator byteBufAllocator, int initialSize) {
        this(byteBufAllocator, new ByteBufBoundedSizeLimitPolicy(initialSize, Integer.MAX_VALUE));
    }

    /**
     * @param byteBufAllocator {@code io.netty.buffer.ByteBufAllocator} to use
     * @param sizeLimitPolicy {@link SizeLimitPolicy} to be applied on creation and {@link #reset(ItemSource)}
     */
    ByteBufPooledObjectOps(UnpooledByteBufAllocator byteBufAllocator, SizeLimitPolicy<ByteBuf> sizeLimitPolicy) {
        this.byteBufAllocator = byteBufAllocator;
        this.sizeLimitPolicy = sizeLimitPolicy;
    }

    @Override
    public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {

        CompositeByteBuf buffer = new CompositeByteBuf(byteBufAllocator, false, NAX_NUM_COMPONENTS);
        sizeLimitPolicy.limit(buffer);

        return new ByteBufItemSource(buffer, releaseCallback);

    }

    @Override
    public void reset(ItemSource<ByteBuf> pooled) {

        ByteBuf buffer = pooled.getSource();
        buffer.clear();

        sizeLimitPolicy.limit(buffer);

    }

    @Override
    public boolean purge(ItemSource<ByteBuf> pooled) {
        return pooled.getSource().release();
    }

    @Override
    public Supplier<String> createMetricsSupplier() {
        return () -> byteBufAllocator.metric().toString();
    }

}
