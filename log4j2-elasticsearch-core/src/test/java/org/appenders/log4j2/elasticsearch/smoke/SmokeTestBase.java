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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicy;
import org.appenders.log4j2.elasticsearch.failover.KeySequenceSelector;
import org.appenders.log4j2.elasticsearch.failover.Log4j2SingleKeySequenceSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Thread.sleep;
import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.util.PropertiesUtil.getInt;
import static org.appenders.log4j2.elasticsearch.failover.ChronicleMapUtil.resolveChronicleMapFilePath;

public abstract class SmokeTestBase {

    public static final long ONE_SECOND_MILLIS = 1000;
    public static final long ONE_SECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(ONE_SECOND_MILLIS);

    final TestConfig config = new TestConfig();

    private final Random random = new Random();
    private final AtomicInteger localCounter = new AtomicInteger();
    private final AtomicInteger totalCounter = new AtomicInteger();

    public abstract ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured);

    protected final TestConfig getConfig() {
        return config;
    }

    protected TestConfig configure(final TestConfig target) {
        return target.add("limitTotal", getInt("smokeTest.limitTotal", 10))
            .add("limitPerSec", getInt("smokeTest.limitPerSec", 10000))
            .add("pooled", Boolean.parseBoolean(System.getProperty("smokeTest.pooled", "true")))
            .add("secure", Boolean.parseBoolean(System.getProperty("smokeTest.secure", "false")))
            .add("logSizeInBytes", getInt("smokeTest.logSizeInBytes", 1))
            .add("lifecycleStopDelayMillis", getInt("smokeTest.lifecycleStopDelayMillis", 10000))
            .add("exitDelayMillis", getInt("smokeTest.exitDelayMillis", 10000))
            .add("numberOfProducers", getInt("smokeTest.noOfProducers", 10))
            .add("producerBatchSize", getInt("smokeTest.producerBatchSize", 100))
            .add("producerSleepMillis", getInt("smokeTest.initialProducerSleepMillis", 20))
            .add("loggerName", System.getProperty("smokeTest.loggerName", "elasticsearch-logger"))
            .add("appenderName", System.getProperty("smokeTest.appenderName", "elasticsearch-appender"))
            .add("singleThread", Boolean.parseBoolean(System.getProperty("smokeTest.singleThread", "true")))
            .add("chroniclemap.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.chroniclemap.enabled", "false")));
    }

    protected Function<Configuration, AsyncLoggerConfigDelegate> createAsyncLoggerConfigDelegateProvider() {
        return Configuration::getAsyncLoggerConfigDelegate;
    }

    protected String createLog() {
        final int logSizeInBytes = getConfig().getProperty("logSizeInBytes", Integer.class);
        byte[] bytes = new byte[logSizeInBytes];
        random.nextBytes(bytes);

        return new String(bytes);
    }

    public final void createLoggerProgrammatically(Supplier<ElasticsearchAppender.Builder> appenderBuilder) {

        LoggerContext ctx = LoggerContext.getContext(false);

        Appender appender = appenderBuilder.get().build();
        appender.start();

        final String loggerName = getConfig().getProperty("loggerName", String.class);
        final LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig(loggerName);
        loggerConfig.addAppender(appender, Level.INFO, null);

        ctx.updateLoggers();

    }

    @BeforeEach
    public void beforeEach() {
        configure(config);
    }

    @Test
    public void publicDocsExampleTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(
                        false,
                        getConfig().getProperty("pooled", Boolean.class),
                        getConfig().getProperty("secure", Boolean.class)));

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
        System.setProperty("log4j2.garbagefree.threadContextMap", "true");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        System.setProperty("AsyncLogger.RingBufferSize", "16384");
        System.setProperty("AsyncLogger.WaitStrategy", "sleep");

        System.setProperty("AsyncLoggerConfig.RingBufferSize", "16384");
        System.setProperty("AsyncLoggerConfig.WaitStrategy", "sleep");

        setLogger(new Log4j2Delegate(LogManager.getLogger("org.appenders.logging")));

        createLoggerProgrammatically(
                () -> createElasticsearchAppenderBuilder(
                        false,
                        getConfig().getProperty("pooled", Boolean.class),
                        getConfig().getProperty("secure", Boolean.class)));

        Logger logger = LogManager.getLogger(getConfig().getProperty("loggerName", String.class));

        final String log = createLog();
        indexLogs(logger, null, getConfig().getProperty("numberOfProducers", Integer.class), () -> log);
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
        indexLogs(logger, null, getConfig().getProperty("numberOfProducers", Integer.class), () -> log);
    }

    @Test
    public void xmlConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2.xml");

        Logger logger = LogManager.getLogger(getConfig().getProperty("loggerName", String.class));
        final String log = createLog();
        indexLogs(logger, null, getConfig().getProperty("numberOfProducers", Integer.class), () -> log);
    }

    @Test
    public void propertiesConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2.properties");
        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger(getConfig().getProperty("loggerName", String.class));
        indexLogs(logger, null, getConfig().getProperty("numberOfProducers", Integer.class), () -> "Message " + counter.incrementAndGet());

    }

    protected FailoverPolicy resolveFailoverPolicy() {

        if (!getConfig().getProperty("chroniclemap.enabled", Boolean.class)) {
            return new NoopFailoverPolicy.Builder().build();
        }

        KeySequenceSelector keySequenceSelector =
                new Log4j2SingleKeySequenceSelector.Builder()
                        .withSequenceId(getConfig().getProperty("chroniclemap.sequenceId", Integer.class))
                        .build();

        return new ChronicleMapRetryFailoverPolicy.Builder()
                .withKeySequenceSelector(keySequenceSelector)
                .withFileName(resolveChronicleMapFilePath(getConfig().getProperty("indexName", String.class) + ".chronicleMap"))
                .withNumberOfEntries(1000000)
                .withAverageValueSize(2048)
                .withBatchSize(5000)
                .withRetryDelay(4000)
                .withMonitored(true)
                .withMonitorTaskInterval(1000)
                .build();
    }

    <T> void indexLogs(Logger logger, Marker marker, int numberOfProducers, Supplier<T> logSupplier) throws InterruptedException {

        final AtomicInteger itemsLeft = new AtomicInteger(getConfig().getProperty("limitTotal", Integer.class));
        int itemsExpected = itemsLeft.get();
        final int producerSleepMillis = getConfig().getProperty("producerSleepMillis", Integer.class);
        final int producerBatchSize = getConfig().getProperty("producerBatchSize", Integer.class);
        final int limitPerSec = getConfig().getProperty("limitPerSec", Integer.class);

        final Supplier<Boolean> shouldContinue = () -> itemsLeft.get() > 0;
        final LoadTask loadTask = new LoadTask() {
            public void generateLoad(final int size, final long sleepMillis) {
                logMicroBatch(size, itemsLeft, logger, marker, logSupplier);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(sleepMillis));
            }
        };

        final Queue<ConditionalLoop> producers = new ConcurrentLinkedQueue<>();
        final LoadGenerator loadGenerator = new LoadGenerator(producers, new LoadProducerFactory(loadTask, shouldContinue), new RampUpPolicy(limitPerSec, 0.1), numberOfProducers, producerSleepMillis, producerBatchSize);
        final long projectedCountPerMillis = loadGenerator.projectedCountPerMillis(ONE_SECOND_MILLIS);
        loadGenerator.reconfigure(projectedCountPerMillis / (double) limitPerSec, limitPerSec);

        loadGenerator.start();

        final AtomicInteger noProgressRecorded = new AtomicInteger();
        while (shouldContinue.get()) {

            LockSupport.parkNanos(ONE_SECOND_NANOS);

            final int count = localCounter.getAndSet(0);

            final double currentLoad = (double) count / (double) limitPerSec;
            String stats = String.format(
                    "Sleep millis: %d, Producers: %d, Micro batch size: %d, Throughput: %d/s; Test Load: %.3f, Progress: %d/%d",
                    loadGenerator.getProducerSleepMillis(),
                    loadGenerator.getNumberOfProducers(),
                    loadGenerator.getProducerBatchSize(),
                    count,
                    currentLoad,
                    totalCounter.get(),
                    itemsExpected);

            System.out.println(stats);

            // On finished or stuck/broken
            if (count == 0) {
                noProgressRecorded.incrementAndGet();
            }

            // Fuse
            if (noProgressRecorded.get() > 5) {
                break;
            }

            loadGenerator.reconfigure(currentLoad, limitPerSec);

        }

        sleep(getConfig().getProperty("lifecycleStopDelayMillis", Integer.class));

        System.out.println("Shutting down");
        LogManager.shutdown();

        sleep(getConfig().getProperty("exitDelayMillis", Integer.class));

    }

    private <T> void logMicroBatch(final int batchSize, final AtomicInteger limitTotal, Logger logger, Marker marker, Supplier<T> logSupplier) {
        for (int i = 0; i < batchSize && limitTotal.decrementAndGet() >= 0; i++) {
            logger.info(marker, logSupplier.get());
            localCounter.incrementAndGet();
            totalCounter.incrementAndGet();
        }
    }

    private static class LoadGenerator {

        private final Queue<ConditionalLoop> producers;
        private final LoadProducerFactory producerFactory;
        private final AtomicInteger numberOfProducers;
        private final AtomicInteger producerSleepMillis;
        private final AtomicInteger producerBatchSize;
        private final ThrottlingPolicy throttlingPolicy;

        public LoadGenerator(final Queue<ConditionalLoop> initialProducers,
                             final LoadProducerFactory producerFactory,
                             final ThrottlingPolicy throttlingPolicy,
                             final int numberOfProducers,
                             final int producerSleepMillis,
                             final int producerBatchSize) {
            this.producers = initialProducers;
            this.producerFactory = producerFactory;
            this.throttlingPolicy = throttlingPolicy;
            this.numberOfProducers = new AtomicInteger(numberOfProducers);
            this.producerSleepMillis = new AtomicInteger(producerSleepMillis);
            this.producerBatchSize = new AtomicInteger(producerBatchSize);
        }

        private int reconfigure(final double currentLoad, final int limitPerSec) {

            if (currentLoad <= 0) {
                return 0;
            }

            final int sleepMillis = producerSleepMillis.get();

            double targetLoad = throttlingPolicy.throttle(currentLoad, limitPerSec);

            final int newSleepMillis = (int) (sleepMillis * currentLoad);
            producerSleepMillis.set(Math.max(newSleepMillis, 5)); // 5ms for now. That's 200 runs per second.

            while (projectedLoad(limitPerSec) > 1.05d) {
                producerSleepMillis.incrementAndGet();
            }

            if (projectedLoad(limitPerSec) < 1.0) {

                final int batchSize = producerBatchSize.get();
                final int producers = numberOfProducers.get();

                if (batchSize != 100) { // 100 logs per call at this point is 20000/sec per producer at targetLoad == 1
                    final int newBatchSize = (int) (batchSize / targetLoad);
                    producerBatchSize.set(Math.min(newBatchSize, 100));
                } else if (producers < 32) {

                    long projectedRate = projectedCountPerMillis(1000);

                    final int moreProducers = (int) (limitPerSec / projectedRate);
                    numberOfProducers.addAndGet(moreProducers);

                    final int lessProducers = (int) (projectedRate / limitPerSec);
                    numberOfProducers.addAndGet(-lessProducers);
                }

            }

            while (producers.size() > numberOfProducers.get()) {
                producers.remove().stopLoop();
            }
            while (producers.size() < numberOfProducers.get()) {
                producers.add(createInternal());
            }
            if (currentLoad - projectedLoad(limitPerSec) > 0.05) {
                System.out.println("Stopping over-producing loop");
                producers.remove().stopLoop();
            }

            return numberOfProducers.get();

        }

        private double projectedLoad(int limitPerSec) {
            return projectedCountPerMillis(1000) / (double) limitPerSec;
        }

        public long projectedCountPerMillis(long millis) {
            return  (millis / producerSleepMillis.get()) * numberOfProducers.get() * producerBatchSize.get();
        }

        public int getNumberOfProducers() {
            return producers.size();
        }

        public int getProducerBatchSize() {
            return producerBatchSize.get();
        }

        public int getProducerSleepMillis() {
            return producerSleepMillis.get();
        }

        public void start() {

            while (producers.size() < getNumberOfProducers()) {
                producers.add(createInternal());
            }

        }

        private ConditionalLoop createInternal() {
            return producerFactory.createProducer(true, this::getProducerBatchSize, this::getProducerSleepMillis);
        }

    }

    private static class LoadProducerFactory {

        private final LoadTask task;
        private final Supplier<Boolean> condition;

        private LoadProducerFactory(final LoadTask task, final Supplier<Boolean> condition) {
            this.task = task;
            this.condition = condition;
        }

        public ConditionalLoop createProducer(final boolean startOnCreate, final Supplier<Integer> sizeSupplier, final Supplier<Integer> sleepMillisSupplier) {

            final Runnable generateLoad = () -> task.generateLoad(sizeSupplier.get(), sleepMillisSupplier.get());
            final ConditionalLoop loop = new ConditionalLoop(generateLoad, condition);

            if (startOnCreate) {
                loop.start();
            }

            return loop;

        }

    }

    private static class ConditionalLoop extends Thread {

        private final Runnable runnable;
        private final Supplier<Boolean> condition;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public ConditionalLoop(final Runnable runnable, final Supplier<Boolean> condition) {
            this.runnable = runnable;
            this.condition = condition;
        }

        @Override
        public void run() {

            while (running.get() && condition.get()) {
                runnable.run();
            }
            stopLoop();

        }

        public void stopLoop() {
            running.compareAndSet(true, false);
        }

    }

    private abstract static class LoadTask {

        public LoadTask() {
        }

        public abstract void generateLoad(final int size, final long sleep);

    }

    private interface ThrottlingPolicy {
        double throttle(final double currentLoad, final int limitPerSec);
    }

    private static class RampUpPolicy implements ThrottlingPolicy {

        private final int max;
        private final double percentOnResize;

        public RampUpPolicy(final int max, final double resizeFactor) {

            if (resizeFactor > 1) {
                throw new IllegalArgumentException("Resize factor too aggressive for ramp up policy: " + resizeFactor);
            }

            this.max = max;
            this.percentOnResize = max * resizeFactor / max;

        }

        @Override
        public double throttle(final double currentLoad, final int limitPerSec) {

            if (currentLoad == 0.0d) {
                return currentLoad;
            }

            if (currentLoad * limitPerSec > max && currentLoad > 1.05d) {
                return 1.0d + percentOnResize;
            } else if (currentLoad < 0.95d){
                return currentLoad + (currentLoad * percentOnResize);
            } else {
                return 1.0d;
            }

        }

    }

}
