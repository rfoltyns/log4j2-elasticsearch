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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RollingIndexNamePluginTest {

    private static final String TEST_INDEX_NAME = "testIndexName";
    private static final String DATE_PATTERN_WITH_MINUTES = "yyyy-MM-dd-HH.mm";
    private static final ZoneId TEST_TIME_ZONE = ZoneId.of(RollingIndexNamePlugin.Builder.DEFAULT_TIME_ZONE);
    private static final long DEFAULT_TEST_TIME_IN_MILLIS = getTestTimeInMillis();
    private static final String TEST_SEPARATOR = ".";

    private static long getTestTimeInMillis() {
        return LocalDateTime.of(2021, 11, 30, 23, 54, 0, 0)
                .atZone(ZoneId.of(RollingIndexNamePlugin.Builder.DEFAULT_TIME_ZONE))
                .toInstant().toEpochMilli();
    }

    public static RollingIndexNamePlugin.Builder createRollingIndexNamePluginBuilder() {

        final RollingIndexNamePlugin.Builder builder = spy(RollingIndexNamePlugin.newBuilder())
                .withIndexName(TEST_INDEX_NAME)
                .withPattern(DATE_PATTERN_WITH_MINUTES)
                .withTimeZone(TEST_TIME_ZONE.getId());

        when(builder.getInitialTimestamp()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        return builder;

    }

    @Test
    public void startsWithoutExceptionsIfSetupIsCorrect() {

        // when
        final IndexNameFormatter<LogEvent> formatter = createRollingIndexNamePluginBuilder().build();

        // then
        assertNotNull(formatter);

    }

    @Test
    public void builderThrowsWhenIndexNameIsNull() {

        // when
        final RollingIndexNamePlugin.Builder builder = createRollingIndexNamePluginBuilder()
                .withIndexName(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No indexName provided for RollingIndexName"));

    }


    @Test
    public void builderThrowsWhenPatternIsNull() {

        // when
        final RollingIndexNamePlugin.Builder builder = createRollingIndexNamePluginBuilder()
                .withPattern(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No pattern provided for RollingIndexName"));

    }

    @Test
    public void returnsFormattedIndexNameDefaultSeparator() {

        // given
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final IndexNameFormatter<LogEvent> formatter = createRollingIndexNamePluginBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent);

        // then
        assertEquals("testIndexName-2021-11-30-23.54", formattedIndexName);

    }

    @Test
    public void returnsFormattedIndexNameWithSeparator() {

        // given
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);
        final RollingIndexNamePlugin formatter = createRollingIndexNamePluginBuilder()
                .withSeparator(TEST_SEPARATOR)
                .build();

        // when
        final String formattedIndexName = formatter.format(logEvent);

        // then
        assertEquals("testIndexName.2021-11-30-23.54", formattedIndexName);

    }

    @Test
    public void returnsCurrentTimeIfEventTimeIsBeforeRolloverTime() {

        // given
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS);

        final IndexNameFormatter<LogEvent> formatter = createRollingIndexNamePluginBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent);

        // then
        assertEquals("testIndexName-2021-11-30-23.54", formattedIndexName);

    }

    @Test
    public void returnsNextRolloverTimeIfEventTimeIsAfterRolloverTime() {

        // given
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS + TimeUnit.HOURS.toMillis(1));

        final IndexNameFormatter<LogEvent> formatter = createRollingIndexNamePluginBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent);

        // then
        assertEquals("testIndexName-2021-12-01-00.54", formattedIndexName);

    }

    @Test
    public void returnsPreviousRolloverTimeIfEventTimeIsBeforeCurrentTime() {

        // given
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(DEFAULT_TEST_TIME_IN_MILLIS - TimeUnit.HOURS.toMillis(1));

        final IndexNameFormatter<LogEvent> formatter = createRollingIndexNamePluginBuilder().build();

        // when
        final String formattedIndexName = formatter.format(logEvent);

        // then
        assertEquals("testIndexName-2021-11-30-22.54", formattedIndexName);

    }

}
