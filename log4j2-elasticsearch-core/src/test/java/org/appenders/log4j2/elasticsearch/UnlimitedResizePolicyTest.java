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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnlimitedResizePolicyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        UnlimitedResizePolicy.Builder builder = UnlimitedResizePolicy.newBuilder();

        // when
        ResizePolicy policy = builder.build();

        // then
        assertNotNull(policy);
    }

    @Test
    public void builderThrowsWhenResizeFactorIsZero() {

        // given
        UnlimitedResizePolicy.Builder builder = UnlimitedResizePolicy.newBuilder();
        builder.withResizeFactor(0);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("must be higher than 0");

        // when
        builder.build();

    }

    @Test
    public void builderThrowsWhenResizeFactorIsLowerThanZero() {

        // given
        UnlimitedResizePolicy.Builder builder = UnlimitedResizePolicy.newBuilder();
        builder.withResizeFactor(-0.1);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("must be higher than 0");

        // when
        builder.build();

    }

    @Test
    public void builderThrowsWhenResizeFactorIsHigherThanOne() {

        // given
        UnlimitedResizePolicy.Builder builder = UnlimitedResizePolicy.newBuilder();
        builder.withResizeFactor(1.01);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("must be lower or equal 1");

        // when
        builder.build();

    }

    @Test
    public void increaseThrowsWhenResizeWouldNotTakeAnyEffect() {

        // given
        ResizePolicy policy = UnlimitedResizePolicy.newBuilder().withResizeFactor(0.1).build();

        ItemSourcePool pool = mock(ItemSourcePool.class);
        Integer initialPoolSize = 5;
        when(pool.getInitialSize()).thenReturn(initialPoolSize);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("will not resize given pool");

        // when
        policy.increase(pool);

    }

    @Test
    public void increaseIncrementsPoolSizeByResizeFactorMultipliedByInitialPoolSize() {

        // given
        double resizeFactor = 0.2;
        ResizePolicy policy = UnlimitedResizePolicy.newBuilder().withResizeFactor(resizeFactor).build();

        int initialPoolSize = 10;
        ItemSourcePool pool = mock(ItemSourcePool.class);
        when(pool.getInitialSize()).thenReturn(initialPoolSize);

        // when
        boolean resized = policy.increase(pool);

        // then
        assertTrue(resized);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(pool).incrementPoolSize(captor.capture());
        assertEquals((Object)(int)(initialPoolSize * resizeFactor), captor.getValue());

    }

    @Test
    public void decreaseShrinksPoolByTotalSizeMultipliedByResizeFactor() {

        // given
        int initialSize = 50;
        int additionalSize = 50;

        int expectedResizedTotalSize = 80;
        double resizeFactor = 0.2;

        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().withResizeFactor(resizeFactor).build();

        ItemSourcePool pool = spy(BufferedItemSourcePoolTest.createDefaultTestBufferedItemSourcePool(initialSize, true));
        pool.incrementPoolSize(additionalSize);

        // when
        boolean resized = resizePolicy.decrease(pool);

        // then
        assertTrue(resized);

        assertEquals(expectedResizedTotalSize, pool.getTotalSize());
    }

    @Test
    public void decreaseNeverShrinksBelowInitialSize() throws PoolResourceException {

        // given
        int initialSize = 40;
        int additionalSize = 60;
        double resizeFactor = 0.75;
        int expectedResizedTotalSize = initialSize + 5; // 5 will be in use

        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().withResizeFactor(resizeFactor).build();

        ItemSourcePool pool = spy(BufferedItemSourcePoolTest.createDefaultTestBufferedItemSourcePool(initialSize, true));
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
        verify(pool, times(55)).remove();

    }

    @Test
    public void decreaseNeverShrinksWhenResizeWouldBeHigherThanAvailableSize() throws PoolResourceException {

        // given
        int initialSize = 40;
        int additionalSize = 60;
        double resizeFactor = 0.55;
        int expectedResizedTotalSize = initialSize + additionalSize;

        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().withResizeFactor(resizeFactor).build();

        ItemSourcePool pool = spy(BufferedItemSourcePoolTest.createDefaultTestBufferedItemSourcePool(initialSize, true));
        pool.incrementPoolSize(additionalSize);

        // when 50 used
        for (int ii = 0; ii < 50; ii++) {
            pool.getPooled();
        }

        // when
        boolean resized = resizePolicy.decrease(pool);

        // then
        assertFalse(resized);

        // initialSiz
        assertEquals(expectedResizedTotalSize, pool.getTotalSize());

        verify(pool, times(0)).remove();

    }
}
