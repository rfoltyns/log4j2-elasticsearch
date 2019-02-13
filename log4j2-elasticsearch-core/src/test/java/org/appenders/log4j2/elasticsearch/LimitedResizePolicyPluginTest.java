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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LimitedResizePolicyPluginTest {

    public static final int TEST_MAX_SZE = 10;

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder();
        builder.withMaxSize(TEST_MAX_SZE);

        // when
        final ResizePolicy policy = builder.build();

        // then
        assertNotNull(policy);
    }

    @Test
    public void builderThrowsWhenResizeFactorIsZero() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder();
        builder.withResizeFactor(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("must be higher than 0"));

    }

    @Test
    public void builderThrowsWhenResizeFactorIsLowerThanZero() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder();
        builder.withResizeFactor(-0.1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("must be higher than 0"));

    }

    @Test
    public void builderThrowsWhenResizeFactorIsHigherThanOne() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder();
        builder.withResizeFactor(1.01);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("must be lower or equal 1"));

    }

    @Test
    public void builderThrowsWhenMaxSizeIsZero() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder()
                .withMaxSize(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxSize must be higher or equal 1"));

    }


    @Test
    public void builderThrowsWhenMaxSizeIsLowerThanZero() {

        // given
        final LimitedResizePolicy.Builder builder = LimitedResizePolicyPlugin.newBuilder()
                .withMaxSize(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxSize must be higher or equal 1"));

    }

    @Test
    public void canResizeWhenTotalSizeLowerThanMaxSize() {

        // given
        final LimitedResizePolicy policy = LimitedResizePolicyPlugin.newBuilder()
                .withMaxSize(1)
                .build();

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(0, true);

        // when
        final boolean result = policy.canResize(pool);

        // then
        assertTrue(result);

    }

    @Test
    public void cannotResizeWhenTotalSizeEqualMaxSize() {

        // given
        final LimitedResizePolicy policy = LimitedResizePolicyPlugin.newBuilder()
                .withMaxSize(1)
                .build();

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(0, true);
        pool.incrementPoolSize(1);

        // when
        final boolean result = policy.canResize(pool);

        // then
        assertFalse(result);

    }

    @Test
    public void cannotResizeWhenTotalHigherThanMaxSize() {

        // given
        final LimitedResizePolicy policy = LimitedResizePolicyPlugin.newBuilder()
                .withMaxSize(1)
                .build();

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(0, true);
        pool.incrementPoolSize(2);

        // when
        final boolean result = policy.canResize(pool);

        // then
        assertFalse(result);

    }

    @Test
    public void increaseThrowsWhenResizeWouldNotTakeAnyEffect() {

        // given
        final ResizePolicy policy = LimitedResizePolicyPlugin.newBuilder()
                .withResizeFactor(0.1)
                .withMaxSize(10)
                .build();

        final ItemSourcePool pool = mock(ItemSourcePool.class);
        final Integer initialPoolSize = 5;
        when(pool.getInitialSize()).thenReturn(initialPoolSize);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> policy.increase(pool));

        // then
        assertThat(exception.getMessage(), containsString("will not resize given pool"));

    }

    @Test
    public void increaseIncrementsPoolSizeByResizeFactorMultipliedByInitialPoolSize() {

        // given
        double resizeFactor = 0.2;
        final ResizePolicy policy = spy(createDefaultTestLimitedResizePolicyBuilder(resizeFactor)
                .build());

        final ItemSourcePool pool = mock(ItemSourcePool.class);
        final int initialPoolSize = 10;
        when(pool.getInitialSize()).thenReturn(initialPoolSize);

        // when
        final boolean resized = policy.increase(pool);

        // then
        assertTrue(resized);

        final ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(pool).incrementPoolSize(captor.capture());
        assertEquals((Object)(int)(initialPoolSize * resizeFactor), captor.getValue());

    }

    @Test
    public void increaseNeverExceedsPolicyMaxSize() {

        // given
        final double resizeFactor = 0.2;
        final int expectedMaxSize = 11;
        final ResizePolicy policy = LimitedResizePolicyPlugin.newBuilder()
                .withResizeFactor(resizeFactor)
                .withMaxSize(expectedMaxSize)
                .build();

        final int initialPoolSize = 10;
        final ItemSourcePool pool = mock(ItemSourcePool.class);
        when(pool.getInitialSize()).thenReturn(initialPoolSize);
        when(pool.getTotalSize()).thenReturn(initialPoolSize);

        // when
        final boolean resized = policy.increase(pool);

        // then
        assertTrue(resized);

        final ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(pool).incrementPoolSize(captor.capture());
        assertEquals((Object)(int)(expectedMaxSize - pool.getTotalSize()), captor.getValue());

    }

    @Test
    public void decreaseShrinksPoolByTotalSizeMultipliedByResizeFactor() {

        // given
        final int initialSize = 50;
        final int additionalSize = 50;

        final int expectedResizedTotalSize = 80;
        final double resizeFactor = 0.2;

        final ResizePolicy resizePolicy = createDefaultTestLimitedResizePolicyBuilder(resizeFactor).build();

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true);
        pool.incrementPoolSize(initialSize + additionalSize);

        // when
        final boolean resized = resizePolicy.decrease(pool);

        // then
        assertTrue(resized);

        assertEquals(expectedResizedTotalSize, pool.getTotalSize());

    }

    @Test
    public void decreaseNeverShrinksBelowInitialSize() throws PoolResourceException {

        // given
        final int initialSize = 40;
        final int additionalSize = 60;
        final double resizeFactor = 0.75;
        final int borrowedCount = 5;
        final int expectedResizedTotalSize = initialSize + borrowedCount;

        final ResizePolicy resizePolicy = spy(createDefaultTestLimitedResizePolicyBuilder(resizeFactor).build());

        final PooledObjectOps<? extends Object> pooledObjectOps = spy(ByteBufPooledObjectOpsTest.createTestPooledObjectOps());

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true, pooledObjectOps);

        pool.incrementPoolSize(initialSize + additionalSize);

        for (int ii = 0; ii < borrowedCount; ii++) {
            pool.getPooled();
        }

        // when
        final boolean resized = resizePolicy.decrease(pool);

        // then
        assertTrue(resized);

        // initialSize(40) + 5 in use
        assertEquals(expectedResizedTotalSize, pool.getTotalSize());

        // additionalSize(60) - 5 in use
        verify(pooledObjectOps, times(55)).purge(any(ItemSource.class));

    }

    @Test
    public void decreaseNeverShrinksIfResizeWouldBeHigherThanAvailableSize() throws PoolResourceException {

        // given
        final int initialSize = 10;
        final int additionalSize = 5;
        final double resizeFactor = 0.50;
        final int expectedResizedTotalSize = initialSize + additionalSize;
        final int borrowedCount = 10;

        final ResizePolicy resizePolicy = createDefaultTestLimitedResizePolicyBuilder(resizeFactor).build();

        final PooledObjectOps<? extends Object> pooledObjectOps = spy(ByteBufPooledObjectOpsTest.createTestPooledObjectOps());

        final ItemSourcePool pool = GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true, pooledObjectOps);
        pool.incrementPoolSize(initialSize + additionalSize);

        // when 10 used
        for (int ii = 0; ii < borrowedCount; ii++) {
            pool.getPooled();
        }

        // when
        boolean resized = resizePolicy.decrease(pool);

        // then
        assertFalse(resized);
        assertEquals(expectedResizedTotalSize, pool.getTotalSize());
        verify(pooledObjectOps, times(0)).purge(any());

    }

    private LimitedResizePolicy.Builder createDefaultTestLimitedResizePolicyBuilder(double resizeFactor) {
        return LimitedResizePolicyPlugin.newBuilder()
                .withResizeFactor(resizeFactor)
                .withMaxSize(100);
    }

}
