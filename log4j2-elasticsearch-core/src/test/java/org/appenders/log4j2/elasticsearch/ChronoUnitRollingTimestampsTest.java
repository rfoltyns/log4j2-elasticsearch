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

import org.appenders.log4j2.elasticsearch.util.RolloverUtilTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChronoUnitRollingTimestampsTest {

    public static final int noOfUnitsAhead = 10000;

    public static final List<ZoneId> testZones = Arrays.asList(
            ZoneId.systemDefault(),
            ZoneId.of("Europe/Warsaw"),
            ZoneId.of("Europe/London"),
            ZoneId.of("America/New_York")
    );

    public final ZoneId testZone = testZones.get(0);

    @Test
    public void rolloverMovesCurrentMillisForwardByOneUnit() {

        // given
        final long initialTimestamp = System.currentTimeMillis();
        final long expectedTimestamp = initialTimestamp + ChronoUnit.MILLIS.getDuration().toMillis();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd-HH-mm-ss.SSS", testZone);

        // when
        timestamps.rollover();

        // then
        assertEquals(expectedTimestamp, timestamps.current());

    }

    @Test
    public void rolloverMovesNextMillisForwardByOneUnit() {

        // given
        final long initialTimestamp = System.currentTimeMillis();
        final long expectedNextTimestamp = initialTimestamp + ChronoUnit.MILLIS.getDuration().toMillis() * 2;

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd-HH-mm-ss.SSS", testZone);

        // when
        timestamps.rollover();

        // then
        assertEquals(expectedNextTimestamp, timestamps.next());

    }

    @Test
    public void supportsMillisecondsDateTimePattern() {

        // given
        final long expectedTimestamp = System.currentTimeMillis();
        final long expectedNextTimestamp = expectedTimestamp + ChronoUnit.MILLIS.getDuration().toMillis();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(expectedTimestamp, "yyyy-MM-dd-HH-mm-ss.SSS", testZone);

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsSecondsDateTimePattern() {

        // given
        final long millis = ChronoUnit.SECONDS.getDuration().toMillis();

        final long initialTimestamp = System.currentTimeMillis();
        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd-HH-mm-ss", testZone);

        final long expectedTimestamp = (initialTimestamp / millis) * millis;
        final long expectedNextTimestamp = expectedTimestamp + millis;

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsMinutesDateTimePattern() {

        // given
        final long millis = ChronoUnit.MINUTES.getDuration().toMillis();

        final long initialTimestamp = System.currentTimeMillis();
        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd-HH-mm", testZone);

        final long expectedTimestamp = (System.currentTimeMillis() / millis) * millis;
        final long expectedNextTimestamp = expectedTimestamp + millis;

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsHoursDateTimePattern() {

        // given
        final long millis = ChronoUnit.HOURS.getDuration().toMillis();

        final long initialTimestamp = System.currentTimeMillis();
        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd-HH", testZone);

        final long expectedTimestamp = (System.currentTimeMillis() / millis) * millis;
        final long expectedNextTimestamp = expectedTimestamp + millis;

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsDaysDateTimePattern() {

        // given
        final long millis = ChronoUnit.DAYS.getDuration().toMillis();

        final long initialTimestamp = System.currentTimeMillis();
        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd", testZone);

        final long expectedTimestamp = saveTheDay(ChronoUnit.DAYS, initialTimestamp / millis * millis);
        final long expectedNextTimestamp = expectedTimestamp + millis;

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsHalfDaysDateTimePattern() {

        // given
        final long millis = ChronoUnit.HALF_DAYS.getDuration().toMillis();

        final long initialTimestamp = System.currentTimeMillis();
        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM-dd a", testZone);

        final long expectedTimestamp = saveTheDay(ChronoUnit.HALF_DAYS, initialTimestamp / millis * millis);
        final long expectedNextTimestamp = expectedTimestamp + millis;

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsWeeksDateTimePattern() {

        // given
        final long initialTimestamp = System.currentTimeMillis();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-ww", testZone);

        final long expectedTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli();

        final long expectedNextTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plus(1, ChronoUnit.WEEKS)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli();

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsMonthsDateTimePattern() {

        // given
        final long initialTimestamp = System.currentTimeMillis();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy-MM", testZone);

        final long expectedTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli();

        final long expectedNextTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .with(TemporalAdjusters.firstDayOfMonth())
                .plus(1, ChronoUnit.MONTHS)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli();

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsYearsDateTimePattern() {

        // given
        final long initialTimestamp = System.currentTimeMillis();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, "yyyy", testZone);

        final long expectedTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .with(TemporalAdjusters.firstDayOfYear())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli();

        final long expectedNextTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZone)
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.firstDayOfYear())
                .plus(1, ChronoUnit.YEARS)
                .toInstant()
                .toEpochMilli();

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current);
        assertEquals(expectedNextTimestamp, next);

    }

    @Test
    public void supportsTimeBasedPatternsLongerThanMicros() {

        final List<ChronoUnit> units = Arrays.stream(ChronoUnit.values())
                .filter(unit -> unit.compareTo(ChronoUnit.MILLIS) >= 0)
                .filter(unit -> unit.compareTo(ChronoUnit.DAYS) <= 0)
                .filter(unit -> unit != ChronoUnit.HALF_DAYS) // unit only
                .collect(Collectors.toList());

        for (ChronoUnit unit : units) {
            testTimeBasedPatternUntil(noOfUnitsAhead, unit, RolloverUtilTest.Pattern.of(unit).getDateTimePattern());
        }

    }

    @Test
    public void supportsTimeBasedUnitsLongerThanMicros() {

        final List<ChronoUnit> units = Arrays.stream(ChronoUnit.values())
                .filter(unit -> unit.compareTo(ChronoUnit.MILLIS) >= 0)
                .filter(unit -> unit.compareTo(ChronoUnit.DAYS) <= 0)
                .filter(unit -> unit != ChronoUnit.HALF_DAYS)
                .collect(Collectors.toList());

        for (ChronoUnit unit : units) {
            testTimeBasedUnitUntil(noOfUnitsAhead, unit);
        }

    }

    @Test
    public void supportsYearsUntilMillenniaInclusive() {

        final List<ChronoUnit> dateBasedUnits = Arrays.stream(ChronoUnit.values())
                .filter(unit -> unit.compareTo(ChronoUnit.YEARS) > 0)
                .filter(unit -> !ChronoUnit.ERAS.equals(unit)) // just 2 eras so far in 2022
                .filter(unit -> !ChronoUnit.FOREVER.equals(unit)) // don't push it..
                .collect(Collectors.toList());


        for (ChronoUnit unit : dateBasedUnits) {
            testYearsUntil(noOfUnitsAhead, unit);
        }

    }

    private void testTimeBasedUnitUntil(final int limit, final ChronoUnit unit) {

        final long initialMillis = System.currentTimeMillis();
        for (long i = 0; i < limit; i++) {
            testTimeBasedUnit(unit, initialMillis, i);
        }

    }

    private void testTimeBasedPatternUntil(final int limit, final ChronoUnit unit, final String dateTimePattern) {

        final long initialMillis = System.currentTimeMillis();
        for (long i = 0; i < limit; i++) {
            testTimeBasedUnitWithPattern(unit, initialMillis, i, dateTimePattern, getZoneId());
        }

    }

    private void testTimeBasedUnitWithPattern(final ChronoUnit unit,
                                              final long initialMillis,
                                              final long unitsLater,
                                              final String datePattern,
                                              final String zoneId) {

        // given
        final long nanosOrMillis = ChronoUnit.MILLIS.compareTo(unit) <= 0 ? unit.getDuration().toMillis() : unit.getDuration().toNanos();

        final long delta = nanosOrMillis * unitsLater;
        final long millis = initialMillis + delta;

        long expectedTimestamp = millis / nanosOrMillis * nanosOrMillis;
        long expectedNextTimestamp = expectedTimestamp + nanosOrMillis;

        if (unit.compareTo(ChronoUnit.HALF_DAYS) >= 0 && unit.compareTo(ChronoUnit.WEEKS) < 0) {

            final ExpectedOffsets expectedOffsets = calculateOffsets(unit, expectedTimestamp, expectedNextTimestamp);
            expectedTimestamp -= expectedOffsets.expectedOffset;
            expectedNextTimestamp -= expectedOffsets.expectedNextOffset;

        }

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(millis, datePattern, ZoneId.of(zoneId));

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current, "expected: " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(expectedTimestamp), testZone) + ", actual: " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(current), testZone) + ", initialMillis: " + initialMillis + ", unit: " + unit.name() + ", datePattern: " + datePattern + ", unitsLater: " + unitsLater);
        assertEquals(expectedNextTimestamp, next, "expected: " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(expectedNextTimestamp), testZone) + ", actual: " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), testZone) + ", initialMillis: " + initialMillis + ", unit: " + unit.name() + ", datePattern: " + datePattern + ", unitsLater: " + unitsLater);

    }

    private ExpectedOffsets calculateOffsets(final ChronoUnit unit, final long expectedTimestamp, final long expectedNextTimestamp) {

        ZoneOffset offset = testZone.getRules().getOffset(Instant.ofEpochMilli(expectedTimestamp));
        ZoneOffset nextOffset = testZone.getRules().getOffset(Instant.ofEpochMilli(expectedNextTimestamp));
        final int seconds = offset.getTotalSeconds();
        long expectedOffset = seconds * 1000L;
        long expectedNextOffset = nextOffset.getTotalSeconds() * 1000L;

        return new ExpectedOffsets(expectedOffset, expectedNextOffset);

    }

    private void testTimeBasedUnit(final ChronoUnit unit, final long initialMillis, long unitsLater) {

        // given
        final long nanosOrMillis = ChronoUnit.MILLIS.compareTo(unit) <= 0 ? unit.getDuration().toMillis() : unit.getDuration().toNanos();

        final long delta = nanosOrMillis * unitsLater;
        final long millis = initialMillis + delta;

        long expectedTimestamp = millis / nanosOrMillis * nanosOrMillis; // truncate to unit
        long expectedNextTimestamp = expectedTimestamp + nanosOrMillis;

        if (unit.compareTo(ChronoUnit.HALF_DAYS) >= 0 && unit.compareTo(ChronoUnit.WEEKS) < 0) {

            final ExpectedOffsets expectedOffsets = calculateOffsets(unit, expectedTimestamp, expectedNextTimestamp);
            expectedTimestamp -= expectedOffsets.expectedOffset;
            expectedNextTimestamp -= expectedOffsets.expectedNextOffset;

        }

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(millis, unit, testZone);

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current, "initialMillis: " + initialMillis + ", unit: " + unit.name() + ", " + unit.name() + " later: " + unitsLater);
        assertEquals(expectedNextTimestamp, next, "initialMillis: " + initialMillis + ", unit: " + unit.name() + ", "  + unit.name() + " later: " + unitsLater);

    }

    private long saveTheDay(final ChronoUnit unit, final long expectedTimestamp) {

        if (unit.compareTo(ChronoUnit.HALF_DAYS) >= 0 && unit.compareTo(ChronoUnit.WEEKS) < 0) {

            ZoneOffset offset = testZone.getRules().getOffset(Instant.ofEpochMilli(expectedTimestamp));
            int totalSeconds = offset.getTotalSeconds();
            return expectedTimestamp - totalSeconds * 1000L;

        }
        return expectedTimestamp;

    }

    private void testYearsUntil(final int endYear, final ChronoUnit unit) {
        testYears(1970, endYear, unit);
    }

    private void testYears(final int startYear, final int endYear, final ChronoUnit unit) {
        for (int i = startYear; i < endYear; i++) {
            testYearUnit(unit, i);
        }
    }

    private void testYearUnit(final ChronoUnit unit, final int year) {

        final ZonedDateTime localDateTime = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.of(getZoneId()));

        final long initialTimestamp = localDateTime.toInstant().toEpochMilli();
        final ZoneId testZoneId = ZoneId.of(System.getProperty("chronotest.zone", getZoneId()));

        final ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), testZoneId)
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.firstDayOfYear());

        final int nextYear = unit.addTo(dateTime, 1).getYear();
        final int delta = nextYear - dateTime.getYear();

        long expectedYear = 0;
        try {
            expectedYear = (long) dateTime.getYear() / delta * delta;
        } catch (ArithmeticException e) {
            System.out.println(e.getMessage() + ", expected year: " + expectedYear + ", unit " + unit.name());
        }
        final long expectedNextYear = expectedYear + delta;

        final long expectedTimestamp = dateTime
                .with(ChronoField.YEAR, expectedYear)
                .toInstant()
                .toEpochMilli();

        final long expectedNextTimestamp = dateTime
                .with(ChronoField.YEAR, expectedNextYear)
                .toInstant()
                .toEpochMilli();

        final RollingTimestamps timestamps = new ChronoUnitRollingTimestamps(initialTimestamp, unit, testZoneId);

        // when
        final long current = timestamps.current();
        final long next = timestamps.next();

        // then
        assertEquals(expectedTimestamp, current, "tested year: " + year + ", expected year: " + expectedYear + ", unit " + unit.name());
        assertEquals(expectedNextTimestamp, next, "tested year: " + year + ", expected year: " + expectedYear + ", unit " + unit.name());

    }

    @NotNull
    private String getZoneId() {
        return testZone.getId();
    }

    @Test
    public void doesNotSupportEras() {

        // given
        final long initialTimestamp = System.currentTimeMillis();
        final String dateTimePattern = "G";

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ChronoUnitRollingTimestamps(initialTimestamp, dateTimePattern, testZone));

        // then
        assertThat(exception.getMessage(), containsString("Unable to derive rollover period from pattern: " + dateTimePattern));

    }

    private static class ExpectedOffsets {

        private final long expectedOffset;
        private final long expectedNextOffset;

        public ExpectedOffsets(final long expectedOffset, final long expectedNextOffset) {
            this.expectedOffset = expectedOffset;
            this.expectedNextOffset = expectedNextOffset;
        }

    }
}