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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPlugin;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn;
import org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicy;
import org.appenders.log4j2.elasticsearch.failover.KeySequenceSelector;
import org.appenders.log4j2.elasticsearch.failover.SingleKeySequenceSelector;
import org.appenders.log4j2.elasticsearch.jest.BasicCredentials;
import org.appenders.log4j2.elasticsearch.jest.BufferedJestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.jest.XPackAuth;
import org.appenders.log4j2.elasticsearch.smoke.SmokeTestBase;
import org.junit.BeforeClass;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.core.util.PropertiesUtil.getInt;

public class SmokeTest extends SmokeTestBase {

    @Override
    public ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured) {

        final int batchSize = getInt("smokeTest.batchSize", 10000);
        final int additionalBatchSize = (int) (batchSize * 0.2); // prevent tiny batches
        final int initialItemPoolSize = getInt("smokeTest.initialItemPoolSize", 40000);
        final int initialItemBufferSizeInBytes = getInt("smokeTest.initialItemBufferSizeInBytes", 1024);
        final int initialBatchPoolSize = getInt("smokeTest.initialBatchPoolSize", 4);
        final String indexName = System.getProperty("smokeTest.indexName", "log4j2-elasticsearch-jest");
        final boolean ecsEnabled = Boolean.parseBoolean(System.getProperty("smokeTest.ecs.enabled", "false"));

        params.add("batchSize", batchSize)
                .add("initialBatchPoolSize", initialBatchPoolSize)
                .add("initialItemBufferSizeInBytes", initialItemBufferSizeInBytes)
                .add("initialBatchPoolSize", initialBatchPoolSize)
                .add("indexName", indexName)
                .add("ecs.enabled", ecsEnabled);

        getLogger().info("Running SmokeTest {}", params.getAll());

        JestHttpObjectFactory.Builder jestHttpObjectFactoryBuilder;
        if (buffered) {
            jestHttpObjectFactoryBuilder = BufferedJestHttpObjectFactory.newBuilder();

            int estimatedBatchSizeInBytes = batchSize * initialItemBufferSizeInBytes;

            ((BufferedJestHttpObjectFactory.Builder)jestHttpObjectFactoryBuilder).withItemSourceFactory(
                    PooledItemSourceFactory.newBuilder()
                            .withPoolName("batchPool")
                            .withInitialPoolSize(initialBatchPoolSize)
                            .withItemSizeInBytes(estimatedBatchSizeInBytes)
                            .withMonitored(true)
                            .withMonitorTaskInterval(10000)
                            .build()
            );
        } else {
            jestHttpObjectFactoryBuilder = JestHttpObjectFactory.newBuilder();
        }

        Configuration configuration = LoggerContext.getContext(false).getConfiguration();

        jestHttpObjectFactoryBuilder.withConnTimeout(1000)
                .withReadTimeout(10000)
                .withIoThreadCount(8)
                .withDefaultMaxTotalConnectionPerRoute(8)
                .withMaxTotalConnection(8)
                .withMappingType("_doc")
                .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()));

        if (secured) {
            jestHttpObjectFactoryBuilder.withServerUris("https://localhost:9200")
                    .withAuth(getAuth());
        } else {
            jestHttpObjectFactoryBuilder.withServerUris("http://localhost:9200");
        }

        ComponentTemplate indexSettings = new ComponentTemplate.Builder()
                .withName(indexName + "-settings")
                .withPath("classpath:componentTemplate-7-settings.json")
                .build();

        ComponentTemplate indexSettingsIlm = new ComponentTemplate.Builder()
                .withName(indexName + "-settings-ilm")
                .withPath("classpath:componentTemplate-7-settings-ilm.json")
                .build();

        ComponentTemplate indexMappings = new ComponentTemplate.Builder()
                .withName(indexName + "-mappings")
                .withPath(ecsEnabled ? "classpath:componentTemplate-7-mappings-ecs.json": "classpath:componentTemplate-7-mappings.json")
                .build();

        IndexTemplate componentIndexTemplate = new IndexTemplate.Builder()
                .withApiVersion(8)
                .withName(indexName + "-composed-index-template")
                .withPath("classpath:composableIndexTemplate-7.json")
                .build();

        ILMPolicy ilmPolicy = new ILMPolicyPlugin.Builder()
                .withName(indexName + "-ilm-policy")
                .withRolloverAlias(indexName)
                .withPath("classpath:ilmPolicy-7.json")
                .build();

        KeySequenceSelector keySequenceSelector = new SingleKeySequenceSelector(2);

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(jestHttpObjectFactoryBuilder.build())
                .withBatchSize(batchSize + additionalBatchSize)
                .withDeliveryInterval(1000)
                .withFailoverPolicy(new ChronicleMapRetryFailoverPolicy.Builder()
                        .withKeySequenceSelector(keySequenceSelector)
                        .withFileName(resolveChronicleMapFilePath(indexName + ".chronicleMap"))
                        .withAverageValueSize(2048)
                        .withNumberOfEntries(1000000)
                        .withBatchSize(5000)
                        .withRetryDelay(3000)
                        .withMonitored(true)
                        .build())
                .withSetupOpSources(indexSettings, indexMappings, indexSettingsIlm, componentIndexTemplate, ilmPolicy)
                .build();

        IndexNameFormatter indexNameFormatter = NoopIndexNameFormatter.newBuilder()
                .withIndexName(indexName)
                .build();

        JacksonJsonLayout.Builder layoutBuilder = JacksonJsonLayout.newBuilder()
                .setConfiguration(configuration)
                .withVirtualProperties(
                        new VirtualProperty("hostname", "${env:hostname:-undefined}", false),
                        new VirtualProperty("progField", "constantValue", false)
                );

        if (ecsEnabled) {
            layoutBuilder.withMixins(JacksonMixIn.newBuilder()
                    .withMixInClass(LogEventJacksonEcsJsonMixIn.class.getName())
                    .withTargetClass(LogEvent.class.getName())
                    .build());
        }

        if (buffered) {
            PooledItemSourceFactory sourceFactoryConfig = PooledItemSourceFactory.newBuilder()
                    .withPoolName("itemPool")
                    .withInitialPoolSize(initialItemPoolSize)
                    .withItemSizeInBytes(initialItemBufferSizeInBytes)
                    .withMonitored(true)
                    .withMonitorTaskInterval(10000)
                    .build();
            layoutBuilder.withItemSourceFactory(sourceFactoryConfig).build();
        }

        return ElasticsearchAppender.newBuilder()
                .withName(DEFAULT_APPENDER_NAME)
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

}
