package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class NoopIndexNameFormatterTest {

    public static final String TEST_INDEX_NAME = "testIndexName";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void returnsIndexNameUnchanged() {

        // given
        NoopIndexNameFormatter.Builder builder = NoopIndexNameFormatter.newBuilder();
        builder.withIndexName(TEST_INDEX_NAME);
        NoopIndexNameFormatter formatter = builder.build();

        // when
        String formattedIndexName = formatter.format(Mockito.mock(LogEvent.class));

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);
    }

    @Test
    public void builderThrowsWhenNameIsNull() {

        // given
        NoopIndexNameFormatter.Builder builder = NoopIndexNameFormatter.newBuilder();

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("indexName");

        // when
        builder.build();
    }


}
