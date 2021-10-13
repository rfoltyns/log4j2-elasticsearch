package org.appenders.log4j2.elasticsearch.jmh;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.SimpleMessage;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class LogEventGenerator {

    private int counter;

    private final MutableInstant mutableInstant = new MutableInstant();
    private final Log4jLogEvent logEvent;

    public LogEventGenerator(int size) {
        final byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        logEvent = new Log4jLogEvent.Builder()
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(new String(bytes, StandardCharsets.UTF_8)))
                .setInstant(mutableInstant)
                .setThreadName(Thread.currentThread().getName())
                .setLoggerName("jmh")
                .build();
    }

    public LogEvent next() {
        mutableInstant.initFromEpochMilli(counter++, 0);
        return logEvent;
    }

}
