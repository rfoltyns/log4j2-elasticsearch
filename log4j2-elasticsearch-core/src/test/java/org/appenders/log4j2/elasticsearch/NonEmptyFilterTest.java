package org.appenders.log4j2.elasticsearch;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NonEmptyFilterTest {

    @Test
    public void shouldExcludeNullValue() {

        // given
        NonEmptyFilter filter = new NonEmptyFilter();

        // when
        boolean result = filter.isIncluded(UUID.randomUUID().toString(), null);

        // then
        assertFalse(result);

    }

    @Test
    public void shouldExcludeNonNullValueWithZeroLength() {

        // given
        NonEmptyFilter filter = new NonEmptyFilter();

        // when
        boolean result = filter.isIncluded(UUID.randomUUID().toString(), "");

        // then
        assertFalse(result);

    }

    @Test
    public void shouldIncludeNonNullValueWithNonZeroLength() {

        // given
        NonEmptyFilter filter = new NonEmptyFilter();

        // when
        boolean result = filter.isIncluded(UUID.randomUUID().toString(), " ");

        // then
        assertTrue(result);

    }

}