package org.appenders.core.logging;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LoggerTest {

    private static final String EXPECTED_MESSAGE = "Not implemented";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void errorThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.error("test");

    }

    @Test
    public void warnThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.warn("test");

    }

    @Test
    public void infoThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.info("test");

    }

    @Test
    public void debugThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.debug("test");

    }

    @Test
    public void traceThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.trace("test");

    }

}