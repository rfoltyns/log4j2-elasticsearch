# log4j2-elasticsearch-jest
This log4j2 appender plugin uses Jest HTTP client to push logs in batches to Elasticsearch 2.x, 5.x and 6.x clusters. By default, FasterXML is used generate output via [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java).

## Maven

To use it, add this XML snippet to your `pom.xml` file:
```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-jest</artifactId>
    <version>1.4.6</version>
</dependency>
```

## Appender configuration

Add one of following config elements to your `logj2.xml`:
* [`jestHttp`](#http)
* [`jestBufferedHttp`](#buffered-http)

or [log4j2.properties](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/resources/log4j2.properties)
or [configure programatically](#programmatic-config).

It's highly encouraged to put this plugin behind `Async` appender or `AsyncLogger`. See [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/resources/log4j2.xml) example.

### HTTP

`JestHttp` - Jest HTTP client using Apache HC.

Add this snippet to `log4j2.xml` configuration:
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <IndexName indexName="log4j2" />
        <JacksonJsonLayout />
        <AsyncBatchDelivery batchSize="1000" deliveryInterval="5000" >
            <IndexTemplate name="log4j2" path="classpath:indexTemplate.json" />
            <JestHttp serverUris="http://localhost:9200" mappingType="<see mappingType description>"/>
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
serverUris | Attribute | yes | None | List of semicolon-separated `http[s]://host:[port]` addresses of Elasticsearch nodes to connect with. Unless `discoveryEnabled=true`, this will be the final list of available nodes. 
connTimeout | Attribute | no | -1 | Number of milliseconds before ConnectException is thrown while attempting to connect.
readTimeout | Attribute | no | -1 | Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes.
maxTotalConnection | Attribute | no | 40 | Number of connections available.
defaultMaxTotalConnectionPerRoute | Attribute | no | 4 | Number of connections available per Apache CPool.
discoveryEnabled | Attribute | no | false | If `true`, `io.searchbox.client.config.discovery.NodeChecker` will use `serverUris` to auto-discover Elasticsearch nodes. Otherwise, `serverUris` will be the final list of available nodes.
ioThreadCount | Attribute | no | No. of available processors | Number of `I/O Dispatcher` threads started by Apache HC `IOReactor`
mappingType | Attribute | no | `index` | Name of index mapping type to use in ES cluster. Use `_doc` for Elasticsearch 7.x.

### Buffered HTTP

`JestBufferedHttp` - extension of `JestHttp`. Uses [org.appenders.log4j2.elasticsearch.BufferedBulk](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/main/java/org/appenders/log4j2/elasticsearch/jest/BufferedBulk.java) and [org.appenders.log4j2.elasticsearch.BufferedIndex](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/main/java/org/appenders/log4j2/elasticsearch/jest/BufferedIndex.java) to replace Jest default (de)serialization and utilize [pooled buffers](../log4j2-elasticsearch-core#object-pooling) to reduce memory allocation.
[PooledItemSourceFactory](../log4j2-elasticsearch-core#object-pooling) MUST be configured in order for this client to work.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
All `JestHttp` properties | - | - | - | -
itemSourceFactory | Element | yes | None | `ItemSourceFactory` used to create wrappers for batch requests. `PooledItemSourceFactory` and it's extensions can be used.

Example:
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <IndexName indexName="log4j2" />
        <JacksonJsonLayout>
            <PooledItemSourceFactory itemSizeInBytes="1024" initialPoolSize="4000" />
        </JacksonJsonLayout>
        <AsyncBatchDelivery batchSize="1000" deliveryInterval="5000" >
            <IndexTemplate name="log4j2" path="classpath:indexTemplate.json" />
            <JestBufferedHttp serverUris="http://localhost:9200" mappingType="<see mappingType description>">
                <PooledItemSourceFactory itemSizeInBytes="1024000" initialPoolSize="4" />
            </JestBufferedHttp>
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

### Programmatic config
See [programmatc config example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/SmokeTest.java).

### Delivery frequency
See [delivery frequency](../log4j2-elasticsearch-core#delivery-frequency)

### Message output
See [available output configuration methods](../log4j2-elasticsearch-core#message-output)

### Failover
See [failover options](../log4j2-elasticsearch-core#failover)

### Index name
See [index name](../log4j2-elasticsearch-core#index-name) or [index rollover](../log4j2-elasticsearch-core#index-rollover)

### Index template
See [index template docs](../log4j2-elasticsearch-core#index-template)

### SSL/TLS
Since 1.2, HTTPS can be configured using `XPackAuth` tag:

#### PEM cert config
```xml
<JestHttp serverUris="https://localhost:9200" >
    <XPackAuth>
        <BasicCredentials username="admin" password="changeme" />
        <PEM keyPath="${sys:pemCertInfo.keyPath}"
             keyPassphrase="${sys:pemCertInfo.keyPassphrase}"
             clientCertPath="${sys:pemCertInfo.clientCertPath}"
             caPath="${sys:pemCertInfo.caPath}" />
    </XPackAuth>
</JestHttp>
```

#### JKS cert config
```xml
<JestHttp serverUris="https://localhost:9200" >
    <XPackAuth>
        <BasicCredentials username="admin" password="changeme" />
        <JKS keystorePath="${sys:jksCertInfo.keystorePath}"
             keystorePassword="${sys:jksCertInfo.keystorePassword}"
             truststorePath="${sys:jksCertInfo.truststorePath}"
             truststorePassword="${sys:jksCertInfo.truststorePassword}" />
    </XPackAuth>
</JestHttp>
```

### Compatibility matrix

Feature/Version | 2.x | 5.x | 6.x| 7.x
------------ | ------------- | ------------- | -------------| -------------
IndexTemplate | Yes | Yes | Yes| Yes
BasicCredentials | Yes | Yes | Yes| Yes
JKS | Yes | Not tested | Not tested| Not tested
PEM | Not tested | Yes | Yes| Yes

## Pluggable JCTools

See [Pluggable JCTools](../log4j2-elasticsearch-core#pluggable-jctools)

JVM params:

Param | Type | Default
------------ | ------------- | -------------
-Dappenders.BufferedBulk.jctools.enabled | boolean | true
-Dappenders.BufferedBulk.initialSize | int | 10000

NOTE: `JestBufferedHttp` support only

## Dependencies

Be aware that following jars have to be provided by user for this library to work in default mode:
* Jackson FasterXML: `com.fasterxml.jackson.core:jackson-core,jackson-databind,jackson-annotations`
* Jackson FasterXML Afterburner module if `JacksonJsonLayout:afterburner=true`: `com.fasterxml.jackson.module:jackson-module-afterburner`
* Log4j2: `org.apache.logging.log4j:log4-api,log4j-core`
* Disruptor (if using `AsyncAppender`): `com.lmax:distuptor`
* Bouncy Castle (if using `XPackAuth`): `org.bouncycastle:bcprov-jdk15on,bcpkix-jdk15on`

See `pom.xml` or deps summary at [Maven Repository](https://mvnrepository.com/artifact/org.appenders.log4j/log4j2-elasticsearch-jest/latest) for a list of dependencies.
