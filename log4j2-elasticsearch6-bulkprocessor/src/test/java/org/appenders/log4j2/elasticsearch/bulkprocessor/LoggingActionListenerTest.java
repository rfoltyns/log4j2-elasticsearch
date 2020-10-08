package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.junit.After;
import org.junit.Test;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoggingActionListenerTest {

    @After
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void logsOnResponse() {

        // given
        Logger logger = mockTestLogger();

        String expectedActionName = UUID.randomUUID().toString();

        LoggingActionListener<AcknowledgedResponse> listener = new LoggingActionListener<>(expectedActionName);

        AcknowledgedResponse response = mock(AcknowledgedResponse.class);

        // when
        listener.onResponse(response);

        // then
        verify(logger).info("{}: success", expectedActionName);

    }

    @Test
    public void logsOnFailure() {

        // given
        Logger logger = mockTestLogger();

        String expectedActionName = UUID.randomUUID().toString();

        LoggingActionListener<AcknowledgedResponse> listener = new LoggingActionListener<>(expectedActionName);

        Exception testException = new Exception("test exception");

        // when
        listener.onFailure(testException);

        // then
        verify(logger).error("{}: failure", expectedActionName, testException);

    }

}