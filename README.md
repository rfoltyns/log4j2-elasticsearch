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
        <AsyncBatchDelivery indexName="log4j2">
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