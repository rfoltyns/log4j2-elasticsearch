# log4j2-elasticsearch-core
Core provides a skeleton for `ClientObjectFactory` implementations: a set of interfaces and base classes to push logs in batches to Elasticsearch cluster.

### Maven

To use it, add this XML snippet to your `pom.xml` file:
```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-core</artifactId>
    <version>1.5.4</version>
</dependency>
```

However, direct use of this library is required only in case of Log4j2 configuration with Java API or user-provided extensions. This library is in `compile` scope for `jest` and `bulkprocessor` implementations.

## Extensibility

Main parts of the skeleton are:
* `ClientObjectFactory` - provider of client-specific request classes, factories and error handlers
* `BatchEmitter` - intermediate log collector which will trigger batch delivery as configured (see below).
* `AsyncBatchDelivery` - glues `ClientObjectFactory` and `BatchEmitter` together

### ItemSource API
Since 1.3, `org.appenders.log4j2.elasticsearch.ItemSource` and a set of related interfaces are available.

The main goal was to introduce an envelope with API to process and manage underlying payloads. A good example is `org.appenders.log4j2.elasticsearch.ByteBufItemSource` - poolable payload container backed by Netty buffer.

Main parts of default implementation are:
* `ItemSource` - envelope for payloads; [StringItemSource](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/StringItemSource.java) is used by default
* `ItemSourceFactory` - `ItemSource` producer capable of serializing given objects into one of underlying `ItemSource` implementations; [StringItemSourceFactory](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/StringItemSourceFactory.java) is used by default
* `ItemSourceLayout` - new layout meant to bind Log4j2 API to `ItemSourceFactory` calls; [JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java) by default

### AsyncBatchDelivery

`AsyncBatchDelivery` uses `ClientObjectFactory` objects to produce client specific requests and deliver them to cluster via `BatchEmitter` implementations.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
clientObjectFactory | Element | yes | n/a | Provider of all client-specific objects: batch handlers, failover handlers, clients, setup operations, etc.
batchSize | Attribute | no | 1000 | Maximum (rough) number of logs in one batch.
deliveryInterval | Attribute | no | 1000 | Millis between deliveries, even if triggered by `batchSize` in the meantime.
failoverPolicy | Element | no | NoopFailoverPolicy | Sink for failed batch items. By default, `NoopFailoverPolicy` drops failed batch items on the floor.
shutdownDelayMillis| Attribute | no | 5000 | Millis before batch delivery is actually shutdown after Lifecycle.stop() call. This allow last batch items to be flushed and delivered to cluster or to configured `failoverPolicy`.
setupOperation | Element[] | no | [] | List of operations to execute on before first batch. Exact moment of execution depends on client implementation.

Delivery is triggered after `deliveryInterval` or when number of undelivered logs reached `batchSize`.

