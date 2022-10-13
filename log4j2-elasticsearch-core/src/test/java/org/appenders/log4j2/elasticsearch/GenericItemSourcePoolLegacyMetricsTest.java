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

import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOpsTest.createTestPooledObjectOps;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class GenericItemSourcePoolLegacyMetricsTest {

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
    public void metricsPrinterGivenNoAllocatorMetricsContainsPoolStatsOnly() throws InterruptedException {

        // given
        System.setProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay", "0");
        final Logger logger = mockTestLogger();

        final ResizePolicy resizePolicy = new UnlimitedResizePolicy.Builder().build();
        ByteBufPooledObjectOps pooledObjectOps = new ByteBufPooledObjectOps(
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

        final GenericItemSourcePool.MetricPrinter metricPrinter = pool.new MetricPrinter(
                DEFAULT_TEST_ITEM_POOL_NAME,
                pool.new PoolMetrics(DEFAULT_TEST_ITEM_POOL_NAME, new DefaultMetricsFactory(Collections.emptyList())), () -> null);

        // when
        metricPrinter.run();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger).info(captor.capture());

        final String formattedMetrics = captor.getValue();
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize"));
        assertTrue(formattedMetrics.contains("totalPoolSize"));
        assertTrue(formattedMetrics.contains("availablePoolSize"));
        assertFalse(formattedMetrics.contains("additionalMetrics"));

        reset(logger);
        InternalLogging.setLogger(null);

    }

    @Test
    public void metricsPrinterContainsPoolStats() {

        // given
        System.setProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay", "0");
        final Logger logger = mockTestLogger();

        GenericItemSourcePool pool = createDefaultTestGenericItemSourcePool(true);
        pool.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(500)).info(captor.capture());

        final String formattedMetrics = captor.getValue();

        // then
        assertTrue(formattedMetrics.contains("poolName: " + DEFAULT_TEST_ITEM_POOL_NAME));
        assertTrue(formattedMetrics.contains("initialPoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("totalPoolSize: " + (DEFAULT_TEST_INITIAL_POOL_SIZE)));
        assertTrue(formattedMetrics.contains("availablePoolSize: " + DEFAULT_TEST_INITIAL_POOL_SIZE));
        assertTrue(formattedMetrics.contains("additionalMetrics"));

        reset(logger);
        InternalLogging.setLogger(null);

        System.clearProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay");

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
