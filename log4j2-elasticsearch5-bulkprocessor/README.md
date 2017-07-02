# Overview
This log4j2 appender plugin uses Elasticsearch `org.elasticsearch.action.bulk.BulkProcessor` to push logs to Elasticsearch 5.x cluster. By default, FasterXML is used generate output via `org.apache.logging.log4j.core.layout.JsonLayout`.

### Example

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <AsyncBatchDelivery indexName="log4j2">
            <ElasticsearchBulkProcessor serverUris="tcp://localhost:9300" />
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```
##### It's highly encouraged to put this plugin behind `Async` appender or `AsyncLogger`. See [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch2-bulkprocessor/src/test/resources/log4j2.xml) example.

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

### Provided
Be aware that Jackson FasterXML jars that has to be provided by user for this library to work in default mode.

### Compile
Be aware that this project has following transitive dependencies:
* Netty 4.1.11
* Lucene 6.5.1
* Elasticsearch 5.4.2
