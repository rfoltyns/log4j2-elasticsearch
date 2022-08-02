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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicyTest.createDefaultTestBoundedSizeLimitPolicy;
import static org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOpsTest.createTestPooledObjectOps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class GenericItemSourcePoolFastPathTest {

    public static final String DEFAULT_TEST_ITEM_POOL_NAME = "testPool";
    public static final int DEFAULT_TEST_INITIAL_POOL_SIZE = 10;
    public static final int DEFAULT_TEST_ITEM_SIZE_IN_BYTES = 1024;
    public static final long DEFAULT_TEST_MONITOR_TASK_INTERVAL = 1000;
    public static final int DEFAULT_TEST_RESIZE_TIMEOUT = 100;

    static {
        System.setProperty("io.netty.allocator.maxOrder", "2");
   }

    public static UnpooledByteBufAllocator byteBufAllocator = new UnpooledByteBufAllocator(false, false, false);

    @Test
    public void incrementSizeAddsOnePooledElement() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(0,false);

        // when
        pool.incrementPoolSize();

        // then
        assertEquals(1, pool.getAvailableSize());
        ItemSource<ByteBuf> itemSource = pool.getPooledOrNull();
        assertNotNull(itemSource);

    }

    @Test
    public void incrementSizeByNumberAddsExactNumberOfPooledElements() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = spy(createDefaultTestGenericItemSourcePool(0,false));

        int expectedNumberOfPooledItemSources = 10;
        pool.incrementPoolSize(expectedNumberOfPooledItemSources);

        ItemSource<ByteBuf> itemSource;

        int remaining = expectedNumberOfPooledItemSources;

        // when
        do {
            itemSource = pool.getPooledOrNull();
            assertEquals(--remaining, pool.getAvailableSize());
        } while (remaining > 0);

        // then
        assertNotNull(itemSource);
        assertEquals(0, remaining);


    }

    @Test
    public void defaultReleaseCallbackReturnsPooledElement() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(false);
        pool.start();

        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE, pool.getAvailableSize());

        pool.incrementPoolSize();
        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE + 1, pool.getAvailableSize());

        // when
        ItemSource<ByteBuf> itemSource = pool.getPooledOrNull();
        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE, pool.getAvailableSize());
        itemSource.release();

        // then
        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE + 1, pool.getAvailableSize());

    }

    @Test
    public void defaultReleaseCallbackDoesntReturnToPoolIfPoolIsStopped() throws PoolResourceException {

        // given
        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                createDefaultTestBoundedSizeLimitPolicy()) {
            @Override
            public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {
                return spy(super.createItemSource(releaseCallback));
            }
        };
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                new UnlimitedResizePolicy.Builder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1
                ));

        pool.start();

        assertTrue(pool.isStarted());
        assertTrue(pool.getAvailableSize() > 0);

        ItemSource<ByteBuf> pooled = pool.getPooledOrNull();
        ByteBuf byteBuf = spy(pooled.getSource());
        when(pooled.getSource()).thenReturn(byteBuf);

        pool.stop();

        // when
        pooled.release();

        // then
        assertEquals(0, pool.getAvailableSize());
        verify(byteBuf).release();
    }

    @Test
    public void throwsWhenNoMorePooledElementsAvailableAndResizePolicyDoesNotCopeWithTheLoad() {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).thenReturn(true);

        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0
        ));

        // when
        final ItemSource result = pool.getPooledOrNull();

        // then
        assertNull(result);

    }

    @Test
    public void throwsWhenResizePolicyDoesNotResize() {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.canResize(any())).thenReturn(true);
        when(resizePolicy.increase(any())).thenReturn(false);

        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0
        ));

        // when
        final ItemSource result = pool.getPooledOrNull();

        // then
        assertNull(result);

    }

    @Test
    public void multipleThreadsGetPooledWhenResizePolicyEventuallyCopeWithTheLoad() throws InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.canResize(any())).thenReturn(true);

        int expectedIneffectiveResizes = 1;

        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                100,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0
        ));

        final AtomicInteger failedCounter = new AtomicInteger();

        when(resizePolicy.increase(eq(pool))).thenAnswer(new Answer<Boolean>() {

            private final AtomicInteger counter = new AtomicInteger(expectedIneffectiveResizes);
            private final Random random = new Random();

            @Override
            public Boolean answer(InvocationOnMock invocation) throws InterruptedException {
                Thread.sleep(random.nextInt(10) + 10);
                if (counter.getAndDecrement() <= 0) {
                    pool.incrementPoolSize(1);
                    return true;
                }
                failedCounter.incrementAndGet();
                return true;
            }
        });

        final AtomicInteger caughtIneffectiveResizes = new AtomicInteger();

        // when
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(10);
        AtomicInteger pooledCounter = new AtomicInteger();
        for (int ii = 0; ii < 10; ii++) {
            new Thread(() -> {
                try {
                    start.await();
                    pool.getPooledOrNull();
                    pooledCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    end.countDown();
                }
            }).start();
        }

        start.countDown();
        end.await();

        // then
        assertEquals(10, pooledCounter.get());
        assertEquals(0, caughtIneffectiveResizes.get());
        assertEquals(expectedIneffectiveResizes, failedCounter.get());

    }

    @Test
    public void escalatedWhenMultithreadedWaitForResizeInterrupted() throws InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.canResize(any())).thenReturn(true);

        int resizeTimeout = 1000;
        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                resizeTimeout,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0
        ));


        when(resizePolicy.increase(eq(pool))).thenAnswer((Answer<Boolean>) invocation -> {
            Thread.sleep(1000);
            return true;
        });

        final Exception[] caught = new Exception[1];

        // when
        List<Thread> threads = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(2);
        for (int ii = 0; ii < 2; ii++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    pool.getPooledOrNull();
                } catch (InterruptedException e) {
                    caught[0] = e;
                }
                finally {
                    end.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        Thread.sleep(500);
        start.countDown();
        Thread.sleep(100);
        threads.forEach(thread -> thread.interrupt());
        end.await();

        // then
        assertEquals(InterruptedException.class, caught[0].getClass());
        assertEquals("sleep interrupted", caught[0].getMessage());

    }

    @Test
    public void lifecycleStopStopsReturningBuffersBackOnRelease() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(1, false);

        pool.start();

        ItemSource itemSource = pool.getPooledOrNull();

        pool.stop();

        assertEquals(0, pool.getAvailableSize());

        // when
        itemSource.release();

        // then
        assertEquals(0, pool.getAvailableSize());

    }

    @Test
    public void lifecycleStopCausesReturnedBuffersRelease() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(1, false);

        pool.start();

        ItemSource<ByteBuf> itemSource = spy(pool.getPooledOrNull());

        ByteBuf byteBuf = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        pool.stop();

        assertEquals(0, pool.getAvailableSize());

        // when
        itemSource.release();

        // then
        verify(byteBuf).release();

    }

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(boolean isMonitored) {
        return createDefaultTestGenericItemSourcePool(DEFAULT_TEST_INITIAL_POOL_SIZE, isMonitored);
    }

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored) {
        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        return new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                monitored,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                initialSize
        );
    }

}
