package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ItemSource;

public interface ItemSourceWriter<T> {

    ItemSource<T> write(ItemSource<T> itemSource, byte[] bytes);

}
