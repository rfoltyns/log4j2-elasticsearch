package org.appenders.log4j2.elasticsearch.jest.smoke;

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




import io.searchbox.client.config.HttpClientConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.RollingIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.jest.BasicCredentials;
import org.appenders.log4j2.elasticsearch.jest.BufferedJestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.jest.XPackAuth;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

@Ignore
public class SmokeTest {

    public static final int BATCH_SIZE = 5000;
    public static final int INITIAL_ITEM_POOL_SIZE = 10000;
    public static final int INITIAL_ITEM_SIZE_IN_BYTES = 512;
    public static final int INITIAL_BATCH_POOL_SIZE = 2;
    protected String defaultLoggerName = "elasticsearch";

    private final AtomicInteger numberOfLogs = new AtomicInteger(10000000);
    private final AtomicInteger counter = new AtomicInteger();

    // TODO: expose via system property
    private boolean secure = false;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
    }

    @Test
    public void publicDocsExampleTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
        createLoggerProgrammatically(createElasticsearchAppenderBuilder(false, false, secure));
 
        String loggerThatReferencesElasticsearchAppender = "elasticsearch";
        Logger log = LogManager.getLogger(loggerThatReferencesElasticsearchAppender);
        log.info("Hello, World!");
        sleep(5000);
    }

    @Test
    public void programmaticConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
        createLoggerProgrammatically(createElasticsearchAppenderBuilder(false, false, secure));

        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger(defaultLoggerName);
        indexLogs(logger, null, 100, () -> "Message " + counter.incrementAndGet());
    }

    @Test
    public void programmaticBufferedConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
        createLoggerProgrammatically(createElasticsearchAppenderBuilder(false, true, secure));

        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger(defaultLoggerName);
        indexLogs(logger, null, 100, () -> "Message " + counter.incrementAndGet());
    }

    @Test
    public void fileOutputTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-file.xml");
        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger("file");
        indexLogs(logger, null, 100, () -> "Message " + counter.incrementAndGet());
    }

    @Test
    public void xmlConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2.xml");
        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger(defaultLoggerName);
        indexLogs(logger, null, 100, () -> "Message " + counter.incrementAndGet());
    }


    @Test
    public void propertiesConfigTest() throws InterruptedException {

        System.setProperty("log4j.configurationFile", "log4j2-buffered-example.properties");
        AtomicInteger counter = new AtomicInteger();

        Logger logger = LogManager.getLogger("elasticsearch");
        indexLogs(logger, null, 100, () -> "Message " + counter.incrementAndGet());
    }

    static void createLoggerProgrammatically(ElasticsearchAppender.Builder appenderBuilder) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        Appender appender = appenderBuilder.build();

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

    static ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured) {

        JestHttpObjectFactory.Builder jestHttpObjectFactoryBuilder;
        if (buffered) {
            jestHttpObjectFactoryBuilder = BufferedJestHttpObjectFactory.newBuilder();

            int estimatedBatchSizeInBytes = BATCH_SIZE * INITIAL_ITEM_SIZE_IN_BYTES;

            ((BufferedJestHttpObjectFactory.Builder)jestHttpObjectFactoryBuilder).withItemSourceFactory(
                    PooledItemSourceFactory.newBuilder()
                            .withPoolName("batchPool")
                            .withInitialPoolSize(INITIAL_BATCH_POOL_SIZE)
                            .withItemSizeInBytes(estimatedBatchSizeInBytes)
                            .withMonitored(true)
                            .withMonitorTaskInterval(10000)
                            .build()
            );
        } else {
            jestHttpObjectFactoryBuilder = JestHttpObjectFactory.newBuilder();
        }

        jestHttpObjectFactoryBuilder.withConnTimeout(1000)
                .withReadTimeout(10000)
                .withDefaultMaxTotalConnectionPerRoute(4)
                .withMaxTotalConnection(4);

        if (secured) {
            jestHttpObjectFactoryBuilder.withServerUris("https://localhost:9200")
                    .withAuth(getAuth());
        } else {
            jestHttpObjectFactoryBuilder.withServerUris("http://localhost:9200");
        }

        IndexTemplate indexTemplate = new IndexTemplate.Builder()
                .withName("log4j2_test_jest")
                .withPath("classpath:indexTemplate.json")
                .build();

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(jestHttpObjectFactoryBuilder.build())
                .withBatchSize(BATCH_SIZE)
                .withDeliveryInterval(1000)
                .withIndexTemplate(indexTemplate)
                .build();

        IndexNameFormatter indexNameFormatter = RollingIndexNameFormatter.newBuilder()
                .withIndexName("log4j2_test_jest")
                .withPattern("yyyy-MM-dd-HH")
                .build();


        JacksonJsonLayout.Builder layoutBuilder = JacksonJsonLayout.newBuilder();
        if (buffered) {
            PooledItemSourceFactory sourceFactoryConfig = PooledItemSourceFactory.newBuilder()
                    .withPoolName("itemPool")
                    .withInitialPoolSize(INITIAL_ITEM_POOL_SIZE)
                    .withItemSizeInBytes(INITIAL_ITEM_SIZE_IN_BYTES)
                    .withMonitored(true)
                    .withMonitorTaskInterval(10000)
                    .build();
            layoutBuilder.withItemSourceFactory(sourceFactoryConfig).build();
        }

        return ElasticsearchAppender.newBuilder()
                .withName("elasticsearch")
                .withMessageOnly(messageOnly)
                .withBatchDelivery(asyncBatchDelivery)
                .withIndexNameFormatter(indexNameFormatter)
                .withLayout(layoutBuilder.build())
                .withIgnoreExceptions(false);
    }

    private static Auth<HttpClientConfig.Builder> getAuth() {
        CertInfo certInfo = PEMCertInfo.newBuilder()
                .withKeyPath(System.getProperty("pemCertInfo.keyPath"))
                .withKeyPassphrase(System.getProperty("pemCertInfo.keyPassphrase"))
                .withClientCertPath(System.getProperty("pemCertInfo.clientCertPath"))
                .withCaPath(System.getProperty("pemCertInfo.caPath"))
                .build();

//        CertInfo certInfo = JKSCertInfo.newBuilder()
//                .withKeystorePath(System.getProperty("jksCertInfo.keystorePath"))
//                .withKeystorePassword(System.getProperty("jksCertInfo.keystorePassword"))
//                .withTruststorePath(System.getProperty("jksCertInfo.truststorePath"))
//                .withTruststorePassword(System.getProperty("jksCertInfo.truststorePassword"))
//                .build();

        Credentials credentials = BasicCredentials.newBuilder()
                .withUsername("admin")
                .withPassword("changeme")
                .build();

        return XPackAuth.newBuilder()
                .withCertInfo(certInfo)
                .withCredentials(credentials)
                .build();
    }

    <T> void indexLogs(Logger logger, Marker marker, int numberOfProducers, Supplier<T> logSupplier) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);

        for (int thIndex = 0; thIndex < numberOfProducers; thIndex++) {
            new Thread(() -> {
                for (;numberOfLogs.getAndDecrement() > 0;) {
                    logger.info(marker, logSupplier.get());
                    counter.incrementAndGet();
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        interrupted();
                    }
                }
                latch.countDown();
            }).start();
        }

        while (latch.getCount() != 0) {
            sleep(1000);
            System.out.println("Added " + counter.get() + " messages");
        }
        System.out.println("Done");
        sleep(100000000);
    }
}
