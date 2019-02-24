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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import static org.appenders.log4j2.elasticsearch.BufferedItemSourceTest.createDefaultTestByteBuf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({ItemSourcePool.class, PooledItemSourceFactory.Builder.class})
@RunWith(PowerMockRunner.class)
public class PooledItemSourceFactoryTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "2");
        System.setProperty("log4j2.disable.jmx", "true");
    }

    private static final int DEFAULT_TEST_POOL_SIZE = 10;
    private static final int DEFAULT_TEST_ITEM_SIZE_IN_BYTES = 512;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static PooledItemSourceFactory.Builder createDefaultTestSourceFactoryConfig() {
        return PooledItemSourceFactory.newBuilder()
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig();

        // when
        PooledItemSourceFactory factory = builder.build();

        // then
        Assert.assertNotNull(factory);

    }

    @Test
    public void builderThrowsOnInitialPoolSizeZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(0);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("initialPoolSize must be higher than 0");
        expectedException.expectMessage(PooledItemSourceFactory.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnInitialPoolSizeLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(-1);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("initialPoolSize must be higher than 0");
        expectedException.expectMessage(PooledItemSourceFactory.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnItemSizeInBytesZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(0);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("itemSizeInBytes must be higher than 0");
        expectedException.expectMessage(PooledItemSourceFactory.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnItemSizeInBytesLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(-1);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("itemSizeInBytes must be higher than 0");
        expectedException.expectMessage(PooledItemSourceFactory.PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void throwsWhenCreateCantGetPooledElement() throws PoolResourceException {
        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class));

    }

    @Test
    public void throwsWhenCreateEmptySourceCantGetPooledElement() throws PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        pooledItemSourceFactory.createEmptySource();

    }
    @Test
    public void isBufferedReturnsTrue() {

        // given
        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mock(ItemSourcePool.class));

        // when
        boolean isBuffered = pooledItemSourceFactory.isBuffered();

        // then
        assertEquals(true, isBuffered);

    }

    @Test
    public void createEmptySourceRemovesFromPool() throws PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);
        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        // when
        pooledItemSourceFactory.createEmptySource();

        // then
        verify(mockedPool).getPooled();

    }

    @Test
    public void createRemovesFromPool() throws PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        ByteBuf byteBuf = createDefaultTestByteBuf();
        ItemSource<ByteBuf> bufferedItemSource = new BufferedItemSource(byteBuf, source -> {});
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        // when
        pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class));

        // then
        verify(mockedPool).getPooled();

    }

    @Test
    public void createWritesItemSource() throws IOException, PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        ByteBuf byteBuf = createDefaultTestByteBuf();
        ItemSource<ByteBuf> bufferedItemSource = new BufferedItemSource(byteBuf, source -> {});
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        // when
        pooledItemSourceFactory.create(logEvent, objectWriter);

        // then
        verify(objectWriter).writeValue(any(DataOutput.class), eq(logEvent));

    }

    @Test
    public void createExceptionReleasesPooledElement() throws IOException, PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        ByteBuf byteBuf = createDefaultTestByteBuf();
        ItemSource<ByteBuf> bufferedItemSource = spy(new BufferedItemSource(byteBuf, source -> {}));
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        doThrow(new IOException("test exception")).when(objectWriter).writeValue(any(DataOutput.class), eq(logEvent));

        ItemSource<ByteBuf> result = null;
        Exception caught = null;

        // when
        try {
            result = pooledItemSourceFactory.create(logEvent, objectWriter);
        } catch (Exception e) {
            caught = e;
        }

        // then
        assertNull(result);
        verify(bufferedItemSource).release();
        assertEquals(IllegalArgumentException.class, caught.getClass());
    }

    @Test
    public void builderConfiguresItemSourcePool() throws Exception {

        // given
        boolean monitored = true;
        long monitorTaskInterval = new Random().nextInt();
        long resizeTimeout = new Random().nextInt(1000) + 100;
        int initialPoolSize = new Random().nextInt(100) + 1;
        int itemSizeInBytes = new Random().nextInt(1024) + 1024;
        String poolName = UUID.randomUUID().toString();
        ResizePolicy resizePolicy = mock(ResizePolicy.class);

        PooledItemSourceFactory.Builder builder = PowerMockito.spy(createDefaultTestSourceFactoryConfig())
                .withMonitored(monitored)
                .withMonitorTaskInterval(monitorTaskInterval)
                .withResizePolicy(resizePolicy)
                .withResizeTimeout(resizeTimeout)
                .withInitialPoolSize(initialPoolSize)
                .withItemSizeInBytes(itemSizeInBytes)
                .withPoolName(poolName);

        BufferedItemSourcePool pool = BufferedItemSourcePoolTest.createDefaultTestBufferedItemSourcePool(DEFAULT_TEST_POOL_SIZE, monitored);
        PowerMockito.whenNew(BufferedItemSourcePool.class).withAnyArguments().thenReturn(pool);

        // when
        builder.build();

        // then
        PowerMockito.verifyNew(BufferedItemSourcePool.class).withArguments(
                eq(poolName),
                any(UnpooledByteBufAllocator.class),
                eq(resizePolicy),
                eq(resizeTimeout),
                eq(monitored),
                eq(monitorTaskInterval),
                eq(initialPoolSize),
                eq(itemSizeInBytes)
        );

    }

    @Test
    public void lifecycleStopShutsDownPoolOnlyOnce() {

        // given
        ItemSourcePool pool = mock(ItemSourcePool.class);
        when(pool.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        PooledItemSourceFactory.Builder sourceFactoryConfig = spy(createDefaultTestSourceFactoryConfig());
        when(sourceFactoryConfig.configuredBufferedItemSourcePool()).thenReturn(pool);

        PooledItemSourceFactory factory = sourceFactoryConfig.build();
        factory.start();

        // when
        factory.stop();
        factory.stop();

        // then
        verify(pool).stop();

    }

    @Test
    public void lifecycleStartStartsPoolOnlyOnce() {

        // given
        ItemSourcePool pool = mock(ItemSourcePool.class);

        PooledItemSourceFactory.Builder sourceFactoryConfig = spy(createDefaultTestSourceFactoryConfig());
        when(sourceFactoryConfig.configuredBufferedItemSourcePool()).thenReturn(pool);

        when(pool.isStarted()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        PooledItemSourceFactory factory = sourceFactoryConfig.build();

        // when
        factory.start();
        factory.start();

        // then
        verify(pool).start();

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
        return createDefaultTestSourceFactoryConfig().build();
    }

}
