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
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest.createTestItemSource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PooledItemSourceFactoryTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "2");
        System.setProperty("log4j2.disable.jmx", "true");
        System.setProperty("log4j2.configurationFactory", "org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory");
    }

    private static final int DEFAULT_TEST_POOL_SIZE = 10;
    private static final int DEFAULT_TEST_ITEM_SIZE_IN_BYTES = 512;

    public static PooledItemSourceFactory.Builder<Object, ByteBuf> createDefaultTestSourceFactoryConfig() {
        return new PooledItemSourceFactory.Builder<Object, ByteBuf>()
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withPooledObjectOps(new ByteBufPooledObjectOps(
                        UnpooledByteBufAllocator.DEFAULT,
                        new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)))
                .withReuseStreams(false);
    }

    public static PooledItemSourceFactory.Builder<Object, ByteBuf> createTestItemSourceFactoryBuilder(Function<ItemSourcePool<ByteBuf>, ItemSourcePool<ByteBuf>> wrapper) {
        return new PooledItemSourceFactory.Builder<Object, ByteBuf>() {
            @Override
            ItemSourcePool<ByteBuf> configuredItemSourcePool() {
                return wrapper.apply(super.configuredItemSourcePool());
            }
        }
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)));
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig();

        // when
        PooledItemSourceFactory<Object, ByteBuf> factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void builderBuildsSuccessfullyWithDeprecatedSizeLimits() {

        // given
        PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(1)
                .withMaxItemSizeInBytes(2);

        // when
        PooledItemSourceFactory<Object, ByteBuf> factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void builderThrowsOnInitialPoolSizeZero() {

        // given
        PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(0);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnInitialPoolSizeLessThanZero() {

        // given
        PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(-1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnDeprecatedItemSizeInBytesUsedOnTopOfPooledObjectOps() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(ByteBufPooledObjectOpsTest.createTestPooledObjectOps());

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withItemSizeInBytes(0));

        // then
        assertThat(exception.getMessage(), containsString("Cannot use both itemSizeInBytes and pooledObjectOps. Set size limits with pooledObjectOps.sizeLimitPolicy instead"));

    }

    @Test
    public void builderThrowsOnDeprecatedMaxItemSizeInBytesUsedOnTopOfPooledObjectOps() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(ByteBufPooledObjectOpsTest.createTestPooledObjectOps());

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withMaxItemSizeInBytes(0));

        // then
        assertThat(exception.getMessage(), containsString("Cannot use both maxItemSizeInBytes and pooledObjectOps. Set size limits with pooledObjectOps.sizeLimitPolicy instead"));

    }

    @Test
    public void builderThrowsOnPooledObjectOpsUsedOnTopOfDeprecatedItemSizeInBytes() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withPooledObjectOps(ByteBufPooledObjectOpsTest.createTestPooledObjectOps()));

        // then
        assertThat(exception.getMessage(), containsString("Cannot use both [max]itemSizeInBytes and pooledObjectOps. Set size limits pooledObjectOps.sizeLimitPolicy instead"));

    }

    @Test
    public void builderThrowsOnPooledObjectOpsUsedOnTopOfDeprecatedMaxItemSizeInBytes() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withMaxItemSizeInBytes(1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withPooledObjectOps(ByteBufPooledObjectOpsTest.createTestPooledObjectOps()));

        // then
        assertThat(exception.getMessage(), containsString("Cannot use both [max]itemSizeInBytes and pooledObjectOps. Set size limits pooledObjectOps.sizeLimitPolicy instead"));

    }

    @Test
    public void builderThrowsOnDeprecatedItemSizeInBytesZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(0);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnDeprecatedItemSizeInBytesLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(-1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(1)
                .withMaxItemSizeInBytes(0);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnDeprecatedMaxItemSizeInBytesLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withItemSizeInBytes(1)
                .withMaxItemSizeInBytes(-1);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void builderThrowsOnDeprecatedMaxItemSizeInBytesLowerThanItemSizeInBytes() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPooledObjectOps(null)
                .withMaxItemSizeInBytes(1)
                .withItemSizeInBytes(2);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than or equal to itemSizeInBytes"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.class.getSimpleName()));

    }

    @Test
    public void throwsWhenCreateCantGetPooledElement() throws PoolResourceException {
        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class)));

        // then
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    public void throwsWhenCreateEmptySourceCantGetPooledElement() throws PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, pooledItemSourceFactory::createEmptySource);

        // then
        assertThat(exception.getMessage(), containsString(expectedMessage));

    }

    @Test
    public void throwsWhenResizeIsIneffective() {

        // given
        final PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig()
                .withResizePolicy(new ResizePolicy() {
                    @Override
                    public boolean increase(ItemSourcePool itemSourcePool) {
                        return true;
                    }

                    @Override
                    public boolean decrease(ItemSourcePool itemSourcePool) {
                        return false;
                    }
                })
                .withResizeTimeout(0);

        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = builder.build();

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, itemSourceFactory::createEmptySource);

        // then
        assertThat(exception.getMessage(), containsString("ResizePolicy is ineffective"));

    }

    @Test
    public void isBufferedReturnsTrue() {

        // given
        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<Object, ByteBuf>(mock(ItemSourcePool.class));

        // when
        boolean isBuffered = pooledItemSourceFactory.isBuffered();

        // then
        assertEquals(true, isBuffered);

    }

    @Test
    public void createEmptySourceRemovesFromPool() throws PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);
        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        // when
        pooledItemSourceFactory.createEmptySource();

        // then
        verify(mockedPool).getPooled();

    }

    @Test
    public void createRemovesFromPool() throws PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        // when
        pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class));

        // then
        verify(mockedPool).getPooled();

    }

    @Test
    public void deprecatedCreateWithObjectWriterWritesItemSource() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        // when
        pooledItemSourceFactory.create(logEvent, objectWriter);

        // then
        verify(objectWriter).writeValue(any(OutputStream.class), eq(logEvent));

    }

    @Test
    public void deprecatedCreateWithObjectWriterExceptionReleasesPooledElement() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        ItemSource<ByteBuf> bufferedItemSource = spy(createTestItemSource());
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory<LogEvent, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        doThrow(new IOException("test exception")).when(objectWriter).writeValue(any(OutputStream.class), eq(logEvent));

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
    public void deprecatedCreateWithObjectWriterReusesStreamIfConfigured() throws IOException, PoolResourceException {

        // given
        ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool, new ReusableOutputStreamProvider<>());

        LogEvent logEvent1 = mock(LogEvent.class);
        LogEvent logEvent2 = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        // when
        pooledItemSourceFactory.create(logEvent1, objectWriter);
        pooledItemSourceFactory.create(logEvent2, objectWriter);

        // then
        ArgumentCaptor<OutputStream> captor = ArgumentCaptor.forClass(OutputStream.class);
        verify(objectWriter, times(2)).writeValue(captor.capture(), any());

        assertEquals(2, captor.getAllValues().size());
        assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));

    }

    @Test
    public void createWithSerializerWritesItemSource() throws IOException, PoolResourceException {

        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        final PooledItemSourceFactory<LogEvent, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        final LogEvent logEvent = mock(LogEvent.class);
        final ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        final JacksonSerializer<LogEvent> serializer = new JacksonSerializer<>(objectWriter);

        // when
        pooledItemSourceFactory.create(logEvent, serializer);

        // then
        verify(objectWriter).writeValue(any(OutputStream.class), eq(logEvent));

    }

    @Test
    public void createWithSerializerExceptionReleasesPooledElement() throws Exception {

        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final ItemSource<ByteBuf> bufferedItemSource = spy(createTestItemSource());
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        final PooledItemSourceFactory<LogEvent, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<LogEvent, ByteBuf>(mockedPool);

        final LogEvent logEvent = mock(LogEvent.class);
        final ObjectWriter objectWriter = new ObjectMapper().writerFor(LogEvent.class);

        final Serializer<LogEvent> serializer = spy(new JacksonSerializer<>(objectWriter));

        doThrow(new IOException("test exception")).when(serializer).write(any(OutputStream.class), eq(logEvent));

        ItemSource<ByteBuf> result = null;
        Exception caught = null;

        // when
        try {
            result = pooledItemSourceFactory.create(logEvent, serializer);
        } catch (Exception e) {
            caught = e;
        }

        // then
        assertNull(result);
        verify(bufferedItemSource).release();
        assertNotNull(caught);
        assertEquals(IllegalArgumentException.class, caught.getClass());

    }

    @Test
    public void createWithSerializerReusesStreamIfConfigured() throws IOException, PoolResourceException {

        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        final PooledItemSourceFactory<LogEvent, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory.Builder<LogEvent, ByteBuf>() {
            @Override
            ItemSourcePool<ByteBuf> configuredItemSourcePool() {
                return mockedPool;
            }
        }
        .withInitialPoolSize(2)
        .withReuseStreams(true)
        .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)))
        .build();

        final LogEvent logEvent1 = mock(LogEvent.class);
        final LogEvent logEvent2 = mock(LogEvent.class);
        final ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        final JacksonSerializer<LogEvent> serializer = new JacksonSerializer<>(objectWriter);

        // when
        pooledItemSourceFactory.create(logEvent1, serializer);
        pooledItemSourceFactory.create(logEvent2, serializer);

        // then
        ArgumentCaptor<OutputStream> captor = ArgumentCaptor.forClass(OutputStream.class);
        verify(objectWriter, times(2)).writeValue(captor.capture(), any());

        assertEquals(2, captor.getAllValues().size());
        assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));

    }

    @Test
    public void createWithSerializerDoesNotReuseStreamIfNotConfigured() throws IOException, PoolResourceException {

        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        final PooledItemSourceFactory<LogEvent, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory.Builder<LogEvent, ByteBuf>() {
            @Override
            ItemSourcePool<ByteBuf> configuredItemSourcePool() {
                return mockedPool;
            }
        }
        .withInitialPoolSize(2)
        .withReuseStreams(false)
        .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_ITEM_SIZE_IN_BYTES, DEFAULT_TEST_ITEM_SIZE_IN_BYTES)))
        .build();

        final LogEvent logEvent1 = mock(LogEvent.class);
        final LogEvent logEvent2 = mock(LogEvent.class);
        final ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        final JacksonSerializer<LogEvent> serializer = new JacksonSerializer<>(objectWriter);

        // when
        pooledItemSourceFactory.create(logEvent1, serializer);
        pooledItemSourceFactory.create(logEvent2, serializer);

        // then
        ArgumentCaptor<OutputStream> captor = ArgumentCaptor.forClass(OutputStream.class);
        verify(objectWriter, times(2)).writeValue(captor.capture(), any());

        assertEquals(2, captor.getAllValues().size());
        assertNotSame(captor.getAllValues().get(0), captor.getAllValues().get(1));

    }

    @Test
    public void printsPoolMetricsIfConfigured() {

        // given
        final Logger logger = mockTestLogger();

        System.setProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay", "0");

        final String expectedPoolName = UUID.randomUUID().toString();

        final PooledItemSourceFactory.Builder<Object, ByteBuf> builder = createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedPoolName)
                .withMonitored(true)
                .withMonitorTaskInterval(100);

        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = builder.build();

        // when
        itemSourceFactory.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(1000).atLeastOnce()).info(captor.capture());

        assertThat(captor.getValue(), containsString(expectedPoolName));

        itemSourceFactory.stop();

        InternalLogging.setLogger(null);

    }


    @Test
    public void lifecycleStopShutsDownPoolOnlyOnce() {

        // given
        ItemSourcePool<ByteBuf> pool = mock(ItemSourcePool.class);
        when(pool.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        PooledItemSourceFactory.Builder<Object, ByteBuf> sourceFactoryConfig = createTestItemSourceFactoryBuilder( defaultPool -> pool);

        PooledItemSourceFactory<Object, ByteBuf> factory = sourceFactoryConfig.build();
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
        ItemSourcePool<ByteBuf> pool = mock(ItemSourcePool.class);

        PooledItemSourceFactory.Builder<Object, ByteBuf> sourceFactoryConfig = createTestItemSourceFactoryBuilder( defaultPool -> pool);

        when(pool.isStarted()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        PooledItemSourceFactory<Object, ByteBuf> factory = sourceFactoryConfig.build();

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

    public static class Introspector {

        public int getInitialSize(final PooledItemSourceFactory<Object, ByteBuf> factory) {
            return factory.bufferedItemSourcePool.getInitialSize();
        }

        public int getTotalSize(final PooledItemSourceFactory<Object, ByteBuf> factory) {
            return factory.bufferedItemSourcePool.getTotalSize();
        }

        public int getAvailablelSize(final PooledItemSourceFactory<Object, ByteBuf> factory) {
            return factory.bufferedItemSourcePool.getAvailableSize();
        }

    }
}
