# log4j2-elasticsearch-hc
This log4j2 appender plugin uses Apache Async HTTP client to push logs in batches to Elasticsearch clusters.

Netty buffer based API guarantees lower memory allocation than [log4j2-elasticsearch-jest](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-jest).

Output is generated with FasterXML Jackson based [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java).

## Maven

```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-hc</artifactId>
    <version>1.5.5</version>
</dependency>
```
## Usage

Add `HCHttp` to
* [`logj2.xml`](#example)
* or [log4j2.properties](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/log4j2.properties)
* or [configure programatically](#programmatic-config)

It's highly recommended to put this plugin behind `AsyncLogger`. See [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/log4j2.xml) example.

##### Example
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <IndexName indexName="log4j2"/>
        <JacksonJsonLayout>
            <PooledItemSourceFactory poolName="itemPool" itemSizeInBytes="1024" initialPoolSize="3000"/>
        </JacksonJsonLayout>
        <AsyncBatchDelivery batchSize="1000" deliveryInterval="10000" >
            <IndexTemplate name="log4j2" path="classpath:indexTemplate-7.json" />
            <HCHttp serverUris="http://localhost:9200">
                <PooledItemSourceFactory poolName="batchPool" itemSizeInBytes="1024000" initialPoolSize="3"/>
            </HCHttp>
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

### HCHttp Properties
| Name                             | Type      | Required                                                        | Default                     | Description                                                                                                                                                                                                                                                                                                                                                      |
|----------------------------------|-----------|-----------------------------------------------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| serverUris                       | Attribute | no (MUST be specified by either `HCHttp` or `ServiceDiscovery`) | None                        | List of semicolon-separated `http[s]://host:[port]` addresses of Elasticsearch nodes to connect with.                                                                                                                                                                                                                                                            |
| connTimeout                      | Attribute | no                                                              | 1000                        | Number of milliseconds before ConnectException is thrown while attempting to connect.                                                                                                                                                                                                                                                                            |
| readTimeout                      | Attribute | no                                                              | 0                           | Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes.                                                                                                                                                                                                                                                                 |
| maxTotalConnections              | Attribute | no                                                              | 8                           | Number of connections available.                                                                                                                                                                                                                                                                                                                                 |
| ioThreadCount                    | Attribute | no                                                              | No. of available processors | Number of `I/O Dispatcher` threads started by Apache HC `IOReactor`                                                                                                                                                                                                                                                                                              |
| itemSourceFactory                | Element   | yes                                                             | None                        | `ItemSourceFactory` used to create wrappers for batch requests. `PooledItemSourceFactory` and it's extensions can be used.                                                                                                                                                                                                                                       |
| mappingType                      | Attribute | no                                                              | `null` since 1.6            | Name of index mapping type to use. Applicable to Elasticsearch <8.x. See [removal of types](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/removal-of-types.html). <br/> DEPRECATED: As of 1.7, this attribute will be removed. Use [ElasticsearchBulk](#elasticsearchbulk) instead.                                                               |
| pooledResponseBuffers            | Attribute | no                                                              | yes                         | If `true`, pooled `SimpleInputBuffer`s will be used to handle responses. Otherwise, new `SimpleInputBuffer` wil be created for every response.                                                                                                                                                                                                                   |
| pooledResponseBuffersSizeInBytes | Attribute | no                                                              | 1MB (1048756 bytes)         | Single response buffer size.                                                                                                                                                                                                                                                                                                                                     |
| auth                             | Element   | no                                                              | None                        | Security config. [Security](#pem-cert-config)                                                                                                                                                                                                                                                                                                                    |
| serviceDiscovery                 | Element   | no                                                              | None                        | Service discovery config. [ServiceDiscovery](#service-discovery)                                                                                                                                                                                                                                                                                                 |
| clientAPIFactory                 | Element   | no                                                              | `ElasticsearchBulk`         | Batch API factory. [ElasticsearchBulk](#elasticsearchbulk)                                                                                                                                                                                                                                                                                                       |
| name                             | Attribute | No                                                              | `HCHttp`                    | Metric component name                                                                                                                                                                                                                                                                                                                                            |
| metricConfig                     | Element[] | No                                                              | Disabled `MetricConfig`(s)  | `Metrics` supported by this component:<br/>- `serverTookMs`<br/>- `itemsSent`<br/>- `itemsDelivered`<br/>- `itemsFailed`<br/>- `backoffApplied`<br/>- `batchesFailed`<br/>-`failoverTookMs`<br/>-`responseBytes` <br/> - `initial`<br/>- `total`<br/>- `available`<br/>- `noSuchElementCaught`<br/>- `resizeAttempts`<br/>See `Metrics` docs below for more info |

### Service Discovery

Since 1.5, service discovery can be configured using `ServiceDiscovery` tag. Once defined, batches and setup operations will be executed against addresses retrieved from [Nodes API](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-nodes-info.html).

Supported across all versions of Elasticsearch.

Custom [`ServiceDiscoveryFactory`](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/discovery/ServiceDiscoveryFactory.java) can be defined to integrate with other address sources.

| Name                             | Type      | Required | Default               | Description                                                                                                                                                                                                                                                     |
|----------------------------------|-----------|----------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| nodesFilter                      | Attribute | no       | `_all`                | Nodes filter as defined in [Elasticsearch 7.x documentation](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/cluster.html#cluster-nodes) (`_nodes/<nodesFilter>/http`).                                                                             |
| serverUris                       | Attribute | no       | inherited from HCHttp | List of semicolon-separated address sources.                                                                                                                                                                                                                    |
| connTimeout                      | Attribute | no       | 500                   | Number of milliseconds before ConnectException is thrown while attempting to connect.                                                                                                                                                                           |
| readTimeout                      | Attribute | no       | 1000                  | Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes.                                                                                                                                                                |
| pooledResponseBuffersSizeInBytes | Attribute | no       | 32KB (32768 bytes)    | Single response buffer size.                                                                                                                                                                                                                                    |
| targetScheme                     | Attribute | no       | http                  | Scheme of resolved addresses passed to HCHttp client.                                                                                                                                                                                                           |
| refreshInterval                  | Attribute | no       | 30000                 | Number of milliseconds between the end of previous request and start of new one.                                                                                                                                                                                |
| configPolicies                   | Attribute | no       | serverList,security   | List of comma-separated config policies to apply. Available policies: `shared`, `none`, `serverList`, `security`. See [Config Policies](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc#config-policies) for more details. |

Example:
```xml
<Elasticsearch ...>
    <AsyncBatchDelivery ... >
        ...
        <HCHttp ...>
            ...
            <ServiceDiscovery serverUris="http://localhost:9250"
                              refreshInterval="10000"
                              nodesFilter="ingest:true" />
            ...
        </HCHttp>
    </AsyncBatchDelivery>
</Elasticsearch>
```

#### Config policies

`ServiceDiscovery` can inherit parts of HCHttp client config or even reuse whole client. By default, `HCHttp.serverUris` and `Security` settings are reused during creation of new `ServiceDiscovery` HTTP client. Following policies are available:
* `serverList` - create a new client, inherit HCHttp.serverUris
* `security` - create a new client, inherit HCHttp.auth
* `shared` - reuse HCHttp client
* `none` - create a new client, don't inherit anything

`serverList` and `security` can be used together. New client will inherit both config elements.

`shared` and `none` can only be used individually. They can't be mixed with other policies.

Example:
```xml
<Elasticsearch ...>
    <AsyncBatchDelivery ... >
        ...
        <HCHttp>
            ...
            <Security .../>
            <ServiceDiscovery serverUris="https://localhost:9200"
                              refreshInterval="10000"
                              targetScheme="https"
                              configPolicies="security" />
            ...
        </HCHttp>
    </AsyncBatchDelivery>
</Elasticsearch>
```

NOTE: Config policies were added for convenience. Recommended configuration should contain `configPolices=none`, `serverUris` configured ONLY for `ServiceDiscovery` and separate `Security` configs if needed.

### Client API Factory

Since 1.6, [ClientAPIFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/ClientAPIFactory.java) can be configured to further customize the output and runtime capabilities of batches and batch items. [ElasticsearchBulk](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/ElasticsearchBulkPlugin.java) is used by default.

#### ElasticsearchBulk

Default.

Configures builders and serializers for:
* [BatchRequest](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/BatchRequest.java) - `/_bulk` request (batch)
* [IndexRequest](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/IndexRequest.java) - document (batch item)

```xml
<HCHttp>
    <ElasticsearchBulk/>
</HCHttp>
```

#### ElasticsearchBulk Properties
| Name        | Type      | Required | Default          | Description                                                                                                                                                                                                                 |
|-------------|-----------|----------|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| mappingType | Attribute | no       | `null` since 1.6 | Name of index mapping type to use. Applicable to Elasticsearch <8.x. See [removal of types](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/removal-of-types.html).                                            |
| filterPath  | Attribute | no       | `null`           | If not empty, `filter_path` query param will be added to Bulk API requests. See [Response Filtering](https://www.elastic.co/guide/en/elasticsearch/reference/current/common-options.html#common-options-response-filtering) |

### ElasticsearchDataStream

Since 1.6, [Data streams](https://www.elastic.co/guide/en/elasticsearch/reference/current/data-streams.html) are supported with `DataStream` setup operation.

Configures `BatchOperations`-level builders and serializers for:
* [DataStreamBatchRequest](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/DataStreamBatchRequest.java) - `/<indexName>/_bulk` request (batch)
* [DataStreamItem](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/DataStreamItem.java) - document (batch item)

If `ILMPolicy` is used, `ILMPolicy.createBootstrapIndex` MUST be set to `false`. This behaviour will be changed in future releases - bootstrap index will be created separately, similar to other setup operations.

With `JacksonJsonLayout`, use [LogEventDataStreamMixIn](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/json/jackson/LogEventDataStreamMixIn.java) or equivalent to serialize `LogEvent.timeMillis` as `@timestamp`.

```xml
<Elasticsearch>
    <JacksonJsonLayout>
        <JacksonMixIn targetClass="org.apache.logging.log4j.core.LogEvent"
                      mixInClass="org.appenders.log4j2.elasticsearch.json.jackson.LogEventDataStreamMixIn" />
    </JacksonJsonLayout>
    <AsyncBatchDelivery>
        <ILMPolicy createBootstrapIndex="false" />
        <HCHttp>
            <ElasticsearchDataStream />
        </HCHttp>
    </AsyncBatchDelivery>
</Elasticsearch>
```

### Programmatic config
See [programmatc config example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/java/org/appenders/log4j2/elasticsearch/hc/smoke/SmokeTest.java).

### Delivery frequency
See [delivery frequency](../log4j2-elasticsearch-core#delivery-frequency)

### Message output
See [available output configuration methods](../log4j2-elasticsearch-core#message-output)

### Failover
See [failover options](../log4j2-elasticsearch-core#failover)

### Backoff
Since 1.4, `BackoffPolicy` can provide additional fail-safe during delivery.
In the event of cluster failure or slowdown, when policy gets triggered, batch will be automatically redirected to configured `FailoverPolicy` (see [failover options](../log4j2-elasticsearch-core#failover)).

#### BatchLimitBackoffPolicy

`BatchLimitBackoffPolicy` is a simple `BackoffPolicy` based on a number of currently processed batches (batches that arrived at the `HCHttp` batch listener). Every arriving batch will increase the "in-flight count" by 1 if `maxBatchesInFlight` is not exceeded. Every response to a batch request decreases "in-flight count" by 1. Every batch that arrives when `maxBatchesInFlight` limit is met is redirected to configured `FailoverPolicy`.

:warning: **See [failover options](../log4j2-elasticsearch-core#failover) for available `FailoverPolicy` implementations. If no `FailoverPolicy` is configured, batch will be lost!**

| Name               | Type      | Required | Default | Description                                                                                     |
|--------------------|-----------|----------|---------|-------------------------------------------------------------------------------------------------|
| maxBatchesInFlight | Attribute | no       | 8       | Maximum number of batches delivered simultaneously (including the ones waiting for a response). |

Example:
``` xml
<Elasticsearch ...>
    <AsyncBatchDelivery ... >
        ...
        <HCHttp ...>
            ...
            <BatchLimitBackoffPolicy maxBatchesInFlight="4" />
            ...
        </HCHttp>
    </AsyncBatchDelivery>
</Elasticsearch>
```

### Index name
See [index name](../log4j2-elasticsearch-core#index-name) or [index rollover](../log4j2-elasticsearch-core#index-rollover)

### Index template

Since 1.6, this module is compatible with Elasticsearch 8.x by default. Use `apiVersion` for older clusters.

See [index template docs](../log4j2-elasticsearch-core#index-template)

### SSL/TLS
Can be configured using `Security` tag:

#### PEM cert config
```xml
<HCHttp serverUris="https://localhost:9200" >
    <Security>
        <BasicCredentials username="admin" password="changeme" />
        <PEM keyPath="${sys:pemCertInfo.keyPath}"
             keyPassphrase="${sys:pemCertInfo.keyPassphrase}"
             clientCertPath="${sys:pemCertInfo.clientCertPath}"
             caPath="${sys:pemCertInfo.caPath}" />
    </Security>
</HCHttp>
```

#### JKS cert config
```xml
<HCHttp serverUris="https://localhost:9200" >
    <Security>
        <BasicCredentials username="admin" password="changeme" />
        <JKS keystorePath="${sys:jksCertInfo.keystorePath}"
             keystorePassword="${sys:jksCertInfo.keystorePassword}"
             truststorePath="${sys:jksCertInfo.truststorePath}"
             truststorePassword="${sys:jksCertInfo.truststorePassword}" />
    </Security>
</HCHttp>
```

### Compatibility matrix

| Feature/Version  | 2.x        | 5.x        | 6.x        | 7.x        | 8.x        |
|------------------|------------|------------|------------|------------|------------|
| IndexTemplate    | Yes        | Yes        | Yes        | Yes        | Yes        |
| BasicCredentials | Yes        | Yes        | Yes        | Yes        | Yes        |
| JKS              | Yes        | Not tested | Not tested | Not tested | Not tested |
| PEM              | Not tested | Yes        | Yes        | Yes        | Yes        |

## Pluggable JCTools

See [Pluggable JCTools](../log4j2-elasticsearch-core#pluggable-jctools)

JVM params:

| Param                                    | Type    | Default |
|------------------------------------------|---------|---------|
| -Dappenders.BatchRequest.jctools.enabled | boolean | true    |
| -Dappenders.BatchRequest.initialSize     | int     | 10000   |

## Metrics

See [Core Metrics](../log4j2-elasticsearch-core#metrics) for detailed documentation.

### Measured HC components

All [Measured Core components](../log4j2-elasticsearch-core#measured-core-components) and:

[HCHttp](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/HCHttp.java):
* itemsSent `Count`: number of items sent to cluster
* itemsDelivered `Count`: number of items successfully delivered to cluster
* itemsFailed `Count`: number of items received by failure handler as a result of cluster response or cluster unavailability
* backoffApplied `Count`: number of items dropped due to back-off policy kicking in (still delivered to `FailoverPolicy` and counted as `itemsFailed`)
* batchesFailed `Count`: number of batches received by failure handler as a result of cluster response or cluster unavailability
* serverTookMs `Max`: maximum time spent on cluster side
* failoverTookMs `Max`: maximum time spent handling failover as a result of cluster response, cluster unavailability or back-off policy
* all [GenericItemSourcePool](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/GenericItemSourcePool.java) metrics
* all [PoolingAsyncResponseConsumerFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/PoolingAsyncResponseConsumerFactory.java) metrics if `pooledResponseBuffers` set to `true`

Example:
```xml
<HCHttp name="some-sensible-name" serverUris="https://localhost:9200" >
    <Metrics>
        <Count name="itemsSent" />
        <Count name="itemsDelivered" />
        <Count name="itemsFailed" />
        <Count name="backoffApplied" />
        <Count name="batchesFailed" />
        <Max name="serverTookMs" />
        <Max name="failoverTookMs" />
    </Metrics>
</HCHttp>
```

[ServiceDiscovery](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/discovery/HCServiceDiscovery.java):
* If `policies == "shared"`, all [PoolingAsyncResponseConsumerFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/PoolingAsyncResponseConsumerFactory.java) metrics configured at HTTP client at `HCHttp` level. Otherwise, all [PoolingAsyncResponseConsumerFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/PoolingAsyncResponseConsumerFactory.java) metrics of service discovery client

Example:
```xml
<HCHttp name="some-sensible-name" serverUris="https://localhost:9200" >
    <ServiceDiscovery name="another-sensible-name"
                      serverUris="https://localhost:9200"
                      targetScheme="https"
                      refreshInterval="10000"
                      nodesFilter="ingest:true"
                      policies="none">
        <Count name="available" />
        <Count name="responseBytes" />
    </ServiceDiscovery>
</HCHttp>
```

[PoolingAsyncResponseConsumerFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/main/java/org/appenders/log4j2/elasticsearch/hc/PoolingAsyncResponseConsumerFactory.java) if `pooledResponseBuffers` set to `true`. Actually, `HttpClient` metrics at respective level (`HCHttp` or `ServiceDiscovery`):
* responseBytes `Count`- response content length
* all [GenericItemSourcePool](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/GenericItemSourcePool.java) metrics

Example:
```xml
<HCHttp name="some-sensible-name" serverUris="https://localhost:9200" >
    <Metrics>
        <Count name="responseBytes" />
    </Metrics>
</HCHttp>
```

More metrics will become available in future releases. [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

## Dependencies

Be aware that following jars may have to be provided by user for this library to work:

* Netty: `io.netty:netty-buffer`
* Jackson FasterXML: `com.fasterxml.jackson.core:jackson-core,jackson-databind,jackson-annotations`
* Jackson FasterXML Afterburner module if `JacksonJsonLayout:afterburner=true`: `com.fasterxml.jackson.module:jackson-module-afterburner`
* Log4j2: `org.apache.logging.log4j:log4-api,log4j-core`
* Disruptor (if using `AsyncAppender`): `com.lmax:disruptor`
* Bouncy Castle (if using `Security`): `org.bouncycastle:bcprov-jdk15on,bcpkix-jdk15on`

See `pom.xml` or deps summary at [Maven Repository](https://mvnrepository.com/artifact/org.appenders.log4j/log4j2-elasticsearch-hc/latest) for a list of dependencies.
