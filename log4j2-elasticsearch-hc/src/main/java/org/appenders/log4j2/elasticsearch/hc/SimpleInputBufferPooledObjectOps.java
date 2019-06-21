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

import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.PooledObjectOps;
import org.appenders.log4j2.elasticsearch.ReleaseCallback;

import java.util.function.Supplier;

/**
 * Defines operations to be executed on {@code org.apache.http.nio.util.SimpleInputBuffer}
 * within the implementation of {@link org.appenders.log4j2.elasticsearch.GenericItemSourcePool}.
 */
public class SimpleInputBufferPooledObjectOps implements PooledObjectOps<SimpleInputBuffer> {

    private final ByteBufferAllocator byteBufAllocator;
    private final int bufferSizeInBytes;

    SimpleInputBufferPooledObjectOps(ByteBufferAllocator byteBufAllocator, int bufferSizeInBytes) {
        this.byteBufAllocator = byteBufAllocator;
        this.bufferSizeInBytes = bufferSizeInBytes;
    }

    @Override
    public ItemSource<SimpleInputBuffer> createItemSource(ReleaseCallback<SimpleInputBuffer> releaseCallback) {
        SimpleInputBuffer buffer = new SimpleInputBuffer(bufferSizeInBytes, byteBufAllocator);
        return new InputBufferItemSource(buffer, releaseCallback);
    }

    @Override
    public void reset(ItemSource<SimpleInputBuffer> pooled) {
        pooled.getSource().reset();
    }

    @Override
    public boolean purge(ItemSource<SimpleInputBuffer> pooled) {
        return true;
    }

    @Override
    public Supplier<String> createMetricsSupplier() {
        return () -> null;
    }

}
