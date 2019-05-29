package org.appenders.log4j2.elasticsearch;

public interface PooledObjectOps<T> {

    ItemSource<T> createItemSource(ReleaseCallback<T> releaseCallback);

    void reset(ItemSource<T> pooled);

    boolean purge(ItemSource<T> pooled);

}
