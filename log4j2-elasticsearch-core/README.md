# log4j2-elasticsearch-core
Core provides a skeleton for `ClientObjectFactory` implementations: a set of interfaces and base classes to push logs in batches to Elasticsearch cluster. By default, FasterXML is used generate output using [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java).

### Maven

To use it, add this XML snippet to your `pom.xml` file:
```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-core</artifactId>
    <version>1.3.5</version>
</dependency>
```

However, direct use of this library is required only in case of Log4j2 configuration with Java API or user-provided extensions. This library is in `compile` scope for `jest` and `bulkprocessor` implementations.

## Extensibility

Main parts of the skeleton are:
* `ClientObjectFactory` - provider of client-specific request classes, factories and error handlers
* `BatchEmitter` - intermediate log collector which will trigger batch delivery as configured (see below).

### ItemSource API
Since 1.3, `org.appenders.log4j2.elasticsearch.ItemSource` and a set of related interfaces are available.

The main goal was to introduce an envelope with API to process and manage underlying payloads. A good example is `org.appenders.log4j2.elasticsearch.BufferedItemSource` - poolable payload container backed by Netty buffer.

Main parts of default implementation are:
* `ItemSource` - envelope for payloads; [StringItemSource](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/StringItemSource.java) is used by default
* `ItemSourceFactory` - `ItemSource` producer capable of serializing given objects into one of underlying `ItemSource` implementations; [StringItemSourceFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/StringItemSourceFactory.java) is used by default
* `ItemSourceLayout` - new layout meant to bind Log4j2 API to `ItemSourceFactory` calls; [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java) by default

### AsyncBatchDelivery

`AsyncBatchDelivery` uses `ClientObjectFactory` objects to produce client specific requests and deliver them to cluster via `BatchEmitter` implementations.

### BatchEmitter

`BatchEmitterFactory<T extends BatchEmitter>` implementations are located using `java.util.ServiceLoader`. `org.appenders.log4j2.elasticsearch.BulkEmitter` is the current default implementation.

## Configuration

### Delivery frequency
Delivery frequency can be adjusted via `AsyncBatchDelivery` attributes:
* `deliveryInterval` - millis between deliveries
* `batchSize` - maximum (rough) number of logs in one batch

Delivery is triggered each `deliveryInterval` or when number of undelivered logs reached `batchSize`.

