package org.appenders.log4j2.elasticsearch.ahc.load;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.ExampleJacksonModule;
import org.appenders.log4j2.elasticsearch.GenericItemSourceLayout;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayoutPlugin;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ResourceUtil;
import org.appenders.log4j2.elasticsearch.Serializer;
import org.appenders.log4j2.elasticsearch.SimpleIndexName;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.ahc.AHCBatchOperations;
import org.appenders.log4j2.elasticsearch.ahc.AHCHttp;
import org.appenders.log4j2.elasticsearch.ahc.BasicCredentials;
import org.appenders.log4j2.elasticsearch.ahc.BatchResult;
import org.appenders.log4j2.elasticsearch.ahc.ClientProviderPoliciesRegistry;
import org.appenders.log4j2.elasticsearch.ahc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.ahc.ElasticsearchBulkAPI;
import org.appenders.log4j2.elasticsearch.ahc.ElasticsearchDataStreamAPI;
import org.appenders.log4j2.elasticsearch.ahc.ElasticsearchOperationFactory;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.ahc.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.ahc.Security;
import org.appenders.log4j2.elasticsearch.ahc.SyncStepProcessor;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ElasticsearchNodesQuery;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscoveryFactory;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscoveryRequest;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;
import org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLog4j2JsonModule;
import org.appenders.log4j2.elasticsearch.json.jackson.LogEventDataStreamMixIn;
import org.appenders.log4j2.elasticsearch.load.SmokeTestBase;
import org.appenders.log4j2.elasticsearch.load.TestConfig;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.IncludeExclude;
import org.appenders.log4j2.elasticsearch.metrics.MetricLog;
import org.appenders.log4j2.elasticsearch.metrics.ScheduledMetricsProcessor;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.appenders.log4j2.elasticsearch.util.VersionUtil;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.core.util.PropertiesUtil.getInt;

public class SmokeTest extends SmokeTestBase {

