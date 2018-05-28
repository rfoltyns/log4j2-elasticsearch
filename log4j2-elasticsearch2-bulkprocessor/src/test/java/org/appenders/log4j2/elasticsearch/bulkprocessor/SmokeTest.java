package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.LoggerContext;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

@Ignore
public class SmokeTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }

    @Test
    public void programmaticConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
        createLoggerProgramatically();

        Logger logger = LogManager.getLogger("elasticsearch");
        indexLogs(logger);
    }

    @Test
    public void xmlConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2.xml");

        Logger logger = LogManager.getLogger("elasticsearch");
        indexLogs(logger);
    }

    private static void createLoggerProgramatically() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        CertInfo certInfo = JKSCertInfo.newBuilder()
                .withKeystorePath(System.getProperty("jksCertInfo.keystorePath"))
                .withKeystorePassword(System.getProperty("jksCertInfo.keystorePassword"))
                .withTruststorePath(System.getProperty("jksCertInfo.truststorePath"))
                .withTruststorePassword(System.getProperty("jksCertInfo.truststorePassword"))
                .build();

        PlainCredentials credentials = PlainCredentials.newBuilder()
                .withUsername("admin")
                .withPassword("changeme")
                .build();

        ShieldAuth auth = ShieldAuth.newBuilder()
                .withCertInfo(certInfo)
                .withCredentials(credentials)
                .build();

        BulkProcessorObjectFactory bulkProcessorObjectFactory = BulkProcessorObjectFactory.newBuilder()
                .withServerUris("tcp://localhost:9300")
                .withAuth(auth)
                .build();

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(bulkProcessorObjectFactory)
                .withBatchSize(30000)
                .withDeliveryInterval(1000)
                .build();

        NoopIndexNameFormatter indexNameFormatter = NoopIndexNameFormatter.newBuilder()
                .withIndexName("log4j2_test_es2")
                .build();

        Appender appender = ElasticsearchAppender.newBuilder()
                .withName("elasticsearch")
                .withBatchDelivery(asyncBatchDelivery)
                .withIndexNameFormatter(indexNameFormatter)
                .withIgnoreExceptions(false)
                .build();

        appender.start();

        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef("elasticsearch", null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};

        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "org.apache.logging.log4j",
                "true", refs, null, config, null );

        loggerConfig.addAppender(appender, null, null);

        config.addLogger("elasticsearch", loggerConfig);

        ctx.updateLoggers();
    }

    private void indexLogs(Logger logger) throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(10);

        for (int thIndex = 0; thIndex < 100; thIndex++) {
            new Thread(() -> {
                for (int msgIndex = 0; msgIndex < 10000; msgIndex++) {
                    logger.info("Message " + counter.incrementAndGet());
                    try {
                        sleep(2);
                    } catch (InterruptedException e) {
                        interrupted();
                        e.printStackTrace();
                    }
                }
                latch.countDown();
            }).start();
        }

        while (latch.getCount() != 0) {
            sleep(1000);
            System.out.println("Added " + counter + " messages");
        }
        sleep(10000);
//        LogManager.shutdown();
    }

}
