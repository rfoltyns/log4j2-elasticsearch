package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AppenderRefFailoverPolicyTest {

    @Test
    public void deliversToAppenderRef() {

        // given
        Appender appender = mock(Appender.class);
        when(appender.isStarted()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        String testAppenderRef = "testAppenderRef";
        when(configuration.getAppender(testAppenderRef)).thenReturn(appender);

        FailoverPolicy<String> failoverPolicy = createTestFailoverPolicy(testAppenderRef, configuration);

        String failedMessage = "test failed message";

        // when
        failoverPolicy.deliver(failedMessage);

        // then
        verify(appender, times(1)).append(any(LogEvent.class));
    }

    @Test
    public void resolvesAppenderRefOnlyOnce() {

        // given
        Appender appender = mock(Appender.class);
        when(appender.isStarted()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        String testAppenderRef = "testAppenderRef";
        when(configuration.getAppender(testAppenderRef)).thenReturn(appender);

        FailoverPolicy<String> failoverPolicy = createTestFailoverPolicy(testAppenderRef, configuration);

        String failedMessage = "test failed message";

        // when
        failoverPolicy.deliver(failedMessage);
        failoverPolicy.deliver(failedMessage);

        // then
        verify(configuration, times(1)).getAppender(anyString());
        verify(appender, times(2)).append(any(LogEvent.class));
    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionOnUnresolvedAppender() {

        // given
        Appender appender = mock(Appender.class);
        when(appender.isStarted()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        String testAppenderRef = "testAppenderRef";
        when(configuration.getAppender(testAppenderRef)).thenReturn(null);

        FailoverPolicy<String> failoverPolicy = createTestFailoverPolicy(testAppenderRef, configuration);

        String failedMessage = "test failed message";

        // when
        failoverPolicy.deliver(failedMessage);

    }
    private FailoverPolicy<String> createTestFailoverPolicy(String testAppenderRef, Configuration configuration) {
        AppenderRefFailoverPolicy.Builder builder = AppenderRefFailoverPolicy.newBuilder();
        builder.withAppenderRef(AppenderRef.createAppenderRef(
                testAppenderRef, Level.ALL, null));
        builder.withConfiguration(configuration);
        return builder.build();
    }
}
