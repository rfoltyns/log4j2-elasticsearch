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
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteBufItemSourceFactoryPluginTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "2");
        System.setProperty("log4j2.disable.jmx", "true");
        System.setProperty("log4j2.configurationFactory", "org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory");
    }

    private static final int DEFAULT_TEST_POOL_SIZE = 10;
    private static final int DEFAULT_TEST_ITEM_SIZE_IN_BYTES = 512;

    public static ByteBufItemSourceFactoryPlugin.Builder createDefaultTestSourceFactoryConfig() {
        return ByteBufItemSourceFactoryPlugin.newBuilder()
                .withInitialPoolSize(DEFAULT_TEST_POOL_SIZE)
                .withItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES)
                .withMaxItemSizeInBytes(DEFAULT_TEST_ITEM_SIZE_IN_BYTES);
    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig();

        // when
        final PooledItemSourceFactory<Object, ByteBuf> factory = builder.build();

        // then
        assertNotNull(factory);

    }

    @Test
    public void builderThrowsOnInitialPoolSizeZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));

    }

    @Test
    public void builderThrowsOnInitialPoolSizeLessThanZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withInitialPoolSize(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("initialPoolSize must be higher than 0"));

    }

    @Test
    public void builderThrowsOnItemSizeInBytesZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));

    }

    @Test
    public void builderThrowsOnItemSizeInBytesLessThanZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withItemSizeInBytes(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("itemSizeInBytes must be higher than 0"));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesLessThanZero() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than 0"));

    }

    @Test
    public void builderThrowsOnMaxItemSizeInBytesLowerThanItemSizeInBytes() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withMaxItemSizeInBytes(1)
                .withItemSizeInBytes(2);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("maxItemSizeInBytes must be higher than or equal to itemSizeInBytes"));

    }

    @Test
    public void throwsWhenCreateCantGetPooledElement() throws PoolResourceException {
        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        final PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = new PooledItemSourceFactory<>(mockedPool);

        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pooledItemSourceFactory.create(mock(LogEvent.class), new ObjectMapper().writerFor(LogEvent.class)));

        // when
        assertThat(exception.getMessage(), containsString(expectedMessage));

    }

    @Test
    public void throwsWhenCreateEmptySourceCantGetPooledElement() throws PoolResourceException {

        // given
        final ItemSourcePool<ByteBuf> mockedPool = mock(ItemSourcePool.class);

        final String expectedMessage = UUID.randomUUID().toString();
        when(mockedPool.getPooled()).thenThrow(new PoolResourceException(expectedMessage));

        final ByteBufItemSourceFactoryPlugin pooledItemSourceFactory = new ByteBufItemSourceFactoryPlugin(mockedPool);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, pooledItemSourceFactory::createEmptySource);

        // when
        assertThat(exception.getMessage(), containsString(expectedMessage));

    }
    @Test
    public void throwsWhenResizeIsIneffective() {

        // given
        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
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
    public void printsPoolMetricsIfConfigured() {

        // given
        final Logger logger = mockTestLogger();

        System.setProperty("appenders." + GenericItemSourcePool.class.getSimpleName() + ".metrics.start.delay", "0");

        final String expectedPoolName = UUID.randomUUID().toString();

        final ByteBufItemSourceFactoryPlugin.Builder builder = createDefaultTestSourceFactoryConfig()
                .withPoolName(expectedPoolName)
                .withMonitored(true)
                .withMonitorTaskInterval(100);

        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = builder.build();

        // when
        itemSourceFactory.start();

        // then
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, timeout(500).atLeastOnce()).info(captor.capture());

        assertThat(captor.getValue(), containsString(expectedPoolName));

        itemSourceFactory.stop();
        InternalLogging.setLogger(null);

    }

    @Test
    public void lifecycleStart() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

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
