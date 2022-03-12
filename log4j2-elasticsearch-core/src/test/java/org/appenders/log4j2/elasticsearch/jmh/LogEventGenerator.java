package org.appenders.log4j2.elasticsearch.jmh;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.message.SimpleMessage;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class LogEventGenerator {

    private long counter = System.currentTimeMillis();

    private final MutableLogEvent logEvent;

    public LogEventGenerator(int size) {

        final byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);

        final MutableLogEvent mutableLogEvent = new MutableLogEvent();
        ((MutableInstant)mutableLogEvent.getInstant()).initFrom(new MutableInstant());

        mutableLogEvent.setLevel(Level.INFO);
        mutableLogEvent.setMessage(new SimpleMessage(new String(bytes, StandardCharsets.UTF_8)));
        mutableLogEvent.setThreadName(Thread.currentThread().getName());
        mutableLogEvent.setLoggerName("jmh");

        logEvent = mutableLogEvent;

    }

    public LogEvent next() {
        ((MutableInstant)logEvent.getInstant()).initFromEpochMilli(counter++, 0);
        return logEvent;
    }

}
