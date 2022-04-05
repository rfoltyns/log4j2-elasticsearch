package org.appenders.log4j2.elasticsearch.util;

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

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RolloverUtilTest {

    private static final ZonedDateTime TEST_TIME = ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()).with(ChronoField.NANO_OF_SECOND, System.nanoTime() % 1000000000);

    public enum Pattern {

        NANOS(ChronoUnit.NANOS, "yyyy-MM-dd-HH-mm-ss.SSSSSSSSS", ChronoField.NANO_OF_SECOND, ChronoField.NANO_OF_SECOND.getFrom(TEST_TIME), ChronoField.MICRO_OF_SECOND.getFrom(TEST_TIME) * 1000),
        MICROS(ChronoUnit.MICROS, "yyyy-MM-dd-HH-mm-ss.SSSSSS", ChronoField.MICRO_OF_SECOND, ChronoField.MICRO_OF_SECOND.getFrom(TEST_TIME), ChronoField.MILLI_OF_SECOND.getFrom(TEST_TIME) * 1000),
        MILLIS(ChronoUnit.MILLIS, "yyyy-MM-dd-HH-mm-ss.SSS", ChronoField.MILLI_OF_SECOND, ChronoField.MILLI_OF_SECOND.getFrom(TEST_TIME), 0),
        SECONDS(ChronoUnit.SECONDS, "yyyy-MM-dd-HH-mm-ss", ChronoField.SECOND_OF_MINUTE, ChronoField.SECOND_OF_MINUTE.getFrom(TEST_TIME), 0),
        MINUTES(ChronoUnit.MINUTES, "yyyy-MM-dd-HH-mm", ChronoField.MINUTE_OF_HOUR, ChronoField.MINUTE_OF_HOUR.getFrom(TEST_TIME), 0),
        HOURS(ChronoUnit.HOURS, "yyyy-MM-dd-HH", ChronoField.HOUR_OF_DAY, ChronoField.HOUR_OF_DAY.getFrom(TEST_TIME), ChronoField.HOUR_OF_DAY.getFrom(TEST_TIME) / 12 == 1 ? 12 : 0),
        HALF_DAYS(ChronoUnit.HALF_DAYS, "yyyy-MM-dd a", ChronoField.HOUR_OF_DAY, ChronoField.HOUR_OF_DAY.getFrom(TEST_TIME) / 12 == 1 ? 12 : 0, 0),
        DAYS(ChronoUnit.DAYS, "yyyy-MM-dd", ChronoField.DAY_OF_MONTH, ChronoField.DAY_OF_MONTH.getFrom(TEST_TIME), 1),
        MONTHS(ChronoUnit.MONTHS, "yyyy-MM", ChronoField.MONTH_OF_YEAR, ChronoField.MONTH_OF_YEAR.getFrom(TEST_TIME), 1),
        YEARS(ChronoUnit.YEARS, "yyyy", ChronoField.YEAR, ChronoField.YEAR.getFrom(TEST_TIME), ChronoField.YEAR.getFrom(TEST_TIME) / 10 * 10),
        DECADES(ChronoUnit.DECADES, "yyyyyy", ChronoField.YEAR, ChronoField.YEAR.getFrom(TEST_TIME) / 10 * 10, ChronoField.YEAR.getFrom(TEST_TIME) / 100 * 100),
        CENTURIES(ChronoUnit.CENTURIES, "yyyyyy", ChronoField.YEAR, ChronoField.YEAR.getFrom(TEST_TIME) / 100 * 100, ChronoField.YEAR.getFrom(TEST_TIME) / 1000 * 1000),
        MILLENNIA(ChronoUnit.MILLENNIA, "yyyyyy", ChronoField.YEAR, ChronoField.YEAR.getFrom(TEST_TIME) / 1000 * 1000, 0);

        private final ChronoUnit unit;
        private final String dateTimePattern;
        private final ChronoField field;
        private final long expectedEqual;
        private final long truncatedToHigher;

        Pattern(final ChronoUnit unit, final String dateTimePattern, final ChronoField field, final long expectedEqual, final long truncatedToHigher) {
            this.unit = unit;
            this.dateTimePattern = dateTimePattern;
            this.field = field;
            this.expectedEqual = expectedEqual;
            this.truncatedToHigher = truncatedToHigher;
        }

        public static Pattern of(final ChronoUnit unit) {
            for (Pattern p : Pattern.values()) {
                if (p.unit.equals(unit)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("Unit not supported: " + unit.name());
        }

        public ChronoUnit getUnit() {
            return unit;
        }

        public String getDateTimePattern() {
            return dateTimePattern;
        }

    }

    @Test
    public void throwsIfCannotDeriveMinimumUnitFromInvalidPattern() {

        // given
        final List<ChronoUnit> supportedUnits = Collections.emptyList();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RolloverUtil.getMinimumUnit("b", supportedUnits));

        // then
        assertThat(exception.getMessage(), CoreMatchers.is("Unknown pattern letter: b"));

    }

    @Test
    public void throwsIfCannotDeriveMinimumUnitFromUnsupportedPattern() {

        // given
        final List<ChronoUnit> supportedUnits = Collections.singletonList(ChronoUnit.ERAS);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RolloverUtil.getMinimumUnit("G", supportedUnits));

        // then
        assertThat(exception.getMessage(), CoreMatchers.is("Unable to derive rollover period from pattern: G"));

    }

    @Test
    public void returnsNanosByDefault() {

        // given
        final List<ChronoUnit> supportedUnits = Collections.emptyList();

        // when
        final ChronoUnit frequency = RolloverUtil.getMinimumUnit("", supportedUnits);

        // then
        assertEquals(ChronoUnit.NANOS, frequency);

    }

    @ParameterizedTest
    @EnumSource(value = Pattern.class, names = {
            "NANOS",
            "MICROS",
            "MILLIS",
            "SECONDS",
            "MINUTES",
            "HOURS",
            "DAYS",
            "MONTHS",
            "YEARS",
    })
    public void discoversAndTruncatesMillisUpToYears(final Pattern pattern) {

        // given
        final ZonedDateTime dateTime = TEST_TIME;

        // when
        final ChronoUnit unit = pattern.unit;

        final ChronoUnit frequency = RolloverUtil.getMinimumUnit(pattern.dateTimePattern);
        final ZonedDateTime truncated = RolloverUtil.truncate(dateTime, unit);

        // then
        assertEquals(unit, frequency);
        assertEquals(zeroFieldsUntil(unit, dateTime), truncated, unit.name());

    }

    @ParameterizedTest
    @EnumSource(value = Pattern.class, names = {
            "DECADES",
            "CENTURIES",
            "MILLENNIA",
    })
    public void truncatesDecadesUpToMillennia(final Pattern pattern) {

        // given
        final ZonedDateTime dateTime = TEST_TIME;

        // when
        final ChronoUnit unit = pattern.unit;

        final ZonedDateTime truncated = RolloverUtil.truncate(dateTime, unit);

        // then
        assertEquals(zeroFieldsUntil(unit, dateTime), truncated, unit.name());

    }

    @Test
    public void discoversAndTruncatesWeeks() {

        // given
        final ZonedDateTime dateTime = TEST_TIME;

        // when
        final ZonedDateTime truncated = RolloverUtil.truncate(dateTime, ChronoUnit.WEEKS);

        // then
        assertEquals(dateTime.get(ChronoField.ALIGNED_WEEK_OF_YEAR), truncated.get(ChronoField.ALIGNED_WEEK_OF_YEAR), ChronoUnit.WEEKS.name());

    }

    private ZonedDateTime zeroFieldsUntil(final ChronoUnit unit, ZonedDateTime dateTime) {

        Pattern previous = null;

        for (Pattern p : Pattern.values()) {

            if (p.unit.compareTo(unit) <= 0) {

                dateTime = dateTime.with(p.field, p.expectedEqual);
                if (previous != null) {
                    dateTime = dateTime.with(previous.field, previous.truncatedToHigher);
                }
                previous = p;

            } else {
                break;
            }
        }
        return dateTime;
    }

}