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
import io.netty.buffer.TestPooledByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class GenericItemSourcePoolTest {

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
    public void poolShutdownShutsDownExecutor() {

        // given
        final ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);

        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true, mockedExecutor);
        pool.start();

        // when
        pool.shutdown();

        // then
        verify(mockedExecutor).shutdown();

    }

    @Test
    public void monitoredPoolExecutorSchedulesMetricPrinterThread() {

        // given
        final ScheduledExecutorService spiedExecutor = spy(ScheduledExecutorService.class);
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true, spiedExecutor);

        // when
        pool.start();

        // then
        ArgumentCaptor<GenericItemSourcePool.Recycler> runnableCaptor = ArgumentCaptor.forClass(GenericItemSourcePool.Recycler.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(DEFAULT_TEST_MONITOR_TASK_INTERVAL), any(TimeUnit.class));
    }

    @Test
    public void poolShutdownClearsSourceList() {

        // given
        final ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);

        GenericItemSourcePool pool = started(createDefaultTestGenericItemSourcePool(false, mockedExecutor));

        pool.incrementPoolSize();
        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE + 1, pool.getAvailableSize());

        // when
        pool.shutdown();

        // then
        assertEquals(0, pool.getAvailableSize());

    }

    private GenericItemSourcePool started(GenericItemSourcePool managed) {
        managed.start();
        return managed;
    }

    @Test
    public void monitoredPoolExecutorFactoryDoesNotReturnNull() {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true);

        // when
        ScheduledExecutorService executor = pool.createExecutor();

        // then
        assertNotNull(executor);

    }

    @Test
    public void metricsPrinterToStringDelegatesToFormattedMetrics() {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true);
        GenericItemSourcePool.PoolMetrics metrics = spy(pool.new PoolMetrics());

        // when
        metrics.toString();

        // then
        verify(metrics).formattedMetrics(eq(null));

    }

    @Test
    public void metricsPrinterGivenNoAllocatorMetricsContainsPoolStatsOnly() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true);
        pool.start();

        GenericItemSourcePool.PoolMetrics metrics = pool.new PoolMetrics();

        // when
        pool.incrementPoolSize();
        pool.getPooled();
        String formattedMetrics = metrics.formattedMetrics(null);

        // then
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE + 1)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertFalse(formattedMetrics.contains("allocatorMetric"));

    }

    @Test
    public void metricsPrinterContainsPoolStats() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true);
        pool.start();

        GenericItemSourcePool.PoolMetrics metrics = pool.new PoolMetrics();

        TestPooledByteBufAllocatorMetric allocatorMetrics = new TestPooledByteBufAllocatorMetric();

        // when
        pool.incrementPoolSize();
        pool.getPooled();
        String formattedMetrics = metrics.formattedMetrics(allocatorMetrics.getDelegate().toString());

        // then
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE + 1)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("additionalMetrics"));
    }

    @Test
    public void incrementSizeAddsOnePooledElement() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(0,false);

        // when
        pool.incrementPoolSize();

        // then
        assertEquals(1, pool.getAvailableSize());
        ItemSource<ByteBuf> itemSource = pool.getPooled();
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
            itemSource = pool.getPooled();
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
        ItemSource<ByteBuf> itemSource = pool.getPooled();
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
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
            @Override
            public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {
                return spy(super.createItemSource(releaseCallback));
            }
        };
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1
                ));

        pool.start();

        assertTrue(pool.isStarted());
        assertTrue(pool.getAvailableSize() > 0);

        ItemSource<ByteBuf> pooled = pool.getPooled();
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
    public void throwsWhenNoMorePooledElementsAvailableAndResizePolicyDoesNotCopeWithTheLoad() throws PoolResourceException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).thenReturn(true);

        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
        };
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
        final PoolResourceException exception = assertThrows(PoolResourceException.class, pool::getPooled);

        // then
        assertThat(exception.getMessage(), containsString("has to be reconfigured to handle current load"));
        assertThat(exception.getMessage(), containsString(DEFAULT_TEST_ITEM_POOL_NAME));

    }

    @Test
    public void throwsWhenResizePolicyDoesNotResize() throws PoolResourceException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).thenReturn(false);

        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
        };
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
        final PoolResourceException exception = assertThrows(PoolResourceException.class, pool::getPooled);

        // then
        assertThat(exception.getMessage(), containsString("Unable to resize. Creation of ItemSource was unsuccessful"));

    }

    @Test
    public void removeReturnFalseInsteadOfThrowingAfterUnderlyingPoolResourceException ()  {

        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).then((Answer<Boolean>) invocationOnMock -> {
            throw new PoolResourceException("test");
        });

        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
        };
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
        boolean resized = pool.remove();

        // then
        assertFalse(resized);

    }

    @Test
    public void multipleThreadsGetPooledWhenResizePolicyEventuallyCopeWithTheLoad() throws InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);

        int expectedIneffectiveResizes = 30;

        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
        };
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
                    pool.incrementPoolSize(2);
                    return true;
                }
                failedCounter.incrementAndGet();
                return true;
            }
        });

        // when
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(10);
        for (int ii = 0; ii < 10; ii++) {
            new Thread(() -> {
                try {
                    start.await();
                    pool.getPooled();
                } catch (PoolResourceException e) {
                    System.out.println(e.getMessage());
                    Assertions.fail();
                } catch (InterruptedException e) {
                    Assertions.fail();
                } finally {
                    end.countDown();
                }
            }).start();
        }

        Thread.sleep(100);
        start.countDown();
        end.await();

        // then
        assertEquals(expectedIneffectiveResizes, failedCounter.get());

    }

    @Test
    public void throwsWhenMultithreadedWaitForResizeInterrupted() throws InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);

        int resizeTimeout = 1000;
        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
        };
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
                    pool.getPooled();
                } catch (PoolResourceException e) {
                    System.out.println(e.getMessage());
                    Assertions.fail();
                } catch (InterruptedException e) {
                    // noop
                } catch (IllegalStateException e) {
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
        assertEquals(IllegalStateException.class, caught[0].getClass());
        assertEquals("Thread interrupted while waiting for resizing to complete", caught[0].getMessage());

    }

    @Test
    public void lifecycleStartCreatesScheduledExecutor() {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(1, false);

        // when
        pool.start();

        // then
        assertNotNull(pool.executor);

    }

    @Test
    public void lifecycleStartSchedulesRecyclerThread() {

        // given
        final ScheduledExecutorService spiedExecutor = spy(ScheduledExecutorService.class);
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(false, spiedExecutor);

        // when
        pool.start();
        pool.start();

        // then
        ArgumentCaptor<GenericItemSourcePool.Recycler> runnableCaptor = ArgumentCaptor.forClass(GenericItemSourcePool.Recycler.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(10000L), any(TimeUnit.class));
        assertEquals(DEFAULT_TEST_ITEM_POOL_NAME + "-Recycler", runnableCaptor.getValue().getName());

    }

    @Test
    public void lifecycleStartSchedulesMonitorThread() {

        // given
        final ScheduledExecutorService spiedExecutor = spy(ScheduledExecutorService.class);
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true, spiedExecutor);

        // when
        pool.start();
        pool.start();

        // then
        ArgumentCaptor<GenericItemSourcePool.MetricPrinter> runnableCaptor = ArgumentCaptor.forClass(GenericItemSourcePool.MetricPrinter.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(DEFAULT_TEST_MONITOR_TASK_INTERVAL), any(TimeUnit.class));
        assertEquals(DEFAULT_TEST_ITEM_POOL_NAME + "-MetricPrinter", runnableCaptor.getValue().getName());

    }

    @Test
    public void lifecycleStopShutsDownPoolOnlyOnce() {

        // given
        GenericItemSourcePool pool = spy(createDefaultTestGenericItemSourcePool(1, false));

        pool.start();

        // when
        pool.stop();
        pool.stop();

        // then
        verify(pool).shutdown();

    }

    @Test
    public void lifecycleStopStopsReturningBuffersBackOnRelease() throws PoolResourceException {

        // given
        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(1, false);

        pool.start();

        ItemSource itemSource = pool.getPooled();

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

        ItemSource<ByteBuf> itemSource = spy(pool.getPooled());

        ByteBuf byteBuf = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        pool.stop();

        assertEquals(0, pool.getAvailableSize());

        // when
        itemSource.release();

        // then
        verify(byteBuf).release();

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestGenericItemSourcePool(false);
    }

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(boolean isMonitored) {
        return createDefaultTestGenericItemSourcePool(DEFAULT_TEST_INITIAL_POOL_SIZE, isMonitored);
    }

    public GenericItemSourcePool createDefaultTestGenericItemSourcePool(boolean isMonitored, ScheduledExecutorService spiedExecutor) {
        return createDefaultTestGenericItemSourcePool(DEFAULT_TEST_INITIAL_POOL_SIZE, isMonitored,spiedExecutor);
    }

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored) {
        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().build();
        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

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

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored, PooledObjectOps<? extends Object> pooledObjectOps) {
        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().build();
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

    static GenericItemSourcePool<ByteBuf> createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored, ScheduledExecutorService mockedExecutor) {
        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().build();
        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        return new GenericItemSourcePool<ByteBuf>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                monitored,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                initialSize
        ) {
            @Override
            ScheduledExecutorService createExecutor() {
                return mockedExecutor;
            }

            @Override
            ScheduledExecutorService createExecutor(String threadName) {
                return createExecutor();
            }
        };
    }
}
