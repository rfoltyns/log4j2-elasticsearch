# log4j2-elasticsearch-hc
This log4j2 appender plugin uses Apache Async HTTP client to push logs in batches to Elasticsearch clusters.

Netty buffer based API guarantees lower memory allocation than [log4j2-elasticsearch-jest](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-jest).

Output is generated with FasterXML Jackson based [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java).

## Maven

```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-hc</artifactId>
    <version>1.5.2</version>
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
| Name                             | Type      | Required                                                        | Default                     | Description                                                                                                                                    |
|----------------------------------|-----------|-----------------------------------------------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| serverUris                       | Attribute | no (MUST be specified by either `HCHttp` or `ServiceDiscovery`) | None                        | List of semicolon-separated `http[s]://host:[port]` addresses of Elasticsearch nodes to connect with.                                          |
| connTimeout                      | Attribute | no                                                              | 1000                        | Number of milliseconds before ConnectException is thrown while attempting to connect.                                                          |
| readTimeout                      | Attribute | no                                                              | 0                           | Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes.                                               |
| maxTotalConnections              | Attribute | no                                                              | 8                           | Number of connections available.                                                                                                               |
| ioThreadCount                    | Attribute | no                                                              | No. of available processors | Number of `I/O Dispatcher` threads started by Apache HC `IOReactor`                                                                            |
| itemSourceFactory                | Element   | yes                                                             | None                        | `ItemSourceFactory` used to create wrappers for batch requests. `PooledItemSourceFactory` and it's extensions can be used.                     |
| mappingType                      | Attribute | no                                                              | `_doc`                      | Name of index mapping type to use in ES cluster. `_doc` is used by default for compatibility with Elasticsearch 7.x.                           |
| pooledResponseBuffers            | Attribute | no                                                              | yes                         | If `true`, pooled `SimpleInputBuffer`s will be used to handle responses. Otherwise, new `SimpleInputBuffer` wil be created for every response. |
| pooledResponseBuffersSizeInBytes | Attribute | no                                                              | 1MB (1048756 bytes)         | Single response buffer size.                                                                                                                   |
| auth                             | Element   | no                                                              | None                        | Security config. [Security](#pem-cert-config)                                                                                                  |
| serviceDiscovery                 | Element   | no                                                              | None                        | Service discovery config. [ServiceDiscovery](../log4j2-elasticsearch-hc#service-discovery)                                                     |

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

| Feature/Version  | 2.x        | 5.x        | 6.x        | 7.x        |
|------------------|------------|------------|------------|------------|
| IndexTemplate    | Yes        | Yes        | Yes        | Yes        |
| BasicCredentials | Yes        | Yes        | Yes        | Yes        |
| JKS              | Yes        | Not tested | Not tested | Not tested |
| PEM              | Not tested | Yes        | Yes        | Yes        |

## Pluggable JCTools

See [Pluggable JCTools](../log4j2-elasticsearch-core#pluggable-jctools)

JVM params:

| Param                                    | Type    | Default |
|------------------------------------------|---------|---------|
| -Dappenders.BatchRequest.jctools.enabled | boolean | true    |
| -Dappenders.BatchRequest.initialSize     | int     | 10000   |

## Dependencies

Be aware that following jars may have to be provided by user for this library to work:

* Netty: `io.netty:netty-buffer`
* Jackson FasterXML: `com.fasterxml.jackson.core:jackson-core,jackson-databind,jackson-annotations`
* Jackson FasterXML Afterburner module if `JacksonJsonLayout:afterburner=true`: `com.fasterxml.jackson.module:jackson-module-afterburner`
* Log4j2: `org.apache.logging.log4j:log4-api,log4j-core`
* Disruptor (if using `AsyncAppender`): `com.lmax:disruptor`
* Bouncy Castle (if using `Security`): `org.bouncycastle:bcprov-jdk15on,bcpkix-jdk15on`

See `pom.xml` or deps summary at [Maven Repository](https://mvnrepository.com/artifact/org.appenders.log4j/log4j2-elasticsearch-hc/latest) for a list of dependencies.
