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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Stream;

/**
 * Rolling date-time pattern formatter. Caches current formatted value between rolls to minimize overhead.
 * <p>
 * Format: {@code <prefix><separator><datePattern>}
 * <p>
 * Thread-safe
 */
public class RollingMillisFormatter implements MillisFormatter {


    private final RollingTimestamps timestamps;

    /**
     * Actual formatter
     */
    private final MillisFormatter formatter;

    private final StampedLock lock = new StampedLock();
    private final AtomicReference<String> currentName;

    protected RollingMillisFormatter(
            final MillisFormatter formatter,
            final RollingTimestamps timestamps) {
        this.formatter = formatter;
        this.timestamps = timestamps;
        this.currentName = new AtomicReference<>(formatter.format(timestamps.current()));
    }

    public final long getNextRolloverTime() {
        return timestamps.next();
    }

    /**
     * Compares given timestamp with current {@link RollingTimestamps} state: <i>current</i> and <i>next</i>, rolls forward if needed and returns result with configured format.
     * <p>
     * This method will not allocate unless concurrent rollover is pending (usually ~1 microsecond or less) or given timestamp is lower than current rollover start time.
     *
     * @param millis timestamp to format
     *
     * @return Formatted timestamp truncated to {@code rolloverUnit} with {@code <prefix><separator><datePattern>} format
     */
    @Override
    public final String format(final long millis) {

        // handle "old" events - after rollover timestamps.current() is ahead already
        if (millis < timestamps.current()) {
            return formatter.format(millis);
        }

        long writeStamp = 0;
        final long stamp = lock.tryOptimisticRead();
        try {

            // rollover
            writeStamp = rolloverIfNeeded(millis, stamp);

            final String currentName = this.currentName.get();
            final long current = timestamps.current();

            // happy path - no rollover or right after rollover
            if (lock.validate(writeStamp) && millis >= current) {
                return currentName;
            }

        } finally {
            if (stamp != writeStamp) {
                lock.unlock(writeStamp);
            }
        }

        // fail-safe for pending rollover
        return formatter.format(millis);

    }

    /**
     * Rolls if {@code millis} is higher or equal to next rollover timestamp
     *
     * @param millis timestamp relative to <i>current</i> and <i>next</i>
     * @param stamp read lock stamp
     * @return write lock stamp if rolled over, read lock stamp otherwise
     */
    private long rolloverIfNeeded(long millis, long stamp) {

        long result;
        if (millis >= timestamps.next() && (result = lock.tryConvertToWriteLock(stamp)) != 0) {

            while (millis >= timestamps.next()) {
                timestamps.rollover();
            }

            final String newName = formatter.format(timestamps.current());
            this.currentName.set(newName);

            return result;
        } else {
            return stamp;
        }

    }


    public static class Builder {

        public static final String DEFAULT_TIME_ZONE = ZoneId.systemDefault().getId();

        protected long initialTimestamp = System.currentTimeMillis();
        protected String timeZone = DEFAULT_TIME_ZONE;
        protected String prefix;
        protected String separator = "";
        protected String pattern;

        public RollingMillisFormatter build() {

            validate();

            final MillisFormatter formatter = createFormatter();
            final RollingTimestamps rollingTimestamps = createRollingTimestamps();

            return build(formatter, rollingTimestamps);

        }

        protected RollingMillisFormatter build(
                final MillisFormatter patternFormatter,
                final RollingTimestamps rollingTimestamps) {
            return new RollingMillisFormatter(
                    patternFormatter,
                    rollingTimestamps);
        }

        public MillisFormatter createFormatter() {

            final DateTimeFormatter dateTimeFormatter = appendFields(new DateTimeFormatterBuilder())
                    .toFormatter()
                    .withZone(ZoneId.of(timeZone));

            return new DateTimeFormatterWrapper(dateTimeFormatter, getBufferSize());

        }

        public RollingTimestamps createRollingTimestamps() {
            return new ChronoUnitRollingTimestamps(initialTimestamp, RolloverUtil.getMinimumUnit(pattern), ZoneId.of(timeZone));
        }

        int getBufferSize() {

            return Stream.of(prefix, separator, pattern)
                    .filter(Objects::nonNull)
                    .map(String::length)
                    .mapToInt(Integer::intValue)
                    .sum();

        }

        private DateTimeFormatterBuilder appendFields(final DateTimeFormatterBuilder dateTimeFormatterBuilder) {

            if (prefix != null) {
                dateTimeFormatterBuilder.appendLiteral(prefix + separator);
            }

            dateTimeFormatterBuilder.appendPattern(pattern);

            return dateTimeFormatterBuilder;

        }

        public void validate() {

            if (pattern == null) {
                throw new IllegalArgumentException("No pattern provided for " + getClass().getSimpleName());
            }

        }

        public Builder withInitialTimestamp(final long initialTimestamp) {
            this.initialTimestamp = initialTimestamp;
            return this;
        }

        public Builder withPrefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withPattern(final String dateTimePattern) {
            this.pattern = dateTimePattern;
            return this;
        }

        public Builder withSeparator(final String separator) {
            this.separator = separator;
            return this;
        }

        public Builder withTimeZone(final String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

    }

}
