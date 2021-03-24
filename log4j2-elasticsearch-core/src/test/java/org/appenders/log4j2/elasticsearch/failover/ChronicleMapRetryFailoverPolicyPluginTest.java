package org.appenders.log4j2.elasticsearch.failover;

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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicyTest.ExceptionMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicyTest.DEFAULT_TEST_MONITOR_TASK_INTERVAL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChronicleMapRetryFailoverPolicyPluginTest {

    public static final long DEFAULT_TEST_SEQUENCE_ID = 1;

    private static final Random random = new Random();

    public static final int TEST_NUMBER_OF_ENTRIES = 100;
    public static final int DEFAULT_TEST_RETRY_INTERVAL = random.nextInt(100) + 200;

    @BeforeAll
    public static void globalSetup() {
        InternalLoggingTest.mockTestLogger();
    }

    @AfterAll
    public static void globalTeardown() {
        InternalLogging.setLogger(null);
    }

    @BeforeEach
    public void setup() {
        System.setProperty("appenders.failover.keysequence.consistencyCheckDelay", "10");
    }

    @Test
    public void builderBuildsSuccessfully() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder();

        // when
        ChronicleMapRetryFailoverPolicy policy = builder.build();

        // then
        assertNotNull(policy);

    }

    @Test
    public void builderThrowsIfFileNameIsNull() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withFileName(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("fileName was not provided"));

    }

    @Test
    public void builderThrowsIfAverageValueSizeIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withAverageValueSize(511);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("averageValueSize must be higher than or equal 1024"));

    }

    @Test
    public void builderThrowsIfNumberOfEntriesIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withNumberOfEntries(2);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("numberOfEntries must be higher than 2"));

    }

    @Test
    public void builderThrowsIfBatchSizeIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withBatchSize(0);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("batchSize must be higher than 0"));

    }

    @Test
    public void builderThrowsIfKeySequenceSelectorIsNull() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withKeySequenceSelector(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(KeySequenceSelector.class.getSimpleName() + " was not provided for " +
                ChronicleMapRetryFailoverPolicy.class.getSimpleName()));

    }

    @Test
    public void builderThrowsIfKeySequenceSelectorReturnsNull() throws IOException {

        // given
        KeySequenceSelector keySequenceSelector = mock(KeySequenceSelector.class);
        when(keySequenceSelector.firstAvailable()).thenReturn(null);

        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withKeySequenceSelector(keySequenceSelector);

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Could not initialize " +
                ChronicleMapRetryFailoverPolicy.class.getSimpleName()));
        assertThat(exception.getCause(), new ExceptionMatcher(IllegalStateException.class, "Failed to find a valid key sequence for ChronicleMapRetryFailoverPolicy"));

    }

    @Test
    public void builderConfiguresRetryDelay() throws IOException {

        // given
        long expectedInterval = DEFAULT_TEST_RETRY_INTERVAL;
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withRetryDelay(expectedInterval);

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy(builder.build());
        failoverPolicy.addListener(mock(RetryListener.class));

        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        when(failoverPolicy.createExecutor(any())).thenReturn(executorService);

        // when
        failoverPolicy.start();

        // then
        verify(executorService).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), eq(expectedInterval), any(TimeUnit.class));

    }

    @Test
    public void failoverListenerIsAddedIfInstanceOfRetryListener() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withRetryDelay(DEFAULT_TEST_RETRY_INTERVAL);

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy(builder.build());

        // when
        failoverPolicy.addListener(mock(RetryListener.class));

        // then
        assertEquals(1, failoverPolicy.retryListeners.length);

    }

    @Test
    public void failoverListenerIsNotAddedIfNotInstanceOfRetryListener() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = createDefaultTestFailoverPolicyPluginBuilder()
                .withRetryDelay(DEFAULT_TEST_RETRY_INTERVAL);

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy(builder.build());

        // when
        failoverPolicy.addListener(mock(FailoverListener.class));

        // then
        assertEquals(0, failoverPolicy.retryListeners.length);

    }

    @Test
    public void deliverClaimsNextWriterKey() throws IOException {

        // given
        KeySequence keySequence = mock(KeySequence.class);

        KeySequenceSelector keySequenceSelector = mock(KeySequenceSelector.class);
        when(keySequenceSelector.firstAvailable()).thenReturn(keySequence);
        when(keySequenceSelector.currentKeySequence()).thenReturn(() -> keySequence);

        String fileName = createTempFile().getAbsolutePath();
        ChronicleMapRetryFailoverPolicyPlugin.Builder builder = new ChronicleMapRetryFailoverPolicyPlugin.Builder()
                .withKeySequenceSelector(keySequenceSelector)
                .withFileName(fileName)
                .withNumberOfEntries(TEST_NUMBER_OF_ENTRIES);

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();

        FailedItemSource failedItemSource = mock(FailedItemSource.class);

        // when
        failoverPolicy.deliver(failedItemSource);

        // then
        verify(keySequence).nextWriterKey();

    }

    @Test
    public void lifecycleStart() throws IOException {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() throws IOException {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    @Test
    public void lifecycleStartSchedulesMetricsPrinterIfConfigured() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicyPlugin failoverPolicy = spy(createDefaultTestFailoverPolicyPluginBuilder()
                .withMonitored(true)
                .withMonitorTaskInterval(DEFAULT_TEST_MONITOR_TASK_INTERVAL)
                .build()
        );
        failoverPolicy.addListener((RetryListener)itemSourceDelegate -> false);

        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        when(failoverPolicy.createExecutor(anyString())).thenReturn(executorService);

        // when
        failoverPolicy.start();

        // then
        ArgumentCaptor<ChronicleMapRetryFailoverPolicy.MetricsPrinter> captor = ArgumentCaptor.forClass(
                ChronicleMapRetryFailoverPolicy.MetricsPrinter.class
        );

        verify(executorService).scheduleAtFixedRate(
                captor.capture(),
                anyLong(),
                eq(DEFAULT_TEST_MONITOR_TASK_INTERVAL),
                eq(TimeUnit.MILLISECONDS));

    }

    private LifeCycle createLifeCycleTestObject() throws IOException {

        ChronicleMapRetryFailoverPolicy failoverPolicy = createDefaultTestFailoverPolicyPluginBuilder()
                .withKeySequenceSelector(createDefaultTestKeySequenceSelector())
                .build();

        failoverPolicy.addListener((RetryListener)itemSourceDelegate -> false);
        return failoverPolicy;

    }

    public ChronicleMapRetryFailoverPolicyPlugin.Builder createDefaultTestFailoverPolicyPluginBuilder() throws IOException {
        File tempFile = createTempFile();
        return ChronicleMapRetryFailoverPolicyPlugin.newBuilder()
                .withKeySequenceSelector(createDefaultTestKeySequenceSelector())
                .withFileName(tempFile.getAbsolutePath())
                .withNumberOfEntries(TEST_NUMBER_OF_ENTRIES);
    }

    private KeySequenceSelector createDefaultTestKeySequenceSelector() {
        return new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID);
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("failedItems", "test");
        tempFile.deleteOnExit();
        return tempFile;
    }

}
