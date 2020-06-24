package org.appenders.log4j2.elasticsearch.failover;

import java.util.Map;

public interface MapProxy<K, V> extends Map<K, V> {

    void close();

}
