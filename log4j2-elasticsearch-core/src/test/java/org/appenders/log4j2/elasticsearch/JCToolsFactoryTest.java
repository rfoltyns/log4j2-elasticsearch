package org.appenders.log4j2.elasticsearch;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JCToolsFactoryTest {

    @Test
    public void throwsOnUnsupportedType() {

        // given
        final JCToolsFactory provider = new JCToolsFactory();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> provider.create("java.lang.Object"));

        // then
        assertThat(exception.getMessage(), containsString("Class not supported"));

    }

}