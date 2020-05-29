package org.appenders.log4j2.elasticsearch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;

public class NonEmptyFilterPluginTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        NonEmptyFilterPlugin.Builder builder = NonEmptyFilterPlugin.newBuilder();

        // when
        NonEmptyFilterPlugin filter = builder.build();

        // then
        assertNotNull(filter);

    }

}