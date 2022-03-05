package org.appenders.log4j2.elasticsearch;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeFormatterWrapperTest {

    @Test
    public void formatsMillisWithGivenFormatter() {

        // given
        final long millis = 1646511741015L;
        final String expectedFormattedMillis = "2022-03-05-20-22-21-015";
        final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd-HH-mm-ss-SSS").toFormatter().withZone(ZoneId.systemDefault());

        // when
        final DateTimeFormatterWrapper formatter = new DateTimeFormatterWrapper(dateTimeFormatter, 32);
        final String result = formatter.format(millis);

        // then
        assertEquals(expectedFormattedMillis, result);

    }

}