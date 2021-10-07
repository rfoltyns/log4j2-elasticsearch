package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ItemSourcePoolTest {

    @Test
    public void tryGetPooledThrowsByDefault() {

        // given
        final ItemSourcePool<Object> pool = new ItemSourcePool<Object>() {
            @Override
            public void incrementPoolSize(int delta) {
            }

            @Override
            public void incrementPoolSize() {
            }

            @Override
            public ItemSource<Object> getPooled() throws PoolResourceException {
                return null;
            }

            @Override
            public boolean remove() {
                return false;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public int getInitialSize() {
                return 0;
            }

            @Override
            public int getTotalSize() {
                return 0;
            }

            @Override
            public int getAvailableSize() {
                return 0;
            }

            @Override
            public void shutdown() {

            }

            @Override
            public void start() {

            }

            @Override
            public boolean isStarted() {
                return false;
            }

            @Override
            public boolean isStopped() {
                return false;
            }
        };

         // when
        final UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class, pool::getPooledOrNull);

        // then
        assertThat(exception.getMessage(), equalTo("Not implemented"));

    }

}
