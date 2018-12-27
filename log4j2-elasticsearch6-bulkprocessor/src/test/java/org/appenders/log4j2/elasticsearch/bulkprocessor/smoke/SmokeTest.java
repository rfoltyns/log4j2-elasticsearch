package org.appenders.log4j2.elasticsearch.bulkprocessor.smoke;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.bulkprocessor.BasicCredentials;
import org.appenders.log4j2.elasticsearch.bulkprocessor.BulkProcessorObjectFactory;
import org.appenders.log4j2.elasticsearch.bulkprocessor.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.bulkprocessor.XPackAuth;
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

        PEMCertInfo certInfo = PEMCertInfo.newBuilder()
                .withKeyPath(System.getProperty("pemCertInfo.keyPath"))
                .withKeyPassphrase(System.getProperty("pemCertInfo.keyPassphrase"))
                .withClientCertPath(System.getProperty("pemCertInfo.clientCertPath"))
                .withCaPath(System.getProperty("pemCertInfo.caPath"))
                .build();

        BasicCredentials credentials = BasicCredentials.newBuilder()
                .withUsername("admin")
                .withPassword("changeme")
                .build();

        XPackAuth auth = XPackAuth.newBuilder()
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
                .withIndexName("log4j2_test_es6")
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
    }

}
