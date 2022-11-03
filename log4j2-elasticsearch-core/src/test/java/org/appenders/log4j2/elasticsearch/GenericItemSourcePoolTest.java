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
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutputTest;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicyTest.createDefaultTestBoundedSizeLimitPolicy;
import static org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOpsTest.createTestPooledObjectOps;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
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
    public void metricsPrinterContainsPoolStats() throws PoolResourceException {

        // given
        System.setProperty("appenders.GenericItemSourcePool.metrics.start.delay", "0");

        final Logger logger = mockTestLogger();
        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        final TestPooledByteBufAllocatorMetric allocatorMetrics = new TestPooledByteBufAllocatorMetric();
        final ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)) {
            @Override
            public Supplier<String> createMetricsSupplier() {
                return () -> allocatorMetrics.getDelegate().toString();
            }
        };
        final GenericItemSourcePool pool = new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                true,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                DEFAULT_TEST_INITIAL_POOL_SIZE
        );

        // when
        pool.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(1000)).info(captor.capture());

        final String formattedMetrics = captor.getValue();
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("additionalMetrics"));

        reset(logger);
        InternalLogging.setLogger(null);

        System.clearProperty("appenders.GenericItemSourcePool.metrics.start.delay");

    }

    @Test
    public void metricsPrinterGivenNoAllocatorMetricsContainsPoolStatsOnly() throws PoolResourceException {

        // given
        System.setProperty("appenders.GenericItemSourcePool.metrics.start.delay", "0");

        final Logger logger = mockTestLogger();
        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        final ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)) {
            @Override
            public Supplier<String> createMetricsSupplier() {
                return () -> null;
            }
        };
        final GenericItemSourcePool pool = new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_RESIZE_TIMEOUT,
                true,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                DEFAULT_TEST_INITIAL_POOL_SIZE
        );

        // when
        pool.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(500)).info(captor.capture());

        final String formattedMetrics = captor.getValue();
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertFalse(formattedMetrics.contains("allocatorMetric"));

        reset(logger);
        InternalLogging.setLogger(null);

        System.clearProperty("appenders.GenericItemSourcePool.metrics.start.delay");

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
        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "initial", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "available", "count");
        final Metric.Key expectedKey3 = new Metric.Key(expectedComponentName, "total", "count");
        final Metric.Key expectedKey4 = new Metric.Key(expectedComponentName, "noSuchElementCaught", "count");
        final Metric.Key expectedKey5 = new Metric.Key(expectedComponentName, "resizeAttempts", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        final ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
                byteBufAllocator,
                createDefaultTestBoundedSizeLimitPolicy()) {
            @Override
            public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {
                return spy(super.createItemSource(releaseCallback));
            }
        };
        final GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                expectedComponentName,
                pooledObjectOps,
                new UnlimitedResizePolicy.Builder().withResizeFactor(1).build(),
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                2,
                new DefaultMetricsFactory(GenericItemSourcePool.metricConfigs(true))));

        pool.start();
        pool.register(registry);

        assertTrue(pool.isStarted());
        assertTrue(pool.getAvailableSize() > 0);

        metricProcessor.process();
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(2L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(2L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(2L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        reset(metricOutput);

        final ItemSource<ByteBuf> pooled = pool.getPooled();
        final ByteBuf byteBuf = spy(pooled.getSource());
        when(pooled.getSource()).thenReturn(byteBuf);

        metricProcessor.process();
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq(2L));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq(1L));
        verify(metricOutput).write(anyLong(), eq(expectedKey3), eq(2L));
        verify(metricOutput).write(anyLong(), eq(expectedKey4), eq(0L));
        verify(metricOutput).write(anyLong(), eq(expectedKey5), eq(0L));
        reset(metricOutput);

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
        final PoolResourceException exception = assertThrows(PoolResourceException.class, pool::getPooled);

        // then
        assertThat(exception.getMessage(), containsString("has to be reconfigured to handle current load"));
        assertThat(exception.getMessage(), containsString(DEFAULT_TEST_ITEM_POOL_NAME));

    }

    @Test
    public void removeDoesNotRethrowPoolResourceExceptions()  {

        // given
        final ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.increase(any())).then((Answer<Boolean>) invocationOnMock -> {
            throw new PoolResourceException("test");
        });

        final ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        final GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                0,
                false,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                0
        ));

        // when
        final boolean resized = pool.remove();

        // then
        assertFalse(resized);
        verify(resizePolicy).increase(any());

    }

    @Test
    public void getPooledTriesToResizeUpToConfiguredRecursionDepth() throws PoolResourceException {

        // given
        final int expectedRetries = new Random().nextInt(10) + 10;
        final AtomicInteger retriesLeft = new AtomicInteger(expectedRetries);
        final AtomicInteger totalRetries = new AtomicInteger();

        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key expectedKey1 = new Metric.Key(expectedComponentName, "noSuchElementCaught", "count");
        final Metric.Key expectedKey2 = new Metric.Key(expectedComponentName, "resizeAttempts", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        System.setProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".resize.retries", "" + expectedRetries);

        final ResizePolicy resizePolicy = mock(ResizePolicy.class);
        final ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        final MetricsFactory metricsFactory = new DefaultMetricsFactory(GenericItemSourcePool.metricConfigs(true));
        final GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                expectedComponentName,
                pooledObjectOps,
                resizePolicy,
                100,
                0,
                metricsFactory
        ));

        when(resizePolicy.increase(any())).then((Answer<Boolean>) invocationOnMock -> {

            totalRetries.incrementAndGet();

            if (retriesLeft.decrementAndGet() == 0) {
                pool.incrementPoolSize();
            }
            return true;
        });

        // when
        pool.register(registry);
        pool.getPooled();
        metricProcessor.process();

        // then
        assertEquals(expectedRetries, totalRetries.get());
        verify(metricOutput).write(anyLong(), eq(expectedKey1), eq((long) expectedRetries - 1));
        verify(metricOutput).write(anyLong(), eq(expectedKey2), eq((long) expectedRetries));

    }

    @Test
    public void multipleThreadsGetPooledWhenResizePolicyEventuallyCopeWithTheLoad() throws InterruptedException {

        // given
        final ResizePolicy resizePolicy = mock(ResizePolicy.class);
        when(resizePolicy.canResize(any())).thenReturn(true);

        final int expectedResizeCount = 5;

        final String expectedComponentName = UUID.randomUUID().toString();
        final Metric.Key resizeAttemptsKey = new Metric.Key(expectedComponentName, "resizeAttempts", "count");

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final MetricsProcessor metricProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

        final ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        final GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                expectedComponentName,
                pooledObjectOps,
                resizePolicy,
                DEFAULT_TEST_MONITOR_TASK_INTERVAL,
                1,
                new DefaultMetricsFactory(GenericItemSourcePool.metricConfigs(true))
        ));

        final AtomicInteger noResizeCount = new AtomicInteger(expectedResizeCount);
        final AtomicInteger passThroughResizeCount = new AtomicInteger();

        when(resizePolicy.increase(eq(pool))).thenAnswer(new Answer<Boolean>() {

            private final Random random = new Random();

            @Override
            public Boolean answer(InvocationOnMock invocation) throws InterruptedException {

                if (noResizeCount.decrementAndGet() == 0) {
                    pool.incrementPoolSize(10);
                    return true;
                }

                Thread.sleep(random.nextInt(10) + 10);
                passThroughResizeCount.incrementAndGet();

                return true;
            }
        });

        final AtomicInteger caughtIneffectiveResizes = new AtomicInteger();

        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(10);
        final AtomicInteger pooledCounter = new AtomicInteger();
        for (int ii = 0; ii < 10; ii++) {
            new Thread(() -> {
                try {
                    start.await();
                    pool.getPooled();
                    pooledCounter.incrementAndGet();
                } catch (PoolResourceException e) {
                    caughtIneffectiveResizes.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    end.countDown();
                }
            }).start();
        }

        pool.register(registry);

        // when
        start.countDown();
        end.await();
        metricProcessor.process();

        // then
        assertEquals(10, pooledCounter.get());
        assertEquals(0, caughtIneffectiveResizes.get());
        assertEquals(expectedResizeCount - 1, passThroughResizeCount.get());

        verify(metricOutput).write(anyLong(), eq(resizeAttemptsKey), eq((long) expectedResizeCount));

    }

    @Test
    public void throwsWhenConcurrentWaitForResizeInterrupted() throws InterruptedException {

        // given
        ResizePolicy resizePolicy = mock(ResizePolicy.class);

        int resizeTimeout = 1000;
        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
        GenericItemSourcePool<ByteBuf> pool = spy(new GenericItemSourcePool<>(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pooledObjectOps,
                resizePolicy,
                resizeTimeout,
                0,
                new DefaultMetricsFactory(GenericItemSourcePool.metricConfigs(true))
        ));

        when(resizePolicy.increase(eq(pool))).thenAnswer((Answer<Boolean>) invocation -> {
            Thread.sleep(1000);
            return true;
        });

        final MetricsRegistry registry = new BasicMetricsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);
        pool.register(registry);
        final MetricsProcessor metricsProcessor = new MetricsProcessor(registry, new BasicMetricOutputsRegistry(metricOutput));

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

        metricsProcessor.process();

        // then
        assertEquals(IllegalStateException.class, caught[0].getClass());
        assertEquals("Thread interrupted while waiting for resizing to complete", caught[0].getMessage());

        verify(metricOutput).write(anyLong(), eq(new Metric.Key(DEFAULT_TEST_ITEM_POOL_NAME, "resizeAttempts", "count")), eq(1L));

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
    public void lifecycleStopDeregistersOnlyOnce() {

        // given
        final MetricsRegistry registry = spy(new BasicMetricsRegistry());

        final GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(1, false);

        pool.start();
        pool.register(registry);

        assertEquals(5, registry.getMetrics(m -> true).size());

        // when
        pool.stop();
        pool.stop();

        // then
        verify(registry, times(5)).deregister(any());

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

    public static GenericItemSourcePool createDefaultTestGenericItemSourcePool(int initialSize, boolean monitored, PooledObjectOps<? extends Object> pooledObjectOps) {
        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
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
        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        ByteBufPooledObjectOps pooledObjectOps = createTestPooledObjectOps(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);

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
