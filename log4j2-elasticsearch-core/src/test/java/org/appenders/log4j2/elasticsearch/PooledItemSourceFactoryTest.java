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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
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

    @AfterEach
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    public static PooledItemSourceFactory.Builder createDefaultTestSourceFactoryConfig() {
        return PooledItemSourceFactory.newBuilder()
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES)
                .withMaxItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }

    public static PooledItemSourceFactory.Builder createTestItemSourceFactoryBuilder(Function<ItemSourcePool, ItemSourcePool> wrapper) {
        return new PooledItemSourceFactory.Builder() {
            @Override
            ItemSourcePool configuredItemSourcePool() {
                return wrapper.apply(super.configuredItemSourcePool());
            }
        }
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES)
                .withMaxItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig();

        // when
        PooledItemSourceFactory factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void builderThrowsOnInitialPoolSizeZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnInitialPoolSizeLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnItemSizeInBytesZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnItemSizeInBytesLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesLessThanZero() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesLowerThanItemSizeInBytes() {

        // given
        PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(1)
                .withItemSizeInBytes(2);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than or equal to itemSizeInBytes"));
        assertThat(exception.getMessage(), containsString(PooledItemSourceFactory.PLUGIN_NAME));

    }

    @Test
    public void throwsWhenCreateCantGetPooledElement() throws PoolResourceException {
        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class)));

        // then
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    public void throwsWhenCreateEmptySourceCantGetPooledElement() throws PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> pooledItemSourceFactory.createEmptySource());

        // then
        assertThat(exception.getMessage(), containsString(expectedMessage));

    }

    @Test
    public void throwsWhenResizeIsIneffective() {

        // given
        final PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
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

        final PooledItemSourceFactory itemSourceFactory = builder.build();

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, itemSourceFactory::createEmptySource);

        // then
        assertThat(exception.getMessage(), containsString("ResizePolicy is ineffective"));

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

        ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
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

        ItemSource<ByteBuf> bufferedItemSource = createTestItemSource();
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

        LogEvent logEvent = mock(LogEvent.class);
        ObjectWriter objectWriter = spy(new ObjectMapper().writerFor(LogEvent.class));

        // when
        pooledItemSourceFactory.create(logEvent, objectWriter);

        // then
        verify(objectWriter).writeValue(any(OutputStream.class), eq(logEvent));

    }

    @Test
    public void createExceptionReleasesPooledElement() throws IOException, PoolResourceException {

        // given
        ItemSourcePool mockedPool = mock(ItemSourcePool.class);

        ItemSource<ByteBuf> bufferedItemSource = spy(createTestItemSource());
        when(mockedPool.getPooled()).thenReturn(bufferedItemSource);

        PooledItemSourceFactory pooledItemSourceFactory = new PooledItemSourceFactory(mockedPool);

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
    public void printsPoolMetricsIfConfigured() {

        // given
        final Logger logger = mockTestLogger();

        final String expectedPoolName = UUID.randomUUID().toString();

        final PooledItemSourceFactory.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedPoolName)
                .withMonitored(true)
                .withMonitorTaskInterval(1000);

        final String propertyName = "appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay";
        final String previous = System.getProperty(propertyName, "1000");
        System.setProperty(propertyName, "0");

        final PooledItemSourceFactory itemSourceFactory = builder.build();

        // when
        itemSourceFactory.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(1000).atLeastOnce()).info(captor.capture());

        assertThat(captor.getValue(), containsString(expectedPoolName));

        itemSourceFactory.stop();

        System.setProperty(propertyName, previous);

    }


    @Test
    public void lifecycleStopShutsDownPoolOnlyOnce() {

        // given
        ItemSourcePool pool = mock(ItemSourcePool.class);
        when(pool.isStopped()).thenAnswer(LifecycleTestHelper.falseOnlyOnce());

        PooledItemSourceFactory.Builder sourceFactoryConfig = createTestItemSourceFactoryBuilder( defaultPool -> pool);

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

        PooledItemSourceFactory.Builder sourceFactoryConfig = createTestItemSourceFactoryBuilder( defaultPool -> pool);

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