    static final String MODULE_NAME = "log4j2-elasticsearch-ahc";

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
                .add("datastreams.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.datastreams.enabled", "false")))
                .add("indexName", indexName)
                .add("filterPath", System.getProperty("smokeTest.filterPath", null))
                .add("servicediscovery.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.servicediscovery.enabled", "true")))
                .add("servicediscovery.nodesFilter", System.getProperty("smokeTest.servicediscovery.nodesFilter", ElasticsearchNodesQuery.DEFAULT_NODES_FILTER))
                .add("chroniclemap.sequenceId", 1)
                .add("metrics.enabled", Boolean.parseBoolean(System.getProperty("smokeTest.metrics.enabled", "true")))
                .add("metrics.includes", System.getProperty("smokeTest.metrics.includes", ""))
                .add("metrics.excludes", System.getProperty("smokeTest.metrics.excludes", ""))
                .add("api.version", System.getProperty("smokeTest.api.version", "8.3.2"));
    }

    private TestConfig addSecurityConfig(final TestConfig target) {
        return target.add("pemCertInfo.keyPath", System.getProperty("pemCertInfo.keyPath"))
                .add("pemCertInfo.keyPassphrase", System.getProperty("pemCertInfo.keyPassphrase"))
                .add("pemCertInfo.clientCertPath", System.getProperty("pemCertInfo.clientCertPath"))
                .add("pemCertInfo.caPath", System.getProperty("pemCertInfo.caPath"));
    }

    @Override
    public ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(final boolean messageOnly, final boolean buffered, final boolean secured) {

        final int batchSize = getConfig().getProperty("batchSize", Integer.class);
        final int initialItemPoolSize = getConfig().getProperty("initialItemPoolSize", Integer.class);
        final int initialItemBufferSizeInBytes = getConfig().getProperty("initialItemBufferSizeInBytes", Integer.class);
        final int initialBatchPoolSize = getConfig().getProperty("initialBatchPoolSize", Integer.class);
        final boolean ecsEnabled = getConfig().getProperty("ecs.enabled", Boolean.class);
        final boolean dataStreamsEnabled = getConfig().getProperty("datastreams.enabled", Boolean.class);
        final String filterPath = getConfig().getProperty("filterPath", String.class);
        final String indexName = getConfig().getProperty("indexName", String.class);
        final boolean serviceDiscoveryEnabled = getConfig().getProperty("servicediscovery.enabled", Boolean.class);
        final String version = getConfig().getProperty("api.version", String.class);
        final boolean metricsEnabled = getConfig().getProperty("metrics.enabled", Boolean.class);
        final List<String> metricsIncludes = SplitUtil.split(getConfig().getProperty("metrics.includes", String.class));
        final List<String> metricsExcludes = SplitUtil.split(getConfig().getProperty("metrics.excludes", String.class));

        getLogger().info("{}", getConfig().getAll());

        final Configuration configuration = LoggerContext.getContext(false).getConfiguration();


        final UnpooledByteBufAllocator byteBufAllocator = new UnpooledByteBufAllocator(false, false, false);

        final int estimatedBatchSizeInBytes = batchSize * initialItemBufferSizeInBytes;
        final PooledItemSourceFactory<Object, ByteBuf> pooledItemSourceFactory = batchItemPool(initialBatchPoolSize, estimatedBatchSizeInBytes, metricsEnabled);

        final List<String> serverList = getServerList(secured, getConfig().getProperty("serverList", String.class));
        final HttpClientFactory.Builder httpConfig = new HttpClientFactory.Builder()
                .withServerList(serverList)
                .withConnTimeout(500)
                .withReadTimeout(10000)
                .withIoThreadCount(4)
                .withMaxTotalConnections(16)
                .withAuth(secured ? getAuth() : null)
                .withName("http-main")
                .withMetricConfigs(HttpClient.metricConfigs(metricsEnabled));

        final HttpClientProvider clientProvider = new HttpClientProvider(httpConfig);

        final AHCHttp.Builder httpObjectFactoryBuilder = (AHCHttp.Builder) new AHCHttp.Builder()
                .withBatchOperations(batchOperations(pooledItemSourceFactory, VersionUtil.parse(version), filterPath, dataStreamsEnabled))
                .withClientProvider(clientProvider)
                .withBackoffPolicy(new BatchLimitBackoffPolicy<>(16))
                .withName("http-main")
                .withMetricConfigs(AHCHttp.metricConfigs(metricsEnabled));

        if (serviceDiscoveryEnabled) {

            final HttpClientProvider serviceDiscoveryClientProvider = new HttpClientProvider(new HttpClientFactory.Builder()
                    .withServerList(serverList)
                    .withReadTimeout(1000)
                    .withConnTimeout(500)
                    .withMaxTotalConnections(1)
                    .withIoThreadCount(1));

            final ClientProviderPolicy<HttpClient> clientProviderPolicy = new ClientProviderPoliciesRegistry().get(
                    new HashSet<>(Arrays.asList("serverList", "security")),
                    serviceDiscoveryClientProvider);

            final ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory = new ServiceDiscoveryFactory<>(
                    clientProviderPolicy,
                    serviceDiscoveryQuery(getConfig().getProperty("servicediscovery.nodesFilter", String.class)),
                    5000L
            );

            httpConfig.withServiceDiscovery(serviceDiscoveryFactory.create(clientProvider));

        }

        httpObjectFactoryBuilder
                .withClientProvider(clientProvider)
                .withOperationFactory(new ElasticsearchOperationFactory(
                        new SyncStepProcessor(clientProvider, new JacksonDeserializer<>(ElasticsearchBulkAPI.defaultObjectMapper().readerFor(BatchResult.class))),
                        new Log4j2Lookup(configuration.getStrSubstitutor())));

        final BasicMetricsRegistry metricRegistry = new BasicMetricsRegistry();
        final BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(httpObjectFactoryBuilder.build())
                .withBatchSize(batchSize)
                .withDeliveryInterval(1000)
                .withSetupOpSources(setupOpSources(VersionUtil.parse(version), indexName, ecsEnabled, dataStreamsEnabled))
                .withFailoverPolicy(resolveFailoverPolicy())
                .withShutdownDelayMillis(10000)
                .withMetricProcessor(new ScheduledMetricsProcessor(0L, 5000L, Clock.systemDefaultZone(), metricRegistry, new BasicMetricOutputsRegistry(
                        new MetricLog(indexName, new LazyLogger(InternalLogging::getLogger), new IncludeExclude(metricsIncludes, metricsExcludes))
                )))
                .build();

        final IndexNameFormatter<Object> indexNameFormatter = new SimpleIndexName.Builder<>()
                .withIndexName(indexName)
                .build();

        final PooledItemSourceFactory<LogEvent, ByteBuf> sourceFactoryConfig = new PooledItemSourceFactory.Builder<LogEvent, ByteBuf>()
                .withPoolName("itemPool")
                .withPooledObjectOps(new ByteBufPooledObjectOps(
                        byteBufAllocator,
                        new ByteBufBoundedSizeLimitPolicy(initialItemBufferSizeInBytes, initialItemBufferSizeInBytes * 2)))
                .withInitialPoolSize(initialItemPoolSize)
                .withResizePolicy(new UnlimitedResizePolicy.Builder().build())
                .withReuseStreams(true)
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(metricsEnabled))
                .build();

        final Serializer<LogEvent> serializer = createLogEventSerializer(ecsEnabled, dataStreamsEnabled, configuration);
        final GenericItemSourceLayout.Builder<LogEvent, ByteBuf> layoutBuilder = new GenericItemSourceLayout.Builder<LogEvent, ByteBuf>()
                .withSerializer(serializer)
                .withItemSourceFactory(sourceFactoryConfig);

        //noinspection rawtypes
        @SuppressWarnings("unchecked")
        final JacksonJsonLayoutPlugin layout = new JacksonJsonLayoutPlugin(layoutBuilder);

        return ElasticsearchAppender.newBuilder()
                .withName(getConfig().getProperty("appenderName", String.class))
                .withMessageOnly(messageOnly)
                .withBatchDelivery(asyncBatchDelivery)
                .withIndexNameFormatter(indexNameFormatter)
                .withLayout(layout)
                .withIgnoreExceptions(false);
    }

    private Serializer<LogEvent> createLogEventSerializer(final boolean ecsEnabled, final boolean dataStreamsEnabled, final Configuration configuration) {

        final JacksonSerializer.Builder<LogEvent> serializerBuilder = new JacksonSerializer.Builder<LogEvent>()
                .withVirtualProperties(
                        new VirtualProperty("hostname", "${env:hostname:-undefined}", false),
                        new VirtualProperty("progField", "constantValue", false)
                )
                .withValueResolver(new Log4j2Lookup(configuration.getStrSubstitutor()))
                .withSingleThread(getConfig().getProperty("singleThread", Boolean.class))
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

    private BatchOperations batchOperations(final PooledItemSourceFactory pooledItemSourceFactory,
                                            final Version version,
                                            final String filterPath,
                                            final boolean dataStreamsEnabled) {
        if (dataStreamsEnabled) {
            return new AHCBatchOperations(pooledItemSourceFactory, new ElasticsearchDataStreamAPI(filterPath));
        } else {
            return new AHCBatchOperations(pooledItemSourceFactory, new ElasticsearchBulkAPI(mappingType(version), filterPath));
        }
    }

    private ServiceDiscoveryRequest<HttpClient> serviceDiscoveryQuery(final String nodesFilter) {

        final boolean secure = getConfig().getProperty("secure", Boolean.class);
        final String scheme = secure ? "https" : "http";
        return new ElasticsearchNodesQuery(scheme, nodesFilter);

    }

    private PooledItemSourceFactory<Object, ByteBuf> batchItemPool(final int initialBatchPoolSize, final int estimatedBatchSizeInBytes, final boolean metricsEnabled) {
        return new PooledItemSourceFactory.Builder<Object, ByteBuf>()
                .withPoolName("batchPool")
                .withInitialPoolSize(initialBatchPoolSize)
                .withPooledObjectOps(new ByteBufPooledObjectOps(
                        UnpooledByteBufAllocator.DEFAULT,
                        new ByteBufBoundedSizeLimitPolicy(estimatedBatchSizeInBytes, estimatedBatchSizeInBytes)))
                .withMetricConfigs(GenericItemSourcePool.metricConfigs(metricsEnabled))
                .build();

    }

    private List<String> getServerList(final boolean secured, final String hostPortList) {
        return SplitUtil.split(hostPortList, ";").stream()
                .map(uri -> String.format("%s://%s", secured ? "https" : "http", uri))
                .collect(Collectors.toList());
    }

    private Auth<HttpClientFactory.Builder> getAuth() {
        final CertInfo<HttpClientFactory.Builder> certInfo = PEMCertInfo.newBuilder()
                .withKeyPath(getConfig().getProperty("pemCertInfo.keyPath", String.class))
                .withKeyPassphrase(getConfig().getProperty("pemCertInfo.keyPassphrase", String.class))
                .withClientCertPath(getConfig().getProperty("pemCertInfo.clientCertPath", String.class))
                .withCaPath(getConfig().getProperty("pemCertInfo.caPath", String.class))
                .build();

//        CertInfo<HttpClientFactory.Builder> certInfo = JKSCertInfo.newBuilder()
//                .withKeystorePath(System.getProperty("jksCertInfo.keystorePath"))
//                .withKeystorePassword(System.getProperty("jksCertInfo.keystorePassword"))
//                .withTruststorePath(System.getProperty("jksCertInfo.truststorePath"))
//                .withTruststorePassword(System.getProperty("jksCertInfo.truststorePassword"))
//                .build();

        final Credentials<HttpClientFactory.Builder> credentials = BasicCredentials.newBuilder()
                .withUsername("admin")
                .withPassword("changeme")
                .build();

        return new Security.Builder()
                .withCertInfo(certInfo)
                .withCredentials(credentials)
                .build();
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
                    .withPath(resolveIndexTemplatePath(ecsEnabled, dataStreamsEnabled))
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

    private String resolveIndexTemplatePath(final boolean ecsEnabled, final boolean dataStreamsEnabled) {

        if (ecsEnabled) {
            return "classpath:componentTemplate-7-mappings-ecs.json";
        }

        if (dataStreamsEnabled) {
            return "classpath:componentTemplate-7-mappings-data-stream.json";
        }

        return "classpath:componentTemplate-7-mappings.json";

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

    private String resolveIndexName(final boolean dataStreamsEnabled) {

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
        public void info(final String messageFormat, final Object... parameters) {
            loggerSupplier.get().info(messageFormat, parameters);
        }

    }

}
