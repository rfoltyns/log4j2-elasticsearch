package org.appenders.log4j2.elasticsearch;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormatterWrapper implements MillisFormatter {

    private final DateTimeFormatter formatter;
    private final int bufferSize;

    protected DateTimeFormatterWrapper(final DateTimeFormatter formatter,
                                       final int bufferSize) {
        this.formatter = formatter;
        this.bufferSize = bufferSize;
    }

    @Override
    public String format(final long millis) {
        final StringBuilder buffer = new StringBuilder(bufferSize);
        formatter.formatTo(Instant.ofEpochMilli(millis), buffer);
        return buffer.toString();
    }

}
