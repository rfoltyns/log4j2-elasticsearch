# log4j2-elasticsearch overview

[![Build Status](https://travis-ci.org/dwyl/learn-travis.svg?branch=master)](https://travis-ci.org/rfoltyns/log4j2-elasticsearch)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.appenders.log4j/parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.appenders.log4j/parent)
[![codecov](https://codecov.io/gh/rfoltyns/log4j2-elasticsearch/branch/master/graph/badge.svg)](https://codecov.io/gh/rfoltyns/log4j2-elasticsearch)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/rfoltyns/log4j2-elasticsearch)

This is a parent project for log4j2 appender plugins capable of pushing logs in batches to Elasticsearch clusters.

Project consists of:
* `log4j-elasticsearch-core` - skeleton provider for conrete implementations
* `log4j-elasticsearch-jest` - [Jest HTTP Client](https://github.com/searchbox-io/Jest) compatible with Elasticsearch 2.x, 5.x and 6.x clusters
* `log4j-elasticsearch2-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/2.4/java-docs-bulk-processor.html) compatible with 2.x clusters
* `log4j-elasticsearch5-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/5.6/java-docs-bulk-processor.html) compatible with 5.x and 6.x clusters
* `log4j-elasticsearch6-bulkprocessor` - [TCP client](https://www.elastic.co/guide/en/elasticsearch/client/java-api/6.2/java-docs-bulk-processor.html) compatible with 6.x clusters

## Features

* Asynchronous log delivery
* Batch size and flush interval configuration
* Failover (redirect failed batch to alternative target)
* JSON message format ([user-provided](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/CustomMessageFactoryTest.java) or [JacksonJsonLayout](log4j2-elasticsearch-core#jacksonjsonlayout) by default since 1.3 or Log4j2 JsonLayout)
* (since 1.1) Index rollover (hourly, daily, etc.)
* (1.1) Index template configuration
* (1.2) Basic Authentication (XPack Security and Shield support)
* (1.2) HTTPS support (XPack Security and Shield - visit submodules for compatibility matrix)
* (1.3) [Buffer object pool](log4j2-elasticsearch-core#object-pooling) (memory allocation reduced by ~80%)
* (1.3) Buffered Jest HTTP client
* (1.3) Fully configurable JSON output using [JacksonJsonLayout](log4j2-elasticsearch-core#jacksonjsonlayout)
* (1.4 - Q1 2019) Reliable, file-based failover/retry

## Usage

1. Add this snippet to your `pom.xml` file:
```xml
<dependency>
    <groupId>org.appenders.log4j</groupId>
    <artifactId>log4j2-elasticsearch-jest</artifactId>
    <version>1.3.3</version>
</dependency>
```
(ensure that Log4j2 and Jackson FasterXML jars are added as well - see `Dependencies` section below)

2. Add this snippet to `log4j2.xml` configuration:
```xml
<Appenders>
    <Elasticsearch name="elasticsearchAsyncBatch">
        <IndexName indexName="log4j2" />
        <AsyncBatchDelivery>
            <IndexTemplate name="log4j2" path="classpath:indexTemplate.json" />
            <JestHttp serverUris="http://localhost:9200" />
        </AsyncBatchDelivery>
    </Elasticsearch>
</Appenders>
```

or log4j2.properties (see [example](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/resources/log4j2-buffered-example.properties))

or [configure programmatically](https://github.com/rfoltyns/log4j2-elasticsearch/blob/master/log4j2-elasticsearch-jest/src/test/java/org/appenders/log4j2/elasticsearch/jest/smoke/SmokeTest.java)

3. Start logging directly to Elasticsearch!
```java
Logger log = LogManager.getLogger("Logger that references elasticsearchAsyncBatch")
log.info("Hello, World!");
```
## Dependencies

Be aware that Jackson FasterXML jars that has to be provided by user for this library to work in default mode.
Please visit [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for an overview of provided and compile dependencies

## Released to [Sonatype OSS repos](https://oss.sonatype.org/content/repositories/releases/org/appenders/log4j/)
Visit submodules' documentation or [mvnrepository](https://mvnrepository.com/artifact/org.appenders.log4j) for XML snippets.
