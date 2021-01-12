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

import net.openhft.chronicle.hash.ChronicleHashCorruption;
import net.openhft.chronicle.map.ChronicleMap;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.InternalLoggingTest;
import org.appenders.log4j2.elasticsearch.DelayedShutdown;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.appenders.log4j2.elasticsearch.failover.UUIDSequenceTest.createDefaultTestKeySequence;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChronicleMapRetryFailoverPolicyTest {

    public static final long DEFAULT_TEST_SEQUENCE_ID = 1;

    private static final Random random = new Random();

    public static final int TEST_NUMBER_OF_ENTRIES = 100;
    public static final long DEFAULT_TEST_MONITOR_TASK_INTERVAL = random.nextInt(100) + 100;
    public static final int DEFAULT_TEST_RETRY_INTERVAL = random.nextInt(100) + 200;

    @BeforeClass
    public static void globalSetup() {
        InternalLoggingTest.mockTestLogger();
    }

    @AfterClass
    public static void globalTeardown() {
        InternalLogging.setLogger(null);
    }

    @Before
    public void setup() {
        System.setProperty("appenders.failover.keysequence.consistencyCheckDelay", "10");
    }

    @Test
    public void builderBuildsSuccessfully() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder();

        // when
        ChronicleMapRetryFailoverPolicy policy = builder.build();

        // then
        assertNotNull(policy);

    }

    @Test
    public void builderThrowsIfFileNameIsNull() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
                .withFileName(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("fileName was not provided"));

    }

    @Test
    public void builderThrowsIfAverageValueSizeIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
                .withAverageValueSize(511);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("averageValueSize must be higher than or equal 1024"));

    }

    @Test
    public void builderThrowsIfNumberOfEntriesIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
                .withNumberOfEntries(2);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("numberOfEntries must be higher than 2"));

    }

    @Test
    public void builderThrowsIfBatchSizeIsTooLow() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
                .withBatchSize(0);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("batchSize must be higher than 0"));

    }

    @Test
    public void builderThrowsOnMapInitializationIOException() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = spy(createDefaultTestFailoverPolicyBuilder());
        IOException testException = spy(new IOException("test exception"));
        doThrow(testException).when(builder).createChronicleMap();

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Could not initialize"));
        assertThat(exception.getCause(), new ExceptionCauseMatcher(testException));

    }

    @Test
    public void builderThrowsIfKeySequenceSelectorIsNull() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
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

        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
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
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
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
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
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
        ChronicleMapRetryFailoverPolicy.Builder builder = createDefaultTestFailoverPolicyBuilder()
                .withRetryDelay(DEFAULT_TEST_RETRY_INTERVAL);

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy(builder.build());

        // when
        failoverPolicy.addListener(mock(FailoverListener.class));

        // then
        assertEquals(0, failoverPolicy.retryListeners.length);

    }

    @Test
    public void defaultHashCorruptionListenerExtractsExceptionIfAvailable() throws IOException {

        // given
        ChronicleHashCorruption corruption = mock(ChronicleHashCorruption.class);
        when(corruption.exception()).thenReturn(mock(IOException.class));

        ChronicleMapRetryFailoverPolicy.HashCorruptionListener listener = createDefaultTestFailoverPolicyBuilder()
                .createCorruptionListener();

        // when
        listener.onCorruption(corruption);

        // then
        verify(corruption, times(2)).exception();
        verify(corruption, times(1)).message();

    }

    @Test
    public void defaultHashCorruptionListenerDoesNotExtractExceptionIfNotAvailable() throws IOException {

        // given
        ChronicleHashCorruption corruption = mock(ChronicleHashCorruption.class);

        ChronicleMapRetryFailoverPolicy.HashCorruptionListener listener = createDefaultTestFailoverPolicyBuilder()
                .createCorruptionListener();

        // when
        listener.onCorruption(corruption);

        // then
        verify(corruption, times(1)).exception();
        verify(corruption, times(1)).message();

    }

    @Test
    public void deliverClaimsNextWriterKey() throws IOException {

        // given
        KeySequence keySequence = mock(KeySequence.class);

        KeySequenceSelector keySequenceSelector = mock(KeySequenceSelector.class);
        when(keySequenceSelector.firstAvailable()).thenReturn(keySequence);
        when(keySequenceSelector.currentKeySequence()).thenReturn(() -> keySequence);

        String fileName = createTempFile().getAbsolutePath();
        ChronicleMapRetryFailoverPolicy.Builder builder = new ChronicleMapRetryFailoverPolicy.Builder()
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
    public void failedItemIsStoredIfBothKeyAndFailedItemAreNotNull() throws IOException {

        // given
        String fileName = createTempFile().getAbsolutePath();

        ChronicleMap<CharSequence, ItemSource> failedItems = createDefaultTestChronicleMap();

        ChronicleMapRetryFailoverPolicy.Builder builder =
                createDefaultTestFailoverPolicyBuilder(fileName, failedItems);

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();

        ItemSource<Object> failedItemSource = mock(FailedItemSource.class);
        String expectedKey = UUID.randomUUID().toString();

        // when
        boolean result = failoverPolicy.tryPut(expectedKey, failedItemSource);

        // then
        assertTrue(result);
        verify(failedItems).put(eq(expectedKey), eq(failedItemSource));

    }

    @Test
    public void failedItemIsNotStoredIfFailedItemIsNull() throws IOException {

        // given
        String fileName = createTempFile().getAbsolutePath();

        ChronicleMap<CharSequence, ItemSource> failedItems = createDefaultTestChronicleMap();

        ChronicleMapRetryFailoverPolicy.Builder builder =
                createDefaultTestFailoverPolicyBuilder(fileName, failedItems);

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();

        CharSequence unexpectedKey = UUID.randomUUID().toString();

        // when
        boolean result = failoverPolicy.tryPut(unexpectedKey, null);

        // then
        assertFalse(result);
        verify(failedItems, times(0)).put(eq(unexpectedKey), any());
    }

    @Test
    public void exceptionCountIsIncrementedOnStorageFailure() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy failoverPolicy = createDefaultTestFailoverPolicyBuilder().build();

        ItemSource<Object> failedItemSource = mock(FailedItemSource.class);

        // when
        boolean result = failoverPolicy.tryPut(null, failedItemSource);

        // then
        assertFalse(result);
        assertEquals(1, failoverPolicy.storeFailureCount.get());

    }

    @Test
    public void metricPrinterUsesMapSize() throws IOException {

        // given

        ChronicleMap<CharSequence, ItemSource> failedItems = createDefaultTestChronicleMap();

        SingleKeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(1)
                .withRepository(new KeySequenceConfigRepository(failedItems));
        ItemSource itemSource = mock(FailedItemSource.class);
        RetryProcessorTest.fillMap(failedItems, 1, keySequenceSelector, () -> itemSource);

        ChronicleMapRetryFailoverPolicy.Builder builder =
                createDefaultTestFailoverPolicyBuilder(createTempFile().getAbsolutePath(), failedItems)
                .withKeySequenceSelector(keySequenceSelector);

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();

        ChronicleMapRetryFailoverPolicy.MetricsPrinter metricsPrinter = failoverPolicy.new MetricsPrinter();

        verify(failedItems, never()).size();

        // when
        metricsPrinter.run();

        // then
        verify(failedItems).size();

    }

    @Test
    public void lifecycleStartFailsIfNoListenersConfigured() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy failoverPolicy = createDefaultTestFailoverPolicyBuilder().build();

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, failoverPolicy::start);

        // then
        assertThat(exception.getMessage(), containsString(RetryListener.class.getSimpleName()
                + " was not provided for "
                + ChronicleMapRetryFailoverPolicy.class.getSimpleName()
        ));

    }

    @Test
    public void lifecycleStartSchedulesMetricsPrinterIfConfigured() throws IOException {

        // given
        ChronicleMapRetryFailoverPolicy failoverPolicy = spy(new ChronicleMapRetryFailoverPolicy(
                createDefaultTestFailoverPolicyBuilder()
                        .withMonitored(true)
                        .withMonitorTaskInterval(DEFAULT_TEST_MONITOR_TASK_INTERVAL)
        ));
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
    public void lifecycleStartStartsFailoverPolicyOnlyOnce() throws IOException {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy((ChronicleMapRetryFailoverPolicy)lifeCycle);

        DelayedShutdown delayedShutdown = spy(new DelayedShutdown(() -> {}));
        when(failoverPolicy.delayedShutdown()).thenReturn(delayedShutdown);

        // when
        failoverPolicy.start();
        failoverPolicy.start();

        // then
        verify(failoverPolicy).delayedShutdown();

    }

    @Test
    public void lifecycleStopStopsFailoverPolicyOnlyOnce() throws IOException {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy((ChronicleMapRetryFailoverPolicy)lifeCycle);

        DelayedShutdown delayedShutdown = spy(new DelayedShutdown(() -> {}));
        when(failoverPolicy.delayedShutdown()).thenReturn(delayedShutdown);

        failoverPolicy.start();
        assertTrue(failoverPolicy.isStarted());

        // when
        failoverPolicy.stop();
        failoverPolicy.stop();

        // then
        verify(failoverPolicy).stop(anyLong(), anyBoolean());

    }

    @Test
    public void lifecycleStopWithTimeoutStopsFailoverPolicyOnlyOnce() throws IOException {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertFalse(lifeCycle.isStarted());

        ChronicleMapRetryFailoverPolicy failoverPolicy = spy((ChronicleMapRetryFailoverPolicy)lifeCycle);

        InvocationCounter counter = new InvocationCounter();
        DelayedShutdown delayedShutdown = spy(new DelayedShutdown(counter::increment));
        // Design is a bit dodgy, Mockito doesn't like it
        // InvocationCount helps to prove that it's still ok
        when(failoverPolicy.delayedShutdown()).thenReturn(delayedShutdown);

        failoverPolicy.start();
        assertTrue(failoverPolicy.isStarted());

        // when
        failoverPolicy.stop(0, false);
        failoverPolicy.stop(0, false);

        // then
        assertEquals(1, counter.count.get());

    }

    @Test
    public void lifecycleStopWithTimeoutClosesGivenMap() throws IOException {

        // given
        String fileName = createTempFile().getAbsolutePath();

        ChronicleMap<CharSequence, ItemSource> failedItems = createDefaultTestChronicleMap();

        ChronicleMapRetryFailoverPolicy.Builder builder =
                createDefaultTestFailoverPolicyBuilder(fileName, failedItems)
                .withKeySequenceSelector(createDummyKeySequenceSelector());

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();
        failoverPolicy.addListener((RetryListener)failedItemSource -> true);

        failoverPolicy.start();
        assertTrue(failoverPolicy.isStarted());

        // when
        failoverPolicy.stop(DelayedShutdown.DEFAULT_DECREMENT_IN_MILLIS, false);

        // then
        verify(failedItems).close();

    }

    @Test
    public void lifecycleStopWithTimeoutClosesGivenKeySequenceSelector() throws IOException {

        // given
        String fileName = createTempFile().getAbsolutePath();

        ChronicleMap<CharSequence, ItemSource> failedItems = createDefaultTestChronicleMap();

        KeySequenceSelector keySequenceSelector = spy(createDummyKeySequenceSelector());
        ChronicleMapRetryFailoverPolicy.Builder builder =
                createDefaultTestFailoverPolicyBuilder(fileName, failedItems)
                        .withKeySequenceSelector(keySequenceSelector);

        ChronicleMapRetryFailoverPolicy failoverPolicy = builder.build();
        failoverPolicy.addListener((RetryListener)failedItemSource -> true);

        failoverPolicy.start();
        assertTrue(failoverPolicy.isStarted());

        // when
        failoverPolicy.stop(100, false);

        // then
        verify(keySequenceSelector).close();

    }

    public KeySequenceSelector createDummyKeySequenceSelector() {
        return new KeySequenceSelector() {

            KeySequence keySequence = createDefaultTestKeySequence();

            @Override
            public Supplier<KeySequence> currentKeySequence() {
                return () -> keySequence;
            }

            @Override
            public KeySequence firstAvailable() {
                return keySequence;
            }

            @Override
            public KeySequenceSelector withRepository(KeySequenceConfigRepository keySequenceConfigRepository) {
                return null;
            }

            @Override
            public void close() {
                // noop
            }
        };
    }

    @NotNull
    public KeySequenceSelector createMockKeySequenceSelector() {
        KeySequenceSelector keySequenceSelector = mock(KeySequenceSelector.class);
        when(keySequenceSelector.firstAvailable()).thenReturn(mock(KeySequence.class));
        return keySequenceSelector;
    }

    @NotNull
    public ChronicleMap<CharSequence, ItemSource> createDefaultTestChronicleMap() {
        ChronicleMap<CharSequence, ItemSource> failedItems = mock(ChronicleMap.class);

        KeySequence sequence = createDefaultTestKeySequence();
        KeySequenceConfig config = spy(sequence.getConfig(true));
        CharSequence sequenceConfigKey = config.getKey();

        ArrayList<CharSequence> keySequenceConfigKeys = new ArrayList<>();
        keySequenceConfigKeys.add(sequenceConfigKey);

        when(failedItems.get(eq(KeySequenceConfigRepository.INDEX_KEY_NAME)))
                .thenReturn(new KeySequenceConfigKeys(keySequenceConfigKeys));

        when(failedItems.get(eq(sequenceConfigKey))).thenReturn(config);
        return failedItems;
    }

    private LifeCycle createLifeCycleTestObject() throws IOException {

        ChronicleMapRetryFailoverPolicy failoverPolicy = createDefaultTestFailoverPolicyBuilder()
                .withKeySequenceSelector(createDefaultTestKeySequenceSelector())
                .build();

        failoverPolicy.addListener((RetryListener)itemSourceDelegate -> false);
        return failoverPolicy;

    }

    public ChronicleMapRetryFailoverPolicy.Builder createDefaultTestFailoverPolicyBuilder() throws IOException {
        File tempFile = createTempFile();
        return new ChronicleMapRetryFailoverPolicy.Builder()
                .withKeySequenceSelector(createDefaultTestKeySequenceSelector())
                .withFileName(tempFile.getAbsolutePath())
                .withNumberOfEntries(TEST_NUMBER_OF_ENTRIES);
    }

    public ChronicleMapRetryFailoverPolicy.Builder createDefaultTestFailoverPolicyBuilder(String fileName, ChronicleMap<CharSequence, ItemSource> failedItems) {
        return new ChronicleMapRetryFailoverPolicy.Builder() {
            @Override
            ChronicleMap<CharSequence, ItemSource> createChronicleMap() {
                return failedItems;
            }
        }
                .withFileName(fileName)
                .withNumberOfEntries(TEST_NUMBER_OF_ENTRIES)
                .withKeySequenceSelector(createMockKeySequenceSelector());
    }


    private KeySequenceSelector createDefaultTestKeySequenceSelector() {
        return new SingleKeySequenceSelector(DEFAULT_TEST_SEQUENCE_ID);
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("failedItems", "test");
        tempFile.deleteOnExit();
        return tempFile;
    }

    private static class InvocationCounter {

        private final AtomicInteger count = new AtomicInteger();

        public void increment() {
            count.incrementAndGet();
        }

    }

    static class ExceptionCauseMatcher extends BaseMatcher<Throwable> {

        final Exception expectedCause;

        public ExceptionCauseMatcher(Exception testException) {
            this.expectedCause = testException;
        }

        @Override
        public boolean matches(Object item) {
            return expectedCause.equals(item);
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    static class ExceptionMatcher extends BaseMatcher<Throwable> {

        private final Class expectedType;
        private final String expectedMessage;

        public ExceptionMatcher(Class expectedType, String expectedMessage) {
            this.expectedType = expectedType;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public boolean matches(Object item) {
            return expectedType == item.getClass() && expectedMessage.equals(((Exception)item).getMessage());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedType.getName());
            description.appendText(":");
            description.appendText(expectedMessage);
        }
    }

}
