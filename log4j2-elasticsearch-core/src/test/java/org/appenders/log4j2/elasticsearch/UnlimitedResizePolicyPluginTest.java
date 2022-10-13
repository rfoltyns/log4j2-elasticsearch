package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

public class UnlimitedResizePolicyPluginTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final UnlimitedResizePolicyPlugin.Builder builder = createTestResizePolicyBuilder();

        // when
        final ResizePolicy policy = builder.build();

        // then
        assertNotNull(policy);

    }


    private UnlimitedResizePolicyPlugin.Builder createTestResizePolicyBuilder() {
        return UnlimitedResizePolicyPlugin.newBuilder();
    }

    @Test
    public void builderThrowsWhenResizeFactorIsZero() {

        // given
        final UnlimitedResizePolicyPlugin.Builder builder = createTestResizePolicyBuilder()
                .withResizeFactor(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resizeFactor must be higher than 0"));

    }

    @Test
    public void builderThrowsWhenResizeFactorIsLowerThanZero() {

        // given
        final UnlimitedResizePolicyPlugin.Builder builder = createTestResizePolicyBuilder()
                .withResizeFactor(-0.1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resizeFactor must be higher than 0"));

    }

    @Test
    public void builderThrowsWhenResizeFactorIsHigherThanOne() {

        // given
        final UnlimitedResizePolicyPlugin.Builder builder = createTestResizePolicyBuilder()
                .withResizeFactor(1.01);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("resizeFactor must be lower or equal 1"));

    }

    @Test
    public void canResizeByDefault() {

        // given
        final UnlimitedResizePolicy policy = UnlimitedResizePolicyPlugin.newBuilder().build();

        // when
        final boolean result = policy.canResize(null);

        // then
        assertTrue(result);

    }

    @Test
    public void increaseThrowsWhenResizeWouldNotTakeAnyEffect() {

        // given
        final ResizePolicy policy = createTestResizePolicyBuilder()
                .withResizeFactor(0.1)
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
        final double resizeFactor = 0.2;
        final ResizePolicy policy = createTestResizePolicyBuilder()
                .withResizeFactor(resizeFactor)
                .build();

        final int initialPoolSize = 10;
        final ItemSourcePool pool = mock(ItemSourcePool.class);
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
    public void decreaseShrinksPoolByTotalSizeMultipliedByResizeFactor() {

        // given
        final int initialSize = 50;
        final int additionalSize = 50;

        final int expectedResizedTotalSize = 80;
        final double resizeFactor = 0.2;

        final ResizePolicy resizePolicy = createTestResizePolicyBuilder()
                .withResizeFactor(resizeFactor)
                .build();

        final ItemSourcePool pool = spy(GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true));
        pool.start();
        pool.incrementPoolSize(additionalSize);

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
        final int expectedResizedTotalSize = initialSize + 5; // 5 will be in use

        final ResizePolicy resizePolicy = createTestResizePolicyBuilder()
                .withResizeFactor(resizeFactor)
                .build();

        final PooledObjectOps pooledObjectOps = mock(PooledObjectOps.class);
        when(pooledObjectOps.createItemSource(any())).thenReturn(mock(ItemSource.class));

        final ItemSourcePool pool = spy(GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true, pooledObjectOps));
        pool.start();
        pool.incrementPoolSize(additionalSize);

        // when only 5 used
        for (int ii = 0; ii < 5; ii++) {
            pool.getPooled();
        }

        // when
        boolean resized = resizePolicy.decrease(pool);

        // then
        assertTrue(resized);

        // initialSize(40) + 5 in use
        assertEquals(expectedResizedTotalSize, pool.getTotalSize());

        // additionalSize(60) - 5 in use
        verify(pooledObjectOps, times(55)).purge(any()); // remove is final, so verifying via internal calls

    }

    @Test
    public void decreaseNeverShrinksWhenResizeWouldBeHigherThanAvailableSize() throws PoolResourceException {

        // given
        final int initialSize = 40;
        final int additionalSize = 60;
        final double resizeFactor = 0.55;
        final int expectedResizedTotalSize = initialSize + additionalSize;

        final ResizePolicy resizePolicy = createTestResizePolicyBuilder()
                .withResizeFactor(resizeFactor)
                .build();

        final PooledObjectOps pooledObjectOps = mock(PooledObjectOps.class);
        when(pooledObjectOps.createItemSource(any())).thenReturn(mock(ItemSource.class));

        final ItemSourcePool pool = spy(GenericItemSourcePoolTest.createDefaultTestGenericItemSourcePool(initialSize, true));
        pool.start();
        pool.incrementPoolSize(additionalSize);

        // when 50 used
        for (int ii = 0; ii < 50; ii++) {
            pool.getPooled();
        }

        // when
        final boolean resized = resizePolicy.decrease(pool);

        // then
        assertFalse(resized);

        // initialSiz
        assertEquals(expectedResizedTotalSize, pool.getTotalSize());

        verify(pooledObjectOps, times(0)).purge(any()); // remove is final, so verifying via internal calls

    }

}
