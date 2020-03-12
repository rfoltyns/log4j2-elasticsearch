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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.PatternProcessor;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RollingIndexNameFormatterTest {

    private static final String TEST_INDEX_NAME = "testIndexName";
    private static final String DATE_PATTERN_WITH_MINUTES = "yyyy-MM-dd-HH.mm";
    private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone(RollingIndexNameFormatter.Builder.DEFAULT_TIME_ZONE);
    private static final long DEFAULT_TEST_TIME_IN_MILLIS = getTestTimeInMillis();

    private static long getTestTimeInMillis() {
        return LocalDateTime.of(2017, 12, 20, 23, 54, 0, 0)
                            .atZone(ZoneId.of(RollingIndexNameFormatter.Builder.DEFAULT_TIME_ZONE))
                            .toInstant().toEpochMilli();
    }

    public static RollingIndexNameFormatter.Builder createRollingIndexNameFormatterBuilder() {

        RollingIndexNameFormatter.Builder builder = spy(RollingIndexNameFormatter.newBuilder());

        when(builder.getInitTimeInMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        builder.withIndexName(TEST_INDEX_NAME);
        builder.withPattern(DATE_PATTERN_WITH_MINUTES);
        builder.withTimeZone(TEST_TIME_ZONE.getID());
        return builder;
    }

    @Test
    public void startsWithoutExceptionsIfSetupIsCorrect() {

        // when
        IndexNameFormatter formatter = createRollingIndexNameFormatterBuilder().build();

        // then
        Assert.assertNotNull(formatter);
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenIndexNameIsNull() {

        // when
        RollingIndexNameFormatter.Builder builder = createRollingIndexNameFormatterBuilder();
        builder.withIndexName(null);

        // then
        builder.build();
    }


    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenPatternIsNull() {

        // when
        RollingIndexNameFormatter.Builder builder = createRollingIndexNameFormatterBuilder();
        builder.withPattern(null);

        // then
        builder.build();
    }

    @Test
    public void returnsCurrentTimeIfEventTimeIsBeforeRolloverTime() {

        // given
        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        IndexNameFormatter formatter = createRollingIndexNameFormatterBuilder().build();

        // when
        String formattedIndexName = formatter.format(logEvent);

        // then
        Assert.assertEquals("testIndexName-2017-12-20-23.54", formattedIndexName);
    }


    @Test
    public void returnsNextRolloverTimeIfEventTimeIsAfterRolloverTime() {

        // given
        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.HOURS.toMillis(1));

        IndexNameFormatter formatter = createRollingIndexNameFormatterBuilder().build();

        // when
        String formattedIndexName = formatter.format(logEvent);

        // then
        Assert.assertEquals("testIndexName-2017-12-21-00.54", formattedIndexName);
    }

    @Test
    public void returnsPreviousRolloverTimeIfEventTimeIsBeforeCurrentTime() {

        // given
        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS - TimeUnit.HOURS.toMillis(1));

        IndexNameFormatter formatter = createRollingIndexNameFormatterBuilder().build();

        // when
        String formattedIndexName = formatter.format(logEvent);

        // then
        Assert.assertEquals("testIndexName-2017-12-20-22.54", formattedIndexName);
    }

    @Test
    public void returnsCustomSeparatorFormattedIndexName() {

        // given
        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);
        RollingIndexNameFormatter.Builder builder = createRollingIndexNameFormatterBuilder();
        builder.withSeparator(".");
        IndexNameFormatter formatter = builder.build();

        // when
        String formattedIndexName = formatter.format(logEvent);

        // then
        Assert.assertEquals("testIndexName.2017-12-20-23.54", formattedIndexName);
    }


    @Test
    public void returnsEventTimeBasedNameInsteadOfCurrentNameDuringRollover() throws InterruptedException {

        // given
        long testNextTimeResult = DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.MINUTES.toMillis(123);

        TestFormatter.TEST_PATTERN_PROCESSOR = spy(new PatternProcessor("%d{" + DATE_PATTERN_WITH_MINUTES + "}"));
        when(TestFormatter.TEST_PATTERN_PROCESSOR.getNextTime(any(long.class), eq(1), eq(false))).thenAnswer(new Answer<Long>() {
            private int count = 0;
            @Override
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (++count > 1) {
                    return (Long) invocationOnMock.callRealMethod();
                }
                Thread.sleep(100);
                return testNextTimeResult;
            }
        });

        LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.MINUTES.toMillis(1));

        IndexNameFormatter formatter = new TestFormatter(TEST_INDEX_NAME, DATE_PATTERN_WITH_MINUTES, DEFAULT_TEST_TIME_IN_MILLIS, TEST_TIME_ZONE,
            RollingIndexNameFormatter.DEFAULT_SEPARATOR);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                formatter.format(logEvent);
                countDownLatch.countDown();
            }
        }.start();

        Thread.sleep(50);

        // when
        String formattedIndexName = formatter.format(logEvent);

        // then
        Assert.assertEquals("testIndexName-2017-12-20-23.55", formattedIndexName);
        countDownLatch.await();
        Assert.assertEquals(testNextTimeResult, ((RollingIndexNameFormatter)formatter).getNextRolloverTime());

    }

    @Test
    public void concurrencyTest() throws InterruptedException {

        // given
        //        TestFormatter.TEST_PATTERN_PROCESSOR = Mockito.spy(new PatternProcessor("%d{" + DATE_PATTERN_WITH_MINUTES + "}"));

        for (int ii = 0; ii < 100; ii++) {
            IndexNameFormatter formatter = createRollingIndexNameFormatterBuilder().build();
            runSingleConcurrencyTest(formatter, 20);
        }

        //        Mockito.verify(TestFormatter.TEST_PATTERN_PROCESSOR, Mockito.times(100)).getNextTime(Mockito.any(long.class), Mockito.eq(1), Mockito.eq(false));
    }

    private void runSingleConcurrencyTest(IndexNameFormatter formatter, int numberOfThreads) throws InterruptedException {

        ConcurrentLinkedQueue<TestTuple> logEvents = generateLogEvents();
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        List<Thread> threads = new ArrayList<>();

        for (int ii = 0; ii < numberOfThreads; ii++) {
            threads.add(new Thread(() -> {
                try {
                    while (!logEvents.isEmpty()) {
                        // when
                        TestTuple tuple = logEvents.poll();
                        if (tuple == null) {
                            continue;
                        }
                        String formattedIndexName = formatter.format(tuple.getLogEvent());

                        // then
                        if (tuple.getIncrement() < 0) {
                            Assert.assertEquals("testIndexName-2017-12-20-23.53", formattedIndexName);
                        }
                        if (tuple.getIncrement() == 0) {
                            Assert.assertEquals("testIndexName-2017-12-20-23.54", formattedIndexName);
                        }
                        if (tuple.getIncrement() > 0) {
                            Assert.assertEquals("testIndexName-2017-12-20-23.55", formattedIndexName);
                        }
                    }
                } finally {
                    countDownLatch.countDown();
                }
            }));
        }

        threads.stream().forEach(th -> th.start());
        countDownLatch.await();
    }

    private ConcurrentLinkedQueue<TestTuple> generateLogEvents() {
        ConcurrentLinkedQueue<TestTuple> events = new ConcurrentLinkedQueue<>();

        Random random = new Random();
        for (int ii = 0; ii < 1000; ii++) {
            LogEvent logEvent = mock(LogEvent.class);

            int increment = random.nextInt(3) - 1;
            when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + increment * 60000 + random.nextInt(60000));
            events.add(new TestTuple(logEvent, increment));
        }
        return events;
    }

    public static class TestFormatter extends RollingIndexNameFormatter {

        public static PatternProcessor TEST_PATTERN_PROCESSOR;

        public TestFormatter(String indexName, String pattern, long initTimeInMillis, TimeZone timeZone, String separator) {
            super(indexName, pattern, initTimeInMillis, timeZone, separator);
        }

        @Override
        protected PatternProcessor createPatternProcessor(String pattern) {
            return TEST_PATTERN_PROCESSOR;
        }

    }

    public static class TestTuple {
        private LogEvent logEvent;
        private int increment;

        public TestTuple(LogEvent logEvent, int increment) {
            this.logEvent = logEvent;
            this.increment = increment;
        }

        public LogEvent getLogEvent() {
            return logEvent;
        }

        public int getIncrement() {
            return increment;
        }
    }
}
