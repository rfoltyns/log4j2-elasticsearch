package org.appenders.log4j2.elasticsearch.jest.load;

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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.AppenderRefFailoverPolicy;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.ExampleJacksonModule;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.GenericItemSourceLayout;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayoutPlugin;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledObjectOps;
import org.appenders.log4j2.elasticsearch.ResourceUtil;
import org.appenders.log4j2.elasticsearch.Serializer;
import org.appenders.log4j2.elasticsearch.SimpleIndexName;
import org.appenders.log4j2.elasticsearch.StringItemSourceFactory;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn;
import org.appenders.log4j2.elasticsearch.jest.BasicCredentials;
import org.appenders.log4j2.elasticsearch.jest.BufferedJestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory;
import org.appenders.log4j2.elasticsearch.jest.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.jest.XPackAuth;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLog4j2JsonModule;
import org.appenders.log4j2.elasticsearch.json.jackson.LogEventDataStreamMixIn;
import org.appenders.log4j2.elasticsearch.load.SmokeTestBase;
import org.appenders.log4j2.elasticsearch.load.TestConfig;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.IncludeExclude;
import org.appenders.log4j2.elasticsearch.metrics.MetricLog;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.ScheduledMetricsProcessor;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.appenders.log4j2.elasticsearch.util.VersionUtil;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.core.util.PropertiesUtil.getInt;

public class SmokeTest extends SmokeTestBase {

    static final String MODULE_NAME = "log4j2-elasticsearch-jest";

    @BeforeEach
    public void beforeEach() {
        super.beforeEach();
        configure();
    }