`deliveryInterval` should be main driver of delivery. However, in high load scenarios, both parameters should be configured accordingly to prevent sub-optimal behaviour. See [Indexing performance tips](https://www.elastic.co/guide/en/elasticsearch/guide/current/indexing-performance.html) and [Performance Considerations](https://www.elastic.co/blog/performance-considerations-elasticsearch-indexing) for more info.

```xml
<Appenders>
    <Elasticsearch name="...">
        <AsyncBatchDelivery batchSize="5000"
                            deliveryInterval="3000"
                            shutdownDelatMillis="10000">
            ... clientObjectFactory, failoverPolicy and setupOperations
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

### BatchEmitter

`BatchEmitterFactory<T extends BatchEmitter>` implementations are located using `java.util.ServiceLoader`. `org.appenders.log4j2.elasticsearch.BulkEmitter` is the current default implementation.

Since 1.5.2, each factory has it's `loadingOrder` to make extensions easier. Default loading order of each of your extensions is `100`. Loading order of all provided implementations is and always will be higher than `100` and can be overridden with `appenders.<FACTORY_CLASS_SIMPLE_NAME>.loadingOrder` property, e.g.
```
-Dappenders.BulkEmitterFactory.loadingOrder=90
```

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
        <!-- zone is optional. OS timezone is used by default. separator is optional, - (hyphen, dash) is used by default. -->
        <RollingIndexName indexName="log4j2" pattern="yyyy-MM-dd" timeZone="Europe/Warsaw" separator="." />
        ...
    </Elasticsearch>
</Appenders>
```

`pattern` accepts any valid date pattern with years down to millis (although rolling daily or weekly should be sufficient for most use cases)
`IndexName` and `RollingIndexName` are mutually exclusive. Only one per appender should be defined, otherwise they'll override each other.

### Index template
Since 1.1, [Index templates](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/indices-templates.html) can be configured with `IndexTemplate``. Template can be loaded from specified file or defined directly in the XML config:

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

Since 1.4.2, template can include variables resolvable with [Log4j2 Lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html) or progammatically provided [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ValueResolver.java). See examples: [index template](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/indexTemplate-7.json), [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/java/org/appenders/log4j2/elasticsearch/hc/smoke/SmokeTest.java)

NOTE: Be aware that template parsing errors on cluster side MAY NOT prevent plugin from loading - error is logged on client side and startup continues.

### Composable index template

Since 1.5, [Composable index templates](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html) can be configured with `IndexTemplate`. Set `IndexTemplate.apiVersion` to 8 (or higher).

By default, `apiversion=7` is used (creates non-composable index templates). Default will be changed to 8 after Elasticsearch 8.x release:

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <AsyncBatchDelivery>
            <IndexTemplate apiVersion="8" name="template1" path="<absolute_path_or_classpath>" />
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
            <IndexTemplate apiVersion="8" name="template1" >
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

### Component templates

Since 1.5, [Component templates](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html) can be created with `ComponentTemplate`:
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <AsyncBatchDelivery>
            <ComponentTemplate name="component-template1" path="<absolute_path_or_classpath>" />
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
            <ComponentTemplate name="component-template1" >
            {
                // your component template in JSON format
            }
            </ComponentTemplate>
            ...
        </AsyncBatchDelivery>
        ...
    </Elasticsearch>
</Appenders>
```

Templates can include variables resolvable with [Log4j2 Lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html) or progammatically provided [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ValueResolver.java). See examples: [ILM settings component template](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/componentTemplate-7-settings-ilm.json), [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/java/org/appenders/log4j2/elasticsearch/hc/smoke/SmokeTest.java)

NOTE: Be aware that template parsing errors on cluster side MAY NOT prevent plugin from loading - error is logged on client side and startup continues.

### Index lifecycle management
Since 1.5, [ILM Policy](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/index-lifecycle-management.html) can be created during appender startup. Policy can be loaded from specified file or defined directly in the XML config:

```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        ...
        <AsyncBatchDelivery>
            <ILMPolicy name="policy1" path="<absolute_path_or_classpath>" />
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
            <ILMPolicy name="policy1" rolloverAlias="alias1" >
            {
                // your ILM policy in JSON format
            }
            </ILMPolicy>
            ...
        </AsyncBatchDelivery>
        ...
    </Elasticsearch>
</Appenders>
```

Policy document can include variables resolvable with [Log4j2 Lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html) or progammatically provided [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ValueResolver.java). See examples: [ILM policy](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/ilmPolicy-7.json), [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/java/org/appenders/log4j2/elasticsearch/hc/smoke/SmokeTest.java)

NOTE: This feature is supported by [log4j2-elasticsearch-jest](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest) and [log4j2-elasticsearch-hc](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc) modules. ILM was introduced to Elasticsearch in version 7, so `log4j2-elasticsearch(x)-bulkprocessor` modules can't use this API.

NOTE: Be aware that policy parsing errors on cluster side MAY NOT prevent plugin from loading - error is logged on client side and startup continues.

### Message output

There are numerous ways to generate JSON output:

#### JacksonJsonLayout

Since 1.3, [org.appenders.log4j2.elasticsearch.JacksonJsonLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonJsonLayout.java) - implemetation of [ItemSourceLayout](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ItemSourceLayout.java) - can be specified to handle incoming LogEvent(s). It will serialize LogEvent(s) using Jackson mapper configured with a set of default and (optional) user-provided mixins (see: [JacksonMixInAnnotations docs](https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations)) and (since 1.4) [Virtual Properties](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/VirtualProperty.java).

Default set of mixins limits LogEvent output by shrinking serialized properties list to a 'reasonable minimum'.
Additional properties can be specified with [VirtualProperty](#virtual-properties) elements.
Customizations of all aspects of LogEvent and Message output are allowed using `JacksonMixIn` elements (see: [JacksonMixInAnnotations docs](https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations)) elements.

Furthermore, [ItemSource API](#itemsource-api) allows to use pooled [ByteByfItemSource](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ByteBufItemSource.java) payloads. Pooling is optional.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
afterburner | Attribute | no | false | if `true`, `com.fasterxml.jackson.module:jackson-module-afterburner` will be used to optimize (de)serialization. Since this dependency is in `provided` scope by default, it MUST be declared explicitly.
singleThread | Attribute | no | false | Use ONLY with `AsyncLogger`. If `true`, `com.fasterxml.jackson.core.JsonFactory` will be replaced with [SingleThreadJsonFactory](https://github.com/appenders/appenders-jackson-st/blob/main/src/main/java/org/appenders/st/jackson/SingleThreadJsonFactory.java) for `LogEvent` serialization. Offers slightly better serialization throughput.
mixins | Element(s) | no | None | Array of `JacksonMixIn` elements. Can be used to override default serialization of LogEvent, Message and related objects
virtualProperties (since 1.4) | Element(s) | no | None | Array of `VirtualProperty` elements. Similar to `KeyValuePair`, can be used to define properties resolvable on the fly, not available in LogEvent(s).
virtualPropertiesFilter (since 1.4.3) | Element(s) | no | None | Array of `VirtualPropertyFilter` elements, can be used to include/exclude `VirtualProperty` dynamically.
jacksonModules (since 1.5) | Element(s) | no | [ExtendedLog4j2JsonModule](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/apache/logging/log4j/core/jackson/ExtendedLog4j2JsonModule.java) | Array of `JacksonModule` elements. Can be used to configure any aspect of (de)serialization.
itemSourceFactory | Element | yes (since 1.4) | n/a | `ItemSourceFactory` used to create wrappers for serialized items. `StringItemSourceFactory` and `PooledItemSourceFactory` are available

Default output:

`{"timeMillis":1545968929481,"loggerName":"elasticsearch","level":"INFO","message":"Hello, World!","thread":"Thread-18"}`

Example with pooled buffers (pools must be configured for both `ClientObjectFactory` and layout, see [object pooling](#object-pooling)):
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout>
        <PooledItemSourceFactory poolName="itemPool" itemSizeInBytes="1024" initialPoolSize="10000" />
        <JacksonMixIn mixInClass="foo.bar.CustomLogEventMixIn"
                      targetClass="org.apache.logging.log4j.core.LogEvent"/>
        <VirtualProperty name="hostname" value="$${env:hostname:-undefined}"/>
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```


Example with no pooled buffers:
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout afterburner="true">
        <JacksonMixIn mixInClass="foo.bar.CustomLogEventMixIn"
                      targetClass="org.apache.logging.log4j.core.LogEvent"/>
        <VirtualProperty name="hostname" value="$${env:hostname:-undefined}"/>
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```

Custom `org.appenders.log4j2.elasticsearch.ItemSourceLayout` can be provided to appender config to use any other serialization mechanism.

##### Virtual Properties

Since 1.4, `VirtualProperty` elements (`KeyValuePair` on steroids) can be appended to serialized objects.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
name | Attribute | yes | n/a |
value | Attribute | yes | n/a | Static value or contextual variable resolvable with <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html">Log4j2 Lookups</a>.
dynamic | Attribute | no | false | if `true`, indicates that value may change over time and should be resolved on every serialization (see [Log4j2Lookup](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/Log4j2Lookup.java)). Otherwise, will be resolved only on startup.
writeRaw (since 1.6) | Attribute | no | false | indicates that the value is a valid, structured object (e.g JSON string) and should be written as such.

Since 1.6, one can put a valid, structured object (e.g. a JSON string) into a VirtualProperty's value, set `writeRaw` to `true` and it will be written without quotes when serialized.

###### Example:

```xml
<NonEmptyFilter/>
<VirtualProperty name="jsonStringField" 
	value="$${ctx:myJsonObject:-}" 
	dynamic="true" writeRaw="true"/>
```

Custom lookup can implemented with [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ValueResolver.java).


##### Virtual Property Filters

Since 1.4.3, implementations of [`VirtualPropertyFilter`](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/VirtualPropertyFilter.java) can be configured to include or exclude `VirtualProperty` by name and/or value resolved by [Log4j2Lookup](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/Log4j2Lookup.java) (or custom [ValueResolver](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/ValueResolver.java)).

Available filters:

* `NonEmptyFilter` - excludes `VirtualProperty` is resolved value is `null` or empty (doesn't exclude blank)

Custom filtering can be implemented with [`VirtualPropertyFilter`](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/VirtualPropertyFilter.java).

Example:

```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout afterburner="true">
        <!-- will be included because it's resolved to "undefined" -->
        <VirtualProperty name="hostname" value="$${env:hostname:-undefined}" />
        <!-- will be included if envVariable is not available on startup because it's resolved to "${env:envVariable}" -->
        <VirtualProperty name="field1" value="${env:envVariable}" />
        <!-- will NOT be included if envVariable is not available on startup because it's resolved to "" -->
        <VirtualProperty name="field2" value="${env:envVariable:-}" />
        <!-- order doesn't matter -->
        <NonEmptyFilter/>
        <!-- will NOT be included if envVariable is not available on startup because it's resolved to "" -->
        <VirtualProperty name="field3" value="$${env:envVariable:-}" />
        <!-- will NOT be included if ctxVariable is not available in runtime -->
        <VirtualProperty name="field4" value="$${ctx:ctxVariable:-}" dynamic="true" />
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```

##### Jackson Modules

Since 1.5, implementations of [`JacksonModule`](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/JacksonModule.java) can be used to configure any aspect of (de)serialization. It was added to allow to configure any arbitrary Jackson FasterXML Module (see [example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/test/java/org/appenders/log4j2/elasticsearch/ExampleJacksonModule.java)), but in fact, it can be used to apply any configuration that's e.g. not supported by other features (mixins, virtual properties, filters, etc) or not supported yet.

Available modules:

* `JacksonAfterburnerModule` - configures `com.fasterxml.jackson.module:jackson-module-afterburner`

Example:
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout>
        <JacksonAfterburnerModule/>
        <ExampleJacksonModule/> <!-- not released - test sources only -->
        <!--<CustomJacksonModule anyProperty="anyValue"/>-->
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```

#### Log4j2 JsonLayout
`JsonLayout` will serialize LogEvent using Jackson mapper configured in log4j-core. Custom `org.apache.logging.log4j.core.Layout` can be provided to appender config to use any other serialization mechanism.

Output may vary across different Log4j2 versions (see: [#9](https://github.com/rfoltyns/log4j2-elasticsearch/issues/15))

Example:
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JsonLayout compact="true"/>
    ...
</Elasticsearch>
```

Also, since `LogEvent.timeMillis` is not included in this layout, [IndexTemplate](#index-template) must include mappings for `instant.epochSeconds`:
```json
{
  "mappings": {
    "properties": {
      "instant.epochSecond": {
        "type": "date",
        "format": "epoch_second"
      }
    }
  }
}
```

#### Elastic Common Schema

ECS output can be configured with `JacksonMixIn`:
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <JacksonJsonLayout>
        <JacksonMixIn mixInClass="org.appenders.log4j2.elasticsearch.ecs.LogEventJacksonEcsJsonMixIn"
                      targetClass="org.apache.logging.log4j.core.LogEvent"/>
        <VirtualProperty name="event.dataset" value="logs-java"/>
    </JacksonJsonLayout>
    ...
</Elasticsearch>
```

At the moment, it provides only a minimal set of fields.

##### ECSLayout

Once [log4j2-ecs-layout](https://mvnrepository.com/artifact/co.elastic.logging/log4j2-ecs-layout) is provided, `ECSLayout` can be configured (`JestHttp` only):
```xml
<Elasticsearch name="elasticsearchAsyncBatch">
    ...
    <ECSLayout/>
    ...
</Elasticsearch>
```

#### Raw log message
`messageOnly="true"` can be configured for all layouts mentioned above to make use of user provided (or default) `org.apache.logging.log4j.message.Message.getFormattedMessage()` implementation.

Raw log message MUST:
 * be logged with Logger that uses `org.apache.logging.log4j.message.MessageFactory` that serializes logged object to a valid JSON output

 or

 * be in JSON format already (default)

See [custom MessageFactory example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/CustomMessageFactoryTest.java)

### Failover
Each unsuccessful batch can be redirected to any given `FailoverPolicy` implementation. By default, each log entry will be separately delivered to configured strategy class, but this behaviour can be amended by providing custom `ClientObjectFactory` implementation.

#### AppenderRefFailoverPolicy

Redirects failed batches to configured `org.apache.logging.log4j.core.Appender`. Output depends on target appender layout.

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
appenderRef | Attribute | yes | n/a | Name of appender available in current configuration

Example:
```xml
<Appenders>
  <Console name="CONSOLE" />
  <Elasticsearch name="elasticsearchAsyncBatch">
      ...
      <AsyncBatchDelivery>
        ...
        <AppenderRefFailoverPolicy>
              <AppenderRef ref="CONSOLE" />
          </AppenderRefFailoverPolicy>
          ...
      </AsyncBatchDelivery>
      ...
  </Elasticsearch>
</Appenders>
```

#### ChronicleMapRetryFailoverPolicy

Since 1.4, failover with retry can be configured to minimize data loss.
Each item is stored separately in [ChronicleMap](https://github.com/OpenHFT/Chronicle-Map) - a file-backed key value store.

##### Overview

This failover policy consists of following key components:
* [ChronicleMapRetryFailoverPolicy](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/ChronicleMapRetryFailoverPolicy.java) - policy setup and failed item inbound handler. Stores each item under unique key provided by `KeySequence`
* [RetryProcessor](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/RetryProcessor.java) - retries a batch of failed items (failed item outbound handler) and persits current `KeySequence` state. See scheduling options below
* [KeySequence](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/KeySequence.java) - keeps track of current reader and writer keys. Writer index increases on failed item write and readex index "chases" the writer index during retries until they're equal
* [KeySequenceSelector](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/KeySequenceSelector.java) - `KeySequence` resolver. Keeps track of the `KeySequence` to use. Recovers old one after restart or creates a new one if none exists
* [KeySequenceConfig](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/KeySequenceConfig.java) - persistable view of `KeySequence`
* [KeySequenceConfigRepository](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/failover/KeySequenceConfigRepository.java) - `KeySequenceConfig` CRUD operations

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
fileName | Attribute | Yes | None | Path to [ChronicleMap](https://github.com/OpenHFT/Chronicle-Map) file. Will get created if doesn't exist. Will TRY to recover previous state if exist.
numberOfEntries | Attribute | Yes | None | Storage capacity. Actual number of stored items MAY exceed this number but it's NOT recommended. Store operations MAY fail below this limit if `averageValueSize` was exceeded.
keySequenceSelector | Element | Yes | None | `KeySequence` provider. See documentation below for available options
averageValueSize | Attribute | No | 1024 | Average size of failed item including additional metadata. By default, suitable for small logs (up to 100-200 characters)
batchSize | Attribute | No | 1000 | Maximum size of failed items list retried by `RetryProcessor` after each `retryDelay`
retryDelay | Attribute | No | 10000 | Delay between the end of previous `RetryProcessor` run and start of next one. This is NOT an interval between two consecutive `RetryProcessor` runs (reasons behind `scheduleAtFixedDelay`: retry runs should not overlap; retry should be a fairly transparent background operation; retry should not generate too much additional load on top of the current load if target cluster is down or slow anyway; retrying itself should be a temporary state, it's the storage capacity that should allow it to recover so there's no need to rush it)
monitored | Attribute | No | false | If `true`, retry metrics will be printed. Metrics are prined by Status Logger at `INFO` level, so be sure to modify your Log4j2 configuration accordingly. <br><br>Example output: `sequenceId: 1, total: 452920, enqueued: 452918` where: <br> `total` is a number of failed items + number of key sequences + key sequence list (internal index of all available key sequences) <br> `enqueued` is a number of entries currently available for retry within `KeySequence` with `sequenceId`=1
monitorTaskInterval | Attribute | No | 30000 | Interval between metrics logs. 30 seconds by default.

##### Considerations

Even though the majority of tests have proven that this policy works as described, there are several limitations that MUST be taken into account before going forward with this approach:

* :warning: it is NOT a fully bullet-proof solution(!) there are still multiple scenarios when logs WILL be lost, so test your application extensively before using it in production!
* :warning: successful setup of this policy depends on `sequenceId` uniqueness. If multiple processes use the same storage file, each one of them MUST specify a unique `sequenceId` (see docs below), otherwise they may overwrite each others' data and lead to data loss
* :warning: as storage setup is synchronous, application startup time will increase and the difference will depend directly on `numberOfEntries` - more entries to store, bigger storage required, more bytes to allocate or recover on startup
* :warning: application MUST shutdown gracefully - if there's a retry or failover in progress and process gets killed (e.g with OOM killer), storage file may be left in a faulty and unrecoverable state and may have to be deleted
* :warning: reliability of this policy depends on underlying hardware performance. SSD > HDD. Heavy load testing and failure injection is encouraged before release
* :warning: reliability of this policy may depend on system load
* :warning: given that performance of this policy may vary and may depend on disk I/O, it was mainly tested and proven to work at ~5000 small logs per second and retrying 5000 items every ~5 seconds at commodity hardware. Effort is being made to make these numbers better
* :warning: long lasting cluster outages or slow ingestion will lead to data loss (as it does with default noop policy anyway)

Example:
```xml
<Elasticsearch>
    ...
    <AsyncBatchDelivery>
        ...
        <ChronicleMapRetryFailoverPolicy fileName="failedItems.chronicleMap"
                                      numberOfEntries="1000000"
                                      monitored="true">
            <SingleKeySequenceSelector sequenceId="1"/>
        </ChronicleMapRetryFailoverPolicy>
        ...
    </AsyncBatchDelivery>
    ...
</Elasticsearch>
```

##### SingleKeySequenceSelector

Since 1.4, single `KeySequence` per process can be defined (all failed logs will be stored and retried using the same `KeySequence`)
When configured correctly, it ensures that processes using the same storage file are operating on different datasets.

:warning: Sharing file over multiple processes is experimental.

:warning: If more than 1 JVM is using the same `sequenceId`, one of them MAY fail to start.

Example:
```xml
<Elasticsearch>
    ...
    <AsyncBatchDelivery>
        ...
        <ChronicleMapRetryFailoverPolicy>
            <SingleKeySequenceSelector sequenceId="1"/>
        </ChronicleMapRetryFailoverPolicy>
        ...
    </AsyncBatchDelivery>
    ...
</Elasticsearch>
```

### Backoff
Since 1.4, `BackoffPolicy` can provide additional fail-safe during delivery. See [backoff policies](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/backoff/) and client-specific implementations.

### Object pooling
Since 1.3, `PooledItemSourceFactory` can be configured, providing `io.netty.buffer.ByteBuf`-backed `ByteBufItemSource` instances for serialized batch items and batch requests.

Internally, [org.appenders.log4j2.elasticsearch.GenericItemSourcePool](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/log4j2/elasticsearch/GenericItemSourcePool.java) is used as default pool implementation.
Pool is resizable. It adjusts it's size automatically depending on current load and configured `ResizePolicy`.

Item and batch pools have to be configured separately. Currently, if item buffers are pooled, batch buffers MUST be pooled as well (see example below).

Config property | Type | Required | Default | Description
------------ | ------------- | ------------- | ------------- | -------------
initialPoolSize | Attribute | Yes | None | Number of pooled elements created at startup
itemSizeInBytes | Attribute | Yes | None | Initial size of single buffer instance
maxItemSizeInBytes | Attribute | No | Integer.MAX_VALUE | Since 1.4.5. Maximum size of single buffer instance when added to the pool. Pooled item size can still exceed this value in runtime if needed, but eventually it will be reduced on `ByteBufItemSource.release()`
resizePolicy | Element | No | `UnlimitedReizePolicy` | `ResizePolicy` used whem pool resizing is triggered
resizeTimeout | Attribute | No | 1000 | When multiple threads try to get a pooled element and pool is empty, only the first thread will trigger resizing. This attribute configures maximum interval in milliseconds between two consecutive attempts to get a pooled element by other threads.
monitored | Attribute | No | false | If `true`, pool metrics will be printed. Metrics are prined by Status Logger at `INFO` level, so be sure to modify your Log4j2 configuration accordingly
monitorTaskInterval | Attribute | No | 30000 | Interval between metrics logs. 30 seconds by default.
poolName | Attribute | No | UUID | Pool ID (useful when `monitored` is set to true)

Example:
``` xml
<Elasticsearch name="elasticsearchAsyncBatch">
    <JacksonJsonLayout>
        <PooledItemSourceFactory itemSizeInBytes="1024" maxItemSizeInBytes="16384" initialPoolSize="20000" />
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

if (shrinkSize > number of available elements) {
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
<PooledItemSourceFactory itemSizeInBytes="1024" initialPoolSize="10000">
    <UnlimitedResizePolicy resizeFactor="0.2" />
</PooledItemSourceFactory>
```

Example above will create 10000 pooled elements at startup. Then, if pool runs out of elements later and attempt to get element is made, 2000 pooled elements will be created. It will be shrinked to 10000 eventually if number of available elements will stay above 20% of total number of managed elements, in this example (10k + 2k) * 0.2 = 2.4k after 1 expansion.

##### Considerations
`UnlimitedResizePolicy` doesn't have any memory constraints and can lead to OOM and log loss if cluster can't index logs on time. Heavy load testing is encouraged before release.

##### LimitedResizePolicy
This resize strategy will resize given pool until pool's current size is equal to `maxSize`.
`resizeFactor` can be configured to adjust expansion and shrink size.

Expansion is triggered when pool runs out of available elements. Expansion size is calculated using following algorithm:

(pseudo-code)
```
expansionSize = initial pool size * resizeFactor
if (expansionSize == 0) {
    throw exception, resize policy misconfigured
}
if (expansionSize + currentSize > maxSize) {
    expansionSize = maxSize - currentSize
}
increase pool size by expansionSize
```

Shrinking is triggered every 10 seconds (not configurable ATM). Shrink size is calculated using following algorithm:

(pseudo-code)
```
shrinkSize = resizeFactor * number of elements managed by the pool (available + used)

if (shrinkSize > number of available elements) {
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
<PooledItemSourceFactory itemSizeInBytes="1024" initialPoolSize="10000">
    <LimitedResizePolicy resizeFactor="0.2" maxSize="20000"/>
</PooledItemSourceFactory>
```

Example above will create 10000 pooled elements at startup. Then, if pool runs out of elements later and attempt to get element is made, 2000 pooled elements will be created. It will be shrinked to 10000 eventually if number of available elements will stay above 20% of total number of managed elements, in this example (10k + 2k) * 0.2 = 2.4k after 1 expansion.

##### Considerations
`LimitedResizePolicy` will cause log loss if `maxSize` limit is hit (pool will NOT be resized anymore) and there are no more elements available. Exception will be thrown. Heavy load testing is encouraged before release.

## Pluggable JCTools

Since 1.5, if [org.jctools:jctools-core:3.x](https://mvnrepository.com/artifact/org.jctools/jctools-core) jar is present on the classpath, `java.util.concurrent.ConcurrentLinkedQueue` based classes will use JCTools unbounded arrays. `CLQ` creates Node internally on each offer() call. JCTools arrays, if used, reduce memory allocation.

JVM params:

Param | Type | Default
------------ | ------------- | -------------
-Dappenders.GenericItemSourcePool.jctools.enabled | boolean | true
-Dappenders.BulkEmitter.jctools.enabled | boolean |  true
-Dappenders.BulkEmitter.initialSize | int | 65536

See submodules documentation for module-specific properties.

## Dependencies
Be aware that Jackson FasterXML jars have to be provided by user for this library to work in default mode.
See `pom.xml` or deps summary at [Maven Repository](https://mvnrepository.com/artifact/org.appenders.log4j/log4j2-elasticsearch-core/latest) for a list of dependencies.