`deliveryInterval` is the main driver of delivery. However, in high load scenarios, both parameters should be configured accordingly to prevent sub-optimal behaviour. See [Indexing performance tips](https://www.elastic.co/guide/en/elasticsearch/guide/current/indexing-performance.html) and [Performance Considerations](https://www.elastic.co/blog/performance-considerations-elasticsearch-indexing) for more info.

### Index name
Since 1.1, index name can be defined using `IndexName` tag:

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <IndexName indexName="log4j2" />
        ...
    </Elasticsearch>
</Appenders>
```

### Index rollover
Since 1.1, rolling index can be defined using `RollingIndexName` tag:

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <!-- zone is optional. OS timezone is used by default -->
        <RollingIndexName indexName="log4j2" pattern="yyyy-MM-dd" timeZone="Europe/Warsaw" />
        ...
    </Elasticsearch>
</Appenders>
```

`pattern` accepts any valid date pattern with years down to millis (although rolling daily or weekly should be sufficient for most use cases)
`IndexName` and `RollingIndexName` are mutually exclusive. Only one per appender should be defined, otherwise they'll override each other.

### Index template
Since 1.1, [Index templates](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/indices-templates.html) can be created during appender startup. Template can be loaded from specified file or defined directly in the XML config:

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <AsyncBatchDelivery>
            <IndexTemplate name="template1" path="<absolute_path_or_classpath>" />
            ...
        </AsyncBatchDelivery>
        ...
    </Elasticsearch>
</Appenders>
```
or
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <AsyncBatchDelivery>
            <IndexTemplate name="template1" >
            {
                // your index template in JSON format
            }
            </IndexTemplate>
            ...
        </AsyncBatchDelivery>
        ...
    </Elasticsearch>
</Appenders>
```

NOTE: Be aware that template parsing errors on cluster side DO NOT prevent plugin from loading - error is logged on client side and startup continues.

### Message output

There are numerous ways to generate JSON output:

#### JacksonJsonLayout

(default)

Since 1.3, [org.appenders.log4j2.elasticsearch.JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java) is the default implemetation of [ItemSourceLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ItemSourceLayout.java). It will serialize LogEvent using Jackson mapper configured with a set of default and (optional) user-provided mixins (see: [JacksonMixInAnnotations docs](https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations)). 

Default set of mixins limits LogEvent output by shrinking serialized properties list to a 'reasonable minimum'.
Customization of all aspects of LogEvent and Message output are allowed using `JacksonMixIn` elements (see: [JacksonMixInAnnotations docs](https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations)) elements.

Furthermore, [ItemSource API](#itemsource-api) allows to use pooled [BufferedItemSource](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/BufferedItemSource.java) payloads. Pooling is optional.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
afterburner | Attribute | no | false | if `true`, `com.fasterxml.jackson.module:jackson-module-afterburner` will be used to optimize (de)serialization. Since this dependency is in `provided` scope by default, it MUST be declared explicitly.
mixins | Element(s) | no | None | Array of `JacksonMixIn` elements. Can be used to override default serialization of LogEvent, Message and related objects
itemSourceFactory | Element | no | `StringItemSourceFactory` | `ItemSourceFactory` used to create wrappers for serialized items. `StringItemSourceFactory` and `PooledItemSourceFactory` are available

Default output:

`{"timeMillis":1545968929481,"loggerName":"elasticsearch","level":"INFO","message":"Hello, World!","thread":"Thread-18"}`

Example:
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout afterburner="true">
        <PooledItemSourceFactory itemSizeInBytes="512" initialPoolSize="10000" />
        <JacksonMixIn mixInClass="foo.bar.CustomLogEventMixIn"
                      targetClass="org.apache.logging.log4j.core.LogEvent"/>
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```

Custom `org.appenders.log4j2.elasticsearch.ItemSourceLayout` can be provided to appender config to use any other serialization mechanism.

#### Log4j2 JsonLayout
JsonLayout will serialize LogEvent using Jackson mapper configured in log4j-core. Custom `org.apache.logging.log4j.core.layout.AbstractLayout` can be provided to appender config to use any other serialization mechanism.

Output may vary across different Log4j2 versions (see: #9)

#### Raw log message
`messageOnly="true"` can be configured for all the layouts mentioned above to make use of user provided (or default) `org.apache.logging.log4j.message.Message.getFormattedMessage()` implementation.

Raw log message MUST:
 * be logged with Logger that uses `org.apache.logging.log4j.message.MessageFactory` that serializes logged object to a valid JSON output

 or

 * be in JSON format already (default)

See [custom MessageFactory example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/CustomMessageFactoryTest.java)

### Failover
Each unsuccessful batch can be redirected to any given `FailoverPolicy` implementation. By default, each log entry will be separately delivered to configured strategy class, but this behaviour can be amended by providing custom `ClientObjectFactory` implementation.

### Object pooling
Since 1.3, `PooledItemSourceFactory` can be configured, providing `io.netty.buffer.ByteBuf`-backed `BufferedItemSource` instances for serialized batch items and batch requests.

Internally, [org.appenders.log4j2.elasticsearch.BufferedItemSourcePool](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/BufferedItemSourcePool.java) is used as default pool implementation.
Pool is resizable. It adjusts it's size automatically depending on current load and configured `ResizePolicy`.

Item and batch pools have to be configured separately. Currently, if item buffers are pooled, batch buffers MUST be pooled as well (see example below).

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
initialPoolSize | Attribute | Yes | None | Number of pooled elements created at startup
itemSizeInBytes | Attribute | Yes | None | Initial size of single buffer instance
resizePolicy | Element | No | `UnlimitedReizePolicy` | `ResizePolicy` used whem pool resizing is triggered
resizeTimeout | Attribute | No | 1000 | When multiple threads try to get a pooled element and pool is empty, only the first thread will trigger resizing. This attribute configures maximum interval in milliseconds between two consecutive attempts to get a pooled element by other threads.
monitored | Attribute | No | false | If `true`, pool metrics will be printed. Metrics are prined by Status Logger at `INFO` level, so be sure to modify your Log4j2 configuration accordingly
monitorTaskInterval | Attribute | No | 30000 | Interval between metrics logs. 30 seconds by default.
poolName | Attribute | No | UUID | Pool ID (useful when `monitored` is set to true)

Example:
``` xml
<Elasticsearch name="elasticsearchAsyncBatch">
    <JacksonJsonLayout>
        <PooledItemSourceFactory itemSizeInBytes="512" initialPoolSize="10000" />
    </JacksonJsonLayout>
    <AsyncBatchDelivery batchSize="5000" deliveryInterval="20000" >
        ...
        <JestBufferedHttp serverUris="https://localhost:9200">
            ...
            <PooledItemSourceFactory itemSizeInBytes="5120000" initialPoolSize="4" />
            ...
        </JestBufferedHttp>
    </AsyncBatchDelivery>
</Elasticsearch>
```

##### UnlimitedResizePolicy (default)
This resize strategy will resize given pool regardless of available memory and pool's current size.
`resizeFactor` can be configured to adjust expansion and shrink size.

Expansion is triggered when pool runs out of available elements. Expansion size is calculated using following algorithm:

(pseudo-code)
```
expansionSize = initial pool size * resizeFactor
if (expansionSize == 0) {
    throw exception, resize policy misconfigured
}
increase pool size by expansionSize
```

Shrinking is triggered every 10 seconds (not configurable ATM). Shrink size is calculated using following algorithm:

(pseudo-code)
```
shrinkSize = resizeFactor * number of elements managed by the pool (available + used)

if (shrinkSize > number of availlable elements) {
    return and don't resize
}

if (shrinkSize < initial pool size) {
    shrinkSize = number of available elements - initial pool size // initial pool size is the minimum number of managed elements
}

decrease pool size by shrinkSize
```

`resizeFactor` is set to 0.5 by default.

Example:
```xml
<PooledItemSourceFactory itemSizeInBytes="512" initialPoolSize="10000">
    <UnlimitedResizePolicy resizeFactor="0.2" />
</PooledItemSourceFactory>
```

Example above will create 10000 pooled elements at startup. Then, if pool runs out of elements later and attempt to get element is made, 2000 pooled elements will be created. It will be shrinked to 10000 eventually if number of available elements will stay above 20% of total number of managed elements, in this example (10k + 2k) * 0.2 = 2.4k after 1 expansion.

##### Considerations
`UnlimitedResizePolicy` doesn't have any memory constraints and can lead to OOM and log loss if cluster can't index logs on time. Heavy load testing is encouraged before release.

## Dependencies
Be aware that Jackson FasterXML jars have to be provided by user for this library to work in default mode.
See `pom.xml` or deps summary at [Maven Repository](https://mvnrepository.com/artifact/org.appenders.log4j/log4j2-elasticsearch-core/latest) for a list of dependencies.
