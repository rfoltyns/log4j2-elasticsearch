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


import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ResourceUtil;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn;
import org.appenders.log4j2.elasticsearch.jest.BasicCredentials;
import org.appenders.log4j2.elasticsearch.jest.BufferedJestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.jest.XPackAuth;
import org.appenders.log4j2.elasticsearch.smoke.SmokeTestBase;
import org.appenders.log4j2.elasticsearch.smoke.TestConfig;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.appenders.log4j2.elasticsearch.util.VersionUtil;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.core.util.PropertiesUtil.getInt;

public class SmokeTest extends SmokeTestBase {

    @BeforeEach
    public void beforeEach() {
        super.beforeEach();
        configure();
    }

    protected TestConfig configure() {
        return addSecurityConfig(getConfig())
                .add("serverList", System.getProperty("smokeTest.serverList", "localhost:9200"))
                .add("batchSize", getInt("smokeTest.batchSize", 10000))
                .add("initialItemPoolSize", getInt("smokeTest.initialItemPoolSize", 40000))
                .add("initialItemBufferSizeInBytes", getInt("smokeTest.initialItemBufferSizeInBytes", 1024))
                .add("initialBatchPoolSize", getInt("smokeTest.initialBatchPoolSize", 4))
                .add("indexName", System.getProperty("smokeTest.indexName", "log4j2-elasticsearch-jest"))
                .add("ecs.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.ecs.enabled", "false")))
                .add("chroniclemap.sequenceId", 2)
                .add("api.version", System.getProperty("smokeTest.api.version", "7.10.2"));

    }

    private TestConfig addSecurityConfig(TestConfig target) {
        return target.add("pemCertInfo.keyPath", System.getProperty("pemCertInfo.keyPath"))
                .add("pemCertInfo.keyPassphrase", System.getProperty("pemCertInfo.keyPassphrase"))
                .add("pemCertInfo.clientCertPath", System.getProperty("pemCertInfo.clientCertPath"))
                .add("pemCertInfo.caPath", System.getProperty("pemCertInfo.caPath"));
    }

    @Override
    public ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured) {

        final int batchSize = getConfig().getProperty("batchSize", Integer.class);
        final int initialItemPoolSize = getConfig().getProperty("initialItemPoolSize", Integer.class);
        final int initialItemBufferSizeInBytes = getConfig().getProperty("initialItemBufferSizeInBytes", Integer.class);
        final int initialBatchPoolSize = getConfig().getProperty("initialBatchPoolSize", Integer.class);
        final String indexName = getConfig().getProperty("indexName", String.class);
        final boolean ecsEnabled = getConfig().getProperty("ecs.enabled", Boolean.class);
        final String version = getConfig().getProperty("api.version", String.class);

        getLogger().info("Running SmokeTest {}", getConfig().getAll());

        JestHttpObjectFactory.Builder jestHttpObjectFactoryBuilder;
        if (buffered) {
            jestHttpObjectFactoryBuilder = BufferedJestHttpObjectFactory.newBuilder();

            int estimatedBatchSizeInBytes = batchSize * initialItemBufferSizeInBytes;

            ((BufferedJestHttpObjectFactory.Builder)jestHttpObjectFactoryBuilder).withItemSourceFactory(
                    new PooledItemSourceFactory.Builder<Object, ByteBuf>()
                            .withPoolName("batchPool")
                            .withPooledObjectOps(new ByteBufPooledObjectOps(
                                    UnpooledByteBufAllocator.DEFAULT,
                                    new ByteBufBoundedSizeLimitPolicy(estimatedBatchSizeInBytes, estimatedBatchSizeInBytes)))
                            .withInitialPoolSize(initialBatchPoolSize)
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
                .withMappingType(mappingType(VersionUtil.parse(version)))
                .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()));

        final String serverList = getServerList(secured, getConfig().getProperty("serverList", String.class));
        jestHttpObjectFactoryBuilder.withServerUris(serverList);

        if (secured) {
            jestHttpObjectFactoryBuilder.withAuth(getAuth());
        }

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(jestHttpObjectFactoryBuilder.build())
                .withBatchSize(batchSize)
                .withDeliveryInterval(1000)
                .withFailoverPolicy(resolveFailoverPolicy())
                .withSetupOpSources(setupOpSources(VersionUtil.parse(version), indexName, ecsEnabled))
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
            layoutBuilder.withMixins(new JacksonMixIn.Builder()
                    .withMixInClass(LogEventJacksonEcsJsonMixIn.class.getName())
                    .withTargetClass(LogEvent.class.getName())
                    .build());
        }

        if (buffered) {
            PooledItemSourceFactory<Object, ByteBuf> sourceFactoryConfig = new PooledItemSourceFactory.Builder<Object, ByteBuf>()
                    .withPoolName("itemPool")
                    .withPooledObjectOps(new ByteBufPooledObjectOps(
                            UnpooledByteBufAllocator.DEFAULT,
                            new ByteBufBoundedSizeLimitPolicy(initialItemBufferSizeInBytes, initialItemBufferSizeInBytes * 2)))
                    .withInitialPoolSize(initialItemPoolSize)
                    .withMonitored(true)
                    .withMonitorTaskInterval(10000)
                    .build();
            layoutBuilder.withItemSourceFactory(sourceFactoryConfig).build();
        }

        return ElasticsearchAppender.newBuilder()
                .withName(getConfig().getProperty("appenderName", String.class))
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

    private String getServerList(final boolean secured, final String hostPortList) {
        return SplitUtil.split(hostPortList, ";").stream()
                .map(uri -> String.format("%s://%s", secured ? "https" : "http", uri))
                .collect(Collectors.joining(";"));
    }

    private OpSource[] setupOpSources(final Version version, final String indexName, boolean ecsEnabled) {

        final ArrayList<OpSource> result = new ArrayList<>();

        if (!version.lowerThan("7.8.0")) {
            result.add(new ComponentTemplate.Builder()
                    .withName(indexName + "-settings")
                    .withPath("classpath:componentTemplate-7-settings.json")
                    .build());

            result.add(new ComponentTemplate.Builder()
                    .withName(indexName + "-settings-ilm")
                    .withPath("classpath:componentTemplate-7-settings-ilm.json")
                    .build());

            result.add(new ComponentTemplate.Builder()
                    .withName(indexName + "-mappings")
                    .withPath(ecsEnabled ? "classpath:componentTemplate-7-mappings-ecs.json": "classpath:componentTemplate-7-mappings.json")
                    .build());

            result.add(new IndexTemplate.Builder()
                    .withApiVersion(8)
                    .withName(indexName + "-composed-index-template")
                    .withPath("classpath:composableIndexTemplate-7.json")
                    .build());
        } else {
            result.add(new IndexTemplate.Builder()
                    .withApiVersion(7)
                    .withName(indexName + "-index-template")
                    .withPath("classpath:indexTemplate-" + version.major() + ".json")
                    .build());
        }

        if (!version.lowerThan("7.2.0")) {
            result.add(new ILMPolicy(
                    indexName + "-ilm-policy",
                    indexName,
                    ResourceUtil.loadResource("classpath:ilmPolicy-7.json")));
        }

        return result.toArray(new OpSource[0]);
    }

    private String mappingType(final Version version) {
        if (version.lowerThan("7.0.0")) {
            return "index";
        }
        if (version.lowerThan("8.0.0")) {
            return "_doc";
        }
        return null;
    }

}
