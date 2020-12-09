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

/**
 * Allows to specify desired capacity of {@code io.netty.buffer.ByteBuf}.
 */
class ByteBufBoundedSizeLimitPolicy implements SizeLimitPolicy<ByteBuf> {

    private final int minSize;
    private final int maxSize;

    /**
     * @param minSize minimum allowed capacity
     * @param maxSize maximum allowed capacity
     */
    public ByteBufBoundedSizeLimitPolicy(final int minSize, final int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    /**
     * Adjusts the capacity of given {@code io.netty.buffer.ByteBuf} to allowed limits if needed.
     * If capacity of given buffer is lower than {@link #minSize}, it will be resized to {@link #minSize}.
     * If capacity of given buffer is higher than {@link #maxSize}, it will be resized to {@link #maxSize}.
     * If capacity of given buffer is between {@link #minSize} and {@link #minSize} (inclusive), it's capacity will remain the same.
     *
     * @param buf buffer to resize
     */
    @Override
    public void limit(ByteBuf buf) {

        int capacity = buf.capacity();

        if (capacity > maxSize) {
            buf.capacity(maxSize);
            return; // don't check min bounds
        }

        if (capacity < minSize) {
            buf.capacity(minSize);
        }

    }
}
