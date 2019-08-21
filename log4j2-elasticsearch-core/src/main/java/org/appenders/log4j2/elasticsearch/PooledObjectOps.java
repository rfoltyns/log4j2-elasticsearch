package org.appenders.log4j2.elasticsearch;

import java.util.function.Supplier;

public interface PooledObjectOps<T> {

    ItemSource<T> createItemSource(ReleaseCallback<T> releaseCallback);

    void reset(ItemSource<T> pooled);

    boolean purge(ItemSource<T> pooled);

    Supplier<String> createMetricsSupplier();

}
