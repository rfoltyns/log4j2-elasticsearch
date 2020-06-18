package org.appenders.log4j2.elasticsearch.smoke;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.junit.Test;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static org.appenders.core.util.PropertiesUtil.getInt;

public abstract class SmokeTestBase {

    public static final String DEFAULT_APPENDER_NAME = "elasticsearchAppender";
    public static final String DEFAULT_LOGGER_NAME = "elasticsearch";
    public static final Random RANDOM = new Random();

    private final AtomicInteger localCounter = new AtomicInteger();

    // TODO: expose all via system properties
    public static final int INITIAL_SLEEP_PER_THREAD = 10;
    public static final int MILLIS_BEFORE_SHUTDOWN = 60000;
    public static final int MILLIS_AFTER_SHUTDOWN = 60000;
    public static final int NUMBER_OF_PRODUCERS = 100;
    public static final int LOG_SIZE = 300;
    protected boolean secure = false;
    private final AtomicInteger numberOfLogs = new AtomicInteger(1000000);

    public abstract ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured);

    protected Function<Configuration, AsyncLoggerConfigDelegate> createAsyncLoggerConfigDelegateProvider() {
        return Configuration::getAsyncLoggerConfigDelegate;
    }

    protected String createLog() {
        byte[] bytes = new byte[LOG_SIZE];
        RANDOM.nextBytes(bytes);

        return new String(bytes);
    }

    public final void createLoggerProgrammatically(Supplier<ElasticsearchAppender.Builder> appenderBuilder, Function<Configuration, AsyncLoggerConfigDelegate> delegateSupplier) {

        LoggerContext ctx = LoggerContext.getContext(false);

        final Configuration config = ctx.getConfiguration();

        Appender appender = appenderBuilder.get().build();
        appender.start();

        AppenderRef ref = AppenderRef.createAppenderRef(DEFAULT_APPENDER_NAME, Level.INFO, null);
        AppenderRef[] refs = new AppenderRef[] {ref};

        // set up disruptor forcefully
        ((LifeCycle)delegateSupplier.apply(config)).start();

        AsyncLoggerConfig loggerConfig = (AsyncLoggerConfig) AsyncLoggerConfig.createLogger(false, Level.INFO, DEFAULT_LOGGER_NAME,
                "false", refs, null, config, null );

        loggerConfig.addAppender(appender, Level.INFO, null);

        config.addAppender(appender);
        config.addLogger(DEFAULT_LOGGER_NAME, loggerConfig);

    }

    @Test
    public void publicDocsExampleTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(false, false, secure),
                createAsyncLoggerConfigDelegateProvider());

        String loggerThatReferencesElasticsearchAppender = "elasticsearch";
        Logger log = LogManager.getLogger(loggerThatReferencesElasticsearchAppender);
        log.info("Hello, World!");
        sleep(5000);
    }

    @Test
    public void programmaticConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("log4j2.garbagefreeThreadContextMap", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", "16384");
        System.setProperty("AsyncLoggerConfig.RingBufferSize", "16384");
        System.setProperty("AsyncLogger.WaitStrategy", "sleep");

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(false, false, secure),
                createAsyncLoggerConfigDelegateProvider());

        final String log = createLog();
        Logger logger = LogManager.getLogger(DEFAULT_LOGGER_NAME);
        indexLogs(logger, null, NUMBER_OF_PRODUCERS, () -> log);
    }

    @Test
    public void programmaticBufferedConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("log4j2.garbagefreeThreadContextMap", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        System.setProperty("AsyncLogger.RingBufferSize", "16384");
        System.setProperty("AsyncLogger.WaitStrategy", "sleep");

        System.setProperty("AsyncLoggerConfig.RingBufferSize", "16384");
        System.setProperty("AsyncLoggerConfig.WaitStrategy", "sleep");

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(false, true, secure),
                createAsyncLoggerConfigDelegateProvider());

        Logger logger = LogManager.getLogger(DEFAULT_LOGGER_NAME);

        final String log = createLog();
        indexLogs(logger, null, NUMBER_OF_PRODUCERS, () -> log);
    }

    @Test
    public void fileOutputTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-file.xml");
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", "16384");

        Logger logger = LogManager.getLogger("file");

        final String log = createLog();
        indexLogs(logger, null, NUMBER_OF_PRODUCERS, () -> log);
    }

    @Test
    public void xmlConfigTest() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger();

        // let's test https://github.com/rfoltyns/log4j2-elasticsearch/issues/15
        URI uri = URI.create("log4j2.xml");
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getFactory().getContext(
                LogManager.class.getName(),
                SmokeTestBase.class.getClassLoader(),
                null,
                false,
                uri,
                null
        );

        context.setConfigLocation(uri);

        Logger logger = LogManager.getLogger(DEFAULT_LOGGER_NAME);
        indexLogs(logger, null, NUMBER_OF_PRODUCERS, () -> "Message " + counter.incrementAndGet());
    }

    @Test
    public void propertiesConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2.properties");
        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger("elasticsearch");
        indexLogs(logger, null, NUMBER_OF_PRODUCERS, () -> "Message " + counter.incrementAndGet());
    }

    <T> void indexLogs(Logger logger, Marker marker, int numberOfProducers, Supplier<T> logSupplier) throws InterruptedException {

        final AtomicInteger sleepTime = new AtomicInteger(INITIAL_SLEEP_PER_THREAD);
        CountDownLatch latch = new CountDownLatch(numberOfProducers);

        int numberOfLogsToDeliver = numberOfLogs.get();
        AtomicInteger totalCounter = new AtomicInteger();
        for (int thIndex = 0; thIndex < numberOfProducers; thIndex++) {
            new Thread(() -> {

                for (;numberOfLogs.decrementAndGet() >= 0; totalCounter.incrementAndGet()) {
                    logger.info(marker, logSupplier.get());
                    localCounter.incrementAndGet();
                    try {
                        sleep(sleepTime.get());
                    } catch (InterruptedException e) {
                        interrupted();
                    }

                }
                latch.countDown();
            }).start();
        }

        final int limitPerSec = getInt("smokeTest.limitPerSec", 10000);
        while (latch.getCount() != 0) {
            sleep(1000);
            int count = localCounter.getAndSet(0);
            if (count > limitPerSec && sleepTime.get() != 1) {
                sleepTime.incrementAndGet();
            } else if (sleepTime.get() > 1) {
                sleepTime.decrementAndGet();
            }

            String stats = String.format(
                    "Sleep millis per thread: %d, Current throughput: %d; Progress: %d/%d",
                    sleepTime.get(),
                    count,
                    totalCounter.get(),
                    numberOfLogsToDeliver);

            System.out.println(stats);
        }

        sleep(MILLIS_BEFORE_SHUTDOWN);

        System.out.println("Shutting down");
        LogManager.shutdown();

        sleep(MILLIS_AFTER_SHUTDOWN);

    }

}