    protected TestConfig configure() {

        final boolean dataStreamsEnabled = Boolean.parseBoolean(System.getProperty("smokeTest.datastreams.enabled", "false"));
        final String indexName = resolveIndexName(dataStreamsEnabled);

        return addSecurityConfig(getConfig())
                .add("serverList", System.getProperty("smokeTest.serverList", "localhost:9200"))
                .add("batchSize", getInt("smokeTest.batchSize", 10000))
                .add("initialItemPoolSize", getInt("smokeTest.initialItemPoolSize", 40000))
                .add("initialItemBufferSizeInBytes", getInt("smokeTest.initialItemBufferSizeInBytes", 1024))
                .add("initialBatchPoolSize", getInt("smokeTest.initialBatchPoolSize", 4))
                .add("ecs.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.ecs.enabled", "false")))
                .add("datastreams.enabled", dataStreamsEnabled)
                .add("indexName", indexName)
                .add("chroniclemap.sequenceId", 2)
                .add("metrics.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.metrics.enabled", "true")))
                .add("metrics.includes", System.getProperty("smokeTest.metrics.includes", ""))
                .add("metrics.excludes", System.getProperty("smokeTest.metrics.excludes", ""))
                .add("api.version", System.getProperty("smokeTest.api.version", "8.3.2"));

    }

    private TestConfig addSecurityConfig(TestConfig target) {
        return target.add("pemCertInfo.keyPath", System.getProperty("pemCertInfo.keyPath"))
                .add("pemCertInfo.keyPassphrase", System.getProperty("pemCertInfo.keyPassphrase"))
                .add("pemCertInfo.clientCertPath", System.getProperty("pemCertInfo.clientCertPath"))
                .add("pemCertInfo.caPath", System.getProperty("pemCertInfo.caPath"));
    }

    @Override
    public ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured) {

        final String version = getConfig().getProperty("api.version", String.class);

        final int batchSize = getConfig().getProperty("batchSize", Integer.class);
        final int initialItemPoolSize = getConfig().getProperty("initialItemPoolSize", Integer.class);
        final int initialItemBufferSizeInBytes = getConfig().getProperty("initialItemBufferSizeInBytes", Integer.class);
        final int initialBatchPoolSize = getConfig().getProperty("initialBatchPoolSize", Integer.class);
        final boolean ecsEnabled = getConfig().getProperty("ecs.enabled", Boolean.class);
        final boolean dataStreamsEnabled = getConfig().getProperty("datastreams.enabled", Boolean.class);
        final String indexName = getConfig().getProperty("indexName", String.class);
        final boolean metricsEnabled = getConfig().getProperty("metrics.enabled", Boolean.class);
        final List<String> metricsIncludes = SplitUtil.split(getConfig().getProperty("metrics.includes", String.class));
        final List<String> metricsExcludes = SplitUtil.split(getConfig().getProperty("metrics.excludes", String.class));

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
                            .withMetricConfigs(GenericItemSourcePool.metricConfigs(metricsEnabled))
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
                .withMappingType(dataStreamsEnabled ? null : mappingType(VersionUtil.parse(version)))
                .withDataStreamsEnabled(dataStreamsEnabled)
                .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()))
                .withName("http-main")
                .withMetricConfigs(JestHttpObjectFactory.metricConfigs(metricsEnabled));

        final String serverList = getServerList(secured, getConfig().getProperty("serverList", String.class));
        jestHttpObjectFactoryBuilder.withServerUris(serverList);

        if (secured) {
            jestHttpObjectFactoryBuilder.withAuth(getAuth());
        }

        final BasicMetricsRegistry metricRegistry = new BasicMetricsRegistry();
        final MetricOutputsRegistry metricOutputsRegistry = new BasicMetricOutputsRegistry(new MetricLog(indexName, new LazyLogger(InternalLogging::getLogger), new IncludeExclude(metricsIncludes, metricsExcludes)));
        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(jestHttpObjectFactoryBuilder.build())
                .withBatchSize(batchSize)
                .withDeliveryInterval(1000)
                .withFailoverPolicy(resolveFailoverPolicy())
                .withSetupOpSources(setupOpSources(VersionUtil.parse(version), indexName, ecsEnabled, dataStreamsEnabled))
                .withMetricProcessor(new ScheduledMetricsProcessor(0L, 5000L, Clock.systemDefaultZone(), metricRegistry, metricOutputsRegistry))
                .build();

        IndexNameFormatter<Object> indexNameFormatter = new SimpleIndexName.Builder<>()
                .withIndexName(indexName)
                .build();

        final GenericItemSourceLayout.Builder<Object, Object> layoutBuilder;
        if (buffered) {
            @SuppressWarnings("rawtypes")
            final PooledObjectOps byteBufPooledObjectOps = new ByteBufPooledObjectOps(
                    UnpooledByteBufAllocator.DEFAULT,
                    new ByteBufBoundedSizeLimitPolicy(initialItemBufferSizeInBytes, initialItemBufferSizeInBytes * 2));
            @SuppressWarnings({"rawtypes", "unchecked"})
            final ItemSourceFactory sourceFactoryConfig = new PooledItemSourceFactory.Builder<>()
                    .withPoolName("itemPool")
                    .withPooledObjectOps(byteBufPooledObjectOps)
                    .withInitialPoolSize(initialItemPoolSize)
                    .withMetricConfigs(GenericItemSourcePool.metricConfigs(metricsEnabled))
                    .build();
            //noinspection unchecked
            layoutBuilder = new GenericItemSourceLayout.Builder<>()
                    .withItemSourceFactory(sourceFactoryConfig);
        } else {
            //noinspection unchecked
            layoutBuilder = new GenericItemSourceLayout.Builder<>()
                    .withItemSourceFactory(new StringItemSourceFactory.Builder().build());
        }

        final Serializer<Object> logEventSerializer = createLogEventSerializer(ecsEnabled, dataStreamsEnabled, configuration);

        @SuppressWarnings({"rawtypes", "unchecked"})
        final JacksonJsonLayoutPlugin layout = new JacksonJsonLayoutPlugin(layoutBuilder.withSerializer(logEventSerializer));

        return ElasticsearchAppender.newBuilder()
                .withName(getConfig().getProperty("appenderName", String.class))
                .withMessageOnly(messageOnly)
                .withBatchDelivery(asyncBatchDelivery)
                .withIndexNameFormatter(indexNameFormatter)
                .withLayout(layout)
                .withIgnoreExceptions(false);
    }

    private Serializer<Object> createLogEventSerializer(boolean ecsEnabled, boolean dataStreamsEnabled, Configuration configuration) {

        final JacksonSerializer.Builder<Object> serializerBuilder = new JacksonSerializer.Builder<>()
                .withVirtualProperties(
                        new VirtualProperty("hostname", "${env:hostname:-undefined}", false),
                        new VirtualProperty("progField", "constantValue", false)
                )
                .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()))
                .withJacksonModules(new ExtendedLog4j2JsonModule(), ExampleJacksonModule.newBuilder().build());

        if (ecsEnabled) {
            serializerBuilder.withMixins(new JacksonMixIn.Builder()
                    .withMixInClass(LogEventJacksonEcsJsonMixIn.class.getName())
                    .withTargetClass(LogEvent.class.getName())
                    .build());
        }

        if (dataStreamsEnabled) {
            serializerBuilder.withMixins(new JacksonMixIn.Builder()
                    .withMixInClass(LogEventDataStreamMixIn.class.getName())
                    .withTargetClass(LogEvent.class.getName())
                    .build());
        }

        return serializerBuilder.build();

    }

    @Override
    protected FailoverPolicy resolveFailoverPolicy() {
        final Configuration configuration = LoggerContext.getContext(false).getConfiguration();
        return AppenderRefFailoverPolicy.newBuilder()
                .withConfiguration(configuration)
                .withAppenderRef(AppenderRef.createAppenderRef("failover-file", Level.INFO, null))
                .build();
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

    private OpSource[] setupOpSources(final Version version, final String indexName, final boolean ecsEnabled, final boolean dataStreamsEnabled) {

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
                    .withPath(resolveMappingsTemplatePath(ecsEnabled, dataStreamsEnabled))
                    .build());

            result.add(new IndexTemplate.Builder()
                    .withApiVersion(8)
                    .withName(indexName)
                    .withPath(indexTemplatePath())
                    .build());

        } else {
            result.add(new IndexTemplate.Builder()
                    .withApiVersion(7)
                    .withName(indexName)
                    .withPath("classpath:indexTemplate-" + version.major() + ".json")
                    .build());
        }

        if (!version.lowerThan("7.2.0")) {
            result.add(new ILMPolicy(
                    indexName + "-ilm-policy",
                    indexName,
                    !dataStreamsEnabled,
                    ResourceUtil.loadResource("classpath:ilmPolicy-7.json")));
        }

        if (dataStreamsEnabled) {
            // Optional, ES will create one if it's missing
            result.add(new DataStream.Builder()
                    .withName(indexName)
                    .build());
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

    private String resolveMappingsTemplatePath(final boolean ecsEnabled, final boolean dataStreamsEnabled) {
        if (ecsEnabled) {
            return "classpath:componentTemplate-7-mappings-ecs.json";
        }
        if (dataStreamsEnabled) {
            return "classpath:componentTemplate-7-mappings-data-stream.json";
        }
        return "classpath:componentTemplate-7-mappings.json";
    }

    private String resolveIndexName(boolean dataStreamsEnabled) {
        String indexName = System.getProperty("smokeTest.indexName");
        if (MODULE_NAME.equals(indexName) || indexName == null) {
            final String suffix = (dataStreamsEnabled ? "-data-stream" : "-index");
            indexName = MODULE_NAME + suffix;
        }
        System.setProperty("smokeTest.indexName", indexName);
        return indexName;
    }

    private String indexTemplatePath() {
        final boolean dsEnabled = getConfig().getProperty("datastreams.enabled", Boolean.class);
        return String.format("classpath:composableIndexTemplate-7%s.json", dsEnabled ? "-data-stream"  : "");
    }

    private static class LazyLogger implements Logger {
        private final Supplier<Logger> loggerSupplier;

        public LazyLogger(final Supplier<Logger> loggerSupplier) {
            this.loggerSupplier = loggerSupplier;
        }

        @Override
        public void info(String messageFormat, Object... parameters) {
            loggerSupplier.get().info(messageFormat, parameters);
        }
    }
}
