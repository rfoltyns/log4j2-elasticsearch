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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BufferedItemSourcePoolTest {

    public static final String DEFAULT_TEST_ITEM_POOL_NAME = "testPool";
    public static final int DEFAULT_TEST_INITIAL_POOL_SIZE = 10;
    public static final int DEFAULT_TEST_ITEM_SIZE_IN_BYTES = 1024;
    public static final long DEFAULT_TEST_MONITOR_TASK_INTERVAL = 1000;
    public static final int DEFAULT_TEST_RESIZE_TIMEOUT = 100;

    static {
        System.setProperty("io.netty.allocator.maxOrder", "2");
    }

    public static UnpooledByteBufAllocator byteBufAllocator = new UnpooledByteBufAllocator(false, false, false);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void poolShutdownShutsDownExecutor() {

        // given
        final ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);

        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true, mockedExecutor);
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
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true, spiedExecutor);

        // when
        pool.start();

        // then
        ArgumentCaptor<BufferedItemSourcePool.Recycler> runnableCaptor = ArgumentCaptor.forClass(BufferedItemSourcePool.Recycler.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(DEFAULT_TEST_MONITOR_TASK_INTERVAL), any(TimeUnit.class));
    }

    @Test
    public void poolShutdownClearsSourceList() {

        // given
        final ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);

        BufferedItemSourcePool pool = started(createDefaultTestBufferedItemSourcePool(false, mockedExecutor));

        pool.incrementPoolSize();
        assertEquals(DEFAULT_TEST_INITIAL_POOL_SIZE + 1, pool.getAvailableSize());

        // when
        pool.shutdown();

        // then
        assertEquals(0, pool.getAvailableSize());

    }

    private BufferedItemSourcePool started(BufferedItemSourcePool managed) {
        managed.start();
        return managed;
    }

    @Test
    public void monitoredPoolExecutorFactoryDoesNotReturnNull() {

        // given
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true);

        // when
        ScheduledExecutorService executor = pool.createExecutor();

        // then
        assertNotNull(executor);

    }

    @Test
    public void metricsPrinterToStringDelegatesToFormattedMetrics() {

        // given
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true);
        BufferedItemSourcePool.PoolMetrics metrics = spy(pool.new PoolMetrics());

        // when
        metrics.toString();

        // then
        verify(metrics).formattedMetrics(eq(null));

    }

    @Test
    public void metricsPrinterGivenNoAllocatorMetricsContainsPoolStatsOnly() throws PoolResourceException {

        // given
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true);
        pool.start();

        BufferedItemSourcePool.PoolMetrics metrics = pool.new PoolMetrics();

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
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true);
        pool.start();

        BufferedItemSourcePool.PoolMetrics metrics = pool.new PoolMetrics();

        TestPooledByteBufAllocatorMetric allocatorMetrics = new TestPooledByteBufAllocatorMetric();

        // when
        pool.incrementPoolSize();
        pool.getPooled();
        String formattedMetrics = metrics.formattedMetrics(allocatorMetrics.getDelegate());

        // then
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE + 1)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("allocatorMetric"));
    }

    @Test
    public void incrementSizeAddsOnePooledElement() throws PoolResourceException {

        // given
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(0,false);

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
        BufferedItemSourcePool pool = spy(createDefaultTestBufferedItemSourcePool(0,false));

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
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(false);
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
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES) {
            @Override
            BufferedItemSource createBufferedItemSource() {
                return spy(super.createBufferedItemSource());
            }
        });

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

        BufferedItemSourcePool pool = new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        expectedException.expect(PoolResourceException.class);
        expectedException.expectMessage("has to be reconfigured to handle current load");
        expectedException.expectMessage(DEFAULT_TEST_ITEM_POOL_NAME);

        // when
        pool.getPooled();

    }

    @Test
    public void throwsWhenResizePolicyDoesNotResize() throws PoolResourceException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).thenReturn(false);

        BufferedItemSourcePool pool = new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        expectedException.expect(PoolResourceException.class);
        expectedException.expectMessage("Unable to resize. Creation of ItemSource was unsuccessful");

        // when
        pool.getPooled();

    }

    @Test
    public void removeReturnFalseInsteadOfThrowingAfterUnderlyingPoolResourceException ()  {

        ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).thenThrow(PoolResourceException.class);

        BufferedItemSourcePool pool = new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

        // when
        boolean resized = pool.remove();

        // then
        assertFalse(resized);

    }

    @Test
    public void multipleThreadsGetPooledWhenResizePolicyEventuallyCopeWithTheLoad() throws PoolResourceException, InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);

        int expectedIneffectiveResizes = 30;

        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                100,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

        final AtomicInteger failedCounter = new AtomicInteger();

        when(resizePolicy.increase(eq(pool))).thenAnswer(new Answer<Boolean>() {

            private AtomicInteger counter = new AtomicInteger(expectedIneffectiveResizes);
            private Random random = new Random();

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
                    Assert.fail();
                } catch (InterruptedException e) {
                    Assert.fail();
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
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                resizeTimeout,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

        when(resizePolicy.increase(eq(pool))).thenAnswer((Answer<Boolean>) invocation -> {
            Thread.currentThread().sleep(1000);
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
                    Assert.fail();
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
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

        // when
        pool.start();

        // then
        assertNotNull(pool.executor);

    }

    @Test
    public void lifecycleStartSchedulesRecyclerThread() {

        // given
        final ScheduledExecutorService spiedExecutor = spy(ScheduledExecutorService.class);
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(false, spiedExecutor);

        // when
        pool.start();
        pool.start();

        // then
        ArgumentCaptor<BufferedItemSourcePool.Recycler> runnableCaptor = ArgumentCaptor.forClass(BufferedItemSourcePool.Recycler.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(10000L), any(TimeUnit.class));
        assertEquals(DEFAULT_TEST_ITEM_POOL_NAME + "-Recycler", runnableCaptor.getValue().getName());

    }

    @Test
    public void lifecycleStartSchedulesMonitorThread() {

        // given
        final ScheduledExecutorService spiedExecutor = spy(ScheduledExecutorService.class);
        BufferedItemSourcePool pool = createDefaultTestBufferedItemSourcePool(true, spiedExecutor);

        // when
        pool.start();
        pool.start();

        // then
        ArgumentCaptor<BufferedItemSourcePool.MetricPrinter> runnableCaptor = ArgumentCaptor.forClass(BufferedItemSourcePool.MetricPrinter.class);
        verify(spiedExecutor).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), eq(DEFAULT_TEST_MONITOR_TASK_INTERVAL), any(TimeUnit.class));
        assertEquals(DEFAULT_TEST_ITEM_POOL_NAME + "-MetricPrinter", runnableCaptor.getValue().getName());

    }

    @Test
    public void lifecycleStopShutsDownPoolOnlyOnce() {

        // given
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

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
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

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
        BufferedItemSourcePool pool = spy(new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES));

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
        return createDefaultTestBufferedItemSourcePool(false);
    }

    public BufferedItemSourcePool createDefaultTestBufferedItemSourcePool(boolean isMonitored) {
        return createDefaultTestBufferedItemSourcePool(DEFAULT_TEST_INITIAL_POOL_SIZE, isMonitored);
    }

    public BufferedItemSourcePool createDefaultTestBufferedItemSourcePool(boolean isMonitored, ScheduledExecutorService spiedExecutor) {
        return createDefaultTestBufferedItemSourcePool(DEFAULT_TEST_INITIAL_POOL_SIZE, isMonitored,spiedExecutor);
    }

    public static BufferedItemSourcePool createDefaultTestBufferedItemSourcePool(int initialSize, boolean monitored) {
        return new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                UnlimitedResizePolicy.newBuilder().build(),
                DEFAULT_TEST_RESIZE_TIMEOUT,
                monitored,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                initialSize,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }

    static BufferedItemSourcePool createDefaultTestBufferedItemSourcePool(int initialSize, boolean monitored, ScheduledExecutorService mockedExecutor) {
        ResizePolicy resizePolicy = UnlimitedResizePolicy.newBuilder().build();
        return new BufferedItemSourcePool(
                DEFAULT_TEST_ITEM_POOL_NAME,
                byteBufAllocator,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                monitored,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                initialSize,
                DEFAULT_TEST_ITEM_SIZE_IN_BYTES
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
