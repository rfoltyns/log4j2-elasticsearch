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

    private final UnpooledByteBufAllocator byteBufAllocator;
    private final int estimatedSourceSize;

    ByteBufPooledObjectOps(UnpooledByteBufAllocator byteBufAllocator, int estimatedSourceSize) {
        this.byteBufAllocator = byteBufAllocator;
        this.estimatedSourceSize = estimatedSourceSize;
    }

    @Override
    public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {
        CompositeByteBuf buffer = new CompositeByteBuf(byteBufAllocator, false, 2).capacity(estimatedSourceSize);
        return new ByteBufItemSource(buffer, releaseCallback);
    }

    @Override
    public void reset(ItemSource<ByteBuf> pooled) {
        pooled.getSource().clear();
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
