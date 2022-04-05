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
 * {@link ResizePolicy} resizing without upper limits to given {@link ItemSourcePool} size.
 */
public class UnlimitedResizePolicy implements ResizePolicy {

    private final double resizeFactor;

    protected UnlimitedResizePolicy(final double resizeFactor) {
        this.resizeFactor = resizeFactor;
    }

    /**
     * Attempts to resize given pool.
     * <p>
     * Additional pool size is calculated based on it's {@link ItemSourcePool#getInitialSize()}.
     * <p>
     * Single resize operation will never increase pool's size by more than 100%
     *
     * @param itemSourcePool pool to be resized
     * @throws IllegalStateException when {@code resizeFactor * initialPoolSize == 0}
     * @return true, if resize operation was successful, false otherwise
     */
    @Override
    public final boolean increase(final ItemSourcePool itemSourcePool) {

        int initialPoolSize = itemSourcePool.getInitialSize();
        int additionalPoolSize = (int) (initialPoolSize * resizeFactor);

        if (additionalPoolSize == 0) {
            throw new IllegalArgumentException(String.format("Applying %s with resizeFactor %s will not resize given pool [%s] with initialPoolSize %s",
                    ResizePolicy.class.getSimpleName(),
                    resizeFactor,
                    itemSourcePool.getName(),
                    itemSourcePool.getInitialSize()));
        }

        itemSourcePool.incrementPoolSize(additionalPoolSize);

        return true;
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
    public final boolean decrease(final ItemSourcePool itemSourcePool) {

        int availableSize = itemSourcePool.getAvailableSize();
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

    public static class Builder {

        /**
         * Default resize factor
         */
        public static final double DEFAULT_RESIZE_FACTOR = 0.50;

        private double resizeFactor = DEFAULT_RESIZE_FACTOR;

        public UnlimitedResizePolicy build() {

            if (resizeFactor <= 0) {
                throw new IllegalArgumentException("resizeFactor must be higher than 0");
            }

            if (resizeFactor > 1) {
                throw new IllegalArgumentException("resizeFactor must be lower or equal 1");
            }

            return new UnlimitedResizePolicy(resizeFactor);
        }

        /**
         * @param resizeFactor fraction of {@link ItemSourcePool#getInitialSize()} by which given pool will be increased, e.g.:
         *                     GIVEN given initial pool size is 100 and resizeFactor is 0.5
         *                     WHEN pool is resized 3 times
         *                     THEN total pooled items is 250
         * @return this
         */
        public Builder withResizeFactor(final double resizeFactor) {
            this.resizeFactor = resizeFactor;
            return this;
        }

    }

}
