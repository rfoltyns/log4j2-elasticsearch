# Overview
This log4j2 appender plugin uses Jest HTTP client to push logs in batches to Elasticsearch cluster. By default, FasterXML is used generate output via `org.apache.logging.log4j.core.layout.JsonLayout`.

### Example

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <JestBatchDelivery indexName="app_index">
            <JestHttp serverUris="http://localhost:9200" />
        </JestBatchDelivery>
    </Elasticsearch>
</Appenders>
```
##### It's highly encouraged to put this plugin behind `Async` appender or `AsyncLogger`. See [log4j2.xml](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/src/test/resources/log4j2.xml) example.

## Configurability

### Delivery frequency
Delivery frequency can be adjusted via `JestBatchDelivery` attributes :
* `deliveryInterval` - millis between deliveries
* `batchSize` - maximum (rough) number of undelivered logs

### Message output
There are at least three ways to generate output
* (default) JsonLayout will serialize LogEvent using Jackson mapper configured in log4j-core
* `messageOnly="true"` can be configured set to make use of user provided (or default) `org.apache.logging.log4j.message.Message.getFormattedMessage()` implementation
* custom `org.apache.logging.log4j.core.layout.AbstractStringLayout` can be provided to appender config to use any other serialization mechanism

### Failover
Each unsuccessful batch can be redirected to any given `FailoverPolicy` implementation. By default, each log entry will be separately delivered to configured strategy class, but this behaviour can be amended by providing custom `ClientObjectFactory` implementation.

### HTTP parameters
Jest uses Apache HTTP client. Basic configuration parameters were exposed via `JestHttp` appender config element.

## Dependencies

### Provided
Be aware that Jackson FasterXML jars that has to be provided by user for this library to work in default mode.

### Compile
* Apache HTTP components
* Google Guava
* Gson
