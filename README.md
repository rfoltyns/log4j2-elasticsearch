# log4j2-elasticsearch overview

[![Build Status](https://travis-ci.com/dwyl/learn-travis.svg?branch=master)](https://travis-ci.com/github/rfoltyns/log4j2-elasticsearch)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.appenders.log4j/parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.appenders.log4j/parent)
[![codecov](https://codecov.io/gh/rfoltyns/log4j2-elasticsearch/branch/master/graph/badge.svg)](https://codecov.io/gh/rfoltyns/log4j2-elasticsearch)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

This is a parent project for log4j2 appender plugins capable of pushing logs in batches to Elasticsearch clusters.

Latest released code (1.5.x) is available [here](https://github.com/rfoltyns/log4j2-elasticsearch/tree/1.5).

Project consists of:
* `log4j2-elasticsearch-core` - skeleton provider for conrete implementations
* `log4j2-elasticsearch-hc` - optimized Apache Async HTTP client compatible with Elasticsearch 2.x, 5.x, 6.x, 7.x and 8.x clusters
* `log4j2-elasticsearch-jest` - [Jest HTTP Client](https://github.com/searchbox-io/Jest) compatible with Elasticsearch 2.x, 5.x, 6.x and 7.x clusters
* `log4j2-elasticsearch2-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/2.4/java-docs-bulk-processor.html) compatible with 2.x clusters
* `log4j2-elasticsearch5-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/5.6/java-docs-bulk-processor.html) compatible with 5.x and 6.x clusters
* `log4j2-elasticsearch6-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/6.2/java-docs-bulk-processor.html) compatible with 6.x clusters

## Features

* Asynchronous log delivery
* Batch size and flush interval configuration
* Failover (redirect failed batch to alternative target)
* JSON message format ([user-provided](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/CustomMessageFactoryTest.java) or [JacksonJsonLayout](log4j2-elasticsearch-core#jacksonjsonlayout) by default since 1.3 or Log4j2 JsonLayout)
* (Since 1.1) Index rollover (hourly, daily, etc.)
* Index template configuration
* (1.2) Basic Authentication (XPack Security and Shield support)
* HTTPS support (XPack Security and Shield - visit submodules for compatibility matrix)
* (1.3) [Pooled buffers](log4j2-elasticsearch-core#object-pooling) (lower memory footprint)
* Configurable JSON output using [JacksonJsonLayout](log4j2-elasticsearch-core#jacksonjsonlayout)
* (1.4) Failover with persistence and retry
* Log overflow prevention with backoff policies
* [`log4j2-elasticsearch-hc`](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc) module - optimized HTTP client
* Custom JSON output properties support using [VirtualProperty](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-core#virtual-properties) and (since 1.4.3) [filters](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-core#virtual-property-filters)
* Pluggable [internal logging](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-core/src/main/java/org/appenders/core/logging/InternalLogging.java) (since 1.4.3)
* (1.5) [ILM policy](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-core#index-lifecycle-management) configuration
* Configurable [Jackson modules](log4j2-elasticsearch-core#jackson-modules) support
* [Component templates](log4j2-elasticsearch-core#component-templates) configuration
* [Composable index templates](log4j2-elasticsearch-core#composable-index-template) configuration
* [Service Discovery](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc#service-discovery) for HC module
* (1.6) Elasticsearch 8.x support (`null` mapping type)
* [Data Streams](https://www.elastic.co/guide/en/elasticsearch/reference/current/data-streams.html) support
* [Metrics](https://github.com/rfoltyns/log4j2-elasticsearch/tree/master/log4j2-elasticsearch-core#metrics)

### Roadmap [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

* More Elasticsearch API integrations

Feature Requests welcome!

## Usage

1. Add this snippet to your `pom.xml` file:
    ```xml
    <dependency>
        <groupId>org.appenders.log4j</groupId>
        <artifactId>log4j2-elasticsearch-jest</artifactId>
        <version>1.5.5</version>
    </dependency>
    ```

    Ensure that Log4j2 and Jackson FasterXML jars are added as well - see `Dependencies` section below

2. Use simple `log4j2.xml` configuration:
    ```xml
    <Appenders>
        <Elasticsearch name="elasticsearchAsyncBatch">
            <IndexName indexName="log4j2" />
            <JacksonJsonLayout />
            <AsyncBatchDelivery>
                <IndexTemplate name="log4j2" path="classpath:indexTemplate.json" />
                <JestHttp serverUris="http://localhost:9200" />
            </AsyncBatchDelivery>
        </Elasticsearch>
    </Appenders>
    ```

    or use new, [optimized Apache HC based HTTP client](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc)

    or [log4j2.properties](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/resources/log4j2.properties)

    or [configure programmatically](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-hc/src/test/java/org/appenders/log4j2/elasticsearch/hc/smoke/SmokeTest.java)

    NOTE: `indexTemplate.json` file is not a part of main jars. You have to create it on your own (because only YOU know which mapping you'd like to use). You can find a few basic ones in tests jars and [log4j2-elasticsearch-examples](https://github.com/rfoltyns/log4j2-elasticsearch-examples).

3. Start logging directly to Elasticsearch!
    ```java
    Logger log = LogManager.getLogger("Logger that references elasticsearchAsyncBatch")
    log.info("Hello, World!");
    ```

    Logs not arriving? Visit [examples](https://github.com/rfoltyns/log4j2-elasticsearch-examples) and verify your config.

## Dependencies

Be aware that Jackson FasterXML, Log4j2, Apache HC, Netty, Chronicle or JCTools jars may need to be provided for this library to work. By design, you can choose which jars you'd like to have on your classpath.
Please visit [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for an overview of provided and compile dependencies

In order to fix [#56](https://github.com/rfoltyns/log4j2-elasticsearch/issues/56), two new modules were extracted from `log4j2-elasticsearch-core`:
* (1.5+) [appenders-logging](https://github.com/appenders/appenders-logging) (`compile`) available [here](https://mvnrepository.com/artifact/org.appenders.logging/appenders-logging)
* (1.5+) [appenders-jackson-st](https://github.com/appenders/appenders-jackson-st) (`compile`) available [here](https://mvnrepository.com/artifact/org.appenders.st/appenders-jackson-st)

This will not cause any issues if you're using packaging tools with transitive dependencies support (Maven, Gradle, etc.). However, in some cases e.g. if you're managing your jars explicitly, classloaders will complain. Sorry for the inconvenience.

## Released to [Sonatype OSS repos](https://oss.sonatype.org/content/repositories/releases/org/appenders/log4j/)
Visit submodules' documentation or [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for XML snippets.
