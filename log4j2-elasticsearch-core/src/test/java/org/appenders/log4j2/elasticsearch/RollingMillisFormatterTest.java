package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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


import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RollingMillisFormatterTest {

    public static final String TEST_PREFIX = "testPrefix";
    public static final String DATE_PATTERN_WITH_MINUTES = "yyyy-MM-dd-HH.mm";
    public static final ZoneId TEST_TIME_ZONE = ZoneId.of(RollingMillisFormatter.Builder.DEFAULT_TIME_ZONE);
    public static final long DEFAULT_TEST_TIME_IN_MILLIS = getTestTimeInMillis();
    public static final String DEFAULT_SEPARATOR = "-";

    private static long getTestTimeInMillis() {
        return LocalDateTime.of(2021, 11, 30, 23, 57, 0, 0)
                .atZone(ZoneId.of(RollingMillisFormatter.Builder.DEFAULT_TIME_ZONE))
                .toInstant().toEpochMilli();
    }

    public static RollingMillisFormatter.Builder createTimeBasedRollingIndexNameFormatterBuilder() {

        final RollingMillisFormatter.Builder builder = spy(new RollingMillisFormatter.Builder())
                .withPrefix(TEST_PREFIX)
                .withPattern(DATE_PATTERN_WITH_MINUTES)
                .withTimeZone(TEST_TIME_ZONE.getId())
                .withInitialTimestamp(DEFAULT_TEST_TIME_IN_MILLIS);

        return builder;
    }

    @Test
    public void startsWithoutExceptionsIfSetupIsCorrect() {

        // when
        final MillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder().build();

        // then
        assertNotNull(formatter);

    }

    @Test
    public void builderThrowsWhenPatternIsNull() {

        // when
        final RollingMillisFormatter.Builder builder = createTimeBasedRollingIndexNameFormatterBuilder()
                .withPattern(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No pattern provided for " + RollingMillisFormatter.class.getSimpleName()));

    }

    @Test
    public void returnsFormattedPatternWithNoSeparatorIfPrefixNotConfigured() {

        // given
        final String testSeparator = ".";
        assertNotEquals(DEFAULT_SEPARATOR, testSeparator);

        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);
        final RollingMillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder()
                .withSeparator(testSeparator)
                .build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix.2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsFormattedPatternWithPrefixAndNoSeparatorIfSeparatorNotConfigured() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);
        final RollingMillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder()
                .withPrefix(TEST_PREFIX)
                .build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsFormattedPatternWithNoPrefixAndNoSeparatorIfPrefixIsNull() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);
        final RollingMillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder()
                .withSeparator(DEFAULT_SEPARATOR)
                .withPrefix(null)
                .build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsCurrentTimeIfEventTimeIsBeforeRolloverTime() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final MillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsEventBasedNextRolloverTimeIfEventTimeIsAfterNextRolloverTime() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.HOURS.toMillis(4));

        final MillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix2021-12-01-03.57", formattedIndexName);

    }

    @Test
    public void returnsEventBasedNextRolloverTimeOnPostRolloverRaceConditionWhenCurrentIsStillEqualToCurrent() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final RollingTimestamps rollingTimestamps = spy(new RollingTimestamps() {

            private final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(DEFAULT_TEST_TIME_IN_MILLIS, DATE_PATTERN_WITH_MINUTES);

            @Override
            public long next() {
                return timestamps.next();
            }

            @Override
            public long current() {
                return timestamps.current();
            }

            @Override
            public void rollover() {
                timestamps.rollover();
            }
        });

        // mimic timestamp moving when current read lock was valid between separate thread getting the write lock and rolling before current thread checked the timestamps.current()
        when(rollingTimestamps.current())
                .thenCallRealMethod()
                .thenCallRealMethod()
                .thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final MillisFormatter formatter = createTestFormatter(rollingTimestamps);

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix-2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsEventBasedNextRolloverTimeOnPostRolloverRaceConditionWhenCurrentIsOld() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final RollingTimestamps rollingTimestamps = spy(new RollingTimestamps() {

            private final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(DEFAULT_TEST_TIME_IN_MILLIS, DATE_PATTERN_WITH_MINUTES);

            @Override
            public long next() {
                return timestamps.next();
            }

            @Override
            public long current() {
                return timestamps.current();
            }

            @Override
            public void rollover() {
                timestamps.rollover();
            }
        });

        // mimic timestamp moving when current read lock was valid between separate thread getting the write lock and rolling before current thread checked the timestamps.current()
        when(rollingTimestamps.current())
                .thenCallRealMethod()
                .thenCallRealMethod()
                .thenReturn(DEFAULT_TEST_TIME_IN_MILLIS - 1); // simply simulate the conditions here.

        final MillisFormatter formatter = createTestFormatter(rollingTimestamps);

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix-2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsEventBasedNextRolloverTimeOnPostRolloverRaceConditionWhenCurrentMovedForward() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final RollingTimestamps rollingTimestamps = spy(new RollingTimestamps() {

            private final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(DEFAULT_TEST_TIME_IN_MILLIS, DATE_PATTERN_WITH_MINUTES);

            @Override
            public long next() {
                return timestamps.next();
            }

            @Override
            public long current() {
                return timestamps.current();
            }

            @Override
            public void rollover() {
                timestamps.rollover();
            }
        });

        // mimic timestamp moving forward when current read lock was valid between separate thread getting the write lock and rolling before current thread checked the timestamps.current()
        when(rollingTimestamps.current())
                .thenCallRealMethod()
                .thenCallRealMethod()
                .thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.HOURS.toMillis(1));

        final MillisFormatter formatter = createTestFormatter(rollingTimestamps);

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix-2021-11-30-23.57", formattedIndexName);

    }

    @Test
    public void returnsPreviousRolloverTimeIfEventTimeIsBeforeCurrentTime() {

        // given
        final TestEvent logEvent = mock(TestEvent.class);
        when(logEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS - TimeUnit.HOURS.toMillis(1));

        final MillisFormatter formatter = createTimeBasedRollingIndexNameFormatterBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimestamp());

        // then
        assertEquals("testPrefix2021-11-30-22.57", formattedIndexName);

    }

    @Test
    public void returnsEventTimeBasedValueInsteadOfCachedDuringRollover() throws InterruptedException {

        // given
        final RollingTimestamps rollingTimestamps = new RollingTimestamps() {

            private final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(DEFAULT_TEST_TIME_IN_MILLIS, DATE_PATTERN_WITH_MINUTES);

            @Override
            public long next() {
                return timestamps.next();
            }

            @Override
            public long current() {
                return timestamps.current();
            }

            @Override
            public void rollover() {

                timestamps.rollover();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        };

        final TestEvent skippingEvent = mock(TestEvent.class);
        when(skippingEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.MINUTES.toMillis(5));

        final RollingMillisFormatter formatter = createTestFormatter(rollingTimestamps);


        final TestEvent blockingEvent = mock(TestEvent.class);
        when(blockingEvent.getTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.MINUTES.toMillis(1));

        final CountDownLatch beforeRolloverLatch = new CountDownLatch(1);
        final CountDownLatch afterRolloverLatch = new CountDownLatch(1);
        final AtomicReference<String> rolledIndexName = new AtomicReference<>();
        new Thread() {
            @Override
            public void run() {
                beforeRolloverLatch.countDown();
                rolledIndexName.set(formatter.format(blockingEvent.getTimestamp()));
                afterRolloverLatch.countDown();
            }
        }.start();

        beforeRolloverLatch.await();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));

        // when
        final String fallbackIndexName = formatter.format(skippingEvent.getTimestamp());

        // then
        assertEquals("testPrefix-2021-12-01-00.02", fallbackIndexName);

        afterRolloverLatch.await();
        assertEquals("testPrefix-2021-11-30-23.58", rolledIndexName.get());

        final long expectedNextRolloverTime = DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.MINUTES.toMillis(2);
        assertEquals(expectedNextRolloverTime, formatter.getNextRolloverTime());

    }

    private RollingMillisFormatter createTestFormatter(final RollingTimestamps rollingTimestamps) {

        return new RollingMillisFormatter.Builder() {
            @Override
            public RollingTimestamps createRollingTimestamps() {
                return rollingTimestamps;
            }
        }
                .withPrefix(TEST_PREFIX)
                .withSeparator(DEFAULT_SEPARATOR)
                .withPattern(DATE_PATTERN_WITH_MINUTES)
                .withTimeZone(TEST_TIME_ZONE.getId())
                .build();

    }

    private static class TestEvent {

        public long getTimestamp() {
            throw new UnsupportedOperationException("Not implemented");
        }

    }

}
