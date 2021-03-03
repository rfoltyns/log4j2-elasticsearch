package org.appenders.log4j2.elasticsearch.hc.smoke;

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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.jackson.LogEventJacksonDataStreamJsonMixIn;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.ExampleJacksonModule;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexNameFormatter;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ResourceUtil;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.VirtualProperty;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;
import org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn;
import org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicy;
import org.appenders.log4j2.elasticsearch.failover.KeySequenceSelector;
import org.appenders.log4j2.elasticsearch.failover.Log4j2SingleKeySequenceSelector;
import org.appenders.log4j2.elasticsearch.hc.BasicCredentials;
import org.appenders.log4j2.elasticsearch.hc.BatchItemResult;
import org.appenders.log4j2.elasticsearch.hc.BatchItemResultMixIn;
import org.appenders.log4j2.elasticsearch.hc.BatchResult;
import org.appenders.log4j2.elasticsearch.hc.BatchResultMixIn;
import org.appenders.log4j2.elasticsearch.hc.ClientProviderPoliciesRegistry;
import org.appenders.log4j2.elasticsearch.hc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.hc.ElasticsearchDataStreamBatchOperations;
import org.appenders.log4j2.elasticsearch.hc.ElasticsearchOperationFactory;
import org.appenders.log4j2.elasticsearch.hc.Error;
import org.appenders.log4j2.elasticsearch.hc.ErrorMixIn;
import org.appenders.log4j2.elasticsearch.hc.HCHttp;
import org.appenders.log4j2.elasticsearch.hc.HttpClient;
import org.appenders.log4j2.elasticsearch.hc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.hc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.hc.PEMCertInfo;
import org.appenders.log4j2.elasticsearch.hc.Security;
import org.appenders.log4j2.elasticsearch.hc.SyncStepProcessor;
import org.appenders.log4j2.elasticsearch.hc.discovery.ElasticsearchNodesQuery;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscoveryFactory;
import org.appenders.log4j2.elasticsearch.smoke.SmokeTestBase;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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
        final String indexName = System.getProperty("smokeTest.indexName", "log4j2-elasticsearch-hc");
        final boolean ecsEnabled = Boolean.parseBoolean(System.getProperty("smokeTest.ecs.enabled", "false"));
        final boolean serviceDiscoveryEnabled = Boolean.parseBoolean(System.getProperty("appenders.servicediscovery.enabled", "true"));
        final String serviceDiscoveryList = System.getProperty("smokeTest.servicediscovery.serverList", "localhost:9200");
        final String nodesFilter = System.getProperty("smokeTest.servicediscovery.nodesFilter", ElasticsearchNodesQuery.DEFAULT_NODES_FILTER);
        final boolean dataStreamsEnabled = Boolean.parseBoolean(System.getProperty("smokeTest.dataStreams.enabled", "true"));

        getConfig().add("batchSize", batchSize)
                .add("initialBatchPoolSize", initialBatchPoolSize)
                .add("initialItemBufferSizeInBytes", initialItemBufferSizeInBytes)
                .add("initialBatchPoolSize", initialBatchPoolSize)
                .add("indexName", indexName)
                .add("ecs.enabled", ecsEnabled)
                .add("dataStreams.enabled", dataStreamsEnabled)
                .add("servicediscovery.nodesFilter", nodesFilter)
                .add("servicediscovery.enabled", serviceDiscoveryEnabled);

        getLogger().info("{}", getConfig().getAll());

        Configuration configuration = LoggerContext.getContext(false).getConfiguration();

        int estimatedBatchSizeInBytes = batchSize * initialItemBufferSizeInBytes;
        PooledItemSourceFactory pooledItemSourceFactory = batchItemPool(initialBatchPoolSize, estimatedBatchSizeInBytes).build();

        HttpClientFactory.Builder httpConfig = new HttpClientFactory.Builder()
                .withServerList(new ArrayList<>())
                .withConnTimeout(500)
                .withReadTimeout(2000)
                .withIoThreadCount(4)
                .withMaxTotalConnections(4)
                .withAuth(secured ? getAuth() : null)
                .withPooledResponseBuffers(true)
                .withPooledResponseBuffersSizeInBytes(1048576);

        HttpClientProvider clientProvider = new HttpClientProvider(httpConfig);

        HCHttp.Builder httpObjectFactoryBuilder = new HCHttp.Builder()
                .withBatchOperations(new ElasticsearchDataStreamBatchOperations(pooledItemSourceFactory))
                .withClientProvider(clientProvider)
                .withBackoffPolicy(new BatchLimitBackoffPolicy<>(4));

        if (serviceDiscoveryEnabled) {

            HttpClientProvider serviceDiscoveryClientProvider = new HttpClientProvider(new HttpClientFactory.Builder()
                    .withServerList(getServerList(secured, serviceDiscoveryList))
                    .withReadTimeout(1000)
                    .withConnTimeout(500)
                    .withMaxTotalConnections(1)
                    .withIoThreadCount(1)
                    .withPooledResponseBuffers(true)
                    .withPooledResponseBuffersSizeInBytes(4096));

            ClientProviderPolicy<HttpClient> clientProviderPolicy = new ClientProviderPoliciesRegistry().get(
                    new HashSet<>(Arrays.asList("none")),
                    serviceDiscoveryClientProvider);

            ServiceDiscoveryFactory<HttpClient> serviceDiscoveryFactory = new ServiceDiscoveryFactory<>(
                    clientProviderPolicy,
                    new ElasticsearchNodesQuery(secured ? "https" : "http", nodesFilter),
                    5000L
            );

            httpConfig.withServiceDiscovery(serviceDiscoveryFactory.create(clientProvider));

        }

        httpObjectFactoryBuilder
                .withClientProvider(clientProvider)
                .withOperationFactory(new ElasticsearchOperationFactory(
                        new SyncStepProcessor(clientProvider, configuredReader()),
                        new Log4j2Lookup(configuration.getStrSubstitutor())));


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
                .withName(indexName + (dataStreamsEnabled ? "-data-stream" : "-composed-index-template"))
                .withPath(getComposableIndexTemplateName())
                .build();

        ILMPolicy ilmPolicy = new ILMPolicy(
                indexName + "-ilm-policy",
                indexName,
                !dataStreamsEnabled,
                ResourceUtil.loadResource("classpath:ilmPolicy-7.json"));

        DataStream dataStream = new DataStream.Builder()
                .withName(indexName + "-data-stream")
                .build();

        KeySequenceSelector keySequenceSelector =
                new Log4j2SingleKeySequenceSelector.Builder()
                        .withSequenceId(1)
                        .build();

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(httpObjectFactoryBuilder.build())
                .withBatchSize(batchSize + additionalBatchSize)
                .withDeliveryInterval(1000)
                .withSetupOpSources(indexSettings, indexSettingsIlm, indexMappings, componentIndexTemplate, ilmPolicy, dataStream)
                .withFailoverPolicy(new ChronicleMapRetryFailoverPolicy.Builder()
                        .withKeySequenceSelector(keySequenceSelector)
                        .withFileName(resolveChronicleMapFilePath(indexName + ".chronicleMap"))
                        .withNumberOfEntries(1000000)
                        .withAverageValueSize(2048)
                        .withBatchSize(5000)
                        .withRetryDelay(4000)
                        .withMonitored(true)
                        .withMonitorTaskInterval(1000)
                        .build())
                .withShutdownDelayMillis(10000)
                .build();

        IndexNameFormatter indexNameFormatter = NoopIndexNameFormatter.newBuilder()
                .withIndexName(indexName + (dataStreamsEnabled ? "-data-stream" : ""))
                .build();

        JacksonJsonLayout.Builder layoutBuilder = JacksonJsonLayout.newBuilder()
                .setConfiguration(configuration)
                .withMixins(new JacksonMixIn.Builder()
                        .withTargetClass(LogEvent.class.getName())
                        .withMixInClass(LogEventJacksonDataStreamJsonMixIn.class.getName())
                        .build())
                .withVirtualProperties(
                        new VirtualProperty("hostname", "${env:hostname:-undefined}", false),
                        new VirtualProperty("progField", "constantValue", false)
                )
                .withSingleThread(getConfig().getProperty("singleThread", Boolean.class))
                .withJacksonModules(ExampleJacksonModule.newBuilder().build());

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
                    .withMaxItemSizeInBytes(initialItemBufferSizeInBytes * 2)
                    .withResizePolicy(new UnlimitedResizePolicy.Builder().build())
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

    @NotNull
    private String getComposableIndexTemplateName() {
        final boolean dsEnabled = getConfig().getProperty("dataStreams.enabled", Boolean.class);
        return String.format("classpath:composableIndexTemplate-7%s.json", dsEnabled ? "-datastream"  :"");
    }

    private PooledItemSourceFactory.Builder batchItemPool(int initialBatchPoolSize, int estimatedBatchSizeInBytes) {
        return PooledItemSourceFactory.newBuilder()
                .withPoolName("batchPool")
                .withInitialPoolSize(initialBatchPoolSize)
                .withItemSizeInBytes(estimatedBatchSizeInBytes)
                .withMonitored(true)
                .withMonitorTaskInterval(10000);
    }

    private List<String> getServerList(boolean secured, String hostPortList) {
        return SplitUtil.split(hostPortList, ";").stream()
                .map(uri -> String.format("%s://%s", (secured ? "https" : "http"), uri))
                .collect(Collectors.toList());
    }

    private ObjectReader configuredReader() {
        return new ObjectMapper()
                .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .addMixIn(BatchResult.class, BatchResultMixIn.class)
                .addMixIn(Error.class, ErrorMixIn.class)
                .addMixIn(BatchItemResult.class, BatchItemResultMixIn.class)
                .readerFor(BatchResult.class);
    }

    private static Auth<HttpClientFactory.Builder> getAuth() {
        CertInfo<HttpClientFactory.Builder> certInfo = PEMCertInfo.newBuilder()
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

        Credentials<HttpClientFactory.Builder> credentials = BasicCredentials.newBuilder()
                .withUsername("admin")
                .withPassword("changeme")
                .build();

        return new Security.Builder()
                .withCertInfo(certInfo)
                .withCredentials(credentials)
                .build();
    }

}
