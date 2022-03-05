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

import org.appenders.log4j2.elasticsearch.util.RolloverUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rollover timestamp provider based on date-time pattern. Rolls forward.
 * <p>
 * Supported rollover units:
 * <ul>
 * <li>Milliseconds</li>
 * <li>Seconds</li>
 * <li>Minutes</li>
 * <li>Hours</li>
 * <li>Half-days</li>
 * <li>Days</li>
 * <li>Weeks</li>
 * <li>Months</li>
 * <li>Years</li>
 * <li>Decades <b>*</b></li>
 * <li>Centuries <b>*</b></li>
 * <li>Millennia <b>*</b></li>
 * </ul>
 * <b> * if initialized with {@link ChronoUnitRollingTimestamps#ChronoUnitRollingTimestamps(long, ChronoUnit, ZoneId)}</b>
 * <p>
 *
 * <b>This class is NOT thread-safe!</b>
 *
 */
public class ChronoUnitRollingTimestamps implements RollingTimestamps {

    private volatile long currentMillis;
    private volatile long nextMillis;

    private final AtomicReference<ZonedDateTime> next;
    private final ChronoUnit rolloverUnit;

    /**
     * @param initialTimestamp initial timestamp (usually System.currentTimeMillis()) will be truncated to {@code rolloverUnit} and used as {@link #current()} until first {@link #rollover()} call
     * @param dateTimePattern valid date-time pattern used to derive {@code rolloverUnit}
     */
    public ChronoUnitRollingTimestamps(final long initialTimestamp, final String dateTimePattern) {
        this(initialTimestamp, dateTimePattern, ZoneId.systemDefault());
    }

    /**
     * @param initialTimestamp initial timestamp (usually System.currentTimeMillis()) that will be truncated to {@code rolloverUnit} and used as {@link #current()} until first {@link #rollover()} call
     * @param dateTimePattern valid date-time pattern used to derive {@code rolloverUnit}
     * @param zone Time zone to use
     */
    public ChronoUnitRollingTimestamps(final long initialTimestamp, final String dateTimePattern, final ZoneId zone) {
        this(initialTimestamp, RolloverUtil.getMinimumUnit(dateTimePattern), zone);
    }

    /**
     * Actual constructor.
     *
     * @param initialTimestamp initial timestamp (usually System.currentTimeMillis()) that will be truncated to {@code rolloverUnit} and used as {@link #current()} until first {@link #rollover()} call
     * @param rolloverUnit {@link ChronoUnit} to add to {@link #current()} on {@link #rollover()}
     * @param zone Time zone to use
     */
    ChronoUnitRollingTimestamps(final long initialTimestamp, final ChronoUnit rolloverUnit, final ZoneId zone) {

        final ZonedDateTime initial = ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialTimestamp), zone);
        final ZonedDateTime truncated = RolloverUtil.truncate(initial, rolloverUnit);
        final ZonedDateTime next = truncated.plus(1, rolloverUnit);

        this.rolloverUnit = rolloverUnit;
        this.currentMillis = truncated.toInstant().toEpochMilli();
        this.nextMillis = next.toInstant().toEpochMilli();
        this.next = new AtomicReference<>(next);

    }

    /**
     * @return current rollover timestamp
     */
    @Override
    public final long current() {
        return currentMillis;
    }

    /**
     * @return next rollover timestamp
     */
    @Override
    public final long next() {
        return nextMillis;
    }

    /**
     * Rolls current and next timestamps by 1 {@code rolloverUnit} forward
     */
    @Override
    public final void rollover() {

        final ZonedDateTime nextDateTime = next.get().plus(1, rolloverUnit);
        next.set(nextDateTime);
        currentMillis = nextMillis;
        nextMillis = nextDateTime.toInstant().toEpochMilli();

    }

}
