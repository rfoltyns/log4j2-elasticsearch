package org.appenders.core.logging;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class InternalLoggingTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void allowsToSetLogger() {

        // given
        Logger expectedLogger = mock(Logger.class);

        // when
        InternalLogging.setLogger(expectedLogger);

        // then
        assertEquals(expectedLogger, InternalLogging.getLogger());

    }

    @Test
    public void returnsDefaultImplWhenLoggerNotSetAndNullLoggerIsAllowed() {

        // given
        System.setProperty(InternalLogging.THROW_ON_NULL_LOGGER, "false");

        InternalLogging.setLogger(null);

        // when
        Logger logger = InternalLogging.getLogger();

        // then
        assertNotNull(logger);
        assertTrue(logger instanceof Log4j2StatusLoggerWrapper);

    }

    @Test
    public void throwsWhenLoggerNotSetAndNullLoggerNotAllowed() {

        // given
        System.setProperty(InternalLogging.THROW_ON_NULL_LOGGER, "true");

        InternalLogging.setLogger(null);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Logger cannot be null. Set Logger instance with InternalLogging.setLogger()()");

        // when
        InternalLogging.getLogger();

    }

    public static Logger mockTestLogger() {
        Logger mockedLogger = mock(Logger.class);
        InternalLogging.setLogger(mockedLogger);
        return mockedLogger;
    }

}