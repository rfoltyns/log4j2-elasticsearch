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

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for {@link org.appenders.log4j2.elasticsearch.ChronoUnitRollingTimestamps}
 */
public class RolloverUtil {

    private RolloverUtil() {
        // static
    }

    /**
     * Derives smallest {@link ChronoUnit} that - once applied to input millis or date-time - will produce different date-time for given date-time pattern.
     * <p>
     * E.g.:
     * <ul>
     *     <li>Given {@code yyyy-MM-dd}, returns {@code ChronoUnit.DAYS}</li>
     *     <li>Given {@code yyyy-MM-dd-HH-mm}, returns {@code ChronoUnit.MINUTES}</li>
     *     <li>Given {@code yyyy-MM-ww}, returns {@code ChronoUnit.WEEKS}</li>
     * </ul>
     *
     * @param dateTimePattern valid date-time pattern to analyze
     *
     * @throws IllegalArgumentException when given date-time pattern produces errors with {@link DateTimeFormatter}
     * @return smallest {@link ChronoUnit} that will produce different date-time for given date-time pattern
     */
    public static ChronoUnit getMinimumUnit(final String dateTimePattern) {
        return getMinimumUnit(dateTimePattern, Arrays.stream(ChronoUnit.values())
                .filter(unit -> ChronoUnit.FOREVER.compareTo(unit) > 0)
                .collect(Collectors.toList()));
    }

    /**
     * Derives smallest {@link ChronoUnit} that - once applied to input millis or date-time - will produce different date-time for given date-time pattern.
     *
     * Consider this method <i>private</i>
     *
     * @param dateTimePattern valid date-time pattern to analyze
     * @param supportedUnits list of units that are allowed to be returned
     *
     * @return smallest {@link ChronoUnit} that will produce different date-time for given date-time pattern
     */
    static ChronoUnit getMinimumUnit(final String dateTimePattern, final List<ChronoUnit> supportedUnits) {

        final ZonedDateTime discovery = Instant.from(Instant.ofEpochMilli(1L))
                .plus(1, ChronoUnit.NANOS)
                .atZone(ZoneId.systemDefault());

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePattern);
        final String formatted = discovery.format(formatter);

        ChronoUnit result = ChronoUnit.NANOS;

        for (ChronoUnit unit : supportedUnits) {

            try {

                final ZonedDateTime next = discovery.plus(1, unit);
                if (!next.format(formatter).equals(formatted)) {
                    return unit;
                }

                result = unit;

            } catch (final DateTimeException e) {
                throw new IllegalArgumentException("Unable to derive rollover period from pattern: " + dateTimePattern);
            }

        }

        return result;

    }

    /**
     * Truncates {@link ZonedDateTime} to {@link ChronoUnit}. Supports {@link ChronoUnit#NANOS} up to {@link ChronoUnit#MILLENNIA} (inclusive)
     *
     * E.g. {@code truncate(date, ChronoUnit.HOURS)} will set minutes and smaller to 0, but will NOT set {@code ChronoUnit.HOURS} to 0.
     *
     * @param initial {@link ZonedDateTime} to truncate
     * @param unit {@link ChronoUnit} to truncate to
     *                               (exclusive: {@code truncate(date, ChronoUnit.HOURS)} will set minutes and smaller to 0, but will NOT set {@code ChronoUnit.HOURS} to 0).
     *                               All smaller units of {@code initial} will be truncated to 0 (or appropriate minimum value in case of months, weeks, half-days, etc.
     *
     * @return Truncated {@link ZonedDateTime}
     */
    public static ZonedDateTime truncate(final ZonedDateTime initial, final ChronoUnit unit) {

        ZonedDateTime result = initial;

        for (final ChronoUnit c : ChronoUnit.values()) {

            if (unit.compareTo(c) >= 0) {
                if (c.compareTo(ChronoUnit.DAYS) <= 0) {
                    result = initial.truncatedTo(c);
                }
            } else if (ChronoUnit.WEEKS.equals(unit)) {
                result = initial.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            } else if (ChronoUnit.MONTHS.equals(unit)) {
                result = initial.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfMonth());
            } else if (ChronoUnit.YEARS.equals(unit)) {
                result = initial.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfYear());
            } else if (ChronoUnit.YEARS.compareTo(unit) < 0) {

                final int nextYear = unit.addTo(result, 1).getYear();
                final int delta = nextYear - initial.getYear();
                final long truncated = (long) initial.getYear() / delta * delta;

                return initial.truncatedTo(ChronoUnit.DAYS)
                        .with(TemporalAdjusters.firstDayOfYear())
                        .with(ChronoField.YEAR, truncated);

            }

        }

        return result;

    }

}
