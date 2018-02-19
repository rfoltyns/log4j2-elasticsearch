# log4j2-elaticsearch overview

This is a parent project for log4j2 appender plugins capable of pushing logs in batches to Elasticsearch cluster.

Project consists of:
* `log4j-elasticsearch-core` module - skeleton provider for conrete implementations
* `log4j-elasticsearch-*` modules - concrete implementations using different clients (e.g.: Jest, BulkProcessor)

### Maven

##### Released to [Sonatype OSS repos](https://oss.sonatype.org/content/repositories/releases/org/appenders/log4j/)
Visit submodules' documentation or [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for XML snippets.

### Example

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <RollingIndexName indexName="log4j2" pattern="yyyy-MM-dd" />
        <AsyncBatchDelivery>
            <JestHttp serverUris="http://localhost:9200" />
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

## Configurability

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
There are at least three ways to generate output
* (default) JsonLayout will serialize LogEvent using Jackson mapper configured in log4j-core
* `messageOnly="true"` can be configured set to make use of user provided (or default) `org.apache.logging.log4j.message.Message.getFormattedMessage()` implementation
* custom `org.apache.logging.log4j.core.layout.AbstractStringLayout` can be provided to appender config to use any other serialization mechanism

### Failover
Each unsuccessful batch can be redirected to any given `FailoverPolicy` implementation. By default, each log entry will be separately delivered to configured strategy class, but this behaviour can be amended by providing custom `ClientObjectFactory` implementation.

## Dependencies

Be aware that Jackson FasterXML jars that has to be provided by user for this library to work in default mode.
Please visit [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for an overview of provided and compile dependencies