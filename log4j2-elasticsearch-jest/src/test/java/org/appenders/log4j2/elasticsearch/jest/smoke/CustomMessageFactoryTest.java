package org.appenders.log4j2.elasticsearch.jest.smoke;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
@Ignore
public class CustomMessageFactoryTest extends SmokeTest {

    @BeforeClass
    public static void beforeClass() {
        // If true or Message is annotated with @AsynchronouslyFormattable
        // LogObject will be serialized in scope of Appender (Disruptor consumer thread)
        // otherwise in Logger
        System.setProperty("log4j.format.msg.async", "true");
    }

    @Test
    public void messageFactoryTest() {

        createLoggerProgrammatically(createElasticsearchAppenderBuilder(true));

        ObjectMapper objectMapper = configuredMapper();
        Logger logger = LogManager.getLogger(defaultLoggerName,
                new SerializedMessageFactory(objectMapper));

        logger.info(new LogObject("Hello, World!"));

    }

    private ObjectMapper configuredMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    private static class LogObject {

        private Long timeStamp;
        private final String userLog;

        public LogObject(String userLog) {
            this.timeStamp = System.currentTimeMillis();
            this.userLog = userLog;
        }

        public String getUserLog() {
            return userLog;
        }

    }

}
