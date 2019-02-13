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

/**
 * {@link ResizePolicy} that can incrementally resize given {@link ItemSourcePool} up to given size.
 */
public class LimitedResizePolicy implements ResizePolicy {

    public static final String PLUGIN_NAME = "LimitedResizePolicy";

    protected final double resizeFactor;
    protected final int maxSize;

    protected LimitedResizePolicy(double resizeFactor, int maxSize) {
        this.resizeFactor = resizeFactor;
        this.maxSize = maxSize;
    }

    /**
     * Attempts to resize given pool.
     * <p>
     * Additional pool size is calculated based on it's {@link ItemSourcePool#getInitialSize()}.
     * <p>
     * Single resize operation will never increase pool size over {@link #maxSize}
     *
     * @param itemSourcePool pool to be resized
     * @throws IllegalArgumentException when {@code resizeFactor * initialPoolSize == 0}
     * @return true, if resize operation was successful, false otherwise
     */
    @Override
    public boolean increase(ItemSourcePool itemSourcePool) {

        final int initialSize = itemSourcePool.getInitialSize();
        int additionalPoolSize = (int) (initialSize * resizeFactor);

        if (additionalPoolSize == 0) {
            throw new IllegalArgumentException(String.format("Applying %s with resizeFactor %s will not resize given pool [%s] with initialPoolSize %s",
                    ResizePolicy.class.getSimpleName(),
                    resizeFactor,
                    itemSourcePool.getName(),
                    initialSize));
        }

        final int totalSize = itemSourcePool.getTotalSize();
        if (additionalPoolSize + totalSize > maxSize) {
            additionalPoolSize = maxSize - totalSize;
        }

        if (additionalPoolSize > 0) {
            itemSourcePool.incrementPoolSize(additionalPoolSize);
            return true;
        } else {
            return false;
        }

    }

    /**
     * Attempts to resize given pool.
     * <p>
     * Number of removed elements is calculated based on {@link ItemSourcePool#getTotalSize()}
     * <p>
     * Single resize operation will never decrease pool's size below it's {@link ItemSourcePool#getInitialSize()}
     *
     * @param itemSourcePool pool to be resized
     * @return true, if resize operation was successful, false otherwise
     */
    @Override
    public boolean decrease(ItemSourcePool itemSourcePool) {

        final int availableSize = itemSourcePool.getAvailableSize();
        int decreaseSize = (int)(itemSourcePool.getTotalSize() * resizeFactor);

        if (decreaseSize > availableSize) {
            return false;
        }

        if (availableSize - decreaseSize < itemSourcePool.getInitialSize()) {
            decreaseSize = availableSize - itemSourcePool.getInitialSize();
        }

        for (int ii = 0; ii < decreaseSize; ii++) {
            itemSourcePool.remove();
        }

        return true;
    }

    @Override
    public boolean canResize(ItemSourcePool itemSourcePool) {
        return itemSourcePool.getTotalSize() < maxSize;
    }

    public static class Builder {

        /**
         * Default resize factor
         */
        public static final double DEFAULT_RESIZE_FACTOR = 0.50;

        protected double resizeFactor = DEFAULT_RESIZE_FACTOR;

        protected int maxSize;

        public LimitedResizePolicy build() {

            if (resizeFactor <= 0) {
                throw new IllegalArgumentException("resizeFactor must be higher than 0");
            }

            if (resizeFactor > 1) {
                throw new IllegalArgumentException("resizeFactor must be lower or equal 1");
            }

            if (maxSize <= 0) {
                throw new IllegalArgumentException("maxSize must be higher or equal 1");
            }

            return new LimitedResizePolicy(resizeFactor, maxSize);

        }

        /**
         * @param resizeFactor fraction of {@link ItemSourcePool#getInitialSize()} by which given pool will be increased, e.g.:
         *                     GIVEN given initial pool size is 100 and resizeFactor is 0.5
         *                     WHEN pool is resized 3 times
         *                     THEN total pooled items is 250
         * @return this
         */
        public LimitedResizePolicy.Builder withResizeFactor(double resizeFactor) {
            this.resizeFactor = resizeFactor;
            return this;
        }

        /**
         * @param maxSize max no. of elements after pool resize
         * @return this
         */
        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
    }

}
